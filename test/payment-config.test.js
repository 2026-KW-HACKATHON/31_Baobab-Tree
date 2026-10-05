const { test } = require('node:test');
const assert = require('node:assert/strict');
const jwt = require('jsonwebtoken');
const { createApp } = require('../src/app');
const { JWT_SECRET } = require('../src/config/auth');

test('payment configuration failures have safe, distinguishable API errors', async t => {
  const originalKey = process.env.TOSS_CLIENT_KEY;
  const originalUrl = process.env.PAYMENT_PUBLIC_BASE_URL;
  const server = createApp({ prisma: {} }).listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  t.after(async () => {
    for (const [name, value] of [['TOSS_CLIENT_KEY', originalKey], ['PAYMENT_PUBLIC_BASE_URL', originalUrl]]) {
      if (value === undefined) delete process.env[name]; else process.env[name] = value;
    }
    await new Promise(resolve => server.close(resolve));
  });
  const token = jwt.sign({ userId: 1 }, JWT_SECRET);
  const url = 'http://127.0.0.1:' + server.address().port + '/api/payments/orders/example/toss/ready';
  async function ready() {
    const response = await fetch(url, { method: 'POST', headers: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json' }, body: '{}' });
    assert.equal(response.status, 503);
    return response.json();
  }
  delete process.env.TOSS_CLIENT_KEY;
  assert.equal((await ready()).code, 'TOSS_NOT_CONFIGURED');
  process.env.TOSS_CLIENT_KEY = 'live_ck_invalid';
  assert.equal((await ready()).code, 'TOSS_NOT_CONFIGURED');
  process.env.TOSS_CLIENT_KEY = 'test_ck_configuration_test';
  delete process.env.PAYMENT_PUBLIC_BASE_URL;
  assert.equal((await ready()).code, 'PAYMENT_URL_NOT_CONFIGURED');
});
