const { test } = require('node:test');
const assert = require('node:assert/strict');
const { SurveyService } = require('../src/services/survey.service');
const account = { name: 'Test', email: 'test@example.com', loginId: 'test', password: 'test-password', memberType: 'WOLGYE_RESIDENT' };

test('verified signup persists server verification and profile region', async () => {
  let stored;
  const service = new SurveyService({ user: { create: async ({ data }) => (stored = data) } });
  const verifiedAt = new Date();
  await service.signup(account, { verifiedAt, code: '1135056000' });
  assert.equal(stored.region, '월계1동');
  assert.equal(stored.regionVerifiedAt, verifiedAt);
  assert.equal(stored.verifiedRegionCode, '1135056000');
});

test('skipping signup verification cannot persist client-supplied verification claims', async () => {
  let stored;
  const service = new SurveyService({ user: { create: async ({ data }) => (stored = data) } });
  await service.signup({ ...account, regionVerifiedAt: new Date(), verifiedRegionCode: '1135056000' });
  assert.equal(stored.regionVerifiedAt, null);
  assert.equal(stored.verifiedRegionCode, null);
  assert.equal(stored.region, undefined);
});

test('mypage verification persists the same region and verification fields', async () => {
  let update;
  const service = new SurveyService({ user: { update: async (args) => (update = args) } });
  await service.verifyUserRegion(3);
  assert.equal(update.where.id, 3);
  assert.equal(update.data.region, '월계1동');
  assert.equal(update.data.verifiedRegionCode, '1135056000');
  assert.ok(update.data.regionVerifiedAt instanceof Date);
});
