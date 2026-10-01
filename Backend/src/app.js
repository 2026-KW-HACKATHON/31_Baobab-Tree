const backend = require('../../src/app');
if (require.main === module) backend.start();
module.exports = backend;
