const moneyFormat = new Intl.NumberFormat('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** 250000 -> "LKR 250,000.00" */
export function money(value) {
  return `LKR ${moneyFormat.format(Number(value ?? 0))}`;
}

/** 250000 -> "250,000.00" (for table cells where the column header says LKR) */
export function amount(value) {
  return moneyFormat.format(Number(value ?? 0));
}

const dateFormat = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
const dateTimeFormat = new Intl.DateTimeFormat('en-GB', {
  day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
});

export function date(value) {
  return value ? dateFormat.format(new Date(value)) : '';
}

export function dateTime(value) {
  return value ? dateTimeFormat.format(new Date(value)) : '';
}

/** Value for <input type="datetime-local">, e.g. "2026-10-08T13:05" */
export function toInputDateTime(value) {
  const d = value ? new Date(value) : new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function toInputDate(value) {
  return toInputDateTime(value).slice(0, 10);
}

export function fileSize(bytes) {
  if (!bytes) return '';
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function plural(n, one, many = `${one}s`) {
  return `${n} ${n === 1 ? one : many}`;
}

const METHOD_LABELS = {
  CARD: 'Card',
  CASH: 'Cash',
  BANK_TRANSFER: 'Bank transfer',
  SLIP_TRANSFER: 'Bank slip',
  ONLINE: 'Online',
};
export function methodLabel(code) {
  return METHOD_LABELS[code] ?? code?.replace(/_/g, ' ').toLowerCase();
}
