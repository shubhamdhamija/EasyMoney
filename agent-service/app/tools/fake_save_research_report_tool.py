from app.models.save_research_report import SaveResearchReportInput, SaveResearchReportResult
from app.tools.fake_research_report_store import FakeResearchReportStore


class FakeSaveResearchReportTool:
    name = "save_research_report"
    input_model = SaveResearchReportInput
    requires_approval = True

    description = (
        "Saves a completed research report. This tool modifies application state and "
        "requires explicit human approval. It must never execute automatically. "
        "It does not send messages, place trades, or modify a portfolio."
    )

    def __init__(self, store: FakeResearchReportStore | None = None) -> None:
        self.store = store if store is not None else FakeResearchReportStore()
        self.execution_calls: list[SaveResearchReportInput] = []

    async def execute(self, tool_input: SaveResearchReportInput) -> SaveResearchReportResult:
        self.execution_calls.append(tool_input)
        record_id = self.store.save(
            symbol=tool_input.symbol,
            report_summary=tool_input.report_summary,
            request_id=tool_input.request_id,
        )
        return SaveResearchReportResult(
            symbol=tool_input.symbol,
            saved=True,
            record_id=record_id,
        )
