import hashlib
import json
from typing import Any


def build_tool_arguments_hash(*, tool_name: str, arguments: dict[str, Any]) -> str:
    payload = {"tool_name": tool_name, "arguments": dict(arguments)}
    canonical = json.dumps(
        payload,
        sort_keys=True,
        separators=(",", ":"),
        ensure_ascii=False,
    )
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()
