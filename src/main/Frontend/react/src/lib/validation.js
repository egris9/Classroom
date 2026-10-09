import { flushSync } from "react-dom";

// Loose on purpose: it catches a missing @ or name. The server accepts addresses like name@localhost, so this must
// not be stricter than it, or an account the server allows could never sign in here. The server decides the rest.
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+$/;

/**
 * Shows `errors` on their fields and then focuses the first of `ids` that has one. The errors are rendered before the
 * focus moves, so the field already carries aria-invalid and aria-describedby when a screen reader reads it.
 */
export function showFieldErrors(setErrors, errors, ids) {
    flushSync(() => setErrors(errors));
    const first = ids.find((id) => errors[id]);
    if (first) {
        document.getElementById(first)?.focus();
    }
}
