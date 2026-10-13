import { api } from '../api.js';
import { date, dateTime, money } from '../format.js';
import StatusStamp from '../components/StatusStamp.jsx';
import Notice from '../components/Notice.jsx';
import { Loading } from '../components/Loading.jsx';
import useLoad from '../components/useLoad.js';

/** Printable invoice, opened in its own tab (no sidebar). */
export default function PrintInvoice({ id }) {
  const { data: inv, error, loading } = useLoad(() => api.invoice(id), [id]);

  return (
    <div className="print-page">
      <div className="print-tools no-print">
        <button type="button" className="btn btn-primary" onClick={() => window.print()} disabled={!inv}>
          Print
        </button>
        <button type="button" className="btn" onClick={() => window.close()}>Close tab</button>
      </div>

      <Notice tone="error">{error}</Notice>
      {loading && <Loading />}

      {inv && (
        <article className="invoice-sheet">
          <div className="airmail sheet-stripe" aria-hidden="true" />
          <header className="sheet-head">
            <div>
              <p className="sheet-brand">Dahamdi Travels</p>
              <p className="muted">Travel bookings, tickets and visas</p>
            </div>
            <div className="sheet-id">
              <p className="sheet-label">Invoice</p>
              <p className="sheet-number">{inv.invoiceNumber}</p>
              <p className="muted">Issued {date(inv.invoiceDate)}</p>
            </div>
          </header>

          <section className="sheet-to">
            <p className="sheet-label">Billed to</p>
            <p className="sheet-customer">{inv.customerName}</p>
          </section>

          <table className="sheet-table">
            <tbody>
              <tr><th scope="row">Invoice total</th><td className="num">{money(inv.totalAmount)}</td></tr>
              <tr><th scope="row">Paid</th><td className="num">{money(inv.paidAmount)}</td></tr>
              <tr className="sheet-due">
                <th scope="row">Balance due</th>
                <td className="num">{money(inv.outstandingBalance)}</td>
              </tr>
            </tbody>
          </table>

          <div className="sheet-stamp"><StatusStamp status={inv.status} large /></div>

          <footer className="sheet-foot muted">
            Printed {dateTime(new Date())}. Pay by bank transfer and upload your slip in your account, or pay at our counter.
          </footer>
        </article>
      )}
    </div>
  );
}
