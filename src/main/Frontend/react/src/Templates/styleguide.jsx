import { useId } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetHeader,
    SheetTitle,
    SheetTrigger,
} from "@/components/ui/sheet";
import { PageContainer } from "@/components/PageContainer";
import { StatusBadge } from "@/components/StatusBadge";
import { cn } from "@/lib/utils";

// Full class names so Tailwind can see them.
const TOKENS = [
    { group: "Mauve: surfaces, text, borders", items: [
        { name: "--background", swatch: "bg-background text-foreground border" },
        { name: "--foreground", swatch: "bg-foreground text-background" },
        { name: "--card", swatch: "bg-card text-card-foreground border" },
        { name: "--muted", swatch: "bg-muted text-muted-foreground border" },
        { name: "--border", swatch: "bg-border text-foreground" },
    ] },
    { group: "Magenta: the action colour", items: [
        { name: "--primary", swatch: "bg-primary text-primary-foreground" },
        { name: "--ring", swatch: "bg-ring text-primary-foreground" },
    ] },
    { group: "Emerald: published, saved, done", items: [
        { name: "--success", swatch: "bg-success text-success-foreground" },
    ] },
    { group: "Amber: pending, needs attention", items: [
        { name: "--warning", swatch: "bg-warning text-warning-foreground" },
    ] },
    { group: "Red: delete, leave, errors", items: [
        { name: "--destructive", swatch: "bg-destructive text-destructive-foreground" },
    ] },
];

function TokenSwatches() {
    return (
        <div className="space-y-5">
            {TOKENS.map((group) => (
                <div key={group.group}>
                    <h4 className="mb-2 text-sm font-semibold">{group.group}</h4>
                    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                        {group.items.map((token) => (
                            <div
                                key={token.name}
                                className={cn("flex h-16 items-end rounded-md p-2 font-mono text-xs", token.swatch)}
                            >
                                {token.name}
                            </div>
                        ))}
                    </div>
                </div>
            ))}
        </div>
    );
}

function TypeScale() {
    return (
        <div className="space-y-3">
            <p className="font-display text-[clamp(2.5rem,6vw,4.5rem)] font-semibold leading-none">Display</p>
            <h2 className="text-3xl font-semibold">Heading two</h2>
            <h3 className="text-xl font-semibold">Heading three</h3>
            <p className="max-w-prose text-base leading-relaxed">
                Body text in Public Sans, kept under 70 characters a line so long reading stays calm.
            </p>
            <p className="text-sm text-muted-foreground">Small muted text for hints and captions.</p>
        </div>
    );
}

function Components() {
    const emailId = useId();
    return (
        <div className="space-y-6">
            <div className="flex flex-wrap gap-2">
                <Button>Primary</Button>
                <Button variant="secondary">Secondary</Button>
                <Button variant="outline">Outline</Button>
                <Button variant="ghost">Ghost</Button>
                <Button variant="link">Link</Button>
                <Button variant="destructive">Delete</Button>
                <Button disabled>Disabled</Button>
            </div>
            <div className="flex flex-wrap gap-2">
                <Badge>Default</Badge>
                <Badge variant="secondary">Secondary</Badge>
                <Badge variant="outline">Outline</Badge>
                <StatusBadge status="success">Published</StatusBadge>
                <StatusBadge status="warning">Pending</StatusBadge>
                <StatusBadge status="destructive">Failed</StatusBadge>
            </div>
            <div className="grid max-w-sm gap-2">
                <Label htmlFor={emailId}>Email</Label>
                <Input id={emailId} type="email" placeholder="you@school.org" />
            </div>
            <Card>
                <CardHeader>
                    <CardTitle>Biology 101</CardTitle>
                    <CardDescription>Taught by Ms. Rivera</CardDescription>
                </CardHeader>
                <CardContent className="text-sm">12 files, 3 summaries published.</CardContent>
            </Card>
            <Separator />
        </div>
    );
}

// A plain function, not a component: it is called twice with a theme class.
const specimen = (theme) => {
    return (
        <div key={theme} className={cn(theme, "rounded-lg border bg-background bg-surface-gradient p-5 text-foreground")}>
            <h3 className="mb-4 text-xl font-semibold capitalize">{theme} theme</h3>
            <div className="space-y-8">
                <TokenSwatches />
                <Separator />
                <TypeScale />
                <Separator />
                <Components />
            </div>
        </div>
    );
};

export default function Styleguide() {
    return (
        <PageContainer className="space-y-10">
            <div className="space-y-2">
                <h1 className="font-display text-[clamp(2.5rem,6vw,4.5rem)] font-semibold leading-none">Styleguide</h1>
                <p className="max-w-prose text-muted-foreground">
                    Every token, the type scale and each shadcn component in use, shown in both themes.
                </p>
            </div>

            <div className="grid gap-6 lg:grid-cols-2">
                {specimen("light")}
                {specimen("dark")}
            </div>

            <section className="space-y-4">
                <h2 className="text-2xl font-semibold">Overlays</h2>
                <p className="max-w-prose text-sm text-muted-foreground">
                    These open above the page, so they follow the theme toggle in the top bar.
                </p>
                <div className="flex flex-wrap gap-2">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button variant="outline">Dropdown menu</Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent>
                            <DropdownMenuItem>Create a course</DropdownMenuItem>
                            <DropdownMenuItem>Join a course</DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                    <Sheet>
                        <SheetTrigger asChild>
                            <Button variant="outline">Sheet</Button>
                        </SheetTrigger>
                        <SheetContent>
                            <SheetHeader>
                                <SheetTitle>Sheet</SheetTitle>
                                <SheetDescription>Slides in from the edge. The mobile menu uses it.</SheetDescription>
                            </SheetHeader>
                        </SheetContent>
                    </Sheet>
                    <Button variant="outline" onClick={() => toast("Saved", { description: "Toast host works." })}>
                        Toast
                    </Button>
                    <Button variant="outline" onClick={() => toast.success("Published")}>
                        Success toast
                    </Button>
                    <Button variant="outline" onClick={() => toast.error("Upload failed")}>
                        Error toast
                    </Button>
                </div>
            </section>
        </PageContainer>
    );
}
