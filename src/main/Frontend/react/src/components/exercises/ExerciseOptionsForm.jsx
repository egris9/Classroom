import { useId, useState } from "react";
import { Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Slider } from "@/components/ui/slider";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import {
    DEFAULT_EXERCISE_OPTIONS,
    DIFFICULTY_HINTS,
    DIFFICULTY_LABELS,
    MAX_EXERCISE_COUNT,
    MIN_EXERCISE_COUNT,
    TYPE_LABELS,
} from "@/lib/exercises";
import { getPref, setPref } from "@/lib/prefs";
import { cn } from "@/lib/utils";

const PREF_KEY = "exercise";

/** The remembered choice, repaired where the browser holds something this version does not know. */
function storedOptions() {
    const stored = getPref(PREF_KEY, DEFAULT_EXERCISE_OPTIONS);
    const types = Array.isArray(stored?.types) ? stored.types.filter((type) => TYPE_LABELS[type]) : [];
    const count = Number.isInteger(stored?.count) ? stored.count : DEFAULT_EXERCISE_OPTIONS.count;
    return {
        types: types.length > 0 ? types : DEFAULT_EXERCISE_OPTIONS.types,
        difficulty: DIFFICULTY_LABELS[stored?.difficulty] ? stored.difficulty : DEFAULT_EXERCISE_OPTIONS.difficulty,
        count: Math.min(MAX_EXERCISE_COUNT, Math.max(MIN_EXERCISE_COUNT, count)),
    };
}

function OptionsFields({ onSubmit, busy }) {
    const [options, setOptions] = useState(storedOptions);
    const id = useId();
    const typesId = `${id}-types`;
    const difficultyId = `${id}-difficulty`;
    const countId = `${id}-count`;

    const submit = (event) => {
        event.preventDefault();
        setPref(PREF_KEY, options);
        onSubmit(options);
    };

    return (
        <form onSubmit={submit} className="space-y-5">
            <div className="grid gap-2">
                <Label id={typesId}>Types</Label>
                <ToggleGroup
                    type="multiple"
                    variant="outline"
                    aria-labelledby={typesId}
                    value={options.types}
                    // Switching the last one off does nothing: a set always has a type.
                    onValueChange={(types) => types.length > 0 && setOptions((current) => ({ ...current, types }))}
                    className="flex-wrap justify-start gap-2"
                >
                    {Object.entries(TYPE_LABELS).map(([type, label]) => (
                        <ToggleGroupItem key={type} value={type} className="h-11 gap-2 px-4">
                            {options.types.includes(type) && <Check aria-hidden="true" />}
                            {label}
                        </ToggleGroupItem>
                    ))}
                </ToggleGroup>
                <p className="text-sm text-muted-foreground">Pick more than one for a mixed set.</p>
            </div>

            <div className="grid gap-2">
                <Label id={difficultyId}>Difficulty</Label>
                <RadioGroup
                    aria-labelledby={difficultyId}
                    value={options.difficulty}
                    onValueChange={(difficulty) => setOptions((current) => ({ ...current, difficulty }))}
                    className="grid-cols-1 gap-2 sm:grid-cols-3"
                >
                    {Object.entries(DIFFICULTY_LABELS).map(([value, label]) => (
                        <Label
                            key={value}
                            htmlFor={`${id}-${value}`}
                            className={cn(
                                "flex min-h-11 cursor-pointer items-center gap-2 rounded-md border px-3 text-sm font-medium transition-colors",
                                options.difficulty === value ? "border-primary bg-accent" : "hover:bg-muted",
                            )}
                        >
                            <RadioGroupItem id={`${id}-${value}`} value={value} />
                            {label}
                        </Label>
                    ))}
                </RadioGroup>
                <p className="text-sm text-muted-foreground">{DIFFICULTY_HINTS[options.difficulty]}</p>
            </div>

            <div className="grid gap-2">
                <div className="flex items-center justify-between gap-2">
                    <Label htmlFor={countId}>Number of questions</Label>
                    <span className="text-sm font-medium tabular-nums" aria-hidden="true">
                        {options.count}
                    </span>
                </div>
                <Slider
                    id={countId}
                    aria-label="Number of questions"
                    min={MIN_EXERCISE_COUNT}
                    max={MAX_EXERCISE_COUNT}
                    step={1}
                    value={[options.count]}
                    onValueChange={([count]) => setOptions((current) => ({ ...current, count }))}
                    className="h-11"
                />
            </div>

            <Button type="submit" className="h-11 w-full" disabled={busy}>
                {busy ? "Generating..." : "Generate"}
            </Button>
        </form>
    );
}

/**
 * The "New exercise set" dialog: types (one or more), difficulty and count. It starts from the choice this browser
 * remembers, saves the new choice when it is submitted, and hands it to `onSubmit({ types, difficulty, count })`.
 */
export function ExerciseOptionsForm({ open, onOpenChange, onSubmit, busy = false }) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>New exercise set</DialogTitle>
                    <DialogDescription>Choose what the questions should be like.</DialogDescription>
                </DialogHeader>
                <OptionsFields onSubmit={onSubmit} busy={busy} />
            </DialogContent>
        </Dialog>
    );
}

export default ExerciseOptionsForm;
