/** The Summary levels, shortest first, as the API names them and as the screens show them. */
export const SUMMARY_LEVELS = [
    { value: "SHORT", label: "Short" },
    { value: "MEDIUM", label: "Medium" },
    { value: "LONG", label: "Long" },
    { value: "EXTRA_LONG", label: "Extra long" },
];

export const DEFAULT_SUMMARY_LEVEL = "MEDIUM";

export const isSummaryLevel = (value) => SUMMARY_LEVELS.some((level) => level.value === value);

/** "Extra long" for "EXTRA_LONG"; nothing for a value that is not a level. */
export const summaryLevelLabel = (value) => SUMMARY_LEVELS.find((level) => level.value === value)?.label;
