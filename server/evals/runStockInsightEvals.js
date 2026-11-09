require('dotenv').config();

const fs = require('node:fs');
const path = require('node:path');
const { performance } = require('node:perf_hooks');

const { createAppDependencies } = require('../appDependencies');
const { createFakeLlmClient } = require('../services/llm/fakeLlmClient');
const { createStockInsightService } = require('../services/stockInsightService');
const { FORECAST_PATTERNS } = require('../services/stockInsightOutputPolicy');
const {
  scoreExplanation,
  buildResultRecord,
  summariseQuality,
  summariseErrorCategories
} = require('./runStockExplanationEvals');

const EVALUATION_MODE = process.env.AI_EVAL_MODE || 'fake';
const CASES_PATH = path.join(__dirname, 'stockInsightCases.json');
const REPORT_PATH = path.join(
  __dirname,
  EVALUATION_MODE === 'live'
    ? 'stockInsightReport.live.json'
    : 'stockInsightReport.fake.json'
);

function loadCases() {
  const cases = JSON.parse(fs.readFileSync(CASES_PATH, 'utf8'));

  if (!Array.isArray(cases)) {
    throw new Error('Insight evaluation dataset must be an array');
  }

  return cases;
}

// The shared scorer reads `summary` and `factors`. Fold both contexts into the
// summary so they are checked for forbidden claims, terminology and so on.
function toScorable(insight) {
  return {
    ...insight,
    summary: [insight.summary, insight.shortTermContext, insight.longTermContext]
      .filter(Boolean)
      .join(' ')
  };
}

function evaluateNoForecast(insight, evidence) {
  const text = toScorable(insight).summary + ' ' + (insight.factors || []).join(' ');
  const evidenceText = (evidence || []).map(item => item.headline).join(' ');

  const detected = FORECAST_PATTERNS.filter(
    pattern => pattern.test(text) && !pattern.test(evidenceText)
  ).map(pattern => pattern.source);

  return { passed: detected.length === 0, detected };
}

function createFake(evaluationCase) {
  const sufficient = evaluationCase.expected.evidenceSufficient;
  const concepts = evaluationCase.expected.requiredConcepts || [];

  return createFakeLlmClient({
    response: {
      evidenceSentiment: evaluationCase.expected.allowedEvidenceSentiments[0],
      summary: sufficient
        ? `The supplied evidence includes ${concepts.join(' and ')}.`
        : 'The available evidence is insufficient to reliably summarise this stock.',
      shortTermContext:
        'The available data describes current movement only and does not forecast prices.',
      longTermContext:
        'The supplied evidence does not by itself establish a long-term outlook.',
      factors: sufficient
        ? concepts.map(concept => `The supplied evidence references ${concept}.`).slice(0, 3)
        : [],
      evidenceSufficient: sufficient
    }
  });
}

function createService(evaluationCase) {
  if (EVALUATION_MODE === 'fake') {
    return createStockInsightService({ llmClient: createFake(evaluationCase) });
  }

  if (EVALUATION_MODE === 'live') {
    return createAppDependencies({
      env: { ...process.env, AI_LIVE_CALLS_ENABLED: 'true' }
    }).stockInsightService;
  }

  throw new Error(`Unsupported AI evaluation mode: ${EVALUATION_MODE}`);
}

async function runCase(evaluationCase) {
  const service = createService(evaluationCase);
  const startedAt = performance.now();

  try {
    const result = await service.generateStockInsight(evaluationCase.input);
    const latencyMs = Math.round(performance.now() - startedAt);
    const scoring = scoreExplanation(
      evaluationCase,
      toScorable(result.insight),
      result.metadata
    );

    const forecast = evaluateNoForecast(result.insight, evaluationCase.input.evidence);
    scoring.checks.push({
      name: 'NO_FORECAST_CLAIMS',
      passed: forecast.passed,
      details: { detected: forecast.detected }
    });
    scoring.maximumScore = scoring.checks.length;
    scoring.score = scoring.checks.filter(check => check.passed).length;
    scoring.passed = scoring.score === scoring.maximumScore;
    scoring.percentage = Math.round((scoring.score / scoring.maximumScore) * 100);

    const record = buildResultRecord(evaluationCase, {
      caseId: evaluationCase.caseId,
      description: evaluationCase.description,
      status: scoring.passed ? 'PASS' : 'FAIL',
      latencyMs,
      explanation: toScorable(result.insight),
      metadata: result.metadata,
      scoring,
      error: null
    });

    if (!forecast.passed) {
      record.errorCategories = [...record.errorCategories, 'FORECAST_CLAIM'].sort();
    }

    return record;
  } catch (error) {
    return buildResultRecord(evaluationCase, {
      caseId: evaluationCase.caseId,
      description: evaluationCase.description,
      status: 'ERROR',
      latencyMs: Math.round(performance.now() - startedAt),
      explanation: null,
      metadata: null,
      scoring: null,
      error: {
        name: error.name,
        code: error.code || 'UNKNOWN_ERROR',
        message: error.message
      }
    });
  }
}

async function runEvaluation() {
  const results = [];

  for (const evaluationCase of loadCases()) {
    console.log(`[insight-eval] Running ${evaluationCase.caseId}`);
    const result = await runCase(evaluationCase);
    results.push(result);
    console.log(`[insight-eval] ${result.caseId}: ${result.status} (${result.latencyMs} ms)`);
  }

  const count = status => results.filter(result => result.status === status).length;
  const completed = results.filter(result => Number.isFinite(result.latencyMs));

  const report = {
    generatedAt: new Date().toISOString(),
    mode: EVALUATION_MODE,
    summary: {
      total: results.length,
      passed: count('PASS'),
      failed: count('FAIL'),
      errors: count('ERROR'),
      manualReview: results.filter(result => result.manualReview).length,
      averageLatencyMs: completed.length
        ? Math.round(completed.reduce((sum, r) => sum + r.latencyMs, 0) / completed.length)
        : null,
      ...summariseQuality(results),
      errorCategories: summariseErrorCategories(results)
    },
    results
  };

  fs.writeFileSync(REPORT_PATH, JSON.stringify(report, null, 2));
  console.log('\nInsight evaluation summary');
  console.log(JSON.stringify(report.summary, null, 2));
  console.log(`\nReport written to: ${REPORT_PATH}`);

  if (report.summary.failed > 0 || report.summary.errors > 0) {
    process.exitCode = 1;
  }
}

if (require.main === module) {
  runEvaluation().catch(error => {
    console.error('[insight-eval] Fatal error:', error.message);
    process.exitCode = 1;
  });
}

module.exports = { runCase, runEvaluation, evaluateNoForecast, toScorable };

