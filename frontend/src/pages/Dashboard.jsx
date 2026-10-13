import { api } from '../api.js';
import { amount, money, plural } from '../format.js';
import { Link } from '../router.jsx';
import BalanceBar from '../components/BalanceBar.jsx';
import StatusStamp from '../components/StatusStamp.jsx';
import Notice from '../components/Notice.jsx';
import { Empty, Loading } from '../components/Loading.jsx';
import useLoad from '../components/useLoad.js';

export default function Dashboard() {
  const { data, error, loading } = useLoad(() => api.dashboard());

  const today = new Intl.DateTimeFormat('en-GB', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());

  return (
    <div className="page">
      <p className="greeting">{today}</p>

      <Notice tone="error">{error}</Notice>
      {loading && <Loading />}

      {data && (
        <>
          <section className="headline" aria-label="Money still owed">
            {data.openInvoices > 0 ? (
              <h1 className="headline-text">
                <span className="num">{money(data.totalOutstanding)}</span> is still owed on{' '}
                {plural(data.openInvoices, 'open invoice')}.
              </h1>
            ) : (
              <h1 className="headline-text">Every invoice is paid in full.</h1>
            )}

            <dl className="facts">
              <div>
                <dt>Collected so far</dt>
                <dd className="num">{money(data.totalRevenue)}</dd>
              </div>
              <div>
                <dt>Payments recorded</dt>
                <dd className="num">{data.totalPayments}</dd>
              </div>
              <div>
                <dt>Invoices issued</dt>
                <dd className="num">{data.totalInvoices}</dd>
              </div>
              <div>
                <dt>Registered users</dt>
                <dd className="num">{data.totalUsers}</dd>
              </div>
            </dl>
          </section>

          {data.pendingSlips > 0 && (
            <Link to="/admin/slips" className="callout">
              <span className="callout-count num">{data.pendingSlips}</span>
              <span>
                <strong>
                  {data.pendingSlips === 1 ? 'bank slip is' : 'bank slips are'} waiting for review.
                </strong>{' '}
                {data.pendingSlips === 1 ? 'Approve it to record the payment.' : 'Approve them to record the payments.'}
              </span>
            </Link>
          )}

          <section className="panel">
            <div className="panel-head">
              <h2>Largest open balances</h2>
              <Link to="/admin/invoices" className="panel-link">All invoices</Link>
            </div>

            {data.topOpenInvoices.length === 0 ? (
              <Empty title="Nothing outstanding">New invoices you create will show up here until they're paid.</Empty>
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th scope="col">Invoice</th>
                      <th scope="col">Customer</th>
                      <th scope="col" className="col-bar">Paid so far</th>
                      <th scope="col" className="right">Outstanding (LKR)</th>
                      <th scope="col">Status</th>
                      <th scope="col"><span className="sr-only">Actions</span></th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.topOpenInvoices.map((inv) => (
                      <tr key={inv.invoiceId}>
                        <td className="strong">{inv.invoiceNumber}</td>
                        <td>{inv.customerName}</td>
                        <td className="col-bar"><BalanceBar paid={inv.paidAmount} total={inv.totalAmount} /></td>
                        <td className="right num owed">{amount(inv.outstandingBalance)}</td>
                        <td><StatusStamp status={inv.status} /></td>
                        <td className="right">
                          <Link to={`/admin/counter?invoice=${inv.invoiceId}`} className="btn btn-small">Take payment</Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}
