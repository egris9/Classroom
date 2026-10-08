import { useState } from "react";
import { ChevronDown, KeyRound, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import CreateCourseDialog from "./CreateCourseDialog.jsx";
import JoinCourseDialog from "./JoinCourseDialog.jsx";

/** The Add course button: a menu with "Join with code" and "Create", each opening a dialog. */
export function AddCourseMenu() {
    const [dialog, setDialog] = useState(null);

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button className="h-11 gap-2 px-4">
                        <Plus className="h-4 w-4" aria-hidden="true" />
                        Add course
                        <ChevronDown className="h-4 w-4" aria-hidden="true" />
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                    <DropdownMenuItem className="min-h-11" onSelect={() => setDialog("join")}>
                        <KeyRound className="mr-2 h-4 w-4" aria-hidden="true" />
                        Join with code
                    </DropdownMenuItem>
                    <DropdownMenuItem className="min-h-11" onSelect={() => setDialog("create")}>
                        <Plus className="mr-2 h-4 w-4" aria-hidden="true" />
                        Create
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>
            <JoinCourseDialog open={dialog === "join"} onOpenChange={(open) => !open && setDialog(null)} />
            <CreateCourseDialog open={dialog === "create"} onOpenChange={(open) => !open && setDialog(null)} />
        </>
    );
}

export default AddCourseMenu;
