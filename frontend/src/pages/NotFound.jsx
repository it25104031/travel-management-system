import { homeFor, useAuth } from '../auth.jsx';
import { Link } from '../router.jsx';

export default function NotFound() {
  const { user } = useAuth();
  return (
    <div className="page">
      <h1>That page doesn't exist</h1>
      <p className="intro">The link may be old or mistyped.</p>
      <p><Link to={homeFor(user)} className="btn">Go to your home page</Link></p>
    </div>
  );
}
