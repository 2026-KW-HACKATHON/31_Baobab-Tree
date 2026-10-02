const express = require('express');
const service = require('../services/survey.service');
const auth = require('../middlewares/auth.middleware');

const router = express.Router();

// Express 4에서도 async 함수의 오류를 중앙 오류 처리기로 전달
const asyncRoute = handler => (req, res, next) =>
  Promise.resolve(handler(req, res)).catch(next);

const requiredId = value => {
  const id = Number(value);

  if (!Number.isSafeInteger(id) || id < 1) {
    throw { status: 400, message: '올바른 ID를 입력해 주세요.' };
  }

  return id;
};

// 회원가입
router.post('/auth/signup', asyncRoute(async (req, res) => {
  const { loginId, password, name } = req.body;

  if (
    typeof loginId !== 'string' || !loginId.trim() ||
    typeof password !== 'string' || !password ||
    typeof name !== 'string' || !name.trim()
  ) {
    throw {
      status: 400,
      message: '아이디, 비밀번호, 이름은 필수 입력 항목입니다.'
    };
  }

  res.status(201).json(
    await service.signup({
      ...req.body,
      loginId: loginId.trim()
    })
  );
}));

router.post('/auth/login', asyncRoute(async (req, res) => {
  const { loginId, password } = req.body;

  if (
    typeof loginId !== 'string' || !loginId.trim() ||
    typeof password !== 'string' || !password
  ) {
    throw {
      status: 400,
      message: '아이디와 비밀번호를 입력해 주세요.'
    };
  }

  res.json(
    await service.login({
      loginId: loginId.trim(),
      password
    })
  );
}));
// 내 정보
router.get('/users/me', auth, asyncRoute(async (req, res) => {
  res.json(await service.getUserMe(req.user.userId));
}));

router.get('/users/me/responses', auth, asyncRoute(async (req, res) => {
  res.json(await service.getUserResponses(req.user.userId));
}));

router.get('/users/me/points', auth, asyncRoute(async (req, res) => {
  res.json(await service.getUserPoints(req.user.userId));
}));

// 설문 목록
router.get('/surveys', asyncRoute(async (req, res) => {
  res.json(await service.getSurveys(req.query));
}));

// 설문 생성
router.post('/surveys', auth, asyncRoute(async (req, res) => {
  const data = {
    ...req.body,
    reward_point: req.body.reward_point ?? req.body.rewardPoint,
    target_headcount: req.body.target_headcount ?? req.body.targetCount
  };

  res.status(201).json(
    await service.createSurvey(req.user.userId, data)
  );
}));

// 설문 결과
router.get('/surveys/:id/results', asyncRoute(async (req, res) => {
  res.json(await service.getSurveyResults(requiredId(req.params.id)));
}));

// 설문 상세
router.get('/surveys/:id', asyncRoute(async (req, res) => {
  res.json(await service.getSurveyDetail(requiredId(req.params.id)));
}));

// 설문 수정
router.patch('/surveys/:id', auth, asyncRoute(async (req, res) => {
  res.json(
    await service.updateSurvey(
      req.user.userId,
      requiredId(req.params.id),
      req.body
    )
  );
}));

// 설문 삭제
router.delete('/surveys/:id', auth, asyncRoute(async (req, res) => {
  res.json(
    await service.deleteSurvey(
      req.user.userId,
      requiredId(req.params.id)
    )
  );
}));

// 질문 추가
router.post('/surveys/:id/questions', auth, asyncRoute(async (req, res) => {
  res.status(201).json(
    await service.addQuestion(
      req.user.userId,
      requiredId(req.params.id),
      req.body
    )
  );
}));

// 질문 수정
router.patch('/questions/:id', auth, asyncRoute(async (req, res) => {
  res.json(
    await service.updateQuestion(
      req.user.userId,
      requiredId(req.params.id),
      req.body
    )
  );
}));

// 질문 삭제
router.delete('/questions/:id', auth, asyncRoute(async (req, res) => {
  res.json(
    await service.deleteQuestion(
      req.user.userId,
      requiredId(req.params.id)
    )
  );
}));

// 설문 응답 제출
router.post('/surveys/:id/responses', auth, asyncRoute(async (req, res) => {
  const result = await service.submitResponse(
    req.user.userId,
    requiredId(req.params.id),
    req.body.answers
  );

  res.status(201).json(result);
}));

module.exports = router;