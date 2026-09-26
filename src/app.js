// 환경변수(.env 파일) 불러오기 설정
require('dotenv').config();

const express = require('express');
const cors = require('cors');

// 작성한 라우터 모듈 불러오기 (survey.routes.js 하나만 사용)
const surveyRoutes = require('./routes/survey.routes');

const app = express();

// 1. 기본 글로벌 미들웨어 설정
app.use(cors());
app.use(express.json());

// [디버깅용 요청 로그 미들웨어]
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

// 2. 라우터 연결
// survey.routes.js 내부에서 /users/register, /surveys 등을 모두 다루도록 연결
app.use('/api', surveyRoutes);

// 3. 중앙 에러 핸들링 미들웨어
app.use((err, req, res, next) => {
  console.error(err);
  const status = err.status || 500;
  const message = err.message || '서버 내부 오류가 발생했습니다.';
  res.status(status).json({ error: message });
});

// 4. Express 서버 실행
const PORT = process.env.PORT || 5000;

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});