require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { createSurveyRouter } = require('./routes/survey.routes');

function createApp(service) {
  const app = express();
  app.use(cors());
  app.use(express.json());
  app.use('/api', createSurveyRouter(service));
  app.use((req, res) => res.status(404).json({ error: 'Endpoint not found' }));
  app.use((err, req, res, next) => {
    const status = err.status || ({ P2002: 409, P2025: 404, P2003: 400 }[err.code]) || 500;
    const message = { P2002: 'Duplicate record', P2025: 'Record not found', P2003: 'Invalid reference' }[err.code]
      || (status < 500 ? err.message : 'Internal server error');
    if (status >= 500) console.error(err);
    res.status(status).json({ error: message });
  });
  return app;
}
function start() {
  return createApp().listen(process.env.PORT || 5000, () => console.log('Survey API is running'));
}
if (require.main === module) start();
module.exports = { createApp, start };
