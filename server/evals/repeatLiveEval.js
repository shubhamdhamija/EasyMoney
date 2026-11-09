const { execSync } = require('node:child_process');
const path = require('node:path');

const server = path.join(__dirname, '..');
const runs = Number(process.argv[2] || 3);
const tally = { PASS: 0, FAIL: 0, ERROR: 0 };
const issues = {};

for (let i = 1; i <= runs; i += 1) {
  try {
    execSync('npm run eval:live', { cwd: server, stdio: 'ignore' });
  } catch (error) {
    // A non-zero exit just means a case failed; the report is still written.
  }

  const report = JSON.parse(
    require('node:fs').readFileSync(path.join(__dirname, 'stockExplanationReport.live.json'), 'utf8')
  );

  for (const result of report.results) {
    tally[result.status] += 1;

    for (const category of result.errorCategories || []) {
      const key = `${result.caseId}: ${category}`;
      issues[key] = (issues[key] || 0) + 1;
    }
  }

  console.log(`run ${i}/${runs}: ${JSON.stringify(report.summary)}`);
}

console.log('\nCase results across runs:', JSON.stringify(tally));
console.log('Category counts (case: category -> runs):');

for (const [key, count] of Object.entries(issues).sort()) {
  console.log(`  ${key} -> ${count}/${runs}`);
}

