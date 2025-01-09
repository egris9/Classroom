import axios from "axios";

// Base URL for your backend
const API_URL = "http://localhost:8080/api/auth";

// Sign Up Function
export const signUp = async (formData) => {
    try {
        const response = await axios.post(`${API_URL}/signup`, formData);
        return response.data; // Return user data
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
        localStorage.setItem('jwt_token', token); // Stocker dans localStorage

        return response.data; // Retourner les données de l'utilisateur et le token
    } catch (error) {
        console.error(
            "Sign in failed",
            error.response?.data?.message || error.message || "An unknown error occurred."
        );
        throw error; // Lancer l'erreur pour la gestion dans la fonction appelante
    }
};

// Exemple d'une fonction API qui utilise le token JWT dans l'en-tête
export const getUserData = async () => {
    try {
        const token = localStorage.getItem('jwt_token'); // Récupérer le token depuis localStorage

        const response = await axios.get(`${API_URL}/user-data`, {
            headers: {
                Authorization: `Bearer ${token}`, // Ajouter le token dans l'en-tête Authorization
            }
        });

        return response.data; // Retourner les données utilisateur
    } catch (error) {
        console.error("Error fetching user data", error);
        throw error;
    }
};

// Déconnexion : supprimer le token du localStorage
export const signOut = () => {
    localStorage.removeItem('jwt_token'); // Supprimer le token JWT du localStorage
};