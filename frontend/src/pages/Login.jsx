import { useState } from 'react';
import { homeFor, useAuth } from '../auth.jsx';
import { Link, useRouter } from '../router.jsx';
import Field from '../components/Field.jsx';
import Notice from '../components/Notice.jsx';

export default function Login() {
  const { login } = useAuth();
  const { navigate, search } = useRouter();
  const justRegistered = new URLSearchParams(search).get('registered') === '1';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const user = await login(username, password);
      navigate(homeFor(user), { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="envelope">
        <div className="envelope-inner">
          <p className="auth-brand">Dahamdi Travels</p>
          <h1 className="auth-title">Log in to the billing desk</h1>

          {justRegistered && <Notice tone="success">Your account is ready. Log in with your new username.</Notice>}
          <Notice tone="error">{error}</Notice>

          <form onSubmit={handleSubmit} className="stack">
            <Field label="Username">
              {(p) => (
                <input {...p} value={username} onChange={(e) => setUsername(e.target.value)}
                  autoComplete="username" required autoFocus />
              )}
            </Field>
            <Field label="Password">
              {(p) => (
                <input {...p} type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password" required />
              )}
            </Field>
            <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
              {busy ? 'Logging in…' : 'Log in'}
            </button>
          </form>

          <p className="auth-switch">
            New customer? <Link to="/register">Create an account</Link> to upload your bank slips.
          </p>
        </div>
      </div>
    </div>
  );
}
