const { test } = require('node:test');
const assert = require('node:assert/strict');
const bcrypt = require('bcryptjs');
const { SurveyService } = require('../src/services/survey.service');

async function fixture() {
  const account = { id: 1, name: 'Test', region: '월계1동', password: await bcrypt.hash('old-password', 4) };
  let update = null;
  const service = new SurveyService({ user: {
    findUnique: async () => account,
    update: async args => { update = args; return { id: 1, name: 'Test' }; }
  } });
  return { service, account, update: () => update };
}

test('password change stores a hash, preserves profile region and excludes password from response selection', async () => {
  const f = await fixture();
  await f.service.updateUserMe(1, { currentPassword: 'old-password', newPassword: 'new-password' });
  const { data, select } = f.update();
  assert.notEqual(data.password, 'new-password');
  assert.equal(await bcrypt.compare('new-password', data.password), true);
  assert.equal(await bcrypt.compare('old-password', data.password), false);
  assert.equal(Object.hasOwn(data, 'region'), false);
  assert.equal(select.password, undefined);
});

test('incorrect current password prevents all changes', async () => {
  const f = await fixture();
  await assert.rejects(f.service.updateUserMe(1, { currentPassword: 'wrong', newPassword: 'new-password' }), e => e.status === 403);
  assert.equal(f.update(), null);
});

test('omitted new password leaves password unchanged', async () => {
  const f = await fixture();
  await f.service.updateUserMe(1, { currentPassword: 'old-password', name: 'New name' });
  assert.equal(Object.hasOwn(f.update().data, 'password'), false);
});

test('invalid or bcrypt-truncated passwords cannot be saved', async () => {
  const f = await fixture();
  for (const newPassword of ['', 'short', '        ', '가'.repeat(25), null, 123]) {
    await assert.rejects(f.service.updateUserMe(1, { currentPassword: 'old-password', newPassword }), e => e.status === 400);
  }
  assert.equal(f.update(), null);
});
