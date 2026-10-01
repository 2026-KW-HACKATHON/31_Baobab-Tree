const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { PrismaClient } = require('@prisma/client');
const { SurveyService } = require('../src/services/survey.service');
const { createApp } = require('../src/app');

test('schema-backed API integration', async t => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'baobab-api-'));
  const schemaPath = path.join(directory, 'schema.prisma');
  fs.copyFileSync(path.join(__dirname, '../prisma/schema.prisma'), schemaPath);
  const prisma = new PrismaClient({ datasources: { db: { url: `file:${path.join(directory, 'dev.db').replaceAll('\\', '/')}` } } });
  let server;
  t.after(async () => {
    if (server) await new Promise(resolve => server.close(resolve));
    await prisma.$disconnect();
    // Only remove this test's randomly generated temporary directory.
    assert.equal(path.dirname(path.resolve(directory)), path.resolve(os.tmpdir()));
    fs.rmSync(directory, { recursive: true, force: true });
  });
  execFileSync(process.execPath, [require.resolve('prisma/build/index.js'), 'db', 'push', '--schema', schemaPath, '--skip-generate'], { stdio: 'pipe' });
  server = createApp(new SurveyService(prisma)).listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  const base = `http://127.0.0.1:${server.address().port}/api`;
  async function request(method, url, body, token) {
    const response = await fetch(base + url, {
      method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    });
    return { status: response.status, data: await response.json() };
  }
  let author, participant, token, participantToken, survey;
  const password = ' pass with spaces ';
  await t.test('signup, login and credentials use the schema fields', async () => {
    let result = await request('POST', '/auth/signup', { loginId: 'author', name: 'Author', email: 'author@example.test', password, ageGroup: '20', region: 'Seoul' });
    assert.equal(result.status, 201);
    author = result.data.user;
    assert.equal(author.point, 0);
    assert.equal(author.name, 'Author');
    assert.equal(author.loginId, 'author');
    assert.equal(author.password, undefined);
    assert.notEqual((await prisma.user.findUnique({ where: { id: author.id } })).password, password);
    result = await request('POST', '/auth/login', { loginId: 'author', password });
    assert.equal(result.status, 200);
    token = result.data.accessToken;
    assert.equal(result.data.user.password, undefined);
    assert.equal((await request('GET', '/users/me', undefined, token)).data.id, author.id);
    assert.equal((await request('POST', '/auth/login', { email: 'author@example.test', password: 'incorrect' })).status, 401);
    assert.equal((await request('POST', '/auth/signup', { loginId: 'author', name: 'Duplicate', email: 'other@example.test', password })).status, 409);
    assert.equal((await request('POST', '/auth/signup', {})).status, 400);
    result = await request('POST', '/auth/signup', { loginId: 'participant', name: 'Participant', email: 'participant@example.test', password });
    participant = result.data.user;
    participantToken = (await request('POST', '/auth/login', { email: participant.email, password })).data.accessToken;
  });
  await t.test('protected routes reject missing and invalid tokens', async () => {
    assert.equal((await request('GET', '/users/me')).status, 401);
    assert.equal((await request('GET', '/users/me', undefined, 'invalid')).status, 401);
    assert.equal((await request('POST', '/surveys', {})).status, 401);
  });
  await t.test('nested survey creation, listing and detail agree with schema', async () => {
    const result = await request('POST', '/surveys', {
      title: 'Travel survey', category: 'Life', rewardPoint: 300, targetCount: 1,
      endDate: '2099-01-01T00:00:00Z', userId: participant.id,
      questions: [
        { question: 'Bus or walk?', questionType: 'single', options: [{ optionText: 'Bus' }, { optionText: 'Walk' }] },
        { question: 'Why?', questionType: 'short' },
      ],
    }, token);
    assert.equal(result.status, 201);
    survey = result.data;
    assert.equal(survey.userId, author.id);
    assert.equal(survey.author.name, author.name);
    assert.equal(survey.author.password, undefined);
    assert.equal(survey.questions[0].options[0].optionText, 'Bus');
    assert.equal(survey.questions[1].options.length, 0);
    assert.equal((await request('GET', `/surveys/${survey.id}`)).data.rewardPoint, 300);
    assert.equal((await request('GET', '/surveys?search=Travel&category=Life&status=OPEN')).data.length, 1);
    assert.equal((await request('GET', '/surveys?search=missing')).data.length, 0);
    assert.equal((await request('GET', '/surveys/not-an-id')).status, 400);
    assert.equal((await request('GET', '/surveys/999999')).status, 404);
    assert.equal((await request('POST', '/surveys', { title: 'bad', rewardPoint: -1, questions: [{ question: 'Q', questionType: 'short' }] }, token)).status, 400);
  });
  await t.test('only authors can edit and question CRUD uses question and optionText', async () => {
    assert.equal((await request('PATCH', `/surveys/${survey.id}`, { title: 'Stolen' }, participantToken)).status, 403);
    assert.equal((await request('GET', `/surveys/${survey.id}/results`, undefined, participantToken)).status, 403);
    const added = await request('POST', `/surveys/${survey.id}/questions`, { question: 'Extra', questionType: 'short' }, token);
    assert.equal(added.status, 201);
    assert.equal((await request('PATCH', `/questions/${added.data.id}`, { question: 'Edited' }, token)).data.question, 'Edited');
    assert.equal((await request('DELETE', `/questions/${added.data.id}`, undefined, token)).status, 200);
    const updated = await request('PATCH', `/questions/${survey.questions[0].id}`, { options: [{ optionText: 'Bus' }, { optionText: 'Walk' }] }, token);
    assert.equal(updated.status, 200);
    assert.equal(updated.data.options.length, 2);
  });
  await t.test('invalid, missing, foreign and duplicate answers never award points', async () => {
    const q1 = survey.questions[0].id, q2 = survey.questions[1].id;
    for (const answers of [[], [{ questionId: q1, answer: 'Bus' }],
      [{ questionId: q1, answer: 'Invalid' }, { questionId: q2, answer: 'Reason' }],
      [{ questionId: q1, answer: 'Bus' }, { questionId: q1, answer: 'Walk' }],
      [{ questionId: q1, answer: 'Bus' }, { questionId: 999999, answer: 'Reason' }],
      [{ questionId: q1, answer: 'Bus' }, { questionId: q2, answer: ' ' }],
    ]) assert.equal((await request('POST', `/surveys/${survey.id}/responses`, { answers }, participantToken)).status, 400);
    assert.equal(await prisma.response.count(), 0);
    assert.equal(await prisma.pointHistory.count(), 0);
    assert.equal((await prisma.user.findUnique({ where: { id: participant.id } })).point, 0);
    assert.equal((await prisma.survey.findUnique({ where: { id: survey.id } })).currentCount, 0);
  });
  const validAnswers = () => survey.questions.map(q => ({ questionId: q.id, answer: q.questionType === 'single' ? 'Bus' : 'Convenient' }));
  await t.test('submission atomically saves answers, participation and reward for token owner', async () => {
    const result = await request('POST', `/surveys/${survey.id}/responses`, { userId: author.id, answers: validAnswers() }, participantToken);
    assert.equal(result.status, 201);
    assert.equal(result.data.rewardPoint, 300);
    assert.equal(result.data.point, 300);
    const response = await prisma.response.findUnique({ where: { id: result.data.responseId }, include: { answers: true } });
    assert.equal(response.userId, participant.id);
    assert.equal(response.answers.length, 2);
    assert.equal((await prisma.survey.findUnique({ where: { id: survey.id } })).currentCount, 1);
    const points = await request('GET', '/users/me/points', undefined, participantToken);
    assert.equal(points.data.point, 300);
    assert.equal(points.data.histories[0].amount, 300);
    assert.ok(points.data.histories[0].description.includes(survey.title));
    assert.equal((await request('GET', '/users/me/responses', undefined, participantToken)).data.length, 1);
    assert.equal((await request('GET', `/surveys/${survey.id}/results`, undefined, token)).data.totalResponses, 1);
  });
  await t.test('repeat submission and full surveys cannot award another reward', async () => {
    assert.equal((await request('POST', `/surveys/${survey.id}/responses`, { answers: validAnswers() }, participantToken)).status, 409);
    assert.equal((await request('POST', `/surveys/${survey.id}/responses`, { answers: validAnswers() }, token)).status, 400);
    assert.equal((await prisma.user.findUnique({ where: { id: participant.id } })).point, 300);
    assert.equal(await prisma.pointHistory.count(), 1);
    assert.equal(await prisma.response.count(), 1);
    assert.equal((await request('PATCH', `/questions/${survey.questions[0].id}`, { question: 'Changed' }, token)).status, 409);
  });
  await t.test('closed and expired surveys reject submissions', async () => {
    await request('PATCH', `/surveys/${survey.id}`, { status: 'CLOSED', targetCount: 0 }, token);
    assert.equal((await request('POST', `/surveys/${survey.id}/responses`, { answers: validAnswers() }, token)).status, 400);
    await request('PATCH', `/surveys/${survey.id}`, { status: 'OPEN', endDate: '2000-01-01T00:00:00Z' }, token);
    assert.equal((await request('POST', `/surveys/${survey.id}/responses`, { answers: validAnswers() }, token)).status, 400);
    assert.equal((await request('PATCH', `/surveys/${survey.id}`, { endDate: 'not a date' }, token)).status, 400);
  });
  await t.test('transaction rolls back participation count when user reference fails', async () => {
    await request('PATCH', `/surveys/${survey.id}`, { endDate: null }, token);
    const service = new SurveyService(prisma);
    await assert.rejects(service.submitResponse(999999, survey.id, validAnswers()));
    assert.equal((await prisma.survey.findUnique({ where: { id: survey.id } })).currentCount, 1);
    assert.equal(await prisma.response.count(), 1);
    assert.equal(await prisma.pointHistory.count(), 1);
  });
  await t.test('schema mirror and Backend wrappers use the same implementation', async () => {
    assert.equal(fs.readFileSync(path.join(__dirname, '../Backend/prisma/schema.prisma'), 'utf8'), fs.readFileSync(path.join(__dirname, '../prisma/schema.prisma'), 'utf8'));
    assert.equal(require('../Backend/src/app').createApp, createApp);
    assert.equal(require('../Backend/src/services/survey.service').SurveyService, SurveyService);
    assert.equal((await request('POST', '/auth/logout', {}, token)).status, 200);
    assert.equal((await request('DELETE', `/surveys/${survey.id}`, undefined, participantToken)).status, 403);
    assert.equal((await request('DELETE', `/surveys/${survey.id}`, undefined, token)).status, 200);
    assert.equal(await prisma.question.count(), 0);
    assert.equal(await prisma.response.count(), 0);
  });
});
