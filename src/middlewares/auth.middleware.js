const jwt = require('jsonwebtoken');
const { JWT_SECRET } = require('../config/auth');

module.exports = function authMiddleware(req, res, next) {
  const match = /^Bearer\s+(\S+)$/i.exec(req.headers.authorization || '');
  if (!match) return res.status(401).json({ error: 'Bearer token required' });
  try {
    const decoded = jwt.verify(match[1], JWT_SECRET, { algorithms: ['HS256'] });
    if (!Number.isSafeInteger(decoded.userId) || decoded.userId < 1) throw new Error('Invalid userId');
    req.user = { userId: decoded.userId };
    next();
  } catch {
    res.status(401).json({ error: 'Invalid or expired token' });
  }
};
