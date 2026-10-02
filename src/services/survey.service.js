const { PrismaClient } = require('@prisma/client');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

const prisma = new PrismaClient();

const JWT_SECRET = process.env.JWT_SECRET;

if (!JWT_SECRET) {
  throw new Error('JWT_SECRET 환경변수를 설정해 주세요.');
}

class SurveyService {
  // 회원가입
  async signup({ loginId, name, password, age_group, region }) {
  const existingUser = await prisma.user.findUnique({
    where: { loginId }
  });

  if (existingUser) {
    throw {
      status: 409,
      message: '이미 사용 중인 아이디입니다.'
    };
  }

  const hashedPassword = await bcrypt.hash(password, 10);

  try {
    const user = await prisma.user.create({
      data: {
        loginId,
        name,
        password: hashedPassword,
        ageGroup: age_group,
        region,
        point: 0
      }
    });

    return {
      message: '회원가입이 완료되었습니다.',
      userId: user.id
    };
  } catch (error) {
    // 중복 확인 직후 다른 사용자가 같은 아이디로 가입한 경우
    if (error.code === 'P2002') {
      throw {
        status: 409,
        message: '이미 사용 중인 아이디입니다.'
      };
    }

    throw error;
  }
}

async login({ loginId, password }) {
  const user = await prisma.user.findUnique({
    where: { loginId }
  });

  if (!user) {
    throw {
      status: 401,
      message: '아이디 또는 비밀번호가 올바르지 않습니다.'
    };
  }

  const isMatch = await bcrypt.compare(password, user.password);

  if (!isMatch) {
    throw {
      status: 401,
      message: '아이디 또는 비밀번호가 올바르지 않습니다.'
    };
  }

  // JWT에는 여전히 DB의 숫자 ID를 넣음
  const accessToken = jwt.sign(
    { userId: user.id },
    JWT_SECRET,
    { expiresIn: '1d' }
  );

  return {
    message: '로그인 성공',
    accessToken,
    user: {
      user_id: user.id,
      login_id: user.loginId,
      name: user.name,
      point: user.point
    }
  };
}

  // 내 프로필 조회
  async getUserMe(userId) {
    const user = await prisma.user.findUnique({
      where: { id: userId }
    });

    if (!user) {
      throw {
        status: 404,
        message: '사용자를 찾을 수 없습니다.'
      };
    }

    return {
      user_id: user.id,
      name: user.name,
      age_group: user.ageGroup,
      region: user.region,
      point: user.point
    };
  }

  // 내가 참여한 설문 목록
  async getUserResponses(userId) {
    return prisma.response.findMany({
      where: { userId },
      include: {
        survey: true,
        answers: {
          include: { question: true }
        }
      }
    });
  }

  // 내 포인트 조회
  async getUserPoints(userId) {
    const user = await prisma.user.findUnique({
      where: { id: userId }
    });

    if (!user) {
      throw {
        status: 404,
        message: '사용자를 찾을 수 없습니다.'
      };
    }

    const histories = await prisma.pointHistory.findMany({
      where: { userId },
      orderBy: { createdAt: 'desc' }
    });

    return {
      current_point: user.point,
      histories
    };
  }

  // 설문 목록 조회
  async getSurveys({ category, status }) {
    const where = {};

    if (category) where.category = category;
    if (status) where.status = status;

    const surveys = await prisma.survey.findMany({
      where,
      orderBy: { createdAt: 'desc' }
    });

    return surveys.map(survey => ({
      survey_id: survey.id,
      title: survey.title,
      category: survey.category,
      reward_point: survey.rewardPoint,
      target_count: survey.targetCount,
      current_count: survey.currentCount,
      end_date: survey.endDate,
      status: survey.status
    }));
  }

  // 설문 상세 조회
  async getSurveyDetail(surveyId) {
    const survey = await prisma.survey.findUnique({
      where: { id: Number(surveyId) },
      include: {
        questions: {
          include: { options: true }
        }
      }
    });

    if (!survey) {
      throw {
        status: 404,
        message: '설문을 찾을 수 없습니다.'
      };
    }

    return {
      survey_id: survey.id,
      title: survey.title,
      reward_point: survey.rewardPoint,
      questions: survey.questions.map(q => ({
        question_id: q.id,
        question: q.question,
        question_type: q.questionType,
        options: q.options.map(option => ({
          option_id: option.id,
          option_text: option.optionText
        }))
      }))
    };
  }

