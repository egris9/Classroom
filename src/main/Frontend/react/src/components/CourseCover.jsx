import { cn } from "@/lib/utils";
import { API_URL } from "../api/client.js";

const HUE_MIN = 270;
const HUE_SPAN = 50; // 270 to 320: mauve to magenta

const hueOf = (name = "") => {
    let hash = 0;
    for (const char of name) {
        hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
    }
    return HUE_MIN + (hash % (HUE_SPAN + 1));
};

/**
 * The course's cover picture, or a gradient made from its name when there is none.
 * `aspect` is a Tailwind aspect class. The picture is decorative: the course name is always written next to it.
 */
export function CourseCover({ course, aspect = "aspect-video", className }) {
    const hue = hueOf(course.courseName);
    const style = {
        backgroundImage: `linear-gradient(135deg, hsl(${hue} 45% 78%), hsl(${(hue + 25) % 360} 55% 62%))`,
    };
    const version = course.pictureVersion ? `?v=${course.pictureVersion}` : "";

    return (
        <div className={cn("w-full overflow-hidden", aspect, className)} style={course.picture ? undefined : style}>
            {course.picture && (
                <img src={`${API_URL}${course.picture}${version}`} alt="" className="h-full w-full object-cover" />
            )}
        </div>
    );
}

export default CourseCover;
