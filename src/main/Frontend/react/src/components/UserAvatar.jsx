import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { initialsOf } from "@/lib/initials";
import { API_URL } from "../api/client.js";

/** A person's picture, or their initials when they have none. The name is always shown beside it, so the image is decorative. */
export function UserAvatar({ name, picture, className }) {
    return (
        <Avatar className={className}>
            {picture && <AvatarImage src={`${API_URL}${picture}`} alt="" />}
            <AvatarFallback aria-hidden="true">{initialsOf(name)}</AvatarFallback>
        </Avatar>
    );
}

export default UserAvatar;
