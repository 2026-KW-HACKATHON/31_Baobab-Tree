const { test } = require('node:test');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const config = require.resolve('../src/config/auth');
test('production refuses missing, short and development JWT secrets', () => {
  for (const secret of ['', 'short', 'local-development-secret']) {
    const result = spawnSync(process.execPath, ['-e', 'require(process.argv[1])', config], {
      env: { ...process.env, NODE_ENV: 'production', JWT_SECRET: secret }, encoding: 'utf8'
    });
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /Production JWT_SECRET/);
  }
  const result = spawnSync(process.execPath, ['-e', 'require(process.argv[1])', config], {
    env: { ...process.env, NODE_ENV: 'production', JWT_SECRET: 'test-only-private-secret-at-least-32-characters' }
  });
  assert.equal(result.status, 0);
});
