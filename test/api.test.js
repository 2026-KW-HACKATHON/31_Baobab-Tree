const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { SurveyService } = require('../src/services/survey.service');
const { createApp } = require('../src/app');

test('schema-backed API integration', async t => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'baobab-api-'));
  const schemaPath = path.join(directory, 'schema.prisma');
  const postgres = process.env.BAOBAB_TEST_POSTGRES === '1';
  const testSchema = 'baobab_test_' + require('node:crypto').randomBytes(12).toString('hex');
  let url;
  if (postgres) {
    url = new URL(process.env.TEST_DATABASE_URL);
    assert.ok(['postgres:', 'postgresql:'].includes(url.protocol));
    url.searchParams.set('schema', testSchema);
    url = url.toString();
  } else {
    url = `file:${path.join(directory, 'dev.db').replaceAll('\\', '/')}`;
  }
  const { PrismaClient } = require(postgres ? '@prisma/client' : '../node_modules/.prisma/baobab-sqlite');
  const prisma = new PrismaClient({ datasources: { db: { url } } });
  let server;
  t.after(async () => {
    if (server) await new Promise(resolve => server.close(resolve));
    try {
      if (postgres) {
        assert.match(testSchema, /^baobab_test_[a-f0-9]{24}$/);
        await prisma.$executeRawUnsafe(`DROP SCHEMA IF EXISTS "${testSchema}" CASCADE`);
      }
    } finally {
      await prisma.$disconnect();
      assert.equal(path.dirname(path.resolve(directory)), path.resolve(os.tmpdir()));
      fs.rmSync(directory, { recursive: true, force: true });
    }
  });
  if (postgres) {
    execFileSync(process.execPath, [require.resolve('prisma/build/index.js'), 'migrate', 'deploy'], {
      cwd: path.join(__dirname, '..'), stdio: 'pipe', env: { ...process.env, DATABASE_URL: url, DIRECT_URL: url },
    });
  } else {
    fs.copyFileSync(path.join(__dirname, '../prisma/schema.sqlite.prisma'), schemaPath);
    execFileSync(process.execPath, [require.resolve('prisma/build/index.js'), 'db', 'push', '--schema', schemaPath, '--skip-generate'], { stdio: 'pipe' });
  }
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
  await t.test('account profile updates persist and protect identity and point balance', async () => {
    assert.equal((await request('PATCH', '/users/me', { name: 'Changed' })).status, 401);
    let result = await request('PATCH', '/users/me', {
      name: 'New Author', email: 'updated@example.test', region: 'Wolgye', ageGroup: '20s'
    }, token);
    assert.equal(result.status, 200);
    assert.equal(result.data.name, 'New Author');
    assert.equal(result.data.email, 'updated@example.test');
    assert.equal(result.data.region, 'Wolgye');
    assert.equal(result.data.password, undefined);
    assert.equal((await request('GET', '/users/me', undefined, token)).data.name, 'New Author');
    for (const payload of [{ point: 99999 }, { id: participant.id }, { loginId: 'stolen' },
      { password: 'changed' }, { name: '' }, { email: 'invalid' }, {}]) {
      assert.equal((await request('PATCH', '/users/me', payload, token)).status, 400);
    }
    assert.equal((await request('PATCH', '/users/me', { email: participant.email }, token)).status, 409);
    assert.equal((await request('GET', '/users/me', undefined, token)).data.point, 0);
    result = await request('PATCH', '/users/me', {
      name: author.name, email: author.email, region: null, ageGroup: null
    }, token);
    assert.equal(result.status, 200);
    assert.equal(result.data.region, null);
  });
  await t.test('registration charges reward budget atomically and rejects insufficient funds', async () => {
    const payload = { title: 'Budget check', rewardPoint: 100, targetCount: 2,
      questions: [{ question: 'Q', questionType: 'short' }] };
    const before = await prisma.survey.count();
    assert.equal((await request('POST', '/surveys', payload, token)).status, 400);
    assert.equal(await prisma.survey.count(), before);
    assert.equal(await prisma.pointHistory.count({ where: { userId: participant.id } }), 0);
    await prisma.user.update({ where: { id: author.id }, data: { point: 1000 } });
    const created = await request('POST', '/surveys', payload, token);
    assert.equal(created.status, 201);
    assert.equal((await request('GET', '/users/me', undefined, token)).data.point, 800);
    assert.equal((await request('GET', '/users/me/points', undefined, token)).data.histories[0].amount, -200);
    assert.equal((await request('PATCH', '/surveys/' + created.data.id, { rewardPoint: 999 }, token)).status, 400);
    assert.equal((await request('POST', '/surveys', { ...payload, targetCount: 0 }, token)).status, 400);
    assert.equal((await request('POST', '/surveys', { ...payload, questions: [{ question: '', questionType: 'short' }] }, token)).status, 400);
    assert.equal((await request('GET', '/users/me', undefined, token)).data.point, 800);
    const deleted = await request('DELETE', '/surveys/' + created.data.id, undefined, token);
    assert.equal(deleted.status, 200);
    assert.equal(deleted.data.refundPoint, 200);
    assert.equal((await request('GET', '/users/me', undefined, token)).data.point, 1000);
    assert.equal((await request('DELETE', '/surveys/' + created.data.id, undefined, token)).status, 404);
    assert.equal((await request('GET', '/users/me', undefined, token)).data.point, 1000);
  });
  await t.test('only unspent funded rewards are refunded; legacy surveys do not create points', async () => {
    const payload = { title: 'Partial refund', rewardPoint: 100, targetCount: 3,
      questions: [{ question: 'Q', questionType: 'short' }] };
    const created = await request('POST', '/surveys', payload, token);
    assert.equal(created.status, 201);
    const url = '/surveys/' + created.data.id;
    assert.equal((await request('DELETE', url, undefined, participantToken)).status, 403);
    assert.equal((await request('POST', url + '/responses', { answers: [{ questionId: created.data.questions[0].id, answer: 'A' }] }, participantToken)).status, 201);
    const deleted = await request('DELETE', url, undefined, token);
    assert.equal(deleted.data.refundPoint, 200);
    assert.equal((await request('GET', '/users/me', undefined, token)).data.point, 900);
    assert.equal((await request('GET', '/users/me', undefined, participantToken)).data.point, 100);
    const legacy = await prisma.survey.create({ data: { userId: author.id, title: 'Unfunded legacy', rewardPoint: 100, targetCount: 10 } });
    assert.equal((await request('DELETE', '/surveys/' + legacy.id, undefined, token)).data.refundPoint, 0);
    assert.equal((await request('GET', '/users/me', undefined, token)).data.point, 900);
    // Reset fixtures for the existing participation/reward tests.
    await prisma.user.update({ where: { id: participant.id }, data: { point: 0 } });
    await prisma.pointHistory.deleteMany({ where: { userId: participant.id } });
  });
  await t.test('backend legacy field names map to the Android contract', async () => {
    let result = await request('POST', '/surveys', {
      title: 'Legacy compatibility', reward_point: 10, target_headcount: 5,
      questions: [{ text: 'Why?', question_type: 'short' }]
    }, token);
    assert.equal(result.status, 201);
    assert.equal(result.data.rewardPoint, 10);
    assert.equal(result.data.targetCount, 5);
    assert.equal(result.data.questions[0].question, 'Why?');
    assert.equal((await request('DELETE', `/surveys/${result.data.id}`, undefined, token)).status, 200);
  });

  await t.test('metadata, images, optional answers and per-question statistics persist', async () => {
    const imageData = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII=';
    const payload = { title: 'Stored fields', description: 'Introduction', audience: 'Students', duration: '5 minutes', imageData,
      questions: [{ question: 'Required', questionType: 'short' },
        { question: 'Optional', questionType: 'single', required: false, options: ['Yes', 'No'] }] };
    for (const invalid of [{ ...payload, imageData: 'data:image/png;base64,YmFk' },
      { ...payload, questions: [{ question: 'Bad', questionType: 'short', required: 'false' }] }]) {
      assert.equal((await request('POST', '/surveys', invalid, token)).status, 400);
    }
    const created = await request('POST', '/surveys', payload, token);
    assert.equal(created.status, 201);
    const saved = (await request('GET', '/surveys/' + created.data.id)).data;
    for (const field of ['description', 'audience', 'duration', 'imageData']) assert.equal(saved[field], payload[field]);
    assert.equal(saved.questions[0].required, true);
    assert.equal(saved.questions[1].required, false);
    const url = '/surveys/' + saved.id;
    assert.equal((await request('POST', url + '/responses', { answers: [] }, participantToken)).status, 400);
    assert.equal((await request('POST', url + '/responses', { answers: [
      { questionId: saved.questions[0].id, answer: 'Reason' }, { questionId: saved.questions[1].id, answer: 'Invalid' }
    ] }, participantToken)).status, 400);
    assert.equal((await request('POST', url + '/responses', { answers: [
      { questionId: saved.questions[0].id, answer: 'Reason' }
    ] }, participantToken)).status, 201);
    let results = (await request('GET', url + '/results', undefined, token)).data;
    assert.equal(results.totalResponses, 1);
    assert.equal(results.questions[1].responseCount, 0);
    assert.ok(results.questions[1].results.every(result => result.percentage === 0));
    const history = (await request('GET', '/users/me/responses', undefined, participantToken)).data;
    assert.equal(history[0].answers.length, 1);
    assert.equal(history[0].answers[0].question.question, 'Required');
    assert.equal((await request('POST', url + '/responses', { answers: [
      { questionId: saved.questions[0].id, answer: 'Another reason' }, { questionId: saved.questions[1].id, answer: 'Yes' }
    ] }, token)).status, 201);
    results = (await request('GET', url + '/results', undefined, token)).data;
    assert.equal(results.totalResponses, 2);
    assert.equal(results.questions[1].responseCount, 1);
    assert.equal(results.questions[1].results.find(result => result.option === 'Yes').percentage, 100);
    const cleared = await request('PATCH', url, { description: null, audience: null, duration: null, imageData: null }, token);
    assert.equal(cleared.status, 200);
    assert.equal(cleared.data.imageData, null);
    assert.equal((await request('DELETE', url, undefined, token)).status, 200);
    const optional = await request('POST', '/surveys', { title: 'All optional',
      questions: [{ question: 'Optional', questionType: 'short', required: false }] }, token);
    assert.equal(optional.status, 201);
    assert.equal((await request('POST', '/surveys/' + optional.data.id + '/responses', { answers: [] }, participantToken)).status, 201);
    assert.equal((await request('DELETE', '/surveys/' + optional.data.id, undefined, token)).status, 200);
    assert.equal(await prisma.response.count(), 0);
    assert.equal(await prisma.pointHistory.count({ where: { userId: participant.id } }), 0);
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
    assert.equal(await prisma.pointHistory.count({ where: { userId: participant.id } }), 0);
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
    assert.equal(await prisma.pointHistory.count({ where: { userId: participant.id } }), 1);
    assert.equal(await prisma.response.count(), 1);
    assert.equal((await request('PATCH', `/questions/${survey.questions[0].id}`, { question: 'Changed' }, token)).status, 409);
  });
  await t.test('closed and expired surveys reject submissions', async () => {
    await request('PATCH', `/surveys/${survey.id}`, { status: 'CLOSED' }, token);
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
    assert.equal(await prisma.pointHistory.count({ where: { userId: participant.id } }), 1);
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
  await t.test('coupon exchange deducts once and replays the same request without another charge', async () => {
    await prisma.user.update({ where: { id: author.id }, data: { point: 1000 } });
    const body = { itemId: 'cafe-americano', requestKey: require('node:crypto').randomUUID() };
    const first = await request('POST', '/coupons/exchange', body, token);
    assert.equal(first.status, 201);
    assert.equal(first.data.point, 500);
    const second = await request('POST', '/coupons/exchange', body, token);
    assert.equal(second.status, 200);
    assert.equal(second.data.coupon.id, first.data.coupon.id);
    assert.equal(second.data.alreadyProcessed, true);
    assert.equal((await request('POST', '/coupons/exchange', { ...body, itemId: 'book-discount' }, token)).status, 409);
    assert.equal((await request('POST', '/coupons/exchange', { itemId: 'book-discount', requestKey: require('node:crypto').randomUUID() }, token)).status, 409);
    assert.equal(await prisma.coupon.count({ where: { userId: author.id } }), 1);
    assert.equal((await prisma.user.findUnique({ where: { id: author.id } })).point, 500);
  });

  await t.test('Toss checkout uses public callbacks and approved orders credit exactly once', async () => {
    const names = ['TOSS_CLIENT_KEY', 'TOSS_SECRET_KEY', 'PAYMENT_PUBLIC_BASE_URL'];
    const saved = Object.fromEntries(names.map(name => [name, process.env[name]]));
    const originalFetch = global.fetch;
    let confirmations = 0;
    let providerPayment;
    try {
      process.env.TOSS_CLIENT_KEY = 'test_ck_contract';
      process.env.TOSS_SECRET_KEY = 'test_sk_contract';
      process.env.PAYMENT_PUBLIC_BASE_URL = 'https://baobab-test.example';
      global.fetch = async (url, options) => {
        if (String(url).startsWith('https://api.tosspayments.com/')) {
          if (String(url).endsWith('/confirm')) confirmations++;
          return new Response(JSON.stringify(providerPayment), { headers: { 'Content-Type': 'application/json' } });
        }
        return originalFetch(url, options);
      };
      const before = (await prisma.user.findUnique({ where: { id: author.id } })).point;
      const created = await request('POST', '/payments/orders', { provider: 'TOSS', amount: 1000 }, token);
      assert.equal(created.status, 201);
      const order = created.data;
      assert.equal((await request('GET', `/payments/orders/${order.id}`, undefined, participantToken)).status, 404);
      const ready = await request('POST', `/payments/orders/${order.id}/toss/ready`, { callbackToken: order.callbackToken }, token);
      assert.equal(ready.status, 200);
      assert.ok(ready.data.checkoutUrl.startsWith('https://baobab-test.example/api/payments/toss/checkout?'));
      const checkout = await fetch(base + '/payments/toss/checkout' + new URL(ready.data.checkoutUrl).search);
      assert.equal(checkout.status, 200);
      assert.match(await checkout.text(), /https:\/\/baobab-test\.example\/api\/payments\/toss\/success/);
      providerPayment = { paymentKey: 'test-payment-' + order.id, orderId: order.id, totalAmount: 1000, currency: 'KRW', status: 'DONE', approvedAt: new Date().toISOString() };
      const query = new URLSearchParams({ orderId: order.id, state: order.callbackToken, paymentKey: providerPayment.paymentKey, amount: '1000' });
      assert.equal((await fetch(base + '/payments/toss/success?' + query)).status, 200);
      assert.equal((await fetch(base + '/payments/toss/success?' + query)).status, 200);
      assert.equal(confirmations, 1);
      assert.equal((await prisma.user.findUnique({ where: { id: author.id } })).point, before + 1000);
      assert.equal(await prisma.pointHistory.count({ where: { paymentOrderId: order.id } }), 1);
      query.set('amount', '3000');
      assert.equal((await fetch(base + '/payments/toss/success?' + query)).status, 400);
      assert.equal((await prisma.user.findUnique({ where: { id: author.id } })).point, before + 1000);
    } finally {
      global.fetch = originalFetch;
      for (const name of names) {
        if (saved[name] === undefined) delete process.env[name]; else process.env[name] = saved[name];
      }
    }
  });

  await t.test('PostgreSQL concurrent participation cannot exceed the funded capacity', { skip: !postgres }, async () => {
    const other = (await request('POST', '/auth/signup', { loginId: 'concurrent', name: 'Concurrent', email: 'concurrent@example.test', password })).data.user;
    const otherToken = (await request('POST', '/auth/login', { loginId: other.loginId, password })).data.accessToken;
    const created = await request('POST', '/surveys', { title: 'One seat', rewardPoint: 100, targetCount: 1, questions: [{ question: 'Why?', questionType: 'short' }] }, token);
    assert.equal(created.status, 201);
    const answer = { answers: [{ questionId: created.data.questions[0].id, answer: 'Answer' }] };
    const results = await Promise.all([participantToken, otherToken].map(auth => request('POST', `/surveys/${created.data.id}/responses`, answer, auth)));
    assert.deepEqual(results.map(result => result.status).sort(), [201, 400]);
    assert.equal(await prisma.response.count({ where: { surveyId: created.data.id } }), 1);
    assert.equal((await prisma.survey.findUnique({ where: { id: created.data.id } })).currentCount, 1);
    assert.equal((await request('DELETE', `/surveys/${created.data.id}`, undefined, token)).data.refundPoint, 0);
  });

  await t.test('PostgreSQL simultaneous coupon replays cannot charge twice', { skip: !postgres }, async () => {
    const before = (await prisma.user.findUnique({ where: { id: author.id } })).point;
    const body = { itemId: 'cafe-americano', requestKey: require('node:crypto').randomUUID() };
    const results = await Promise.all([1, 2].map(() => request('POST', '/coupons/exchange', body, token)));
    assert.deepEqual(results.map(result => result.status).sort(), [200, 201]);
    assert.equal(results[0].data.coupon.id, results[1].data.coupon.id);
    assert.equal((await prisma.user.findUnique({ where: { id: author.id } })).point, before - 500);
    assert.equal(await prisma.coupon.count({ where: { userId: author.id, requestKey: body.requestKey } }), 1);
  });

});
