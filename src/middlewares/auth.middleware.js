const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET;

if (!JWT_SECRET) {
  throw new Error('JWT_SECRET 환경변수를 설정해 주세요.');
}

module.exports = function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;

  if (!authHeader || !/^Bearer\s+\S+$/i.test(authHeader)) {
    return res.status(401).json({
      message: '인증 토큰이 누락되었습니다.'
    });
  }

  const token = authHeader.split(/\s+/)[1];

  try {
    const decoded = jwt.verify(token, JWT_SECRET);

    if (
      !Number.isSafeInteger(decoded.userId) ||
      decoded.userId < 1
    ) {
      return res.status(401).json({
        message: '유효하지 않은 토큰입니다.'
      });
    }

    req.user = decoded;
    next();
  } catch (error) {
    return res.status(401).json({
      message: '유효하지 않거나 만료된 토큰입니다.'
    });
  }
};