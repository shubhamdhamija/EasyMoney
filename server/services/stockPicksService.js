const { buildStockPicksPrompt } = require('./stockPicksPrompt');
const { assertValidLlmClient } = require('./llm/llmClientContract');
const {
  validateStockPicksOutput
} = require('../validators/stockPicksOutputValidator');
const { InvalidAiOutputError } = require('../errors/InvalidAiOutputError');
const {
  containsInstructionLikeContent
} = require('./stockEvidencePolicy');
const {
  PICKS_OUTPUT_POLICY_VERSION,
  findStockPicksPolicyViolations
} = require('./stockPicksOutputPolicy');

const DISCLAIMER =
  'For informational purposes only. This is not a recommendation to buy, sell, or hold any security.';

function formatSignedPercent(value) {
  const sign = value > 0 ? '+' : value < 0 ? '-' : '';
  return `${sign}${Math.abs(value).toFixed(2)}%`;
}

/**
 * Conservative ranking from movement magnitude only. Wording makes no claim
 * about attractiveness, outlook or action.
 */
function buildFallbackPicks({ stocks, maximumPicks }) {
  return stocks
    .filter(stock => stock.priceChangePercent !== 0)
    .sort(
      (first, second) =>
        Math.abs(second.priceChangePercent) -
        Math.abs(first.priceChangePercent)
    )
    .slice(0, maximumPicks)
    .map((stock, index) => ({
      symbol: stock.symbol,
      reason: `${stock.symbol} recorded notable daily movement of ${formatSignedPercent(
        stock.priceChangePercent
      )} in the supplied stock set.`,
      rank: index + 1
    }));
}

function createStockPicksService({ llmClient }) {
  const validatedClient = assertValidLlmClient(llmClient);

  async function generateStockPicks(input) {
    // Company names are untrusted free text: drop instruction-like ones so
    // they never reach the prompt.
    let namesRemoved = 0;

    const stocks = input.stocks.map(stock => {
      if (containsInstructionLikeContent(stock.companyName)) {
        namesRemoved += 1;
        return { ...stock, companyName: null };
      }
      return stock;
    });

    const maximumPicks = Math.min(input.maximumPicks, stocks.length);
    const safeInput = { stocks, maximumPicks };

    const prompt = buildStockPicksPrompt(safeInput);

    const metadataBase = {
      promptVersion: prompt.promptVersion,
      outputPolicyVersion: PICKS_OUTPUT_POLICY_VERSION,
      stockCount: stocks.length,
      companyNamesRemoved: namesRemoved
    };

    // Nothing moved: nothing notable to highlight and no reason to call a model.
    if (stocks.every(stock => stock.priceChangePercent === 0)) {
      return {
        picks: [],
        disclaimer: DISCLAIMER,
        metadata: { ...metadataBase, providerCalled: false, fallbackUsed: false }
      };
    }

    const generated = await validatedClient.generateJson({
      systemInstruction: prompt.systemInstruction,
      userMessage: prompt.userMessage
    });

    const validation = validateStockPicksOutput(generated);

    if (!validation.valid) {
      throw new InvalidAiOutputError(validation.errors);
    }

    const violations = findStockPicksPolicyViolations({
      picks: validation.value.picks,
      allowedSymbols: stocks.map(stock => stock.symbol),
      maximumPicks
    });

    if (violations.length > 0) {
      // Not transient: no retry. Never expose the rejected model wording.
      return {
        picks: buildFallbackPicks(safeInput),
        disclaimer: DISCLAIMER,
        metadata: {
          ...metadataBase,
          providerCalled: true,
          fallbackUsed: true,
          fallbackReason: violations[0],
          violations
        }
      };
    }

    return {
      picks: validation.value.picks.map((pick, index) => ({
        symbol: pick.symbol,
        reason: pick.reason,
        rank: index + 1
      })),
      disclaimer: DISCLAIMER,
      metadata: { ...metadataBase, providerCalled: true, fallbackUsed: false }
    };
  }

  return { generateStockPicks };
}

module.exports = {
  DISCLAIMER,
  buildFallbackPicks,
  createStockPicksService
};
