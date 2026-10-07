import client from "./client.js";

export const createCourse = async (courseData) => {
    try {
        const response = await client.post("/api/courses/create", courseData);
        return response.data;
    } catch (error) {
        if (error.response?.status === 400) {
            throw new Error(error.response.data);
        }
        throw new Error("Erreur lors de la création du cours");
    }
};

// Fonction pour rejoindre un cours avec le code d'accès
export const joinCourseByCode = async (joinCode) => {
    try {
        const response = await client.get("/api/courses/join", { params: { accessCode: joinCode } });
        return { success: true, course: response.data };
    } catch (error) {
        if (error.response) {
            return { success: false, error: error.response.data || "Cours non trouvé." };
        }
        return { success: false, error: "Une erreur inattendue est survenue." };
    }
};

export const getCourses = async (type = "user") => {
    try {
        const response = await client.get("/api/courses/courses", { params: { type } });
        return response.data;
    } catch (error) {
        throw new Error(error.response?.data?.message || "Erreur interne du serveur.");
    }
};
