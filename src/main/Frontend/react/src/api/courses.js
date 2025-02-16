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
        if (error.response) {
            // If response exists, it means the error was returned from the backend
            if (error.response.status === 400) {
                throw new Error(error.response.data); // Use the message from the backend
            }
            throw new Error("Erreur lors de la création du cours");
        }
        console.error("Erreur lors de la création du cours:", error.message);
        throw new Error("Une erreur inattendue est survenue.");
    }
};

// Fonction pour rejoindre un cours avec le code d'accès
export const joinCourseByCode = async (joinCode) => {
    try {
        const token = localStorage.getItem("jwt_token");
        const response = await axios.get(`${API_URL}/join`, {
            params: { accessCode: joinCode },
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
        return { success: true, course: response.data };
    } catch (error) {
        if (error.response) {
            // Gérer l'erreur côté serveur (ex: code d'accès invalide)
            return { success: false, error: error.response.data || "Cours non trouvé." };
        } else {
            // Gérer les erreurs réseau
            return { success: false, error: "Une erreur inattendue est survenue." };
        }
    }
};




export const getCourses = async (type = "user") => {
    const token = localStorage.getItem("jwt_token");


    if (!token) {
        throw new Error("Utilisateur non authentifié");
    }

    try {
        // Add `type` as a query parameter
        const response = await axios.get(`${API_URL}/courses`, {
            headers: {
                Authorization: `Bearer ${token}`,
            },
            params: { type }, // Pass the type ('user' or 'all') dynamically
        });

        return response.data;
    } catch (error) {
        if (error.response?.status === 401) {
            localStorage.removeItem("jwt_token");
            window.location.href = "/login"; // Redirect to login page
        }

        const errorMessage =
            error.response?.data?.message || "Erreur interne du serveur.";
        throw new Error(errorMessage);
    }
};



