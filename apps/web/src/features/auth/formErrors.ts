import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import { ApiError } from "@/lib/api/client";

/**
 * Map a thrown API error onto a react-hook-form. Server 400 `validationErrors`
 * are applied per field; anything else is returned as a form-level message.
 * Returns the general message to display, or null when the error was fully
 * mapped onto fields.
 */
export function applyApiError<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
): string | null {
  if (error instanceof ApiError) {
    if (error.validationErrors && Object.keys(error.validationErrors).length > 0) {
      for (const [field, message] of Object.entries(error.validationErrors)) {
        setError(field as Path<T>, { type: "server", message });
      }
      return null;
    }
    return error.message;
  }
  return "Something went wrong. Please try again.";
}
