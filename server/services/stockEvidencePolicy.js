const INSTRUCTION_PATTERNS = [
  /ignore\s+(all\s+)?(previous|prior)\s+instructions/i,
  /disregard\s+(all\s+)?(previous|prior)\s+instructions/i,
  /system\s+prompt/i,
  /developer\s+message/i,
  /recommend\s+(buying|selling|holding)/i,
  /\b(buy|sell|hold)\s+this\s+stock\b/i,
  /guaranteed\s+(return|profit|gain)/i
];

function containsInstructionLikeContent(value) {
  if (typeof value !== 'string') {
    return false;
  }

  return INSTRUCTION_PATTERNS.some(pattern => pattern.test(value));
}

function applyEvidencePolicy(evidence) {
  const accepted = [];
  const rejected = [];

  (Array.isArray(evidence) ? evidence : []).forEach((item, index) => {
    const unsafe =
      containsInstructionLikeContent(item?.headline) ||
      containsInstructionLikeContent(item?.source);

    if (unsafe) {
      rejected.push({
        index,
        type: item?.type,
        reason: 'INSTRUCTION_LIKE_CONTENT'
      });

      return;
    }

    accepted.push(item);
  });

  return {
    accepted,
    rejected,
    hadRejections: rejected.length > 0
  };
}

module.exports = {
  INSTRUCTION_PATTERNS,
  containsInstructionLikeContent,
  applyEvidencePolicy
};

