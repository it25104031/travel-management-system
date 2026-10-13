import { useId } from 'react';

/** Label + input (or any control passed as children) + optional hint. */
export default function Field({ label, hint, children, className = '' }) {
  const id = useId();
  const hintId = hint ? `${id}-hint` : undefined;
  const control = typeof children === 'function' ? children({ id, 'aria-describedby': hintId }) : children;
  return (
    <div className={`field ${className}`}>
      <label htmlFor={id}>{label}</label>
      {control}
      {hint && (
        <p className="hint" id={hintId}>
          {hint}
        </p>
      )}
    </div>
  );
}
