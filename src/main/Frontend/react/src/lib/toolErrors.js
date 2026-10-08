const NEXT_STEPS = {
    TEXT_TOO_LONG: "Shorten the text and try again.",
    FILE_TOO_LARGE: "Choose a smaller PDF.",
    UNSUPPORTED_MEDIA_TYPE: "Choose a PDF file.",
    VALIDATION_FAILED: "Paste some text or choose a PDF.",
    NO_TEXT: "This PDF has no text that can be read. Paste the text instead.",
    MODEL_UNAVAILABLE: "Try again in a few minutes.",
    MODEL_TIMEOUT: "Try again with a shorter text.",
    NETWORK: "Check your connection and try again.",
    CONNECTION_LOST: "Send your message again.",
};

/** What the person can do after an AI tools call failed with this API code. */
export const nextStep = (code) => NEXT_STEPS[code] ?? "Try again.";
