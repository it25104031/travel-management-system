const LABELS = {
  PAID: 'Paid',
  PARTIAL: 'Part paid',
  UNPAID: 'Unpaid',
  PENDING: 'Pending',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  COMPLETED: 'Completed',
  FAILED: 'Failed',
};

const TONES = {
  PAID: 'good',
  APPROVED: 'good',
  COMPLETED: 'good',
  PARTIAL: 'warn',
  PENDING: 'warn',
  UNPAID: 'bad',
  REJECTED: 'bad',
  FAILED: 'bad',
};

/** A rubber-stamp style status mark. `fresh` plays the stamp animation once. */
export default function StatusStamp({ status, fresh = false, large = false }) {
  const tone = TONES[status] ?? 'muted';
  const className = ['stamp', `stamp-${tone}`, fresh && 'stamp-fresh', large && 'stamp-large']
    .filter(Boolean)
    .join(' ');
  return <span className={className}>{LABELS[status] ?? status}</span>;
}
