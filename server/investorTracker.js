// Very small stub for investor tracking — in production you'd ingest 13F filings

async function getTopBuysByInstitution(institution = 'Berkshire') {
  // Placeholder: return sample data
  return [
    { symbol: 'OXY', shares: 1000000, action: 'buy' },
    { symbol: 'BRK.B', shares: 50000, action: 'buy' }
  ];
}

module.exports = { getTopBuysByInstitution };

