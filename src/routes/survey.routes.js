const express = require('express');
const { SurveyService } = require('../services/survey.service');
const auth = require('../middlewares/auth.middleware');

function createSurveyRouter(service = new SurveyService()) {
  const router = express.Router();
  const handle = (status, action) => async (req, res, next) => {
    try { res.status(status).json(await action(req)); } catch (error) { next(error); }
  };
  router.post('/auth/signup', handle(201, req => service.signup((req.body || {}))));
  router.post('/auth/login', handle(200, req => service.login((req.body || {}))));
  // Stateless JWT logout: clients discard their token; existing tokens expire normally.
  router.post('/auth/logout', auth, handle(200, () => ({ message: 'Discard the access token on the client' })));
  router.get('/users/me', auth, handle(200, req => service.getUserMe(req.user.userId)));
  router.get('/users/me/responses', auth, handle(200, req => service.getUserResponses(req.user.userId)));
  router.get('/users/me/points', auth, handle(200, req => service.getUserPoints(req.user.userId)));
  router.get('/surveys', handle(200, req => service.getSurveys(req.query)));
  router.get('/surveys/:id', handle(200, req => service.getSurveyDetail(req.params.id)));
  router.post('/surveys', auth, handle(201, req => service.createSurvey(req.user.userId, (req.body || {}))));
  router.patch('/surveys/:id', auth, handle(200, req => service.updateSurvey(req.user.userId, req.params.id, (req.body || {}))));
  router.delete('/surveys/:id', auth, handle(200, req => service.deleteSurvey(req.user.userId, req.params.id)));
  router.post('/surveys/:id/questions', auth, handle(201, req => service.addQuestion(req.user.userId, req.params.id, (req.body || {}))));
  router.patch('/questions/:id', auth, handle(200, req => service.updateQuestion(req.user.userId, req.params.id, (req.body || {}))));
  router.delete('/questions/:id', auth, handle(200, req => service.deleteQuestion(req.user.userId, req.params.id)));
  router.post('/surveys/:id/responses', auth, handle(201, req => service.submitResponse(req.user.userId, req.params.id, (req.body || {}).answers)));
  router.get('/surveys/:id/results', auth, handle(200, req => service.getSurveyResults(req.user.userId, req.params.id)));
  return router;
}
module.exports = { createSurveyRouter };
