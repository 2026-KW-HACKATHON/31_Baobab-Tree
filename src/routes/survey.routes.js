// ==============================================================================
// 통합 API 라우터 파일 (src/routes/survey.routes.js)
// 기능: API 명세서에 맞춘 사용자 인증(회원가입/로그인) 및 설문 CRUD, 응답/보상 트랜잭션 처리
// ==============================================================================

const express = require('express');
const bcrypt = require('bcryptjs');//추가: 암호화
const jwt = require('jsonwebtoken');//추가: 토큰 발급

const router = express.Router();

// Prisma ORM 인스턴스 생성 (SQLite 데이터베이스와 통신)
const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();


// ==============================================================================
// [SECTION 1] 사용자 및 인증 관련 API (/api/auth/...)
// ==============================================================================

/**
 * 1-1. 회원가입 API[cite: 3]
 * @route POST /api/auth/signup[cite: 3]
 * @desc 새로운 사용자를 시스템에 등록 (기초 포인트: 0)[cite: 3]
 */
router.post('/auth/signup', async (req, res, next) => {
  try {
    const { name,
      loginId,
      password,
      ageGroup
     } = req.body;

    // 1) 필수 입력 파라미터 검증
    if (!name || !loginId || !password || !ageGroup) {
      return res.status(400).json({
        error: '이름, 아이디, 비밀번호, 나이대는 필수 입력 항목입니다.'
      });
    }
    
    if (!email || !password || !name) {
      return res.status(400).json({ error: '이메일, 비밀번호, 이름은 필수 입력 항목입니다.' });
    }

    // 2) 이메일 중복 검사
    const existingUser = await prisma.user.findUnique({ where: { email } });
    if (existingUser) {
      return res.status(400).json({ error: '이미 가입된 이메일 주소입니다.' });
    }

    // 1. 비밀번호 암호화
    const hashedPassword = await bcrypt.hash(password, 10);

    // 3) DB에 신규 유저 데이터 생성 (비밀번호 원문 저장 - 추후 bcrypt 암호화 권장)
    const user = await prisma.user.create({
      data: {
        email,
        password: hashedPassword,//추가: 암호화 과정 
        name,
        points: 0 // 명세서 기준 초기 포인트 0 설정[cite: 3]
      },
      select: {
        id: true,
        email: true,
        name: true,
        points: true,
        createdAt: true
      }
    });

    // 4) 성공 응답 반환 (HTTP 201 Created)[cite: 3]
    res.status(201).json({ message: '회원가입이 성공적으로 완료되었습니다.', user });
  } catch (error) {
    next(error); // 예외 발생 시 중앙 에러 핸들러로 이관
  }
});

/**
 * 1-2. 로그인 API[cite: 3]
 * @route POST /api/auth/login[cite: 3]
 * @desc 이메일과 비밀번호를 검증하여 로그인 처리[cite: 3]
 */
router.post('/auth/login', async (req, res, next) => {
  try {
    const { email, password } = req.body;

    // 1) 이메일 존재 여부 조회
    const user = await prisma.user.findUnique({ where: { email } });
    
    // 2) 비밀번호 일치 여부 검증
    if (!isPasswordValid) {  //수정: 비밀번호 암호화를 변경함으로써 확인 방식 변경
      return res.status(401).json({ error: '이메일 또는 비밀번호가 올바르지 않습니다.' });
    }

    //JWT 생성 코드
    const accessToken = jwt.sign(
  {
    userId: user.id,
    email: user.email
  },
  process.env.JWT_SECRET,
  {
    expiresIn: '1h'
  }
);

    // 3) 로그인 성공 응답 (유저 정보 반환)[cite: 3]
    res.json({
      message: '로그인 성공',
      accessToken,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        points: user.points
      }
    });
  } catch (error) {
    next(error);
  }
});


// ==============================================================================
// [SECTION 2] 설문조사 관련 API (/api/surveys/...)
// ==============================================================================

/**
 * 2-1. 설문조사 등록 API[cite: 3]
 * @route POST /api/surveys[cite: 3]
 * @desc 설문 제목, 설명, 질문, 선택지 데이터를 한 번에 중첩 저장[cite: 3]
 */
