import { SummaryCard } from "@/components/SummaryCard";
import { ToolsForm } from "@/components/ToolsForm";
import { summarisePdf, summariseText } from "../api/tools.js";

/** The Summary tab of the AI tools page: a summary of pasted text or a PDF that belongs to no course. */
export function ToolsSummary({ attempt, limits }) {
    return (
        <ToolsForm
            action="Summarise"
            busyLabel="Summarising..."
            fromText={summariseText}
            fromPdf={summarisePdf}
            attempt={attempt}
            limits={limits}
        >
            {(summary) => <SummaryCard summary={summary} />}
        </ToolsForm>
    );
}

export default ToolsSummary;
