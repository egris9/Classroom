import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

/** A password field with a button that shows or hides what was typed. */
export function PasswordInput(props) {
    const [shown, setShown] = useState(false);

    return (
        <div className="relative">
            <Input type={shown ? "text" : "password"} className="pr-12" {...props} />
            <Button
                type="button"
                variant="ghost"
                size="icon"
                className="absolute right-0 top-0 h-full w-11"
                aria-label={shown ? "Hide password" : "Show password"}
                aria-pressed={shown}
                onClick={() => setShown(!shown)}
            >
                {shown ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </Button>
        </div>
    );
}

export default PasswordInput;
