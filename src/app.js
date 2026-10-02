require('dotenv').config();

const express = require('express');
const cors = require('cors');
const surveyRoutes = require('./routes/survey.routes');

const app = express();

app.use(cors());
app.use(express.json());

app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

app.use('/api', surveyRoutes);

app.use((req, res) => {
  res.status(404).json({
    error: '요청한 API를 찾을 수 없습니다.'
  });
});

app.use((err, req, res, next) => {
  console.error(err);

  const status =
    Number.isInteger(err.status) &&
    err.status >= 400 &&
    err.status < 600
      ? err.status
      : 500;

  const message =
    status === 500
      ? '서버 내부 오류가 발생했습니다.'
      : err.message;

  res.status(status).json({ error: message });
});

const PORT = process.env.PORT || 5000;

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});