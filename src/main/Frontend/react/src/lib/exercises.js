/** The Exercise types and difficulties, as the API names them and as the screens show them. */
export const TYPE_LABELS = {
    OPEN: "Open question",
    MULTIPLE_CHOICE: "Multiple choice",
    TRUE_FALSE: "True or false",
};

export const DIFFICULTY_LABELS = {
    EASY: "Easy",
    MEDIUM: "Medium",
    HARD: "Hard",
};

/** What each difficulty asks for, shown under the chosen one. */
export const DIFFICULTY_HINTS = {
    EASY: "Short questions on single facts",
    MEDIUM: "Explain or apply one idea",
    HARD: "Longer questions that combine ideas",
};

export const DEFAULT_EXERCISE_OPTIONS = { types: ["OPEN"], difficulty: "MEDIUM", count: 5 };
export const MIN_EXERCISE_COUNT = 3;
export const MAX_EXERCISE_COUNT = 15;

/** The type of an Exercise. A set stored before types existed has none, and its questions are open ones. */
export const typeOf = (exercise) => exercise?.type ?? "OPEN";

/** Multiple choice and true or false can be marked by comparing; an open answer cannot. */
export const isAutoMarked = (exercise) => typeOf(exercise) !== "OPEN";

/** Whether `given` is the right answer to a multiple choice or true or false Exercise. */
export function isCorrect(exercise, given) {
    if (typeof given !== "string") {
        return false;
    }
    switch (typeOf(exercise)) {
        case "MULTIPLE_CHOICE":
            return given === exercise.answer;
        case "TRUE_FALSE":
            return given.toLowerCase() === String(exercise.answer).toLowerCase();
        default:
            return false;
    }
}

/** `{ correct, total }` for a set; an answer is `{ given, correct }`, and a question with none counts as wrong. */
export const scoreOf = (exercises, answers) => ({
    correct: exercises.filter((_, index) => answers[index]?.correct === true).length,
    total: exercises.length,
});

/** The type label to show for a whole set: its one type, or "Mixed". */
export function setTypeLabel(types) {
    const known = (types ?? []).filter((type) => TYPE_LABELS[type]);
    if (known.length === 0) {
        return TYPE_LABELS.OPEN;
    }
    return known.length === 1 ? TYPE_LABELS[known[0]] : "Mixed";
}
