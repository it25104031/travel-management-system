import { useState } from 'react';
import { api } from '../api.js';
import { Link, useRouter } from '../router.jsx';
import Field from '../components/Field.jsx';
import Notice from '../components/Notice.jsx';

export default function Register() {
  const { navigate } = useRouter();
  const [form, setForm] = useState({ fullName: '', email: '', username: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await api.register(form);
      navigate('/login?registered=1', { replace: true });
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
          <h1 className="auth-title">Create your account</h1>
          <Notice tone="error">{error}</Notice>

          <form onSubmit={handleSubmit} className="stack">
            <Field label="Full name">
              {(p) => <input {...p} value={form.fullName} onChange={set('fullName')} autoComplete="name" required maxLength={100} />}
            </Field>
            <Field label="Email">
              {(p) => <input {...p} type="email" value={form.email} onChange={set('email')} autoComplete="email" required maxLength={100} />}
            </Field>
            <Field label="Username" hint="3–50 characters: letters, numbers, dots, dashes or underscores.">
              {(p) => (
                <input {...p} value={form.username} onChange={set('username')} autoComplete="username"
                  required minLength={3} maxLength={50} pattern="[A-Za-z0-9._\-]{3,50}" />
              )}
            </Field>
            <Field label="Password" hint="At least 6 characters.">
              {(p) => (
                <input {...p} type="password" value={form.password} onChange={set('password')}
                  autoComplete="new-password" required minLength={6} />
              )}
            </Field>
            <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
              {busy ? 'Creating account…' : 'Create account'}
            </button>
          </form>

          <p className="auth-switch">
            Already have an account? <Link to="/login">Log in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
