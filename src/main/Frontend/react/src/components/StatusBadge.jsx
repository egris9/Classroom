import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

// One job per colour (see the Palette in docs/IMPLEMENTATION_PLAN.md).
const STATUS_CLASSES = {
    success: "border-transparent bg-success text-success-foreground hover:bg-success",
    warning: "border-transparent bg-warning text-warning-foreground hover:bg-warning",
    destructive: "border-transparent bg-destructive text-destructive-foreground hover:bg-destructive",
};

/** A Badge for published / pending / failed states. */
export function StatusBadge({ status, className, ...props }) {
    return <Badge className={cn(STATUS_CLASSES[status], className)} {...props} />;
}

export default StatusBadge;
