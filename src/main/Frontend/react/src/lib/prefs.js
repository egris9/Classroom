// What this browser remembers of a person's choices: one JSON object under one localStorage key.
// Keys in use: summaryLevel. Later screens add exercise and fileLayout.
const KEY = "prefs";

const read = () => {
    try {
        const stored = JSON.parse(localStorage.getItem(KEY));
        return stored && typeof stored === "object" && !Array.isArray(stored) ? stored : {};
    } catch {
        return {};
    }
};

/** The stored value of a preference, or `fallback` when it was never set or the store cannot be read. */
export const getPref = (key, fallback) => {
    const prefs = read();
    return Object.hasOwn(prefs, key) ? prefs[key] : fallback;
};

/** Remembers a preference. Where the browser refuses storage (private windows), it is simply not kept. */
export const setPref = (key, value) => {
    try {
        localStorage.setItem(KEY, JSON.stringify({ ...read(), [key]: value }));
    } catch {
        // nothing to do: the choice still holds for this page
    }
};
