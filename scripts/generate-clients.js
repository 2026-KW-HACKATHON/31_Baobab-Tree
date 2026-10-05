const { execFileSync } = require('node:child_process');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const cli = require.resolve('prisma/build/index.js');

function generate(schema) {
  execFileSync(process.execPath, [cli, 'generate', '--schema', schema], { cwd: root, stdio: 'inherit' });
}

generate('prisma/schema.prisma');
if (process.env.VERCEL !== '1') generate('prisma/schema.sqlite.prisma');
