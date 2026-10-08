// 프로젝트의 scripts 폴더에 복사한 뒤 실행하세요. 공용 서버에는 사용하지 않습니다.
const assert = require('node:assert/strict');
const { randomUUID } = require('node:crypto');

process.env.NODE_ENV = 'development';
process.env.DATABASE_URL = '';
process.env.DIRECT_URL = '';

const { getPrisma } = require('../src/lib/prisma');
const prisma = getPrisma();
const base = 'http://localhost:5000/api';

async function request(path, method = 'GET', token, body, expected) {
  const response = await fetch(`${base}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    signal: AbortSignal.timeout(20000),
  });
  const text = await response.text();
  let data;
  try { data = JSON.parse(text); } catch { throw new Error(`응답 형식 오류: ${path}`); }
  if (expected !== undefined) {
    assert.equal(response.status, expected, `${path}: ${data.error || response.status}`);
  } else if (!response.ok) {
    throw new Error(`${path}: ${response.status} ${data.error || '요청 실패'}`);
  }
  return data;
}

async function account(role, suffix) {
  const loginId = `recruit_${role}_${suffix}`;
  const password = `LocalTest!${suffix}`;
  const registered = await request('/auth/signup', 'POST', null, {
    loginId, password, email: `${loginId}@example.com`,
    name: `모집 테스트 ${role}`, memberType: 'KW_STUDENT',
  });
  const loggedIn = await request('/auth/login', 'POST', null, { loginId, password });
  // 서버가 같은 로컬 DB를 사용 중인지 확인한 뒤에만 테스트 포인트를 넣습니다.
  const local = await prisma.user.findUnique({ where: { loginId } });
  assert(local && local.id === registered.user.id,
    '서버와 스크립트의 DB가 다릅니다. 서버를 로컬 SQLite 설정으로 다시 실행하세요.');
  return { id: local.id, token: loggedIn.accessToken };
}

async function main() {
  const suffix = randomUUID().replaceAll('-', '').slice(0, 12);
  await request('/health');
  const author = await account('author', suffix);
  const participant = await account('participant', suffix);
  const other = await account('other', suffix);

  // 방금 만든 테스트 회원만 변경합니다.
  await prisma.user.update({ where: { id: author.id }, data: { point: 5000 } });
  await prisma.pointHistory.create({ data: {
    userId: author.id, amount: 5000, description: '로컬 모집 테스트 초기 포인트',
  } });
  assert.equal((await request('/users/me', 'GET', author.token)).point, 5000);
  console.log('PASS: 로컬 DB 연결 및 테스트 계정 생성');

  const now = Date.now();
  const recruitment = await request('/recruitments', 'POST', author.token, {
    title: `모집 API 테스트 ${suffix}`, organization: '테스트 연구실',
    activityType: 'EXPERIMENT', participationMode: 'OFFLINE',
    description: '로컬 기능 검증용 모집입니다.', eligibility: '테스트 계정',
    location: '테스트 장소', durationMinutes: 30, targetCount: 2, rewardPoint: 1000,
    applicationDeadline: new Date(now + 3600000).toISOString(),
    slots: [{ startsAt: new Date(now + 7200000).toISOString(),
      endsAt: new Date(now + 9000000).toISOString() }],
  });
  assert.equal((await request('/users/me', 'GET', author.token)).point, 3000);
  assert((await request('/recruitments')).some(r => r.id === recruitment.id));
  assert((await request('/recruitments/mine', 'GET', author.token))
    .some(r => r.id === recruitment.id));
  assert.equal((await request(`/recruitments/${recruitment.id}`)).id, recruitment.id);
  console.log('PASS: 모집 등록·조회 및 예산 2,000P 차감');

  const applyPath = `/recruitments/${recruitment.id}/applications`;
  const body = { slotId: recruitment.slots[0].id, agreed: true, message: '참여합니다.' };
  await request(applyPath, 'POST', participant.token, { ...body, agreed: false }, 400);
  await request(applyPath, 'POST', author.token, body, 400);
  const application = await request(applyPath, 'POST', participant.token, body);
  const duplicate = await request(applyPath, 'POST', participant.token, body);
  assert.equal(duplicate.id, application.id);
  assert((await request('/recruitment-applications/me', 'GET', participant.token))
    .some(a => a.id === application.id));
  await request(applyPath, 'GET', other.token, undefined, 403);
  assert.equal((await request(applyPath, 'GET', author.token)).length, 1);
  console.log('PASS: 동의·자기 신청 차단·중복 신청 방지·신청자 조회 권한');

  const prefix = `/recruitment-applications/${application.id}`;
  await request(`${prefix}/status`, 'PATCH', other.token, { status: 'ACCEPTED' }, 403);
  await request(`${prefix}/status`, 'PATCH', author.token, { status: 'ACCEPTED' });
  await request(`${prefix}/status`, 'PATCH', author.token, { status: 'ACCEPTED' });
  assert.equal((await request(`/recruitments/${recruitment.id}`)).acceptedCount, 1);
  await request(`${prefix}/cancel`, 'POST', participant.token);
  assert.equal((await request(`/recruitments/${recruitment.id}`)).acceptedCount, 0);
  const reapplied = await request(applyPath, 'POST', participant.token, body);
  assert.equal(reapplied.id, application.id);
  await request(`${prefix}/status`, 'PATCH', author.token, { status: 'ACCEPTED' });
  await request(`${prefix}/complete`, 'POST', author.token, undefined, 409);
  await request(`/recruitments/${recruitment.id}/close`, 'POST', author.token, undefined, 409);
  console.log('PASS: 선정·중복 선정·취소·재신청·조기 보상 및 미처리 종료 차단');

  // 기다리지 않고 지급을 검증하기 위해 이 테스트 일정만 과거로 바꿉니다.
  await prisma.recruitmentSlot.update({
    where: { id: recruitment.slots[0].id },
    data: { startsAt: new Date(Date.now() - 3600000), endsAt: new Date(Date.now() - 60000) },
  });
  await request(`${prefix}/complete`, 'POST', other.token, undefined, 403);
  await request(`${prefix}/complete`, 'POST', author.token);
  await request(`${prefix}/complete`, 'POST', author.token);
  assert.equal((await request('/users/me', 'GET', participant.token)).point, 1000);
  assert.equal(await prisma.pointHistory.count({
    where: { recruitmentApplicationId: application.id },
  }), 1);
  console.log('PASS: 참여 보상 1,000P 지급 및 중복 지급 방지');

  const closed = await request(`/recruitments/${recruitment.id}/close`, 'POST', author.token);
  assert.equal(closed.refundedPoint, 1000);
  await request(`/recruitments/${recruitment.id}/close`, 'POST', author.token);
  assert.equal((await request('/users/me', 'GET', author.token)).point, 4000);
  const final = await prisma.recruitment.findUnique({ where: { id: recruitment.id } });
  assert.equal(final.remainingReward, 0);
  assert.equal(final.status, 'CLOSED');
  console.log('PASS: 미사용 예산 1,000P 환급 및 중복 환급 방지');
  console.log('전체 로컬 모집 테스트 통과');
  console.log('테스트 계정과 종료된 테스트 모집글은 로컬 DB에 남습니다.');
}

main().catch(error => {
  console.error('FAIL:', error.message);
  process.exitCode = 1;
}).finally(() => prisma.$disconnect());
