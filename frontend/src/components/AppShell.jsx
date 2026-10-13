import { useAuth } from '../auth.jsx';
import { Link, useRouter } from '../router.jsx';

const ADMIN_NAV = [
  { to: '/admin', label: 'Overview', exact: true },
  { to: '/admin/invoices', label: 'Invoices' },
  { to: '/admin/payments', label: 'Payments' },
  { to: '/admin/counter', label: 'Take a payment' },
  { to: '/admin/slips', label: 'Bank slips' },
];

const USER_NAV = [{ to: '/my/slips', label: 'My bank slips' }];

export default function AppShell({ children }) {
  const { user, logout } = useAuth();
  const { pathname, navigate } = useRouter();
  const nav = user?.role === 'ADMIN' ? ADMIN_NAV : USER_NAV;

  const isActive = (item) => (item.exact ? pathname === item.to : pathname.startsWith(item.to));

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="shell">
      <div className="airmail" aria-hidden="true" />
      <aside className="sidebar">
        <Link to={user?.role === 'ADMIN' ? '/admin' : '/my/slips'} className="brand">
          <span className="brand-name">Dahamdi Travels</span>
          <span className="brand-sub">Billing desk</span>
        </Link>

        <nav className="nav" aria-label="Main">
          {nav.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className={isActive(item) ? 'nav-link active' : 'nav-link'}
              aria-current={isActive(item) ? 'page' : undefined}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="who">
          <span className="who-name">{user?.fullName}</span>
          <span className="who-role">{user?.role === 'ADMIN' ? 'Administrator' : 'Customer'}</span>
          <button type="button" className="link-button on-dark" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </aside>

      <main className="main" id="main">
        {children}
      </main>
    </div>
  );
}
