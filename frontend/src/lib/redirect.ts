/**
 * Reads the path a guard stashed in router state. Anything that is not a same-site absolute
 * path is discarded, so a crafted link cannot turn the login redirect into an open redirect.
 */
export function redirectTarget(state: unknown, fallback = '/'): string {
  if (typeof state === 'object' && state !== null && 'from' in state) {
    const from: unknown = (state as Record<string, unknown>).from;
    if (typeof from === 'string' && from.startsWith('/') && !from.startsWith('//')) {
      return from;
    }
  }
  return fallback;
}
