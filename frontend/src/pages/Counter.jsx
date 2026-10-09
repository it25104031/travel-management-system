import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { dateTime, methodLabel, money } from '../format.js';
import { Link, useRouter } from '../router.jsx';
import Field from '../components/Field.jsx';
import Notice from '../components/Notice.jsx';
import PageHeader from '../components/PageHeader.jsx';
import StatusStamp from '../components/StatusStamp.jsx';
import { Loading } from '../components/Loading.jsx';
import useLoad from '../components/useLoad.js';

const METHODS = ['CASH', 'CARD', 'BANK_TRANSFER'];
const NEW_INVOICE = 'new';

/**
 * Over-the-counter payments (the old "payment gateway" page).
 * Pay against an open invoice, or create an invoice for a walk-in customer and pay it in one go.
 */
export default function Counter() {
  const { search } = useRouter();
  const preselected = new URLSearchParams(search).get('invoice');

  const { data: invoices, error: loadError, loading, reload } = useLoad(() => api.invoices());
  const open = (invoices ?? []).filter((i) => Number(i.outstandingBalance) > 0);

  const [invoiceId, setInvoiceId] = useState(preselected ?? '');
  const [customerName, setCustomerName] = useState('');
  const [amountValue, setAmountValue] = useState('');
  const [method, setMethod] = useState('CASH');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [receipt, setReceipt] = useState(null);

  // Once invoices load, fall back to "new invoice" if nothing valid is selected.
  useEffect(() => {
    if (!invoices) return;
    if (invoiceId && invoiceId !== NEW_INVOICE && !open.some((i) => String(i.invoiceId) === invoiceId)) {
      setInvoiceId('');
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [invoices]);

  const selected = open.find((i) => String(i.invoiceId) === invoiceId);
  const isNew = invoiceId === NEW_INVOICE;
  const amountNumber = Number(amountValue) || 0;
  const remainingAfter = selected ? Number(selected.outstandingBalance) - amountNumber : 0;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (selected && amountNumber > Number(selected.outstandingBalance)) {
      setError(`That's more than the ${money(selected.outstandingBalance)} still owed on ${selected.invoiceNumber}.`);
      return;
    }
    setBusy(true);
    try {
      const result = await api.gatewayPay({
        invoiceId: isNew ? null : Number(invoiceId),
        customerName: isNew ? customerName : null,
        amount: amountNumber,
        paymentMethod: method,
      });
      setReceipt(result);
      setInvoiceId('');
      setCustomerName('');
      setAmountValue('');
      reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="page">
      <PageHeader
        title="Take a payment"
        intro="Record money received at the counter. It's added to the invoice straight away."
      />
      <Notice tone="error">{loadError}</Notice>
      {loading && !invoices && <Loading />}

      {invoices && (
        <div className="counter">
          <form className="panel form-panel" onSubmit={handleSubmit}>
            <Notice tone="error">{error}</Notice>

            <Field label="What is the customer paying for?">
              {(p) => (
                <select {...p} value={invoiceId} onChange={(e) => { setInvoiceId(e.target.value); setReceipt(null); }} required>
                  <option value="" disabled>Choose an invoice</option>
                  {open.map((i) => (
                    <option key={i.invoiceId} value={i.invoiceId}>
                      {i.invoiceNumber}, {i.customerName}: {money(i.outstandingBalance)} owed
                    </option>
                  ))}
                  <option value={NEW_INVOICE}>A new booking (create an invoice now)</option>
                </select>
              )}
            </Field>

            {isNew && (
              <Field label="Customer name" hint="A new invoice with this name and amount is created and marked paid.">
                {(p) => (
                  <input {...p} value={customerName} onChange={(e) => setCustomerName(e.target.value)}
                    required maxLength={100} />
                )}
              </Field>
            )}

            <Field label="Amount received (LKR)">
              {(p) => (
                <div className="input-with-action">
                  <input {...p} type="number" inputMode="decimal" step="0.01" min="0.01"
                    max={selected ? Number(selected.outstandingBalance) : undefined}
                    value={amountValue} onChange={(e) => setAmountValue(e.target.value)} required />
                  {selected && (
                    <button type="button" className="btn btn-small"
                      onClick={() => setAmountValue(Number(selected.outstandingBalance).toFixed(2))}>
                      Full balance
                    </button>
                  )}
                </div>
              )}
            </Field>

            <fieldset className="choice-group">
              <legend>Paid by</legend>
              {METHODS.map((m) => (
                <label key={m} className={method === m ? 'choice active' : 'choice'}>
                  <input type="radio" name="method" value={m} checked={method === m} onChange={() => setMethod(m)} />
                  {methodLabel(m)}
                </label>
              ))}
            </fieldset>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={busy || !invoiceId}>
                {busy ? 'Recording…' : amountNumber > 0 ? `Record ${money(amountNumber)}` : 'Record payment'}
              </button>
            </div>
          </form>

          <aside className="ticket" aria-live="polite">
            {receipt ? (
              <Receipt receipt={receipt} />
            ) : (
              <>
                <p className="ticket-label">Preview</p>
                <p className="ticket-number">
                  {isNew ? 'New invoice' : selected?.invoiceNumber ?? 'No invoice chosen'}
                </p>
                <p className="ticket-customer">{isNew ? customerName || 'Customer name' : selected?.customerName}</p>
                <div className="ticket-perf" aria-hidden="true" />
                <dl className="ticket-rows">
                  {selected && (
                    <div><dt>Owed now</dt><dd className="num">{money(selected.outstandingBalance)}</dd></div>
                  )}
                  <div><dt>Paying</dt><dd className="num">{money(amountNumber)}</dd></div>
                  {selected && (
                    <div className="ticket-total">
                      <dt>Left after this</dt>
                      <dd className={`num ${remainingAfter < 0 ? 'owed' : ''}`}>{money(Math.max(remainingAfter, 0))}</dd>
                    </div>
                  )}
                  <div><dt>Method</dt><dd>{methodLabel(method)}</dd></div>
                </dl>
              </>
            )}
          </aside>
        </div>
      )}
    </div>
  );
}

function Receipt({ receipt }) {
  const { invoice, payment, autoGenerated } = receipt;
  return (
    <>
      <p className="ticket-label">{autoGenerated ? 'Invoice created and paid' : 'Payment recorded'}</p>
      <p className="ticket-number">{invoice.invoiceNumber}</p>
      <p className="ticket-customer">{invoice.customerName}</p>
      <div className="ticket-stamp"><StatusStamp status={invoice.status} fresh large /></div>
      <div className="ticket-perf" aria-hidden="true" />
      <dl className="ticket-rows">
        <div><dt>Received</dt><dd className="num">{money(payment.amount)}</dd></div>
        <div><dt>Method</dt><dd>{methodLabel(payment.paymentMethod)}</dd></div>
        <div><dt>When</dt><dd>{dateTime(payment.paymentDate)}</dd></div>
        <div className="ticket-total"><dt>Still owed</dt><dd className="num">{money(invoice.outstandingBalance)}</dd></div>
      </dl>
      <Link to={`/admin/invoices/${invoice.invoiceId}/print`} target="_blank" className="btn btn-block">
        Print invoice
      </Link>
    </>
  );
}