router.post('/surveys', async (req, res, next) => {
  try {
    const { title, description, rewardPoint, authorId, questions } = req.body;

    // Prisma 계층형 관계(Relational Nested Write)를 활용해 설문-질문-옵션을 일괄 생성
    const newSurvey = await prisma.survey.create({
      data: {
        title,
        description,
        rewardPoint: rewardPoint || 100, // 지정값이 없으면 기본 보상 100포인트
        authorId: authorId || 1,        // 작성자 ID (테스트용 기본값 1)
        questions: {
          create: questions ? questions.map(q => ({
            text: q.text,
            type: q.type || 'SINGLE_CHOICE', // 질문 유형 (객관식/주관식 등)[cite: 3]
            options: {
              create: q.options ? q.options.map(opt => ({ text: opt })) : []
            }
          })) : []
        }
      },
      include: {
        questions: {
          include: { options: true }
        }
      }
    });

    res.status(201).json(newSurvey);
  } catch (error) {
    next(error);
  }
});

/**
 * 2-2. 설문조사 전체 목록 조회 API[cite: 3]
 * @route GET /api/surveys[cite: 3]
 * @desc 전체 설문 리스트 및 관련 질문/선택지를 최신순으로 조회[cite: 3]
 */
router.get('/surveys', async (req, res, next) => {
  try {
    const surveys = await prisma.survey.findMany({
      include: {
        author: { select: { name: true } }, // 작성자 이름 포함
        questions: {
          include: { options: true }
        }
      },
      orderBy: { createdAt: 'desc' } // 최신순 정렬
    });

    res.json(surveys);
  } catch (error) {
    next(error);
  }
});

/**
 * 2-3. 설문조사 단건 상세 조회 API[cite: 3]
 * @route GET /api/surveys/:id[cite: 3]
 * @desc 특정 설문 ID의 상세 정보, 질문 목록, 옵션 일체 조회[cite: 3]
 */
router.get('/surveys/:id', async (req, res, next) => {
  try {
    const { id } = req.params;

    const survey = await prisma.survey.findUnique({
      where: { id: parseInt(id) },
      include: {
        author: { select: { name: true } },
        questions: {
          include: { options: true }
        }
      }
    });

    if (!survey) {
      return res.status(404).json({ error: '해당 설문조사를 찾을 수 없습니다.' });
    }

    res.json(survey);
  } catch (error) {
    next(error);
  }
});

/**
 * 2-4. 설문 참여, 답변 제출 및 포인트 보상 지급 API[cite: 3]
 * @route POST /api/surveys/:id/responses[cite: 3]
 * @desc 중복 참여 검증 후 트랜잭션으로 응답 생성 + 유저 포인트 적립 + 이력 저장을 동시 수행[cite: 3]
 */
router.post('/surveys/:id/responses', async (req, res, next) => {
  try {
    const surveyId = parseInt(req.params.id);
    const { userId, answers } = req.body;
    const targetUserId = userId || 1; // 제출 사용자 ID (테스트용 기본값 1)

    // 1) 중복 참여 체크 (명세서 규정: UNIQUE(userId, surveyId) 제약조건 검증)[cite: 3]
    const existingResponse = await prisma.response.findUnique({
      where: {
        userId_surveyId: {
          userId: targetUserId,
          surveyId: surveyId
        }
      }
    });

    if (existingResponse) {
      return res.status(400).json({ error: '이미 참여를 완료한 설문조약입니다.' });
    }

    // 2) DB 트랜잭션($transaction) 처리 - 하나라도 실패하면 원자적으로 전체 롤백[cite: 3]
    const result = await prisma.$transaction(async (tx) => {
      // Step A: 사용자 답변(Response & Answer) 저장[cite: 3]
      const response = await tx.response.create({
        data: {
          userId: targetUserId,
          surveyId: surveyId,
          answers: {
            create: answers ? answers.map(a => ({
              questionId: a.questionId,
              value: a.value
            })) : []
          }
        }
      });

      // Step B: 설문조사의 설정된 보상 포인트 금액 확인
      const survey = await tx.survey.findUnique({ where: { id: surveyId } });
      const reward = survey ? survey.rewardPoint : 100;

      // Step C: User 테이블의 points 컬럼 금액 증가[cite: 3]
      await tx.user.update({
        where: { id: targetUserId },
        data: { points: { increment: reward } }
      });

      // Step D: PointHistory 테이블에 원인과 보상 이력 작성[cite: 3]
      await tx.pointHistory.create({
        data: {
          userId: targetUserId,
          amount: reward,
          type: 'EARNED',
          reason: `설문 참여 보상 (${survey ? survey.title : '설문'})`
        }
      });

      return response;
    });

    // 3) 결과 응답[cite: 3]
    res.status(201).json({
      message: '설문 응답 제출 및 포인트 적립이 성공적으로 처리되었습니다.',
      responseId: result.id
    });
  } catch (error) {
    next(error);
  }
});

// 설정된 라우터 모듈 외부로 내보내기
module.exports = router;