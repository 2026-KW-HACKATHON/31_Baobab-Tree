const { PrismaClient } = require('@prisma/client');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { JWT_SECRET } = require('../config/auth');

const userSelect = {
  id: true, email: true, loginId: true, name: true,
  ageGroup: true, region: true, point: true, createdAt: true,
};
const surveyInclude = {
  author: { select: { id: true, name: true } },
  questions: { orderBy: { id: 'asc' }, include: { options: { orderBy: { id: 'asc' } } } },
};
const fail = (status, message) => { throw Object.assign(new Error(message), { status }); };
function id(value) {
  if (!/^\d+$/.test(String(value)) || !Number.isSafeInteger(Number(value)) || Number(value) < 1 || Number(value) > 2147483647) {
    fail(400, 'Invalid ID');
  }
  return Number(value);
}
function text(value, field) {
  if (typeof value !== 'string' || !value.trim()) fail(400, `${field} is required`);
  return value.trim();
}
function count(value, field) {
  if (!Number.isInteger(value) || value < 0 || value > 2147483647) fail(400, `${field} must be a nonnegative integer`);
  return value;
}
function date(value) {
  if (value === null) return null;
  if (typeof value !== 'string' || !value.trim() || !Number.isFinite(new Date(value).getTime())) fail(400, 'Invalid endDate');
  return new Date(value);
}
function questionData(data) {
  if (!data || typeof data !== 'object') fail(400, 'Invalid question');
  const questionType = data.questionType ?? 'single';
  if (!['single', 'short'].includes(questionType)) fail(400, 'questionType must be single or short');
  const options = data.options ?? [];
  if (!Array.isArray(options)) fail(400, 'options must be an array');
  const labels = options.map(o => text(o && o.optionText, 'optionText'));
  if (questionType === 'single' && (labels.length < 2 || new Set(labels).size !== labels.length)) {
    fail(400, 'Single-choice questions need at least two distinct options');
  }
  if (questionType === 'short' && labels.length) fail(400, 'Short-answer questions cannot have options');
  return { question: text(data.question, 'question'), questionType, options: { create: labels.map(optionText => ({ optionText })) } };
}
function surveyData(data, partial = false) {
  const result = {};
  if (!partial || data.title !== undefined) result.title = text(data.title, 'title');
  if (data.category !== undefined) result.category = data.category === null ? null : text(data.category, 'category');
  for (const field of ['rewardPoint', 'targetCount']) {
    if (data[field] !== undefined) result[field] = count(data[field], field);
  }
  if (data.status !== undefined) {
    if (!['OPEN', 'CLOSED'].includes(data.status)) fail(400, 'status must be OPEN or CLOSED');
    result.status = data.status;
  }
  if (data.endDate !== undefined) result.endDate = date(data.endDate);
  return result;
}

class SurveyService {
  constructor(prisma = new PrismaClient()) { this.prisma = prisma; }

  async signup(data) {
    const email = text(data.email, 'email');
    const loginId = text(data.loginId, 'loginId');
    const name = text(data.name, 'name');
    text(data.password, 'password');
    const password = data.password;
    if (data.ageGroup !== undefined && data.ageGroup !== null) text(data.ageGroup, 'ageGroup');
    if (data.region !== undefined && data.region !== null) text(data.region, 'region');
    const user = await this.prisma.user.create({
      data: { email, loginId, name, password: await bcrypt.hash(password, 10), ageGroup: data.ageGroup, region: data.region },
      select: userSelect,
    });
    return { message: 'Signed up', user };
  }

  async login(data) {
    text(data.password, 'password');
    const password = data.password;
    const where = data.loginId !== undefined
      ? { loginId: text(data.loginId, 'loginId') }
      : { email: text(data.email, 'email') };
    const user = await this.prisma.user.findUnique({ where });
    if (!user || !(await bcrypt.compare(password, user.password))) fail(401, 'Invalid credentials');
    const accessToken = jwt.sign({ userId: user.id }, JWT_SECRET, { expiresIn: '1d' });
    const { password: omitted, ...safeUser } = user;
    return { accessToken, user: safeUser };
  }

  async getUserMe(userId) {
    const user = await this.prisma.user.findUnique({ where: { id: id(userId) }, select: userSelect });
    if (!user) fail(404, 'User not found');
    return user;
  }

  async getUserResponses(userId) {
    await this.getUserMe(userId);
    return this.prisma.response.findMany({
      where: { userId: id(userId) }, orderBy: { createdAt: 'desc' },
      include: { survey: true, answers: { include: { question: true } } },
    });
  }

  async getUserPoints(userId) {
    const user = await this.getUserMe(userId);
    const histories = await this.prisma.pointHistory.findMany({ where: { userId: id(userId) }, orderBy: { id: 'desc' } });
    return { point: user.point, histories };
  }

  async getSurveys({ category, status, search } = {}) {
    const where = {};
    if (category !== undefined) where.category = text(category, 'category');
    if (status !== undefined) {
      if (!['OPEN', 'CLOSED'].includes(status)) fail(400, 'Invalid status');
      where.status = status;
    }
    if (search !== undefined && typeof search !== 'string') fail(400, 'Invalid search');
    if (search && search.trim()) where.OR = [
      { title: { contains: search.trim() } }, { author: { name: { contains: search.trim() } } },
    ];
    return this.prisma.survey.findMany({ where, include: surveyInclude, orderBy: { id: 'desc' } });
  }

  async getSurveyDetail(surveyId) {
    const survey = await this.prisma.survey.findUnique({ where: { id: id(surveyId) }, include: surveyInclude });
    if (!survey) fail(404, 'Survey not found');
    return survey;
  }

