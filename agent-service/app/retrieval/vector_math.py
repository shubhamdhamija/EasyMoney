from __future__ import annotations

import math


def cosine_similarity(first: list[float], second: list[float]) -> float:
    if not first or not second:
        raise ValueError("vectors must not be empty")
    if len(first) != len(second):
        raise ValueError("vectors must have equal non-zero dimensions")
    if any(not math.isfinite(value) for value in first + second):
        raise ValueError("vector values must be finite")

    dot = sum(a * b for a, b in zip(first, second))
    norm_first = math.sqrt(sum(value * value for value in first))
    norm_second = math.sqrt(sum(value * value for value in second))
    if norm_first == 0.0 or norm_second == 0.0:
        return 0.0

    score = dot / (norm_first * norm_second)
    return max(-1.0, min(1.0, score))


def normalized_similarity(cosine_score: float) -> float:
    clamped = max(-1.0, min(1.0, float(cosine_score)))
    normalized = (clamped + 1.0) / 2.0
    return max(0.0, min(1.0, normalized))

