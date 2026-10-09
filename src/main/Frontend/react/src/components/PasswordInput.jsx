import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

/** A password field with a button that shows or hides what was typed. The button is as tall as the field. */
export function PasswordInput({ className, ...props }) {
    const [shown, setShown] = useState(false);

    return (
        <div className="relative">
            <Input type={shown ? "text" : "password"} className={cn("pr-12", className)} {...props} />
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
