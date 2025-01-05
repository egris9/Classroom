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
            params: { email, password } // Pass email and password as URL parameters

        });

        console.log("Login successful", response.data);
        return response.data; // Return only the data for simpler consumption
    } catch (error) {
        console.error(
            "Login failed",
            error.response?.data?.message || error.message || "An unknown error occurred."
        );
        throw error; // Throw error for the calling function to handle
    }
};