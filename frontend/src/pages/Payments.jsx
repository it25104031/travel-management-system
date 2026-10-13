import { useMemo, useState } from 'react';
import { api } from '../api.js';
import { amount, dateTime, methodLabel, money, toInputDateTime } from '../format.js';
import ConfirmButton from '../components/ConfirmButton.jsx';
import Field from '../components/Field.jsx';
import Notice from '../components/Notice.jsx';
import PageHeader from '../components/PageHeader.jsx';
import StatusStamp from '../components/StatusStamp.jsx';
import { Empty, Loading } from '../components/Loading.jsx';
import useLoad from '../components/useLoad.js';

export const PAYMENT_METHODS = ['CASH', 'CARD', 'BANK_TRANSFER', 'SLIP_TRANSFER', 'ONLINE'];
const PAYMENT_STATUSES = [
  { value: 'COMPLETED', label: 'Completed (counts towards the invoice)' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'FAILED', label: 'Failed' },
];

export default function Payments() {
  const { data, error: loadError, loading, reload } = useLoad(
    () => Promise.all([api.payments(), api.invoices()]).then(([payments, invoices]) => ({ payments, invoices })),
  );
  const [editing, setEditing] = useState(null); // null | 'new' | payment
  const [query, setQuery] = useState('');
  const [message, setMessage] = useState({});

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (data?.payments ?? []).filter((p) => !q
      || p.invoiceNumber?.toLowerCase().includes(q)
      || p.customerName?.toLowerCase().includes(q));
  }, [data, query]);

  const handleSaved = (saved, wasNew) => {
    setEditing(null);
    setMessage({ tone: 'success', text: `${wasNew ? 'Recorded' : 'Updated'} ${money(saved.amount)} on ${saved.invoiceNumber}.` });
    reload();
  };

  const handleDelete = async (p) => {
    try {
      await api.deletePayment(p.paymentId);
      setMessage({ tone: 'success', text: `Deleted the ${money(p.amount)} payment on ${p.invoiceNumber}.` });
      reload();
    } catch (err) {
      setMessage({ tone: 'error', text: err.message });
    }
  };

  return (
    <div className="page">
      <PageHeader
        title="Payments"
        intro="Every payment recorded against an invoice, newest first."
        actions={editing === null && (
          <button type="button" className="btn btn-primary" onClick={() => { setEditing('new'); setMessage({}); }}>
            Record payment
          </button>
        )}
      />

      <Notice tone={message.tone} onDismiss={() => setMessage({})}>{message.text}</Notice>
      <Notice tone="error">{loadError}</Notice>

      {editing !== null && data && (
        <PaymentForm
          payment={editing === 'new' ? null : editing}
          invoices={data.invoices}
          onCancel={() => setEditing(null)}
          onSaved={handleSaved}
        />
      )}

      <div className="toolbar">
        <span className="toolbar-note">{data ? `${data.payments.length} payments` : ''}</span>
        <input
          type="search"
          className="search"
          placeholder="Search invoice or customer"
          aria-label="Search payments"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      {loading && !data && <Loading />}

      {data && (visible.length === 0 ? (
        <Empty title={data.payments.length === 0 ? 'No payments yet' : 'No payments match'}>
          {data.payments.length === 0 ? 'Use “Record payment” or the counter to add one.' : 'Try another search.'}
        </Empty>
      ) : (
        <div className="table-wrap panel">
          <table className="table">
            <thead>
              <tr>
                <th scope="col">Date</th>
                <th scope="col">Invoice</th>
                <th scope="col">Customer</th>
                <th scope="col">Method</th>
                <th scope="col" className="right">Amount (LKR)</th>
                <th scope="col">Status</th>
                <th scope="col"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {visible.map((p) => (
                <tr key={p.paymentId}>
                  <td className="nowrap">{dateTime(p.paymentDate)}</td>
                  <td className="strong">{p.invoiceNumber}</td>
                  <td>{p.customerName}</td>
                  <td>{methodLabel(p.paymentMethod)}</td>
                  <td className="right num">{amount(p.amount)}</td>
                  <td><StatusStamp status={p.paymentStatus} /></td>
                  <td className="right">
                    <div className="actions">
                    <button type="button" className="btn btn-small" onClick={() => { setEditing(p); setMessage({}); }}>
                      Edit
                    </button>
                    <ConfirmButton className="btn btn-small btn-danger" confirmLabel="Delete?" onConfirm={() => handleDelete(p)}>
                      Delete
                    </ConfirmButton>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}
    </div>
  );
}

function PaymentForm({ payment, invoices, onCancel, onSaved }) {
  const isNew = !payment;

  // Invoices that can take money: open ones, plus the payment's own invoice when editing.
  const choices = invoices.filter((i) => Number(i.outstandingBalance) > 0 || i.invoiceId === payment?.invoiceId);

  const [form, setForm] = useState(() => ({
    invoiceId: payment ? String(payment.invoiceId) : '',
    paymentDate: toInputDateTime(payment?.paymentDate),
    amount: payment ? String(payment.amount) : '',
    paymentMethod: payment?.paymentMethod ?? 'CASH',
    paymentStatus: payment?.paymentStatus ?? 'COMPLETED',
  }));
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const selected = invoices.find((i) => String(i.invoiceId) === form.invoiceId);
  // When editing a completed payment on the same invoice, its own amount is available again.
  let available = selected ? Number(selected.outstandingBalance) : 0;
  if (selected && payment && payment.invoiceId === selected.invoiceId && payment.paymentStatus === 'COMPLETED') {
    available += Number(payment.amount);
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    const body = { ...form, invoiceId: Number(form.invoiceId), amount: Number(form.amount) };
    try {
      const saved = isNew ? await api.createPayment(body) : await api.updatePayment(payment.paymentId, body);
      onSaved(saved, isNew);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="panel form-panel" onSubmit={handleSubmit}>
      <h2>{isNew ? 'Record a payment' : `Edit payment on ${payment.invoiceNumber}`}</h2>
      <Notice tone="error">{error}</Notice>

      {choices.length === 0 ? (
        <p className="form-note">Every invoice is fully paid. Create an invoice first.</p>
      ) : (
        <>
          <div className="form-grid">
            <Field label="Invoice" className="span-2">
              {(p) => (
                <select {...p} value={form.invoiceId} onChange={set('invoiceId')} required>
                  <option value="" disabled>Choose an invoice</option>
                  {choices.map((i) => (
                    <option key={i.invoiceId} value={i.invoiceId}>
                      {i.invoiceNumber}, {i.customerName}: {money(i.outstandingBalance)} outstanding
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field
              label="Amount (LKR)"
              hint={selected ? `Up to ${money(available)} can be paid on this invoice.` : undefined}
            >
              {(p) => (
                <div className="input-with-action">
                  <input {...p} type="number" inputMode="decimal" step="0.01" min="0.01"
                    max={selected ? available : undefined}
                    value={form.amount} onChange={set('amount')} required />
                  {selected && available > 0 && (
                    <button type="button" className="btn btn-small"
                      onClick={() => setForm({ ...form, amount: available.toFixed(2) })}>
                      Full balance
                    </button>
                  )}
                </div>
              )}
            </Field>
            <Field label="Payment date">
              {(p) => <input {...p} type="datetime-local" value={form.paymentDate} onChange={set('paymentDate')} required />}
            </Field>
            <Field label="Method">
              {(p) => (
                <select {...p} value={form.paymentMethod} onChange={set('paymentMethod')}>
                  {PAYMENT_METHODS.map((m) => <option key={m} value={m}>{methodLabel(m)}</option>)}
                </select>
              )}
            </Field>
            <Field label="Status">
              {(p) => (
                <select {...p} value={form.paymentStatus} onChange={set('paymentStatus')}>
                  {PAYMENT_STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
              )}
            </Field>
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? 'Saving…' : isNew ? 'Record payment' : 'Save changes'}
            </button>
            <button type="button" className="btn" onClick={onCancel}>Cancel</button>
          </div>
        </>
      )}
      {choices.length === 0 && (
        <div className="form-actions">
          <button type="button" className="btn" onClick={onCancel}>Close</button>
        </div>
      )}
    </form>
  );
}
