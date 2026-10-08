import { ExerciseList } from "@/components/ExerciseList";
import { ToolsForm } from "@/components/ToolsForm";
import { exercisesFromPdf, exercisesFromText } from "../api/tools.js";

/** The Exercises tab of the AI tools page: an Exercise set from pasted text or a PDF that belongs to no course. */
export function ToolsExercises({ attempt, limits }) {
    return (
        <ToolsForm
            action="Generate exercises"
            busyLabel="Generating..."
            fromText={exercisesFromText}
            fromPdf={exercisesFromPdf}
            attempt={attempt}
            limits={limits}
        >
            {(set) => <ExerciseList set={set} />}
        </ToolsForm>
    );
}

export default ToolsExercises;
