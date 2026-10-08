import client from "./client.js";

const TOKEN_KEY = "jwt_token";
const USER_KEY = "user";

export const getToken = () => localStorage.getItem(TOKEN_KEY);

/** The signed-in user as the signin response returned it: id, firstName, lastName, email, picture. */
export const getUser = () => {
    try {
        return JSON.parse(localStorage.getItem(USER_KEY));
    } catch {
        return null;
    }
};

export const isSignedIn = () => Boolean(getToken());

export const clearSession = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
};

export const signUp = async (formData) => {
    const response = await client.post("/api/auth/signup", formData);
    return response.data;
};

export const signIn = async (email, password) => {
    const response = await client.post("/api/auth/signin", { email, password });
    localStorage.setItem(TOKEN_KEY, response.data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(response.data.user));
    return response.data.user;
};
