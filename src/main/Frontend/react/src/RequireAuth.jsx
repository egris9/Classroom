import { Navigate, Outlet } from "react-router-dom";
import { isSignedIn } from "./api/auth.js";

export default function RequireAuth() {
    return isSignedIn() ? <Outlet /> : <Navigate to="/signin" replace />;
}
