import { Outlet } from "react-router-dom";

/**
 * The pages that still use Material Tailwind hard-code light backgrounds
 * (`bg-white` cards, `bg-purple-50` roots), so in dark mode their inherited text
 * turns near-white on white. Pin them to the light tokens until U2 rebuilds
 * them, then delete this file.
 */
export function LegacyPage() {
    return (
        <div className="light bg-background bg-surface-gradient text-foreground">
            <Outlet />
        </div>
    );
}

export default LegacyPage;
