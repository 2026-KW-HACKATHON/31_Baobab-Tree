require('dotenv').config();
const fs = require('node:fs');
const path = require('node:path');
const { PrismaClient, Prisma } = require('@prisma/client');
const { PrismaClient: SQLiteClient } = require('../node_modules/.prisma/baobab-sqlite');

const order = ['User', 'Survey', 'Question', 'SurveyOption', 'Response', 'Answer', 'PaymentOrder', 'PointHistory', 'Coupon'];
const optionalTables = new Set(['PaymentOrder', 'Coupon']);
const models = order.map(name => Prisma.dmmf.datamodel.models.find(model => model.name === name));
const delegate = model => model.name[0].toLowerCase() + model.name.slice(1);
const quote = value => '"' + value.replaceAll('"', '""') + '"';

function convert(value, field) {
  if (value === null) return null;
  if (field.type === 'DateTime') {
    const date = new Date(typeof value === 'bigint' ? Number(value) : value);
    if (!Number.isFinite(date.getTime())) throw new Error(`Invalid source date: ${field.name}`);
    return date;
  }
  if (field.type === 'Boolean') {
    if (![0, 1, 0n, 1n, false, true].includes(value)) throw new Error(`Invalid source boolean: ${field.name}`);
    return value === 1 || value === 1n || value === true;
  }
  if (field.type === 'Int') {
    const number = Number(value);
    if (!Number.isInteger(number) || number < -2147483648 || number > 2147483647) throw new Error(`Invalid source integer: ${field.name}`);
    return number;
  }
  return value;
}

async function readSnapshot(source) {
  return source.$transaction(async tx => {
    const tables = await tx.$queryRawUnsafe("SELECT name FROM sqlite_master WHERE type = 'table'");
    const available = new Set(tables.map(table => table.name));
    const snapshot = {};
    for (const model of models) {
      const table = model.dbName || model.name;
      if (!available.has(table)) {
        if (!optionalTables.has(model.name)) throw new Error(`Required source table missing: ${table}`);
        snapshot[model.name] = [];
        continue;
      }
      const rows = await tx.$queryRawUnsafe(`SELECT * FROM ${quote(table)}`);
      snapshot[model.name] = rows.map(row => {
        const record = {};
        for (const field of model.fields.filter(field => field.kind === 'scalar')) {
          const column = field.dbName || field.name;
          if (!Object.hasOwn(row, column)) {
            if (field.isRequired && !field.hasDefaultValue) throw new Error(`Required source column missing: ${table}.${column}`);
            continue;
          }
          record[field.name] = convert(row[column], field);
        }
        return record;
      });
    }
    return snapshot;
  }, { timeout: 120000 });
}

async function importSnapshot(target, snapshot) {
  return target.$transaction(async tx => {
    for (const model of models) {
      if (await tx[delegate(model)].count()) throw new Error('Target database must be empty. Import aborted without changing existing data.');
    }
    const counts = {};
    for (const model of models) {
      const rows = snapshot[model.name];
      // Preserve IDs, password hashes, balances, callback hashes and timestamps.
      for (let offset = 0; offset < rows.length; offset += 500) {
        await tx[delegate(model)].createMany({ data: rows.slice(offset, offset + 500) });
      }
      const count = await tx[delegate(model)].count();
      if (count !== rows.length) throw new Error(`Import count mismatch: ${model.name}`);
      counts[model.name] = count;
      const key = model.fields.find(field => field.isId && field.type === 'Int');
      if (key) {
        const table = quote(model.dbName || model.name);
        const column = quote(key.dbName || key.name);
        await tx.$queryRawUnsafe(`SELECT setval(pg_get_serial_sequence('${table}', '${key.dbName || key.name}'), COALESCE(MAX(${column}), 1), MAX(${column}) IS NOT NULL) FROM ${table}`);
      }
    }
    const sourcePoints = snapshot.User.reduce((sum, row) => sum + row.point, 0);
    const targetPoints = (await tx.user.aggregate({ _sum: { point: true } }))._sum.point || 0;
    if (sourcePoints !== targetPoints) throw new Error('Point balance sum mismatch');
    return { counts, totalPoints: targetPoints };
  }, { isolationLevel: 'Serializable', maxWait: 10000, timeout: 120000 });
}

async function main() {
  const url = process.env.DIRECT_URL?.trim();
  if (!/^postgres(?:ql)?:\/\//.test(url || '')) throw new Error('Configure DIRECT_URL for the empty target PostgreSQL database.');
  const file = path.resolve(process.env.SQLITE_IMPORT_PATH || path.join(__dirname, '../prisma/dev.db'));
  if (!fs.existsSync(file)) throw new Error('Source SQLite file does not exist.');
  const source = new SQLiteClient({ datasources: { db: { url: `file:${file.replaceAll('\\', '/')}` } } });
  const target = new PrismaClient({ datasources: { db: { url } } });
  try {
    // Stop the local server first. SQLite creates a consistent backup, including WAL data.
    const backup = path.join(path.dirname(file), `dev.before-postgres-import-${Date.now()}.db`);
    await source.$executeRawUnsafe('VACUUM INTO ?', backup);
    console.log('Source SQLite backup created.');
    const snapshot = await readSnapshot(source);
    console.log(JSON.stringify(await importSnapshot(target, snapshot)));
  } finally {
    await source.$disconnect();
    await target.$disconnect();
  }
}

if (require.main === module) main().catch(() => {
  // Do not print connection URLs, credentials or imported personal records.
  console.error('SQLite import failed. Check target connectivity, empty tables and source schema. The target transaction is rolled back; the source file is retained.');
  process.exitCode = 1;
});
module.exports = { readSnapshot, importSnapshot };
