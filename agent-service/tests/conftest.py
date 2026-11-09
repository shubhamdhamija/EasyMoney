import os
import sys
from pathlib import Path

# Tests must never hit live market-data providers, even if the local .env enables them.
os.environ["AGENT_LIVE_MARKET_DATA"] = "false"
os.environ["AGENT_LIVE_CALLS_ENABLED"] = "false"

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))
