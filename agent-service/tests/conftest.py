import os
import sys
from pathlib import Path

# Tests must never hit live market-data providers, even if the local .env enables them.
os.environ["AGENT_LIVE_MARKET_DATA"] = "false"
os.environ["AGENT_LIVE_CALLS_ENABLED"] = "false"

# Deterministic SnapTrade settings: environment variables take precedence over the developer's
# ignored .env, so tests never load real credentials or an enabled integration.
os.environ["SNAPTRADE_ENABLED"] = "false"
os.environ["SNAPTRADE_ENVIRONMENT"] = "sandbox"
os.environ["SNAPTRADE_CONNECTION_TYPE"] = "read"
os.environ["SNAPTRADE_BROKER"] = "SANDBOX"
os.environ["SNAPTRADE_CLIENT_ID"] = ""
os.environ["SNAPTRADE_CONSUMER_KEY"] = ""
os.environ["SNAPTRADE_ENCRYPTION_KEY"] = ""

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))
