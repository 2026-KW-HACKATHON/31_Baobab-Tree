// JWT 토큰 검증 라이브러리 불러오기
const jwt = require('jsonwebtoken');

// .env 파일에서 시크릿 키를 가져오고, 없으면 임시 키 사용 (토큰 암호화/복호화용)
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key';

// [사용자 인증 미들웨어]
// - 로그인한 유저만 접근할 수 있는 API 전에 실행되어서 토큰이 맞는지 검사함
module.exports = function authMiddleware(req, res, next) {
  // 1. 클라이언트가 헤더에 실어 보낸 authorization 값 가져오기
  const authHeader = req.headers.authorization;

  // 2. 헤더가 없거나 'Bearer ' 형태가 아니면 로그인 안 된 것으로 판단해서 401 에러 리턴
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ message: '인증 토큰이 누락되었습니다.' });
  }

  // 3. 'Bearer eyJhbGci...' 형태에서 공백 기준 오른쪽 진짜 토큰 값만 추출
  const token = authHeader.split(' ')[1];

  try {
    // 4. 토큰이 유효한지 비밀키로 검증하고 내용(payload) 해독하기
    const decoded = jwt.verify(token, JWT_SECRET);

    // 5. 다음 미들웨어나 라우터에서 유저 정보를 쓸 수 있게 req.user에 저장 (ex: { userId: 1 })
    req.user = decoded; 

    // 6. 통과했으니 다음 컨트롤러/서비스 로직으로 넘어가기
    next();
  } catch (error) {
    // 5. 토큰이 조작되었거나 만료시간이 지난 경우 예외 처리
    return res.status(401).json({ message: '유효하지 않거나 만료된 토큰입니다.' });
  }
};