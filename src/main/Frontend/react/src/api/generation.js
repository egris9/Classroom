import client from "./client.js";

const messageOf = (error, fallback) => error.response?.data?.message || fallback;

const call = async (request, fallback) => {
    try {
        const response = await request();
        return response.data;
    } catch (error) {
        throw new Error(messageOf(error, fallback));
    }
};

/**
 * Asks for a summary of the file at a level (SHORT, MEDIUM, LONG or EXTRA_LONG). Resolves at once with a PENDING
 * summary; poll listSummaries for the result.
 */
export const requestSummary = (fileId, level) =>
    call(() => client.post(`/api/files/${fileId}/summaries`, { level }), "Could not request the summary.");

/** The published summaries of the file plus the caller's own. */
export const listSummaries = (fileId) =>
    call(() => client.get(`/api/files/${fileId}/summaries`), "Could not load the summaries.");

/**
 * Asks for an exercise set with `{ types, difficulty, count }`. Any member may ask: a Teacher's set is published, a
 * Student's is private. Resolves at once with a PENDING set; poll listExerciseSets for the result. A second request
 * while one is pending answers 409.
 */
export const requestExerciseSet = (fileId, { types, difficulty, count }) =>
    call(
        () => client.post(`/api/files/${fileId}/exercise-sets`, { types, difficulty, count }),
        "Could not request the exercises.",
    );

/** The published exercise sets of the file plus the caller's own. */
export const listExerciseSets = (fileId) =>
    call(() => client.get(`/api/files/${fileId}/exercise-sets`), "Could not load the exercises.");
