const { test } = require('node:test');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const app = require('../src/app');

test('Vercel entry exports an Express handler and readiness checks the injected DB', async t => {
  assert.equal(typeof app, 'function');
  let queries = 0;
  const server = app.createApp({ prisma: { async $queryRaw() { queries++; return [{ value: 1 }]; } } }).listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  t.after(() => new Promise(resolve => server.close(resolve)));
  const response = await fetch(`http://127.0.0.1:${server.address().port}/api/health`);
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { status: 'ok', database: 'connected' });
  assert.equal(queries, 1);
});

test('production refuses absent or non-PostgreSQL DATABASE_URL; DB client is reused locally', () => {
  const config = require.resolve('../src/lib/prisma');
  for (const url of ['', 'file:./dev.db']) {
    const result = spawnSync(process.execPath, ['-e', 'require(process.argv[1]).getPrisma()', config], {
      env: { ...process.env, NODE_ENV: 'production', DATABASE_URL: url }, encoding: 'utf8',
    });
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /DATABASE_URL/);
  }
  const result = spawnSync(process.execPath, ['-e', 'const p=require(process.argv[1]); if(p.getPrisma()!==p.getPrisma()) process.exit(1); p.getPrisma().$disconnect()', config], {
    env: { ...process.env, NODE_ENV: 'development', VERCEL: '', DATABASE_URL: '' }, encoding: 'utf8',
  });
  assert.equal(result.status, 0, result.stderr);
});

test('PostgreSQL, local SQLite and Backend mirror keep the same API models', () => {
  const read = name => fs.readFileSync(path.join(__dirname, '../prisma', name), 'utf8');
  const postgres = read('schema.prisma');
  const sqlite = read('schema.sqlite.prisma');
  assert.equal(postgres.slice(postgres.indexOf('model User')), sqlite.slice(sqlite.indexOf('model User')));
  assert.equal(postgres, fs.readFileSync(path.join(__dirname, '../Backend/prisma/schema.prisma'), 'utf8'));
});
