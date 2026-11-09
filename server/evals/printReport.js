// Prints the latest evaluation report for manual review.
// Usage: node evals/printReport.js
const path = require('node:path');

// Usage: AI_EVAL_MODE=live node evals/printReport.js (default: live)
const mode = process.env.AI_EVAL_MODE || 'live';
const report = require(path.join(__dirname, `stockExplanationReport.${mode}.json`));

console.log(`Mode: ${report.mode} | Generated: ${report.generatedAt}`);
console.log(JSON.stringify(report.summary, null, 2));

for (const result of report.results) {
  console.log(`\n[${result.caseId}] ${result.status}`);

  if (result.metadata) {
    console.log(`  metadata: ${JSON.stringify(result.metadata)}`);
  }

  if (result.error) {
    console.log(`  error: ${JSON.stringify(result.error)}`);
  }

  if (result.explanation) {
    console.log(`  ${result.explanation.evidenceSentiment} | ${result.explanation.summary}`);

    for (const factor of result.explanation.factors || []) {
      console.log(`   - ${factor}`);
    }
  }

  const failed = (result.scoring?.checks || []).filter(check => !check.passed);

  if (failed.length > 0) {
    console.log(`  FAILED CHECKS: ${JSON.stringify(failed)}`);
  }

  if (result.errorCategories?.length) {
    console.log(`  categories: ${result.errorCategories.join(', ')}`);
  }
}