  async ownedSurvey(userId, surveyId) {
    const survey = await this.getSurveyDetail(surveyId);
    if (survey.userId !== id(userId)) fail(403, 'Only the author can perform this action');
    return survey;
  }

  async createSurvey(userId, data) {
    if (!Array.isArray(data.questions) || !data.questions.length) fail(400, 'questions must be a nonempty array');
    return this.prisma.survey.create({
      data: { ...surveyData(data), userId: id(userId), questions: { create: data.questions.map(questionData) } },
      include: surveyInclude,
    });
  }

  async updateSurvey(userId, surveyId, data) {
    await this.ownedSurvey(userId, surveyId);
    return this.prisma.survey.update({ where: { id: id(surveyId) }, data: surveyData(data, true), include: surveyInclude });
  }

  async deleteSurvey(userId, surveyId) {
    await this.ownedSurvey(userId, surveyId);
    await this.prisma.survey.delete({ where: { id: id(surveyId) } });
    return { message: 'Survey deleted' };
  }

  async editableSurvey(userId, surveyId) {
    const survey = await this.ownedSurvey(userId, surveyId);
    if (await this.prisma.response.count({ where: { surveyId: survey.id } })) fail(409, 'Questions cannot change after responses exist');
    return survey;
  }

  async addQuestion(userId, surveyId, data) {
    await this.editableSurvey(userId, surveyId);
    return this.prisma.question.create({ data: { ...questionData(data), surveyId: id(surveyId) }, include: { options: true } });
  }

  async updateQuestion(userId, questionId, data) {
    const question = await this.prisma.question.findUnique({ where: { id: id(questionId) }, include: { options: true } });
    if (!question) fail(404, 'Question not found');
    await this.editableSurvey(userId, question.surveyId);
    const merged = questionData({ ...question, ...data });
    return this.prisma.question.update({
      where: { id: question.id },
      data: { ...merged, options: { deleteMany: {}, ...merged.options } }, include: { options: true },
    });
  }

  async deleteQuestion(userId, questionId) {
    const question = await this.prisma.question.findUnique({ where: { id: id(questionId) } });
    if (!question) fail(404, 'Question not found');
    const survey = await this.editableSurvey(userId, question.surveyId);
    if (survey.questions.length <= 1) fail(400, 'A survey must have at least one question');
    await this.prisma.question.delete({ where: { id: question.id } });
    return { message: 'Question deleted' };
  }

  async submitResponse(userId, surveyId, answers) {
    const participantId = id(userId);
    const targetSurveyId = id(surveyId);
    if (!Array.isArray(answers)) fail(400, 'answers must be an array');
    return this.prisma.$transaction(async tx => {
      const survey = await tx.survey.findUnique({ where: { id: targetSurveyId }, include: surveyInclude });
      if (!survey) fail(404, 'Survey not found');
      if (survey.status !== 'OPEN' || (survey.endDate && survey.endDate <= new Date())) fail(400, 'Survey is closed');
      const existing = await tx.response.findUnique({ where: { userId_surveyId: { userId: participantId, surveyId: targetSurveyId } } });
      if (existing) fail(409, 'Already participated');
      if (!survey.questions.length || answers.length !== survey.questions.length) fail(400, 'Answer every question exactly once');
      const seen = new Set();
      const normalized = answers.map(a => {
        if (!a || typeof a !== 'object') fail(400, 'Invalid answer');
        const questionId = id(a.questionId);
        const question = survey.questions.find(q => q.id === questionId);
        if (!question || seen.has(questionId)) fail(400, 'Invalid or duplicate questionId');
        seen.add(questionId);
        const answer = text(a.answer, 'answer');
        if (question.questionType === 'single' && !question.options.some(o => o.optionText === answer)) fail(400, 'Invalid option');
        return { questionId, answer };
      });
      const updated = await tx.survey.updateMany({
        where: { id: targetSurveyId, status: 'OPEN',
          OR: [{ endDate: null }, { endDate: { gt: new Date() } }],
          ...(survey.targetCount > 0 ? { currentCount: { lt: survey.targetCount } } : {}),
        }, data: { currentCount: { increment: 1 } },
      });
      if (!updated.count) fail(400, 'Survey is closed or full');
      const response = await tx.response.create({
        data: { userId: participantId, surveyId: targetSurveyId, answers: { create: normalized } },
      });
      const user = await tx.user.update({ where: { id: participantId }, data: { point: { increment: survey.rewardPoint } } });
      if (survey.rewardPoint > 0) await tx.pointHistory.create({
        data: { userId: participantId, amount: survey.rewardPoint, description: `Survey reward: ${survey.title}` },
      });
      return { message: 'Response submitted', responseId: response.id, rewardPoint: survey.rewardPoint, point: user.point };
    });
  }

  async getSurveyResults(userId, surveyId) {
    await this.ownedSurvey(userId, surveyId);
    const survey = await this.prisma.survey.findUnique({
      where: { id: id(surveyId) }, include: { questions: { orderBy: { id: 'asc' }, include: { options: true, answers: true } } },
    });
    const totalResponses = await this.prisma.response.count({ where: { surveyId: survey.id } });
    return { surveyId: survey.id, totalResponses, questions: survey.questions.map(q => {
      const counts = new Map(q.options.map(o => [o.optionText, 0]));
      q.answers.forEach(a => counts.set(a.answer, (counts.get(a.answer) || 0) + 1));
      return { questionId: q.id, results: [...counts].map(([option, value]) => ({ option, count: value,
        percentage: totalResponses ? Math.round(value / totalResponses * 100) : 0 })) };
    }) };
  }
}

module.exports = { SurveyService };
