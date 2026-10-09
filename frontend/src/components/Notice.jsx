/** Inline message. tone: 'error' | 'success' | 'info' */
export default function Notice({ tone = 'info', children, onDismiss }) {
  if (!children) return null;
  return (
    <div className={`notice notice-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <div className="notice-body">{children}</div>
      {onDismiss && (
        <button type="button" className="notice-close" onClick={onDismiss} aria-label="Dismiss message">
          ×
        </button>
      )}
    </div>
  );
}
