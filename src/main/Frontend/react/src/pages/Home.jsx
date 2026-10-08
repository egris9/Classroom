import { isSignedIn } from "../api/auth.js";
import Landing from "./Landing.jsx";
import MyCourses from "./MyCourses.jsx";

/** `/`: My courses for a signed-in user, the landing page for everyone else. */
export default function Home() {
    return isSignedIn() ? <MyCourses /> : <Landing />;
}