  // 설문 생성
  async createSurvey(userId, data) {
    const {
      title,
      category,
      reward_point,
      target_headcount,
      questions
    } = data;

    if (
      typeof title !== 'string' ||
      !title.trim() ||
      !Array.isArray(questions) ||
      questions.length === 0 ||
      !Number.isSafeInteger(reward_point ?? 0) ||
      (reward_point ?? 0) < 0 ||
      !Number.isSafeInteger(target_headcount ?? 0) ||
      (target_headcount ?? 0) < 0
    ) {
      throw {
        status: 400,
        message: '제목, 질문 목록, 보상 포인트와 목표 인원을 확인해 주세요.'
      };
    }

    for (const q of questions) {
      const questionText =
        q?.question_text ?? q?.question ?? q?.text;

      if (
        typeof questionText !== 'string' ||
        !questionText.trim() ||
        (q.options !== undefined && !Array.isArray(q.options)) ||
        (q.options || []).some(opt => {
          const optionText =
            typeof opt === 'string'
              ? opt
              : opt?.option_text ?? opt?.optionText ?? opt?.text;

          return typeof optionText !== 'string';
        })
      ) {
        throw {
          status: 400,
          message: '질문과 선택지 형식을 확인해 주세요.'
        };
      }
    }

    return prisma.survey.create({
      data: {
        userId,
        title,
        category,
        rewardPoint: reward_point ?? 0,
        targetCount: target_headcount ?? 0,
        questions: {
          create: questions.map(q => ({
            question: q.question_text ?? q.question ?? q.text,
            questionType:
              q.question_type ??
              q.questionType ??
              q.type ??
              'single',
            options: {
              create: (q.options || []).map(opt => ({
                optionText:
                  typeof opt === 'string'
                    ? opt
                    : opt.option_text ?? opt.optionText ?? opt.text
              }))
            }
          }))
        }
      }
    });
  }

  // 설문 수정
  async updateSurvey(userId, surveyId, data) {
    const id = Number(surveyId);

    const survey = await prisma.survey.findUnique({
      where: { id }
    });

    if (!survey) {
      throw {
        status: 404,
        message: '설문을 찾을 수 없습니다.'
      };
    }

    if (survey.userId !== userId) {
      throw {
        status: 403,
        message: '수정 권한이 없습니다.'
      };
    }

    return prisma.survey.update({
      where: { id },
      data: {
        title: data.title,
        category: data.category,
        rewardPoint: data.reward_point,
        targetCount: data.target_count,
        status: data.status,
        endDate: data.end_date
          ? new Date(data.end_date)
          : undefined
      }
    });
  }

  // 설문 삭제
  async deleteSurvey(userId, surveyId) {
    const id = Number(surveyId);

    const survey = await prisma.survey.findUnique({
      where: { id }
    });

    if (!survey) {
      throw {
        status: 404,
        message: '설문을 찾을 수 없습니다.'
      };
    }

    if (survey.userId !== userId) {
      throw {
        status: 403,
        message: '삭제 권한이 없습니다.'
      };
    }

    await prisma.survey.delete({
      where: { id }
    });

    return {
      message: '설문이 성공적으로 삭제되었습니다.'
    };
  }

  // 질문 추가
  async addQuestion(userId, surveyId, data) {
    const id = Number(surveyId);

    const survey = await prisma.survey.findUnique({
      where: { id }
    });

    if (!survey) {
      throw {
        status: 404,
        message: '설문을 찾을 수 없습니다.'
      };
    }

    if (survey.userId !== userId) {
      throw {
        status: 403,
        message: '권한이 없습니다.'
      };
    }

    return prisma.question.create({
      data: {
        surveyId: id,
        question: data.question,
        questionType: data.question_type || 'single',
        options: data.options
          ? {
              create: data.options.map(option => ({
                optionText:
                  typeof option === 'string'
                    ? option
                    : option.option_text
              }))
            }
          : undefined
      }
    });
  }

  // 질문 수정
  async updateQuestion(userId, questionId, data) {
    const id = Number(questionId);

    const question = await prisma.question.findUnique({
      where: { id },
      include: { survey: true }
    });

    if (!question) {
      throw {
        status: 404,
        message: '질문을 찾을 수 없습니다.'
      };
    }

    if (question.survey.userId !== userId) {
      throw {
        status: 403,
        message: '권한이 없습니다.'
      };
    }

    return prisma.question.update({
      where: { id },
      data: {
        question: data.question,
        questionType: data.question_type
      }
    });
  }

  // 질문 삭제
  async deleteQuestion(userId, questionId) {
    const id = Number(questionId);

    const question = await prisma.question.findUnique({
      where: { id },
      include: { survey: true }
    });

    if (!question) {
      throw {
        status: 404,
        message: '질문을 찾을 수 없습니다.'
      };
    }

    if (question.survey.userId !== userId) {
      throw {
        status: 403,
        message: '권한이 없습니다.'
      };
    }

    await prisma.question.delete({
      where: { id }
    });

    return {
      message: '질문이 삭제되었습니다.'
    };
  }

