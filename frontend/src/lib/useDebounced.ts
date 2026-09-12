import { useEffect, useState } from 'react';

/**
 * Holds a value back until it stops changing, so a filter bound to a text field issues one
 * request per pause rather than one per keystroke.
 */
export function useDebounced<T>(value: T, delayMs = 300): T {
  const [settled, setSettled] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => {
      setSettled(value);
    }, delayMs);
    return () => {
      clearTimeout(timer);
    };
  }, [value, delayMs]);

  return settled;
}
