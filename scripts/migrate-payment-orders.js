// This script upgrades the existing local SQLite file only.
const { PrismaClient } = require('../node_modules/.prisma/baobab-sqlite');
const fs = require('node:fs');
const path = require('node:path');

const dbPath = path.resolve(__dirname, '../prisma/dev.db');
const backupPath = path.resolve(
  __dirname,
  `../prisma/dev.before-payments-${Date.now()}.db`
);

let prisma;

async function main() {
  // DB가 없을 때 빈 DB를 생성하지 않도록 먼저 확인합니다.
  if (!fs.existsSync(dbPath)) {
    throw new Error('기존 prisma/dev.db 파일을 찾을 수 없습니다.');
  }

  prisma = new PrismaClient({
    datasources: {
      db: {
        url: `file:${dbPath.replaceAll('\\', '/')}`,
      },
    },
  });

  // SQLite가 일관된 상태의 백업을 생성합니다.
  await prisma.$executeRawUnsafe('VACUUM INTO ?', backupPath);
  console.log(`백업 완료: ${backupPath}`);

  await prisma.$transaction(async (tx) => {
    const tables = [
      'USER',
      'SURVEY',
      'QUESTION',
      'SURVEY_OPTION',
      'RESPONSE',
      'ANSWER',
      'POINT_HISTORY',
    ];

    const before = {};

    for (const table of tables) {
      const rows = await tx.$queryRawUnsafe(
        `SELECT COUNT(*) AS count FROM "${table}"`
      );
      before[table] = rows[0].count;
    }

    await tx.$executeRawUnsafe(`
      CREATE TABLE IF NOT EXISTS "PAYMENT_ORDER" (
        "id" TEXT NOT NULL PRIMARY KEY,
        "user_id" INTEGER NOT NULL,
        "provider" TEXT NOT NULL,
        "amount" INTEGER NOT NULL,
        "credit_point" INTEGER NOT NULL,
        "status" TEXT NOT NULL DEFAULT 'CREATED',
        "provider_ref" TEXT,
        "checkout_url" TEXT,
        "callback_hash" TEXT NOT NULL,
        "approved_at" DATETIME,
        "credited_at" DATETIME,
        "created_at" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        "updated_at" DATETIME NOT NULL,
        CONSTRAINT "PAYMENT_ORDER_user_id_fkey"
          FOREIGN KEY ("user_id") REFERENCES "USER" ("user_id")
          ON DELETE RESTRICT ON UPDATE CASCADE
      )
    `);

    await tx.$executeRawUnsafe(`
      CREATE UNIQUE INDEX IF NOT EXISTS
        "PAYMENT_ORDER_provider_provider_ref_key"
      ON "PAYMENT_ORDER" ("provider", "provider_ref")
    `);

    await tx.$executeRawUnsafe(`
      CREATE INDEX IF NOT EXISTS
        "PAYMENT_ORDER_user_id_created_at_idx"
      ON "PAYMENT_ORDER" ("user_id", "created_at")
    `);

    const columns = await tx.$queryRawUnsafe(
      'PRAGMA table_info("POINT_HISTORY")'
    );

    if (!columns.some((column) => column.name === 'payment_order_id')) {
      await tx.$executeRawUnsafe(`
        ALTER TABLE "POINT_HISTORY"
        ADD COLUMN "payment_order_id" TEXT
        REFERENCES "PAYMENT_ORDER" ("id")
        ON DELETE RESTRICT ON UPDATE CASCADE
      `);
    }

    await tx.$executeRawUnsafe(`
      CREATE UNIQUE INDEX IF NOT EXISTS
        "POINT_HISTORY_payment_order_id_key"
      ON "POINT_HISTORY" ("payment_order_id")
    `);

    // 기존 데이터가 보존됐는지 확인합니다.
    for (const table of tables) {
      const rows = await tx.$queryRawUnsafe(
        `SELECT COUNT(*) AS count FROM "${table}"`
      );

      if (rows[0].count !== before[table]) {
        throw new Error(`${table}의 데이터 건수가 달라졌습니다.`);
      }
    }
  }, {
    timeout: 15000,
  });

  console.log('결제 테이블 추가 완료. 기존 데이터 건수 유지 확인.');
}

main()
  .catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  })
  .finally(async () => {
    if (prisma) await prisma.$disconnect();
  });