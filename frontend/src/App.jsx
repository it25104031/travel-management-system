import { homeFor, useAuth } from './auth.jsx';
import { matchPath, Redirect, useRouter } from './router.jsx';
import AppShell from './components/AppShell.jsx';
import Counter from './pages/Counter.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Invoices from './pages/Invoices.jsx';
import Login from './pages/Login.jsx';
import MySlips from './pages/MySlips.jsx';
import NotFound from './pages/NotFound.jsx';
import Payments from './pages/Payments.jsx';
import PrintInvoice from './pages/PrintInvoice.jsx';
import Register from './pages/Register.jsx';
import SlipReview from './pages/SlipReview.jsx';

// role: 'ADMIN' = admins only, 'ANY' = any logged-in user
const ROUTES = [
  { path: '/admin', role: 'ADMIN', page: Dashboard },
  { path: '/admin/invoices', role: 'ADMIN', page: Invoices },
  { path: '/admin/payments', role: 'ADMIN', page: Payments },
  { path: '/admin/counter', role: 'ADMIN', page: Counter },
  { path: '/admin/slips', role: 'ADMIN', page: SlipReview },
  { path: '/admin/invoices/:id/print', role: 'ADMIN', page: PrintInvoice, bare: true },
  { path: '/my/slips', role: 'ANY', page: MySlips },
];

export default function App() {
  const { user, checking } = useAuth();
  const { pathname } = useRouter();

  if (checking) {
    return <div className="boot" role="status">Loading…</div>;
  }

  // Public pages
  if (pathname === '/login' || pathname === '/register') {
    if (user) return <Redirect to={homeFor(user)} />;
    return pathname === '/login' ? <Login /> : <Register />;
  }

  if (!user) return <Redirect to="/login" />;
  if (pathname === '/') return <Redirect to={homeFor(user)} />;

  for (const route of ROUTES) {
    const params = matchPath(route.path, pathname);
    if (!params) continue;
    if (route.role === 'ADMIN' && user.role !== 'ADMIN') return <Redirect to={homeFor(user)} />;
    const Page = route.page;
    if (route.bare) return <Page {...params} />;
    return <AppShell><Page {...params} /></AppShell>;
  }

  return <AppShell><NotFound /></AppShell>;
}
