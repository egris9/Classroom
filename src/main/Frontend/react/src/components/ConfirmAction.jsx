import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button, buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const QUIET_CLASS = "text-destructive hover:bg-destructive/10 hover:text-destructive";

/**
 * A button that asks before it does something that cannot be undone. The label is the outcome, not "Delete".
 * `tone="quiet"` is a red-lettered outline button for a destructive action that sits in a list or beside another
 * button; the solid red button inside the dialog is the one that does it.
 */
export function ConfirmAction({
    label,
    title,
    description,
    confirmLabel,
    onConfirm,
    size = "sm",
    tone = "solid",
    className,
    triggerLabel,
    disabled = false,
}) {
    return (
        <AlertDialog>
            <AlertDialogTrigger asChild>
                <Button
                    variant={tone === "quiet" ? "outline" : "destructive"}
                    size={size}
                    disabled={disabled}
                    aria-label={triggerLabel}
                    className={cn(tone === "quiet" && QUIET_CLASS, className)}
                >
                    {label}
                </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>{title}</AlertDialogTitle>
                    <AlertDialogDescription>{description}</AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                    <AlertDialogAction className={buttonVariants({ variant: "destructive" })} onClick={onConfirm}>
                        {confirmLabel}
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
}

export default ConfirmAction;
