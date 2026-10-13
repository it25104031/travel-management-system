export function Loading({ label = 'Loading…' }) {
  return (
    <p className="loading" role="status">
      {label}
    </p>
  );
}

export function Empty({ title, children }) {
  return (
    <div className="empty">
      <p className="empty-title">{title}</p>
      {children && <div className="empty-body">{children}</div>}
    </div>
  );
}
