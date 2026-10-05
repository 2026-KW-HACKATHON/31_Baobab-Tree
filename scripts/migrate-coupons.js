const { PrismaClient } = require('@prisma/client');
const fs = require('node:fs');
const path = require('node:path');

let prisma;

async function main() {
  const dbPath = path.resolve('prisma/dev.db');

  if (
    !fs.existsSync('src/app.js') ||
    !fs.existsSync('prisma/schema.prisma') ||
    !fs.existsSync(dbPath)
  ) {
    throw new Error(
      '기존 DB가 있는 프로젝트 최상위 폴더에서 실행해주세요.',
    );
  }

  prisma = new PrismaClient({
    datasources: {
      db: {
        url: `file:${dbPath.replaceAll('\\', '/')}`,
      },
    },
  });

  const backupPath = path.resolve(
    `prisma/dev.before-coupons-${Date.now()}.db`,
  );

  await prisma.$executeRawUnsafe('VACUUM INTO ?', backupPath);
  console.log(`DB 백업 완료: ${backupPath}`);

  await prisma.$transaction(async tx => {
    await tx.$executeRawUnsafe(`
      CREATE TABLE IF NOT EXISTS "COUPON" (
        "id" TEXT NOT NULL PRIMARY KEY,
        "user_id" INTEGER NOT NULL,
        "item_id" TEXT NOT NULL,
        "shop" TEXT NOT NULL,
        "title" TEXT NOT NULL,
        "cost" INTEGER NOT NULL,
        "status" TEXT NOT NULL DEFAULT 'AVAILABLE',
        "request_key" TEXT NOT NULL,
        "created_at" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        "used_at" DATETIME,
        CONSTRAINT "COUPON_user_id_fkey"
          FOREIGN KEY ("user_id")
          REFERENCES "USER" ("user_id")
          ON DELETE RESTRICT
          ON UPDATE CASCADE
      )
    `);

    await tx.$executeRawUnsafe(`
      CREATE UNIQUE INDEX IF NOT EXISTS
        "COUPON_user_id_request_key_key"
      ON "COUPON" ("user_id", "request_key")
    `);

    await tx.$executeRawUnsafe(`
      CREATE INDEX IF NOT EXISTS
        "COUPON_user_id_created_at_idx"
      ON "COUPON" ("user_id", "created_at")
    `);
  });

  console.log('쿠폰 테이블 추가 완료.');
}

main()
  .catch(error => {
    console.error(error.message);
    process.exitCode = 1;
  })
  .finally(async () => {
    if (prisma) await prisma.$disconnect();
  });