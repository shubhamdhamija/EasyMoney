from __future__ import annotations

import math

import pytest

from app.retrieval.vector_math import cosine_similarity, normalized_similarity


def test_identical_vectors_return_cosine_similarity_one() -> None:
    assert cosine_similarity([1.0, 2.0, 3.0], [1.0, 2.0, 3.0]) == pytest.approx(1.0)


def test_opposite_vectors_return_negative_one() -> None:
    assert cosine_similarity([1.0, 0.0], [-1.0, 0.0]) == pytest.approx(-1.0)


def test_orthogonal_vectors_return_zero() -> None:
    assert cosine_similarity([1.0, 0.0], [0.0, 1.0]) == pytest.approx(0.0)


def test_zero_vector_returns_zero() -> None:
    assert cosine_similarity([0.0, 0.0, 0.0], [1.0, 0.0, 0.0]) == 0.0


def test_unequal_dimensions_are_rejected() -> None:
    with pytest.raises(ValueError):
        cosine_similarity([1.0, 2.0], [1.0, 2.0, 3.0])


def test_empty_vectors_are_rejected() -> None:
    with pytest.raises(ValueError):
        cosine_similarity([], [1.0, 2.0])


def test_nan_is_rejected() -> None:
    with pytest.raises(ValueError):
        cosine_similarity([1.0, float("nan")], [1.0, 2.0])


def test_positive_infinity_is_rejected() -> None:
    with pytest.raises(ValueError):
        cosine_similarity([1.0, math.inf], [1.0, 2.0])


def test_negative_infinity_is_rejected() -> None:
    with pytest.raises(ValueError):
        cosine_similarity([1.0, -math.inf], [1.0, 2.0])


def test_floating_point_output_is_clamped_to_minus_one_through_one() -> None:
    score = cosine_similarity([1.0, 0.0], [2.0, 0.0])
    assert -1.0 <= score <= 1.0


def test_normalized_similarity_maps_expected_values() -> None:
    assert normalized_similarity(-1.0) == pytest.approx(0.0)
    assert normalized_similarity(0.0) == pytest.approx(0.5)
    assert normalized_similarity(1.0) == pytest.approx(1.0)


def test_normalized_output_remains_between_zero_and_one() -> None:
    for value in (-2.0, -1.0, -0.5, 0.0, 0.25, 0.5, 1.0, 2.0):
        result = normalized_similarity(value)
        assert 0.0 <= result <= 1.0

