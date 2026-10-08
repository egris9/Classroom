import client from "./client.js";

/** An Error whose `code` is the API's error code, so a caller can branch on it (for example TRIAL_USED). */
export const toolsError = (code, message) => {
    const error = new Error(message);
    error.code = code;
    return error;
};

const call = async (request, fallback) => {
    try {
        const response = await request();
        return response.data;
    } catch (error) {
        if (!error.response) {
            throw toolsError("NETWORK", "Could not reach the server.");
        }
        const body = error.response.data;
        throw toolsError(body?.code ?? "REQUEST_FAILED", body?.message || fallback);
    }
};

const pdfBody = (file) => {
    const formData = new FormData();
    formData.append("file", file);
    return formData;
};

/** Whether the caller is signed in and, if not, whether the one free try is still there. Does not spend it. */
export const getTrial = () => call(() => client.get("/api/tools/trial"), "Could not check your free try.");

/** Resolves with { text, model } once the model has answered, which can take up to two minutes. */
export const summariseText = (text) =>
    call(() => client.post("/api/tools/summaries", { text }), "Could not summarise the text.");

export const summarisePdf = (file) =>
    call(() => client.post("/api/tools/summaries", pdfBody(file)), "Could not summarise the PDF.");

/** Resolves with { exercises: [{ question, answer }], model }. */
export const exercisesFromText = (text) =>
    call(() => client.post("/api/tools/exercise-sets", { text }), "Could not generate the exercises.");

export const exercisesFromPdf = (file) =>
    call(() => client.post("/api/tools/exercise-sets", pdfBody(file)), "Could not generate the exercises.");
