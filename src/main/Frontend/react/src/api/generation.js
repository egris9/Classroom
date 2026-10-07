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

/** Asks for a summary of the file. Resolves at once with a PENDING summary; poll listSummaries for the result. */
export const requestSummary = (fileId) =>
    call(() => client.post(`/api/files/${fileId}/summaries`), "Could not request the summary.");

/** The published summaries of the file plus the caller's own. */
export const listSummaries = (fileId) =>
    call(() => client.get(`/api/files/${fileId}/summaries`), "Could not load the summaries.");

/** Teacher only. Resolves at once with a PENDING exercise set; poll listExerciseSets for the result. */
export const requestExerciseSet = (fileId) =>
    call(() => client.post(`/api/files/${fileId}/exercise-sets`), "Could not request the exercises.");

/** The published exercise sets of the file plus the caller's own. */
export const listExerciseSets = (fileId) =>
    call(() => client.get(`/api/files/${fileId}/exercise-sets`), "Could not load the exercises.");
