const cron = require('node-cron');
const { getAllUsers } = require('./usersService');
const { generateWatchlistSummary } = require('./watchlistService');
const portfolioService = require('./portfolioService');
const sectorAnalysis = require('./sectorAnalysis');
const { sendFCM } = require('./notificationService');

async function runDailyBriefing() {
  console.log('Daily briefing started');
  const users = await getAllUsers();
  for (const user of users) {
    try {
      const summary = await generateWatchlistSummary(user.id);
      const portfolio = await portfolioService.generatePortfolio({ risk: 'medium' });
      const sectors = await sectorAnalysis.analyzeSectors();
      const sectorSummary = sectors.sort((a,b) => b.score - a.score).slice(0,3).map(s => s.sector).join(', ');

      const message = {
        title: '🧠 Daily Market Brief',
        body: `${summary}\nTop Picks: ${portfolio.portfolio.slice(0,3).join(', ')}\nSectors: ${sectorSummary}`
      };

      if (user.deviceToken) {
        await sendFCM(user.deviceToken, message);
      } else {
        console.log(`Would notify ${user.id}:`, message);
      }
    } catch (e) {
      console.error('Error generating briefing for', user.id, e.message || e);
    }
  }
}

// schedule: 9am server local time (cron format)
cron.schedule('0 9 * * *', async () => {
  try {
    await runDailyBriefing();
  } catch (e) {
    console.error('Daily briefing failed', e.message || e);
  }
});

// Allow manual run
if (require.main === module) {
  runDailyBriefing().then(() => console.log('Manual run complete')).catch(e => console.error(e));
}

module.exports = { runDailyBriefing };

