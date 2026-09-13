import { useId, type ReactNode } from 'react';

import { cn } from '@/lib/cn';

export interface FieldControlProps {
  id: string;
  invalid: boolean;
  'aria-describedby': string | undefined;
}

interface FieldProps {
  label: string;
  error?: string | undefined;
  hint?: string | undefined;
  className?: string;
  /** Receives the wiring for id, validity and description so no form has to repeat it. */
  children: (control: FieldControlProps) => ReactNode;
}

interface FieldSetProps {
  legend: string;
  error?: string | undefined;
  hint?: string | undefined;
  className?: string;
  children: ReactNode;
}

/**
 * The group counterpart of Field. A set of checkboxes or radios has no single control to label,
 * so the grouping element carries the name and the description instead of an htmlFor.
 */
export function FieldSet({ legend, error, hint, className, children }: FieldSetProps) {
  const id = useId();
  const errorId = `${id}-error`;
  const hintId = `${id}-hint`;

  return (
    <fieldset
      aria-describedby={error ? errorId : hint ? hintId : undefined}
      className={cn('flex min-w-0 flex-col gap-1.5', className)}
    >
      <legend className="text-ink mb-1.5 text-sm font-medium">{legend}</legend>
      {children}
      {error ? (
        <p id={errorId} role="alert" className="text-danger text-xs">
          {error}
        </p>
      ) : hint ? (
        <p id={hintId} className="text-faint text-xs">
          {hint}
        </p>
      ) : null}
    </fieldset>
  );
}

export function Field({ label, error, hint, className, children }: FieldProps) {
  const id = useId();
  const errorId = `${id}-error`;
  const hintId = `${id}-hint`;
  const describedBy = error ? errorId : hint ? hintId : undefined;

  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <label htmlFor={id} className="text-ink text-sm font-medium">
        {label}
      </label>
      {children({ id, invalid: Boolean(error), 'aria-describedby': describedBy })}
      {error ? (
        <p id={errorId} role="alert" className="text-danger text-xs">
          {error}
        </p>
      ) : hint ? (
        <p id={hintId} className="text-faint text-xs">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
