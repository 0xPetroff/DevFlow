const dateTimeFormat = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
  timeStyle: 'short',
});

const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' });

const relativeFormat = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });

const RELATIVE_UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['year', 365 * 24 * 60 * 60_000],
  ['month', 30 * 24 * 60 * 60_000],
  ['week', 7 * 24 * 60 * 60_000],
  ['day', 24 * 60 * 60_000],
  ['hour', 60 * 60_000],
  ['minute', 60_000],
];

export function formatDateTime(value: string | null | undefined): string {
  const date = toDate(value);
  return date ? dateTimeFormat.format(date) : '--';
}

export function formatDate(value: string | null | undefined): string {
  const date = toDate(value);
  return date ? dateFormat.format(date) : '--';
}

export function formatRelative(value: string | null | undefined): string {
  const date = toDate(value);
  if (!date) {
    return 'never';
  }
  const elapsed = date.getTime() - Date.now();
  for (const [unit, span] of RELATIVE_UNITS) {
    if (Math.abs(elapsed) >= span) {
      return relativeFormat.format(Math.round(elapsed / span), unit);
    }
  }
  return 'just now';
}

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return '?';
  }
  const first = parts[0]?.[0] ?? '';
  const last = parts.length > 1 ? (parts[parts.length - 1]?.[0] ?? '') : '';
  return (first + last).toUpperCase();
}

function toDate(value: string | null | undefined): Date | null {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}
