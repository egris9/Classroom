import { useState } from "react";
import { Check, Copy } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";

/** The access code of a course, with a button that copies it. */
export function CopyCode({ code }) {
    const [copied, setCopied] = useState(false);

    const copy = async () => {
        try {
            await navigator.clipboard.writeText(code);
            setCopied(true);
            toast.success("Access code copied.");
            setTimeout(() => setCopied(false), 2000);
        } catch {
            toast.error("Could not copy. Select the code and copy it by hand.");
        }
    };

    return (
        <div className="flex flex-wrap items-center gap-2">
            <span className="text-sm text-muted-foreground">Access code</span>
            <code className="rounded-md border bg-muted px-2 py-1 font-mono text-sm tracking-wider">{code}</code>
            <Button type="button" variant="outline" size="sm" className="h-9 gap-1" onClick={copy}>
                {copied ? <Check className="h-4 w-4" aria-hidden="true" /> : <Copy className="h-4 w-4" aria-hidden="true" />}
                {copied ? "Copied" : "Copy"}
            </Button>
        </div>
    );
}

export default CopyCode;
