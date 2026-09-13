import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';

import { ApiError } from '@/lib/apiError';

/**
 * Routes a failed request onto the form that caused it. Validation messages land on their own
 * fields; anything else is returned for the form-level banner so no error is silently dropped.
 */
export function applyApiError<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): string | null {
  const apiError = ApiError.from(error);
  let placed = false;

  for (const field of fields) {
    const message = apiError.fieldErrors[field];
    if (message !== undefined) {
      setError(field, { type: 'server', message });
      placed = true;
    }
  }

  return placed ? null : apiError.message;
}
