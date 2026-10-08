const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const jwt = require('jsonwebtoken');
const { JWT_SECRET } = require('../src/config/auth');
const { SurveyService } = require('../src/services/survey.service');
const { createApp } = require('../src/app');

test('survey share referrals credit the sharer only after a valid response', async t => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'baobab-referral-'));
  const schemaPath = path.join(directory, 'schema.prisma');
  const { PrismaClient } = require('../node_modules/.prisma/baobab-sqlite');
  const prisma = new PrismaClient({ datasources: { db: { url: `file:${path.join(directory, 'dev.db').replaceAll('\\', '/')}` } } });
  let server;
  t.after(async () => {
    if (server) await new Promise(resolve => server.close(resolve));
    await prisma.$disconnect();
    assert.equal(path.dirname(path.resolve(directory)), path.resolve(os.tmpdir()));
    fs.rmSync(directory, { recursive: true, force: true });
  });
  fs.copyFileSync(path.join(__dirname, '../prisma/schema.sqlite.prisma'), schemaPath);
  execFileSync(process.execPath, [require.resolve('prisma/build/index.js'), 'db', 'push', '--schema', schemaPath, '--skip-generate'], { stdio: 'pipe' });
  const service = new SurveyService(prisma);
  server = createApp(service).listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  const base = `http://127.0.0.1:${server.address().port}/api`;
  async function request(method, url, body, token) {
    const response = await fetch(base + url, { method,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    });
    return { status: response.status, data: await response.json() };
  }
  async function user(name) {
    const row = await prisma.user.create({ data: { loginId: name, email: `${name}@example.test`, password: 'unused', name } });
    return { ...row, token: jwt.sign({ userId: row.id }, JWT_SECRET, { expiresIn: '1h' }) };
  }
  const owner = await user('owner'), sharer = await user('sharer'), participant = await user('participant');
  const survey = await service.createSurvey(owner.id, { title: '<script>alert(1)</script>', questions: [{ question: 'Why?', questionType: 'short' }] });
  const answers = [{ questionId: survey.questions[0].id, answer: 'Useful' }];
  const submit = (who, referralToken, body = {}) => request('POST', `/surveys/${survey.id}/responses`, { answers, referralToken, ...body }, who.token);
  const balance = async () => (await prisma.user.findUnique({ where: { id: sharer.id } })).point;
  let referral;

  await t.test('only authenticated users can issue a survey-bound signed link', async () => {
    assert.equal((await request('POST', `/surveys/${survey.id}/share`, {})).status, 401);
    const result = await request('POST', `/surveys/${survey.id}/share`, { referrerId: owner.id }, sharer.token);
    assert.equal(result.status, 200);
    assert.equal(result.data.rewardPoint, 10);
    referral = result.data.shareToken;
    assert.equal(service.referralOwner(referral, survey.id), sharer.id);
    assert.equal((await request('GET', '/users/me', undefined, referral)).status, 401);
    assert.equal((await request('POST', '/surveys/2147483647/share', {}, sharer.token)).status, 404);
  });
  await t.test('browser link preserves referral and safely displays survey title', async () => {
    const response = await fetch(base + `/surveys/${survey.id}/share?referral=${referral}`);
    assert.equal(response.status, 200);
    assert.equal(response.headers.get('referrer-policy'), 'no-referrer');
    const html = await response.text();
    assert.ok(html.includes(`baobab://surveys/${survey.id}?referral=${referral}`));
    assert.ok(html.includes('&lt;script&gt;'));
    assert.ok(!html.includes('<script>'));
  });
  await t.test('opening or invalid submission does not award points', async () => {
    assert.equal(await balance(), 0);
    assert.equal((await submit(participant, referral, { answers: [] })).status, 400);
    assert.equal(await balance(), 0);
  });
  await t.test('successful response credits 10P to signed owner and records history', async () => {
    const result = await submit(participant, referral, { referrerId: owner.id, referralReward: 9999 });
    assert.equal(result.status, 201);
    assert.equal(result.data.point, 0);
    assert.equal(await balance(), 10);
    const history = await prisma.pointHistory.findMany({ where: { userId: sharer.id } });
    assert.equal(history.length, 1);
    assert.equal(history[0].amount, 10);
    assert.ok(history[0].description.includes(`응답 #${result.data.responseId}`));
    assert.equal((await request('GET', '/users/me/points', undefined, sharer.token)).data.point, 10);
  });
  await t.test('repeated submission and self referral cannot earn extra points', async () => {
    assert.equal((await submit(participant, referral)).status, 409);
    assert.equal((await submit(sharer, referral)).status, 201);
    assert.equal(await balance(), 10);
  });
  await t.test('tampered, expired, auth and wrong-survey tokens give no referral credit', async () => {
    const wrong = await service.createSurvey(owner.id, { title: 'Other', questions: [{ question: 'Why?', questionType: 'short' }] });
    const otherToken = (await service.createShareLink(sharer.id, wrong.id)).shareToken;
    const expired = jwt.sign({ purpose: 'survey-referral', referrerId: sharer.id, surveyId: survey.id }, JWT_SECRET,
      { audience: 'baobab-survey-referral', expiresIn: -1 });
    for (const [index, bad] of [referral + 'tampered', expired, otherToken, sharer.token, undefined].entries()) {
      assert.equal((await submit(await user(`invalid${index}`), bad)).status, 201);
      assert.equal(await balance(), 10);
    }
  });
  await t.test('simultaneous duplicate submission produces a single referral reward', async () => {
    const next = await user('concurrent');
    const results = await Promise.all([submit(next, referral), submit(next, referral)]);
    assert.equal(results.filter(result => result.status === 201).length, 1);
    assert.equal(await balance(), 20);
    assert.equal(await prisma.pointHistory.count({ where: { userId: sharer.id } }), 2);
  });
  await t.test('deleted sharer does not prevent ordinary participation', async () => {
    const deleted = await user('deleted');
    const deletedReferral = (await service.createShareLink(deleted.id, survey.id)).shareToken;
    await prisma.user.delete({ where: { id: deleted.id } });
    assert.equal((await submit(await user('afterdelete'), deletedReferral)).status, 201);
  });
  await t.test('referral history failure rolls back response, count and both balances', async () => {
    const fresh = await user('rollback');
    const beforeCount = (await prisma.survey.findUnique({ where: { id: survey.id } })).currentCount;
    const failing = new SurveyService({ $transaction: (action, options) => prisma.$transaction(tx => action(new Proxy(tx, {
      get(target, key) {
        if (key === 'pointHistory') return { create: async () => { throw new Error('forced history failure'); } };
        return target[key];
      },
    })), options) });
    await assert.rejects(failing.submitResponse(fresh.id, survey.id, answers, referral), /forced history failure/);
    assert.equal(await balance(), 20);
    assert.equal(await prisma.response.count({ where: { userId: fresh.id, surveyId: survey.id } }), 0);
    assert.equal((await prisma.survey.findUnique({ where: { id: survey.id } })).currentCount, beforeCount);
  });
  await t.test('closed surveys can still be shared but cannot award referral rewards', async () => {
    await prisma.survey.update({ where: { id: survey.id }, data: { status: 'CLOSED' } });
    assert.equal((await request('POST', `/surveys/${survey.id}/share`, {}, sharer.token)).status, 200);
    assert.equal((await submit(await user('closed'), referral)).status, 400);
    assert.equal(await balance(), 20);
  });
});
