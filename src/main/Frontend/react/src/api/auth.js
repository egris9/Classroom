import client from "./client.js";

const TOKEN_KEY = "jwt_token";
const FIRST_NAME_KEY = "user_firstname";

export const getToken = () => localStorage.getItem(TOKEN_KEY);

export const getFirstName = () => localStorage.getItem(FIRST_NAME_KEY);

export const isSignedIn = () => Boolean(getToken());

export const clearSession = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(FIRST_NAME_KEY);
};

export const signUp = async (formData) => {
    const response = await client.post("/api/auth/signup", formData);
    return response.data;
};

export const signInn = async (email, password) => {
    const response = await client.post("/api/auth/signin", { email, password });
    localStorage.setItem(TOKEN_KEY, response.data.token);
    localStorage.setItem(FIRST_NAME_KEY, response.data.user.firstName);
    return response.data.user;
};
