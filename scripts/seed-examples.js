const { PrismaClient } = require('@prisma/client');
const bcrypt = require('bcryptjs');
const { randomBytes } = require('node:crypto');

const examples = [
  ['생활·편의', '일상 속 플라스틱 제품 사용 실태 조사', '일회용 플라스틱을 얼마나 자주 사용하시나요?', ['매일', '주 2~3회', '거의 사용하지 않음']],
  ['생활·편의', '플라스틱 빨대와 종이 빨대 선호도', '어떤 빨대를 더 선호하시나요?', ['플라스틱 빨대', '종이 빨대', '다회용 빨대', '빨대 사용 안 함']],
  ['지역·사회', '우리 동네 분리수거 이용 경험', '동네 분리수거 시설을 이용하기 편한가요?', ['편하다', '보통이다', '불편하다']],
  ['지역·사회', '지역 축제와 주민 참여 조사', '참여하고 싶은 지역 행사 유형은 무엇인가요?', ['먹거리 축제', '문화 공연', '환경 봉사', '체육 행사']],
  ['교육·학습', '대학생 학습 공간 선호도 조사', '주로 어디에서 공부하시나요?', ['도서관', '집', '카페', '스터디룸']],
  ['교육·학습', '온라인 강의 학습 경험 조사', '가장 선호하는 수업 방식은 무엇인가요?', ['대면 수업', '실시간 온라인', '녹화 강의', '혼합 수업']],
  ['문화·스포츠', '다회용 용기 사용과 야외 활동', '야외 활동 시 다회용 용기를 챙기시나요?', ['항상 챙김', '가끔 챙김', '챙기지 않음']],
  ['문화·스포츠', '주말 문화 및 스포츠 활동 조사', '주말에 가장 즐기는 활동은 무엇인가요?', ['영화·공연', '운동', '전시 관람', '집에서 휴식']],
  ['경제·상권', '카페 친환경 포장 선택 조사', '친환경 포장을 위해 추가 비용을 낼 의향이 있나요?', ['있다', '가격에 따라 다르다', '없다']],
  ['경제·상권', '동네 상점 이용과 소비 습관', '동네 상점을 선택할 때 가장 중요한 점은 무엇인가요?', ['가격', '품질', '거리', '친절한 서비스']],
  ['건강·의료', '미세플라스틱 건강 영향 인식 조사', '미세플라스틱의 건강 영향을 얼마나 걱정하시나요?', ['많이 걱정함', '조금 걱정함', '걱정하지 않음']],
  ['건강·의료', '대학생 수면과 건강 습관 조사', '하루 평균 수면 시간은 얼마인가요?', ['5시간 미만', '5~6시간', '7~8시간', '9시간 이상']],
];

async function seedExamples(prisma) {
  const password = await bcrypt.hash(randomBytes(32).toString('hex'), 10);
  return prisma.$transaction(async tx => {
    const user = await tx.user.upsert({
      where: { email: 'examples@baobab.invalid' }, update: {},
      create: { email: 'examples@baobab.invalid', loginId: 'baobab_examples', name: '바오밥 예시', password },
    });
    let created = 0;
    for (const [category, title, question, options] of examples) {
      if (await tx.survey.findFirst({ where: { userId: user.id, title } })) continue;
      await tx.survey.create({ data: {
        userId: user.id, category, title, rewardPoint: category === '건강·의료' ? 500 : 100,
        targetCount: 100, status: 'OPEN', endDate: null,
        questions: { create: [
          { question, questionType: 'single', options: { create: options.map(optionText => ({ optionText })) } },
          { question: '선택한 이유나 개선 의견을 자유롭게 적어주세요.', questionType: 'short' },
        ] },
      } });
      created++;
    }
    return { created, categories: 6, examples: examples.length };
  });
}

if (require.main === module) {
  const prisma = new PrismaClient();
  seedExamples(prisma).then(result => console.log(JSON.stringify(result)))
    .catch(error => { console.error(error.message); process.exitCode = 1; })
    .finally(() => prisma.$disconnect());
}
module.exports = { seedExamples };
