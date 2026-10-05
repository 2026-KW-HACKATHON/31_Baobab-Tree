require('dotenv').config();
const { spawnSync } = require('node:child_process');
if (!/^postgres(?:ql)?:\/\//.test(process.env.TEST_DATABASE_URL || '')) {
  console.error('Set TEST_DATABASE_URL to a PostgreSQL test database URL. A temporary schema is created and removed; application data is never reset.');
  process.exit(1);
}
const result = spawnSync(process.execPath, ['--test', 'test/api.test.js'], {
  stdio: 'inherit', env: { ...process.env, BAOBAB_TEST_POSTGRES: '1' },
});
if (result.error) throw result.error;
process.exitCode = result.status ?? 1;
