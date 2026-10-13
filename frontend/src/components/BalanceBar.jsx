import { amount } from '../format.js';

/** Thin bar showing how much of an invoice is paid. */
export default function BalanceBar({ paid, total, showLabel = true }) {
  const t = Number(total) || 0;
  const p = Math.min(Number(paid) || 0, t);
  const pct = t > 0 ? Math.round((p / t) * 100) : 100;
  return (
    <div className="balance">
      <div
        className="balance-track"
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={pct}
        aria-label={`${pct}% paid`}
      >
        <div className="balance-fill" style={{ width: `${pct}%` }} />
      </div>
      {showLabel && <span className="balance-label num">{amount(p)}</span>}
    </div>
  );
}
