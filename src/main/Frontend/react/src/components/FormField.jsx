import { useId } from "react";
import { Label } from "@/components/ui/label";

// Fields are 44px high. An invalid field gets a red edge and a thicker focus ring, so the state is not carried by the message alone.
const FIELD_CLASS =
    "h-11 focus-visible:ring-2 aria-[invalid=true]:border-destructive aria-[invalid=true]:focus-visible:ring-destructive";

/**
 * A label, a control and the field's own error. `children` is a function that receives the props the control must
 * carry (id, aria-invalid, aria-describedby, className), so the message is read with the field and a screen reader
 * hears it when the field is focused. The error text is in the DOM only while there is an error.
 */
export function FormField({ id, label, error, children }) {
    const generatedId = useId();
    const fieldId = id ?? generatedId;
    const errorId = `${fieldId}-error`;

    return (
        <div className="space-y-2">
            <Label htmlFor={fieldId}>{label}</Label>
            {children({
                id: fieldId,
                "aria-invalid": error ? "true" : undefined,
                "aria-describedby": error ? errorId : undefined,
                className: FIELD_CLASS,
            })}
            {error && (
                <p id={errorId} className="text-sm text-destructive">
                    {error}
                </p>
            )}
        </div>
    );
}

export default FormField;
