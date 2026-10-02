const configuredSecret = process.env.JWT_SECRET;
if (process.env.NODE_ENV === 'production' &&
    (!configuredSecret || configuredSecret.length < 32 || configuredSecret === 'local-development-secret')) {
  throw new Error('Production JWT_SECRET must be a private secret of at least 32 characters');
}
module.exports = { JWT_SECRET: configuredSecret || 'local-development-secret' };
