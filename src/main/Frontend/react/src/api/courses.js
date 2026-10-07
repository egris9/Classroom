import client from "./client.js";

const messageOf = (error, fallback) => error.response?.data?.message || fallback;

export const createCourse = async (courseData) => {
    try {
        const response = await client.post("/api/courses", courseData);
        return response.data;
    } catch (error) {
        throw new Error(messageOf(error, "Could not create the course."));
    }
};

export const joinCourseByCode = async (accessCode) => {
    try {
        const response = await client.post("/api/courses/join", { accessCode });
        return { success: true, course: response.data };
    } catch (error) {
        return { success: false, error: messageOf(error, "Could not join the course.") };
    }
};

export const getCourses = async () => {
    try {
        const response = await client.get("/api/courses");
        return response.data;
    } catch (error) {
        throw new Error(messageOf(error, "Could not load your courses."));
    }
};

export const getCourse = async (courseId) => {
    try {
        const response = await client.get(`/api/courses/${courseId}`);
        return response.data;
    } catch (error) {
        throw new Error(messageOf(error, "Could not load the course."));
    }
};

export const listFiles = async (courseId) => {
    try {
        const response = await client.get(`/api/courses/${courseId}/files`);
        return response.data;
    } catch (error) {
        throw new Error(messageOf(error, "Could not load the PDFs."));
    }
};

export const uploadFile = async (courseId, file) => {
    const formData = new FormData();
    formData.append("file", file);
    try {
        const response = await client.post(`/api/courses/${courseId}/files`, formData);
        return response.data;
    } catch (error) {
        throw new Error(messageOf(error, "Could not upload the PDF."));
    }
};

/** Fetches a PDF through the client, so the token is sent, and returns it as a blob. */
export const fetchFileContent = async (fileId) => {
    try {
        const response = await client.get(`/api/files/${fileId}/content`, { responseType: "blob" });
        return response.data;
    } catch (error) {
        throw new Error("Could not open the PDF.");
    }
};
