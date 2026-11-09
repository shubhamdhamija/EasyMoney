from __future__ import annotations

import asyncio
import math
import socket
from pathlib import Path

import pytest

from app.retrieval.fake_embedding_model import DeterministicFakeEmbeddingModel


def test_repeated_text_produces_exactly_same_vector() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    text = "Revenue increased and operating expenses remained disciplined."
    first = asyncio.run(model.embed_text(text))
    second = asyncio.run(model.embed_text(text))
    assert first.values == second.values


def test_separate_model_instances_produce_same_vector() -> None:
    first = DeterministicFakeEmbeddingModel(dimensions=64)
    second = DeterministicFakeEmbeddingModel(dimensions=64)
    left = asyncio.run(first.embed_text("quarterly revenue improved"))
    right = asyncio.run(second.embed_text("quarterly revenue improved"))
    assert left.values == right.values


def test_case_only_differences_produce_same_vector() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    lower = asyncio.run(model.embed_text("revenue increased"))
    upper = asyncio.run(model.embed_text("REVENUE INCREASED"))
    assert lower.values == upper.values


def test_whitespace_only_differences_produce_same_vector() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    one = asyncio.run(model.embed_text("revenue   increased\n in  the quarter"))
    two = asyncio.run(model.embed_text("revenue increased in the quarter"))
    assert one.values == two.values


def test_different_text_normally_produces_different_vector() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    left = asyncio.run(model.embed_text("revenue increased"))
    right = asyncio.run(model.embed_text("risk disclosure and supply constraints"))
    assert left.values != right.values


def test_output_dimensions_match_configuration() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=96)
    vector = asyncio.run(model.embed_text("cash flow remained strong"))
    assert len(vector.values) == 96
    assert vector.dimensions == 96


def test_dimensions_below_supported_minimum_are_rejected() -> None:
    with pytest.raises(ValueError):
        DeterministicFakeEmbeddingModel(dimensions=15)


def test_dimensions_above_supported_maximum_are_rejected() -> None:
    with pytest.raises(ValueError):
        DeterministicFakeEmbeddingModel(dimensions=513)


def test_every_vector_value_is_finite() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    vector = asyncio.run(model.embed_text("cloud operations expanded"))
    assert all(math.isfinite(value) for value in vector.values)


def test_non_empty_output_has_l2_magnitude_approximately_one() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    vector = asyncio.run(model.embed_text("capital expenditure remained prudent"))
    magnitude = math.sqrt(sum(value * value for value in vector.values))
    assert abs(magnitude - 1.0) <= 1e-6


def test_batch_output_order_matches_input_order() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    batch = asyncio.run(model.embed_texts(["revenue", "risk", "guidance"]))
    assert [item.values for item in batch] == [
        asyncio.run(model.embed_text("revenue")).values,
        asyncio.run(model.embed_text("risk")).values,
        asyncio.run(model.embed_text("guidance")).values,
    ]


def test_batch_output_count_matches_input_count() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    texts = ["revenue", "expenses", "liquidity", "guidance"]
    batch = asyncio.run(model.embed_texts(texts))
    assert len(batch) == len(texts)


def test_blank_text_is_rejected() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    with pytest.raises(ValueError):
        asyncio.run(model.embed_text("   "))


def test_blank_batch_items_are_rejected() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    with pytest.raises(ValueError):
        asyncio.run(model.embed_texts(["revenue", "   ", "risk"]))


def test_excessive_batch_size_is_rejected() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64, max_batch_size=2)
    with pytest.raises(ValueError):
        asyncio.run(model.embed_texts(["one", "two", "three"]))


def test_calls_are_recorded() -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    asyncio.run(model.embed_text("revenue"))
    assert model.calls == ["revenue"]


def test_no_network_call_occurs(monkeypatch: pytest.MonkeyPatch) -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    called = {"value": False}

    def fail_connect(*args, **kwargs):
        called["value"] = True
        raise AssertionError("socket connect should not be called")

    monkeypatch.setattr(socket.socket, "connect", fail_connect)
    asyncio.run(model.embed_text("revenue"))
    assert not called["value"]


def test_no_file_is_created(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    model = DeterministicFakeEmbeddingModel(dimensions=64)
    monkeypatch.chdir(tmp_path)
    asyncio.run(model.embed_text("revenue"))
    assert list(tmp_path.iterdir()) == []

