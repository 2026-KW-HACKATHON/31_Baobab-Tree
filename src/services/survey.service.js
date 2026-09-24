// Prisma ORM Client 불러오기 (DB 제어용)
const { PrismaClient } = require('@prisma/client');
// 비밀번호 암호화 라이브러리
const bcrypt = require('bcrypt');
// JWT 토큰 발급 라이브러리
const jwt = require('jsonwebtoken');

const prisma = new PrismaClient();
// 토큰 암호화 시 사용할 시크릿 키
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key';

class SurveyService {
  // 1. 인증(Auth) 로직

  // [회원가입]
  async signup({ nickname, email, password, age_group, region }) {
    // 이메일 중복 체크
    const existingUser = await prisma.user.findUnique({ where: { email } });
    if (existingUser) {
      throw { status: 400, message: '이미 존재하는 이메일입니다.' };
    }

    // 비밀번호 해싱 (보안을 위해 10번 솔팅)
    const hashedPassword = await bcrypt.hash(password, 10);

    // DB에 새 유저 생성
    const user = await prisma.user.create({
      data: {
        nickname,
        email,
        password: hashedPassword,
        ageGroup: age_group,
        region,
        point: 0 // 가입 시 초기 포인트는 0
      }
    });

    return { message: '회원가입이 완료되었습니다.', userId: user.id };
  }

  // [로그인]
  async login({ email, password }) {
    // 유저 존재 여부 확인
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) {
      throw { status: 401, message: '이메일 또는 비밀번호가 올바르지 않습니다.' };
    }

