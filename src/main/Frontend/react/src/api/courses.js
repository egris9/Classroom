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

export const setCoursePicture = async (courseId, file) => {
    const formData = new FormData();
    formData.append("picture", file);
    try {
        const response = await client.put(`/api/courses/${courseId}/picture`, formData);
        return response.data;
    } catch (error) {
        throw new Error(messageOf(error, "Could not upload the cover picture."));
    }
};

export const removeCoursePicture = async (courseId) => {
    try {
        await client.delete(`/api/courses/${courseId}/picture`);
    } catch (error) {
        throw new Error(messageOf(error, "Could not remove the cover picture."));
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

export const deleteFile = async (fileId) => {
    try {
        await client.delete(`/api/files/${fileId}`);
    } catch (error) {
        throw new Error(messageOf(error, "Could not delete the PDF."));
    }
};

export const deleteCourse = async (courseId) => {
    try {
        await client.delete(`/api/courses/${courseId}`);
    } catch (error) {
        throw new Error(messageOf(error, "Could not delete the course."));
    }
};

export const leaveCourse = async (courseId) => {
    try {
        await client.delete(`/api/courses/${courseId}/membership`);
    } catch (error) {
        throw new Error(messageOf(error, "Could not leave the course."));
    }
};

export const listStudents = async (courseId) => {
    try {
        const response = await client.get(`/api/courses/${courseId}/students`);
        return response.data;
    } catch (error) {
        throw new Error(messageOf(error, "Could not load the students."));
    }
};

/** Fetches a PDF through the client, so the token is sent, and returns it as a blob. */
export const fetchFileContent = async (fileId) => {
    try {
        const response = await client.get(`/api/files/${fileId}/content`, { responseType: "blob" });
        return response.data;
    } catch {
        throw new Error("Could not open the PDF.");
    }
};
