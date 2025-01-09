import axios from "axios";

const API_URL = "http://localhost:8080/api/courses";

export const createCourse = async (courseData) => {
    const token = localStorage.getItem("jwt_token");

    if (!token) {
        throw new Error("Utilisateur non authentifié");
    }

    try {
        const response = await axios.post(`${API_URL}/create`, courseData, {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return response.data;
    } catch (error) {
        console.error("Erreur lors de la création du cours:", error.response?.data || error.message);
        throw new Error(error.response?.data?.message || error.message || "Une erreur inattendue est survenue.");
    }
};
