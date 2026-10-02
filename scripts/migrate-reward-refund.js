const { PrismaClient } = require('@prisma/client');
const path = require('node:path');
const fs = require('node:fs');
const prisma = new PrismaClient();
async function main() {
  const db = path.join(__dirname, '../prisma/dev.db');
  if (!fs.existsSync(db)) throw new Error('Existing prisma/dev.db is required');
  const backup = path.join(__dirname, '../prisma/dev.before-reward-refund-' + Date.now() + '.db');
  await prisma.$executeRawUnsafe('VACUUM INTO ?', backup);
  await prisma.$transaction(async tx => {
    const columns = await tx.$queryRawUnsafe('PRAGMA table_info("SURVEY")');
    if (columns.some(column => column.name === 'fundedReward')) return;
    await tx.$executeRawUnsafe('ALTER TABLE "SURVEY" ADD COLUMN "fundedReward" INTEGER NOT NULL DEFAULT 0');
    const surveys = await tx.$queryRawUnsafe('SELECT * FROM "SURVEY"');
    const charges = await tx.$queryRawUnsafe('SELECT * FROM "POINT_HISTORY" WHERE amount < 0');
    const assigned = new Set();
    for (const survey of surveys) {
      const budget = survey.reward_point * survey.target_count;
      if (!budget) continue;
      const matches = charges.filter(charge => charge.user_id === survey.user_id && charge.amount === -budget &&
        charge.description === 'Survey registration reward budget: ' + survey.title &&
        Math.abs(new Date(charge.created_at).getTime() - new Date(survey.created_at).getTime()) < 5000);
      if (matches.length > 1 || matches.some(charge => assigned.has(charge.history_id))) throw new Error('Ambiguous historic reward charge; manual reconciliation required');
      if (matches.length === 1) {
        assigned.add(matches[0].history_id);
        await tx.$executeRawUnsafe('UPDATE "SURVEY" SET "fundedReward" = ? WHERE survey_id = ?', budget, survey.survey_id);
      }
    }
  });
  console.log('Reward refund migration complete. Backup: ' + backup);
}
main().catch(error => { console.error(error.message); process.exitCode = 1; }).finally(() => prisma.$disconnect());
