const { PrismaClient } = require('@prisma/client');
const fs = require('node:fs');
const path = require('node:path');
const prisma = new PrismaClient();
const additions = {
  SURVEY: { description: 'TEXT', audience: 'TEXT', duration: 'TEXT', imageData: 'TEXT' },
  QUESTION: { required: 'BOOLEAN NOT NULL DEFAULT true' },
};
async function main() {
  const backup = path.join(__dirname, '../prisma', `dev.before-survey-fields-${Date.now()}.db`);
  if (!fs.existsSync(path.join(__dirname, '../prisma/dev.db'))) throw new Error('Existing prisma/dev.db is required');
  await prisma.$executeRawUnsafe('VACUUM INTO ?', backup);
  const before = {};
  for (const table of ['USER', 'SURVEY', 'QUESTION', 'RESPONSE', 'ANSWER', 'POINT_HISTORY']) {
    before[table] = (await prisma.$queryRawUnsafe(`SELECT COUNT(*) AS count FROM "${table}"`))[0].count;
  }
  await prisma.$transaction(async tx => {
    for (const [table, fields] of Object.entries(additions)) {
      const columns = await tx.$queryRawUnsafe(`PRAGMA table_info("${table}")`);
      for (const [field, definition] of Object.entries(fields)) {
        if (!columns.some(column => column.name === field)) {
          await tx.$executeRawUnsafe(`ALTER TABLE "${table}" ADD COLUMN "${field}" ${definition}`);
        }
      }
    }
  });
  for (const [table, count] of Object.entries(before)) {
    const after = (await prisma.$queryRawUnsafe(`SELECT COUNT(*) AS count FROM "${table}"`))[0].count;
    if (after !== count) throw new Error(`Unexpected record count change in ${table}`);
  }
  console.log(`Survey fields migrated; all existing records preserved. Backup: ${backup}`);
}
main().catch(error => { console.error(error.message); process.exitCode = 1; }).finally(() => prisma.$disconnect());