    // 입력받은 비밀번호와 DB의 암호화된 비밀번호 비교
    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      throw { status: 401, message: '이메일 또는 비밀번호가 올바르지 않습니다.' };
    }

    // JWT 토큰 발급 (유효기간: 1일)
    const accessToken = jwt.sign({ userId: user.id }, JWT_SECRET, { expiresIn: '1d' });

    return {
      message: "로그인 성공",
      accessToken,
      user: {
        user_id: user.id,
        nickname: user.nickname,
        point: user.point
      }
    };
  }

  // 2. 사용자(마이페이지) 정보 조회

  // [내 프로필 조회]
  async getUserMe(userId) {
    const user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw { status: 404, message: '사용자를 찾을 수 없습니다.' };

    return {
      user_id: user.id,
      nickname: user.nickname,
      age_group: user.ageGroup,
      region: user.region,
      point: user.point
    };
  }

  // [내가 참여한 설문 목록 및 답변 내역 조회]
  async getUserResponses(userId) {
    const responses = await prisma.response.findMany({
      where: { userId },
      include: {
        survey: true, // 참여한 설문 정보 포함
        answers: {
          include: { question: true } // 세부 답변 및 해당 질문 정보까지 함께 조인해서 가져옴
        }
      }
    });
    return responses;
  }

  // [내 포인트 잔액 및 변동 내역 조회]
  async getUserPoints(userId) {
    const user = await prisma.user.findUnique({ where: { id: userId } });
    const histories = await prisma.pointHistory.findMany({
      where: { userId },
      orderBy: { createdAt: 'desc' } // 최신 내역 순 정렬
    });

    return {
      current_point: user.point,
      histories
    };
  }

  // 3. 설문지(Survey) 관리

  // [설문지 목록 조회 (카테고리/상태별 필터링 가능)]
  async getSurveys({ category, status }) {
    const where = {};
    if (category) where.category = category;
    if (status) where.status = status;

    const surveys = await prisma.survey.findMany({
      where,
      orderBy: { createdAt: 'desc' }
    });

    // 프론트엔드 파스칼/스네이크 케이스 규격에 맞게 매핑하여 리턴
    return surveys.map(s => ({
      survey_id: s.id,
      title: s.title,
      category: s.category,
      reward_point: s.rewardPoint,
      target_count: s.targetCount,
      current_count: s.currentCount,
      end_date: s.endDate,
      status: s.status
    }));
  }

  // [특정 설문지 상세 조회 (질문과 선택지까지 가져오기)]
  async getSurveyDetail(surveyId) {
    const survey = await prisma.survey.findUnique({
      where: { id: parseInt(surveyId) },
      include: {
        questions: {
          include: { options: true } // 질문에 딸린 보기도 포함
        }
      }
    });

    if (!survey) throw { status: 404, message: '설문을 찾을 수 없습니다.' };

    return {
      survey_id: survey.id,
      title: survey.title,
      reward_point: survey.rewardPoint,
      questions: survey.questions.map(q => ({
        question_id: q.id,
        question: q.question,
        question_type: q.questionType,
        options: q.options.map(o => ({
          option_id: o.id,
          option_text: o.optionText
        }))
      }))
    };
  }

  // [새 설문지 등록 (질문/보기 중첩 생성)]
  async createSurvey(userId, data) {
    const { title, category, reward_point, target_headcount, questions } = data;

    // Prisma의 nested create를 이용해 설문지 + 질문 + 보기를 한 번에 생성
    const newSurvey = await prisma.survey.create({
      data: {
        userId,
        title,
        category,
        rewardPoint: reward_point,
        targetCount: target_headcount || 0,
        questions: {
          create: questions.map((q) => ({
            // 프론트 요청 키값 차이(question_text vs question) 유연하게 지원
            question: q.question_text || q.question, 
            questionType: q.question_type || q.questionType,
            options: {
              create: q.options.map((opt) => ({
                optionText: opt.option_text || opt.optionText,
              })),
            },
          })),
        },
      },
    });

    return newSurvey;
  }

  // [설문지 기본 정보 수정]
  async updateSurvey(userId, surveyId, data) {
    const survey = await prisma.survey.findUnique({ where: { id: parseInt(surveyId) } });
    if (!survey) throw { status: 404, message: '설문을 찾을 수 없습니다.' };
    if (survey.userId !== userId) throw { status: 403, message: '수정 권한이 없습니다.' }; // 작성자 확인

    return await prisma.survey.update({
      where: { id: parseInt(surveyId) },
      data: {
        title: data.title,
        category: data.category,
        rewardPoint: data.reward_point,
        targetCount: data.target_count,
        status: data.status,
        endDate: data.end_date ? new Date(data.end_date) : undefined
      }
    });
  }

  // [설문지 삭제]
  async deleteSurvey(userId, surveyId) {
    const survey = await prisma.survey.findUnique({ where: { id: parseInt(surveyId) } });
    if (!survey) throw { status: 404, message: '설문을 찾을 수 없습니다.' };
    if (survey.userId !== userId) throw { status: 403, message: '삭제 권한이 없습니다.' };

    // Schema에 onDelete: Cascade가 걸려있어서 질문/응답 등도 같이 삭제됨
    await prisma.survey.delete({ where: { id: parseInt(surveyId) } });
    return { message: '설문이 성공적으로 삭제되었습니다.' };
  }

  // 4. 질문(Question) 개별 관리

  // [질문 개별 추가]
  async addQuestion(userId, surveyId, data) {
    const survey = await prisma.survey.findUnique({ where: { id: parseInt(surveyId) } });
    if (!survey) throw { status: 404, message: '설문을 찾을 수 없습니다.' };
    if (survey.userId !== userId) throw { status: 403, message: '권한이 없습니다.' };

    return await prisma.question.create({
      data: {
        surveyId: parseInt(surveyId),
        question: data.question,
        questionType: data.question_type || 'single',
        options: data.options ? {
          create: data.options.map(o => ({ optionText: typeof o === 'string' ? o : o.option_text }))
        } : undefined
      }
    });
  }

  // [질문 개별 수정]
  async updateQuestion(userId, questionId, data) {
    const question = await prisma.question.findUnique({
      where: { id: parseInt(questionId) },
      include: { survey: true }
    });
    if (!question) throw { status: 404, message: '질문을 찾을 수 없습니다.' };
    if (question.survey.userId !== userId) throw { status: 403, message: '권한이 없습니다.' };

    return await prisma.question.update({
      where: { id: parseInt(questionId) },
      data: {
        question: data.question,
        questionType: data.question_type
      }
    });
  }

  // [질문 개별 삭제]
  async deleteQuestion(userId, questionId) {
    const question = await prisma.question.findUnique({
      where: { id: parseInt(questionId) },
      include: { survey: true }
    });
    if (!question) throw { status: 404, message: '질문을 찾을 수 없습니다.' };
    if (question.survey.userId !== userId) throw { status: 403, message: '권한이 없습니다.' };

    await prisma.question.delete({ where: { id: parseInt(questionId) } });
    return { message: '질문이 삭제되었습니다.' };
  }

  // 5. 설문 응답 제출 및 트랜잭션 처리

  // [설문 응답 제출 및 보상 지급]
  async submitResponse(userId, surveyId, answers) {
    const targetSurveyId = parseInt(surveyId);

    // 1. 설문지 유효성 및 마감 여부 체크
    const survey = await prisma.survey.findUnique({ where: { id: targetSurveyId } });
    if (!survey) throw { status: 404, message: '설문을 찾을 수 없습니다.' };
    if (survey.status !== 'OPEN') throw { status: 400, message: '참여할 수 없는 설문입니다.' };

    // 2. 🔥 중복 참여 체크 (유저 ID + 설문 ID 복합 키 활용)
    const existingResponse = await prisma.response.findUnique({
      where: {
        userId_surveyId: { userId, surveyId: targetSurveyId }
      }
    });
    if (existingResponse) throw { status: 409, message: '이미 참여한 설문입니다.' };

    // 3. 🔥 단일 트랜잭션 처리 (하나라도 실패하면 전체 취소/롤백)
    return await prisma.$transaction(async (tx) => {
      // (1) 응답 및 세부 답변 생성
      const response = await tx.response.create({
        data: {
          userId,
          surveyId: targetSurveyId,
          answers: {
            create: answers.map(a => ({
              questionId: a.question_id,
              answer: String(a.answer)
            }))
          }
        }
      });

      // (2) 설문지 현재 참여 인원수(+1) 증가
      await tx.survey.update({
        where: { id: targetSurveyId },
        data: { currentCount: { increment: 1 } }
      });

      // (3) 보상 포인트가 설정되어 있다면 포인트 적립 및 내역 남기기
      if (survey.rewardPoint > 0) {
        // 유저 보유 포인트 증가
        await tx.user.update({
          where: { id: userId },
          data: { point: { increment: survey.rewardPoint } }
        });

        // 포인트 변동 히스토리 추가
        await tx.pointHistory.create({
          data: {
            userId,
            amount: survey.rewardPoint,
            description: `설문 참여 보상: ${survey.title}`
          }
        });
      }

      return { message: '설문 참여가 완료되었습니다.', response_id: response.id };
    });
  }

  // 6. 설문 통계 결과 계산

  // [설문 결과 통계 조회]
  async getSurveyResults(surveyId) {
    const targetSurveyId = parseInt(surveyId);
    const survey = await prisma.survey.findUnique({
      where: { id: targetSurveyId },
      include: {
        questions: {
          include: {
            options: true,
            answers: true
          }
        }
      }
    });

    if (!survey) throw { status: 404, message: '설문을 찾을 수 없습니다.' };

    // 총 응답 수 집계
    const totalResponses = await prisma.response.count({
      where: { surveyId: targetSurveyId }
    });

    // 질문별 선택지 투표 수 및 퍼센티지(비율) 계산
    const questionsResult = survey.questions.map(q => {
      const optionCounts = {};
      
      // 보기별 카운트 0으로 초기화
      q.options.forEach(o => {
        optionCounts[o.optionText] = 0;
      });

      // 제출된 답변 카운트 증가
      q.answers.forEach(a => {
        if (optionCounts[a.answer] !== undefined) {
          optionCounts[a.answer] += 1;
        } else {
          optionCounts[a.answer] = (optionCounts[a.answer] || 0) + 1;
        }
      });

      // 퍼센티지 백분율 계산해서 결과 배열 구성
      const results = Object.keys(optionCounts).map(opt => {
        const count = optionCounts[opt];
        const percentage = totalResponses > 0 ? Math.round((count / totalResponses) * 100) : 0;
        return { option: opt, count, percentage };
      });

      return {
        question_id: q.id,
        results
      };
    });

    return {
      survey_id: survey.id,
      total_responses: totalResponses,
      questions: questionsResult
    };
  }
}

module.exports = new SurveyService();