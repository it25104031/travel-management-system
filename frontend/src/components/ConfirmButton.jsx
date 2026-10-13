import { useEffect, useState } from 'react';

/** Two-step button: first click asks "Confirm?", second click runs the action. */
export default function ConfirmButton({ children, confirmLabel = 'Click again to confirm', onConfirm, className = '' }) {
  const [armed, setArmed] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!armed) return undefined;
    const t = setTimeout(() => setArmed(false), 4000);
    return () => clearTimeout(t);
  }, [armed]);

  const handleClick = async () => {
    if (!armed) {
      setArmed(true);
      return;
    }
    setBusy(true);
    try {
      await onConfirm();
    } finally {
      setBusy(false);
      setArmed(false);
    }
  };

  return (
    <button
      type="button"
      className={`${className} ${armed ? 'is-armed' : ''}`}
      onClick={handleClick}
      disabled={busy}
      aria-live="polite"
    >
      {armed ? confirmLabel : children}
    </button>
  );
}
