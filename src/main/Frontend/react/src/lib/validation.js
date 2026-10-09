// Loose on purpose: it catches a missing @ or domain, and the server's own check decides the rest.
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Focuses the first control in `ids` that has an error, so a failed submit puts the reader at the problem. */
export function focusFirstError(errors, ids) {
    const first = ids.find((id) => errors[id]);
    if (first) {
        document.getElementById(first)?.focus();
    }
}
