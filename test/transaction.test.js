const { test } = require('node:test');
const assert = require('node:assert/strict');
const { transaction } = require('../src/lib/transaction');

test('serializable transactions retry rolled-back write conflicts and return the committed result', async () => {
  let attempts = 0;
  const prisma = { async $transaction(action, options) {
    assert.equal(options.isolationLevel, 'Serializable');
    if (++attempts < 3) throw Object.assign(new Error('write conflict'), { code: 'P2034' });
    return action({ point: 100 });
  } };
  assert.equal(await transaction(prisma, tx => tx.point), 100);
  assert.equal(attempts, 3);
});

test('transaction retry is bounded and never retries validation or unknown commit errors', async () => {
  for (const code of ['P2034', 'P2028', 'P2002', undefined]) {
    let attempts = 0;
    const error = Object.assign(new Error('failure'), { code });
    await assert.rejects(transaction({ async $transaction() { attempts++; throw error; } }, () => {}), e => e === error);
    assert.equal(attempts, code === 'P2034' ? 3 : 1);
  }
});
