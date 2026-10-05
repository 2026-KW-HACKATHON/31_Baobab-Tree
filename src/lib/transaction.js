// Retry only transactions that the DB has definitely rolled back.
// Callbacks must contain DB operations only, never payment-provider requests.
async function transaction(prisma, action) {
  for (let attempt = 0; ; attempt++) {
    try {
      return await prisma.$transaction(action, {
        isolationLevel: 'Serializable', maxWait: 10000, timeout: 10000,
      });
    } catch (error) {
      if (error.code !== 'P2034' || attempt >= 2) throw error;
    }
  }
}

module.exports = { transaction };
