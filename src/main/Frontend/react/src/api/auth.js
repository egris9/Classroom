import axios from "axios";

// Base URL for your backend
const API_URL = "http://localhost:8080/api/auth";

// Sign Up Function
export const signUp = async (formData) => {
    try {
        const response = await axios.post(`${API_URL}/signup`, formData, { headers: { "Content-Type": "multipart/form-data" } });
        return response.data;
    } catch (error) {
        console.error("Sign Up Error:", error.response?.data || error.message);
        throw error.response?.data || error.message; // Return error for component handling
    }
};

// Sign In Function
export const signInn = async (email, password) => {
    try {
        const response = await axios.post(`${API_URL}/signing`, null, {
            params: { email, password }, // Pass email and password as URL parameters
        });

        console.log("Sign in successful", response.data);

        // Si la connexion réussit, stocker le token JWT dans localStorage
        const token = response.data.token; // Assurez-vous que votre backend renvoie un champ 'token'
        const firstname = response.data.firstname; // Assurez-vous que votre backend renvoie un champ 'firstname'
        localStorage.setItem('jwt_token', token); // Stocker dans localStorage
        localStorage.setItem("user_firstname", firstname); // Stocker le prénom

        console.log("Received data during sign in:", response.data);

        return response.data; // Retourner les données de l'utilisateur et le token
    } catch (error) {
        console.error(
            "Sign in failed",
            error.response?.data?.message || error.message || "An unknown error occurred."
        );
        throw error; // Lancer l'erreur pour la gestion dans la fonction appelante
    }
};
