const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { PrismaClient } = require('../node_modules/.prisma/baobab-sqlite');
const { readSnapshot, importSnapshot } = require('../scripts/import-sqlite');

test('SQLite snapshot preserves IDs, password hashes, balances, foreign keys and dates without rewriting source', async t => {
  const folder = fs.mkdtempSync(path.join(os.tmpdir(), 'baobab-import-'));
  const schema = path.join(folder, 'schema.prisma');
  fs.copyFileSync(path.join(__dirname, '../prisma/schema.sqlite.prisma'), schema);
  execFileSync(process.execPath, [require.resolve('prisma/build/index.js'), 'db', 'push', '--schema', schema, '--skip-generate'], { stdio: 'pipe' });
  const source = new PrismaClient({ datasources: { db: { url: 'file:' + path.join(folder, 'dev.db').replaceAll('\\', '/') } } });
  t.after(async () => {
    await source.$disconnect();
    assert.equal(path.dirname(path.resolve(folder)), path.resolve(os.tmpdir()));
    fs.rmSync(folder, { recursive: true, force: true });
  });
  const createdAt = new Date('2026-10-01T00:00:00.000Z');
  await source.user.create({ data: { id: 42, name: 'Test', loginId: 'import-test', email: 'import@example.test', password: 'existing-hash', point: 1500, createdAt } });
  await source.survey.create({ data: { id: 57, userId: 42, title: 'Imported survey', fundedReward: 500 } });
  await source.question.create({ data: { id: 70, surveyId: 57, question: 'Optional', questionType: 'short', required: false } });
  await source.paymentOrder.create({ data: { id: 'import-order', userId: 42, provider: 'TOSS', amount: 1000, creditPoint: 1000, callbackHash: 'existing-callback-hash', status: 'COMPLETED', creditedAt: createdAt } });
  await source.pointHistory.create({ data: { id: 90, userId: 42, amount: 1000, description: 'Existing payment', paymentOrderId: 'import-order' } });
  await source.coupon.create({ data: { id: 'import-coupon', userId: 42, itemId: 'cafe-americano', shop: 'Test shop', title: 'Test coupon', cost: 500, requestKey: 'request-one' } });
  const before = await source.user.findUnique({ where: { id: 42 } });
  const snapshot = await readSnapshot(source);
  assert.deepEqual(snapshot.User[0], before);
  assert.equal(snapshot.Question[0].required, false);
  assert.equal(snapshot.Survey[0].fundedReward, 500);
  assert.equal(snapshot.PaymentOrder[0].callbackHash, 'existing-callback-hash');
  assert.equal(snapshot.PointHistory[0].paymentOrderId, 'import-order');
  assert.equal(snapshot.Coupon[0].requestKey, 'request-one');
  assert.deepEqual(await source.user.findUnique({ where: { id: 42 } }), before);
  await source.$executeRawUnsafe('DROP TABLE "COUPON"');
  assert.deepEqual((await readSnapshot(source)).Coupon, []);
});

function targetDatabase() {
  let committed = Object.fromEntries(['User', 'Survey', 'Question', 'SurveyOption', 'Response', 'Answer', 'PaymentOrder', 'PointHistory', 'Coupon'].map(name => [name, []]));
  const sequences = [];
  return {
    sequences,
    records: () => structuredClone(committed),
    async $transaction(action, options) {
      assert.equal(options.isolationLevel, 'Serializable');
      const draft = structuredClone(committed);
      const tx = { async $queryRawUnsafe(sql) { sequences.push(sql); return []; } };
      for (const name of Object.keys(draft)) {
        tx[name[0].toLowerCase() + name.slice(1)] = {
          async count() { return draft[name].length; },
          async createMany({ data }) { draft[name].push(...data); },
        };
      }
      tx.user.aggregate = async () => ({ _sum: { point: draft.User.reduce((sum, row) => sum + row.point, 0) } });
      const result = await action(tx);
      committed = draft;
      return result;
    },
  };
}

test('import accepts only an empty target, preserves source rows and updates ID sequences', async () => {
  const target = targetDatabase();
  const snapshot = target.records();
  snapshot.User = [{ id: 42, password: 'existing-hash', point: 1500, createdAt: new Date() }];
  snapshot.Survey = [{ id: 57, userId: 42, title: 'Existing' }];
  const result = await importSnapshot(target, snapshot);
  assert.equal(result.counts.User, 1);
  assert.equal(result.totalPoints, 1500);
  assert.deepEqual(target.records().User, snapshot.User);
  assert.ok(target.sequences.some(sql => sql.includes('pg_get_serial_sequence') && sql.includes('user_id')));
  const before = target.records();
  await assert.rejects(importSnapshot(target, snapshot), /must be empty/);
  assert.deepEqual(target.records(), before);
});