  // 설문 응답 제출 및 포인트 지급
  async submitResponse(userId, surveyId, answers) {
    const targetSurveyId = Number(surveyId);

    if (!Array.isArray(answers) || answers.length === 0) {
      throw {
        status: 400,
        message: '답변 목록을 입력해 주세요.'
      };
    }

    const survey = await prisma.survey.findUnique({
      where: { id: targetSurveyId },
      include: {
        questions: {
          select: { id: true }
        }
      }
    });

    if (!survey) {
      throw {
        status: 404,
        message: '설문을 찾을 수 없습니다.'
      };
    }

    if (survey.status !== 'OPEN') {
      throw {
        status: 400,
        message: '참여할 수 없는 설문입니다.'
      };
    }

    if (survey.endDate && survey.endDate <= new Date()) {
      throw {
        status: 400,
        message: '마감된 설문입니다.'
      };
    }

    const questionIds = new Set(
      survey.questions.map(q => q.id)
    );

    const normalizedAnswers = answers.map(a => ({
      questionId: Number(a?.question_id ?? a?.questionId),
      answer: a?.answer ?? a?.value
    }));

    if (
      normalizedAnswers.some(a =>
        !questionIds.has(a.questionId) ||
        a.answer === undefined ||
        a.answer === null ||
        String(a.answer).trim() === ''
      )
    ) {
      throw {
        status: 400,
        message: '설문에 속한 질문의 답변을 입력해 주세요.'
      };
    }

    const uniqueQuestionIds = new Set(
      normalizedAnswers.map(a => a.questionId)
    );

    if (uniqueQuestionIds.size !== normalizedAnswers.length) {
      throw {
        status: 400,
        message: '같은 질문에 답변을 중복 제출할 수 없습니다.'
      };
    }

    const existingResponse = await prisma.response.findUnique({
      where: {
        userId_surveyId: {
          userId,
          surveyId: targetSurveyId
        }
      }
    });

    if (existingResponse) {
      throw {
        status: 409,
        message: '이미 참여한 설문입니다.'
      };
    }

    try {
      return await prisma.$transaction(async tx => {
        const updated = await tx.survey.updateMany({
          where: {
            id: targetSurveyId,
            status: 'OPEN',
            ...(survey.endDate
              ? { endDate: { gt: new Date() } }
              : {}),
            ...(survey.targetCount > 0
              ? {
                  currentCount: {
                    lt: survey.targetCount
                  }
                }
              : {})
          },
          data: {
            currentCount: {
              increment: 1
            }
          }
        });

        if (updated.count !== 1) {
          throw {
            status: 400,
            message: '마감되었거나 목표 인원이 찬 설문입니다.'
          };
        }

        const response = await tx.response.create({
          data: {
            userId,
            surveyId: targetSurveyId,
            answers: {
              create: normalizedAnswers.map(a => ({
                questionId: a.questionId,
                answer: String(a.answer)
              }))
            }
          }
        });

        if (survey.rewardPoint > 0) {
          await tx.user.update({
            where: { id: userId },
            data: {
              point: {
                increment: survey.rewardPoint
              }
            }
          });

          await tx.pointHistory.create({
            data: {
              userId,
              amount: survey.rewardPoint,
              description: `설문 참여 보상: ${survey.title}`
            }
          });
        }

        return {
          message: '설문 참여가 완료되었습니다.',
          response_id: response.id
        };
      });
    } catch (error) {
      if (error.code === 'P2002') {
        throw {
          status: 409,
          message: '이미 참여한 설문입니다.'
        };
      }

      throw error;
    }
  }

  // 설문 결과 조회
  async getSurveyResults(surveyId) {
    const id = Number(surveyId);

    const survey = await prisma.survey.findUnique({
      where: { id },
      include: {
        questions: {
          include: {
            options: true,
            answers: true
          }
        }
      }
    });

    if (!survey) {
      throw {
        status: 404,
        message: '설문을 찾을 수 없습니다.'
      };
    }

    const totalResponses = await prisma.response.count({
      where: { surveyId: id }
    });

    const questionsResult = survey.questions.map(q => {
      const optionCounts = {};

      q.options.forEach(option => {
        optionCounts[option.optionText] = 0;
      });

      q.answers.forEach(answer => {
        optionCounts[answer.answer] =
          (optionCounts[answer.answer] || 0) + 1;
      });

      const results = Object.keys(optionCounts).map(option => {
        const count = optionCounts[option];

        return {
          option,
          count,
          percentage:
            totalResponses > 0
              ? Math.round((count / totalResponses) * 100)
              : 0
        };
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