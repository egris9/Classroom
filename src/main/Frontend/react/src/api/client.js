import axios from "axios";
import { clearSession, getToken } from "./auth.js";

export const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

const client = axios.create({ baseURL: API_URL });

client.interceptors.request.use((config) => {
    const token = getToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

client.interceptors.response.use(
    (response) => response,
    (error) => {
        const url = error.config?.url ?? "";
        if (error.response?.status === 401 && !url.startsWith("/api/auth/")) {
            clearSession();
            window.location.assign("/signin");
        }
        return Promise.reject(error);
    }
);

export default client;
