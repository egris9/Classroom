const UNITS = ["B", "KB", "MB", "GB"];

/** 1536 -> "1.5 KB". */
export function formatBytes(n) {
    let value = Math.max(0, Number(n) || 0);
    let unit = 0;
    while (value >= 1024 && unit < UNITS.length - 1) {
        value /= 1024;
        unit += 1;
    }
    const text = unit === 0 || Number.isInteger(value) ? String(value) : value.toFixed(1);
    return `${text} ${UNITS[unit]}`;
}

const matchesAccept = (file, accept) => {
    const tokens = accept.split(",").map((token) => token.trim().toLowerCase()).filter(Boolean);
    if (tokens.length === 0) {
        return true;
    }
    const type = (file.type || "").toLowerCase();
    const name = (file.name || "").toLowerCase();
    return tokens.some((token) => {
        if (token.startsWith(".")) {
            return name.endsWith(token);
        }
        if (token.endsWith("/*")) {
            return type.startsWith(token.slice(0, -1));
        }
        return type === token;
    });
};

/** Returns the error text for a file that does not fit `accept` or `maxBytes`, or null when it is fine. */
export function checkFile(file, { accept = "", maxBytes = Infinity } = {}) {
    if (!matchesAccept(file, accept)) {
        return "This file type is not accepted.";
    }
    if (file.size > maxBytes) {
        return `The file is ${formatBytes(file.size)}. The limit is ${formatBytes(maxBytes)}.`;
    }
    return null;
}
