require('dotenv').config();
const path = require('node:path');

let client;

function getPrisma() {
  if (client) return client;
  const url = process.env.DATABASE_URL?.trim();
  const deployed = process.env.NODE_ENV === 'production' || process.env.VERCEL === '1';
  if (url && !/^postgres(?:ql)?:\/\//.test(url)) {
    throw new Error('DATABASE_URL must be a PostgreSQL connection URL');
  }
  if (deployed && !url) {
    throw new Error('Production DATABASE_URL must be configured for PostgreSQL');
  }
  if (url) {
    const { PrismaClient } = require('@prisma/client');
    client = new PrismaClient({ datasources: { db: { url } } });
  } else {
    // SQLite is only for local development. Never create a file DB on Vercel.
    const { PrismaClient } = require('../../node_modules/.prisma/baobab-sqlite');
    const localFile = path.resolve(__dirname, '../../prisma/dev.db').replaceAll('\\', '/');
    client = new PrismaClient({ datasources: { db: { url: `file:${localFile}` } } });
  }
  return client;
}

module.exports = { getPrisma };
