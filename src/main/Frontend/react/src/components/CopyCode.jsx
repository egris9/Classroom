import { useState } from "react";
import { Check, Copy } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";

/** The access code of a course as a chip, with a button that copies it. */
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
        <div className="inline-flex max-w-full flex-wrap items-center gap-1 rounded-lg border bg-card py-1 pl-3 pr-1 shadow">
            <span className="text-sm text-muted-foreground">Access code</span>
            <code className="rounded-md bg-muted px-2 py-1 font-mono text-sm font-medium tracking-wider">{code}</code>
            <Button type="button" variant="ghost" className="h-11 gap-1.5 px-3" onClick={copy}>
                {copied ? <Check className="h-4 w-4" aria-hidden="true" /> : <Copy className="h-4 w-4" aria-hidden="true" />}
                {copied ? "Copied" : "Copy"}
            </Button>
        </div>
    );
}

export default CopyCode;
