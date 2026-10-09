import { useMemo, useState } from 'react';
import { api } from '../api.js';
import { amount, date, money, toInputDateTime } from '../format.js';
import { Link } from '../router.jsx';
import BalanceBar from '../components/BalanceBar.jsx';
import ConfirmButton from '../components/ConfirmButton.jsx';
import Field from '../components/Field.jsx';
import Notice from '../components/Notice.jsx';
import PageHeader from '../components/PageHeader.jsx';
import StatusStamp from '../components/StatusStamp.jsx';
import { Empty, Loading } from '../components/Loading.jsx';
import useLoad from '../components/useLoad.js';

const FILTERS = [
  { key: 'ALL', label: 'All' },
  { key: 'UNPAID', label: 'Unpaid' },
  { key: 'PARTIAL', label: 'Part paid' },
  { key: 'PAID', label: 'Paid' },
];

const EMPTY_FORM = { invoiceNumber: '', customerName: '', invoiceDate: '', totalAmount: '' };

export default function Invoices() {
  const { data: invoices, error: loadError, loading, reload } = useLoad(() => api.invoices());
  const [filter, setFilter] = useState('ALL');
  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState(null); // null = closed, 'new' or an invoice
  const [message, setMessage] = useState({ tone: '', text: '' });

  const counts = useMemo(() => {
    const c = { ALL: 0, UNPAID: 0, PARTIAL: 0, PAID: 0 };
    (invoices ?? []).forEach((i) => {
      c.ALL += 1;
      c[i.status] = (c[i.status] ?? 0) + 1;
    });
    return c;
  }, [invoices]);

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (invoices ?? []).filter((i) =>
      (filter === 'ALL' || i.status === filter) &&
      (!q || i.invoiceNumber.toLowerCase().includes(q) || i.customerName.toLowerCase().includes(q)));
  }, [invoices, filter, query]);

  const handleSaved = (saved, wasNew) => {
    setEditing(null);
    setMessage({ tone: 'success', text: `Invoice ${saved.invoiceNumber} ${wasNew ? 'created' : 'updated'}.` });
    reload();
  };

  const handleDelete = async (inv) => {
    try {
      await api.deleteInvoice(inv.invoiceId);
      setMessage({ tone: 'success', text: `Invoice ${inv.invoiceNumber} deleted.` });
      reload();
    } catch (err) {
      setMessage({ tone: 'error', text: err.message });
    }
  };

  return (
    <div className="page">
      <PageHeader
        title="Invoices"
        intro="Status updates on its own as payments come in."
        actions={editing === null && (
          <button type="button" className="btn btn-primary" onClick={() => { setEditing('new'); setMessage({}); }}>
            New invoice
          </button>
        )}
      />

      <Notice tone={message.tone} onDismiss={() => setMessage({})}>{message.text}</Notice>

      {editing !== null && (
        <InvoiceForm
          invoice={editing === 'new' ? null : editing}
          onCancel={() => setEditing(null)}
          onSaved={handleSaved}
        />
      )}

      <div className="toolbar">
        <div className="tabs" role="group" aria-label="Filter by status">
          {FILTERS.map((f) => (
            <button
              key={f.key}
              type="button"
              className={filter === f.key ? 'tab active' : 'tab'}
              aria-pressed={filter === f.key}
              onClick={() => setFilter(f.key)}
            >
              {f.label} <span className="tab-count num">{counts[f.key] ?? 0}</span>
            </button>
          ))}
        </div>
        <input
          type="search"
          className="search"
          placeholder="Search number or customer"
          aria-label="Search invoices"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      <Notice tone="error">{loadError}</Notice>
      {loading && !invoices && <Loading />}

      {invoices && (visible.length === 0 ? (
        <Empty title={invoices.length === 0 ? 'No invoices yet' : 'No invoices match'}>
          {invoices.length === 0
            ? 'Create the first one with “New invoice”.'
            : 'Try a different status or search term.'}
        </Empty>
      ) : (
        <div className="table-wrap panel">
          <table className="table">
            <thead>
              <tr>
                <th scope="col">Invoice</th>
                <th scope="col">Customer</th>
                <th scope="col">Date</th>
                <th scope="col" className="right">Total (LKR)</th>
                <th scope="col" className="col-bar">Paid</th>
                <th scope="col" className="right">Outstanding</th>
                <th scope="col">Status</th>
                <th scope="col"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {visible.map((inv) => (
                <tr key={inv.invoiceId}>
                  <td className="strong">{inv.invoiceNumber}</td>
                  <td>{inv.customerName}</td>
                  <td className="nowrap">{date(inv.invoiceDate)}</td>
                  <td className="right num">{amount(inv.totalAmount)}</td>
                  <td className="col-bar"><BalanceBar paid={inv.paidAmount} total={inv.totalAmount} /></td>
                  <td className={`right num ${Number(inv.outstandingBalance) > 0 ? 'owed' : 'muted'}`}>
                    {amount(inv.outstandingBalance)}
                  </td>
                  <td><StatusStamp status={inv.status} /></td>
                  <td className="right">
                    <div className="actions">
                    <button type="button" className="btn btn-small" onClick={() => { setEditing(inv); setMessage({}); }}>
                      Edit
                    </button>
                    <Link to={`/admin/invoices/${inv.invoiceId}/print`} className="btn btn-small" target="_blank">
                      Print
                    </Link>
                    <ConfirmButton className="btn btn-small btn-danger" confirmLabel="Delete?" onConfirm={() => handleDelete(inv)}>
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

function InvoiceForm({ invoice, onCancel, onSaved }) {
  const isNew = !invoice;
  const [form, setForm] = useState(() => invoice
    ? {
      invoiceNumber: invoice.invoiceNumber,
      customerName: invoice.customerName,
      invoiceDate: toInputDateTime(invoice.invoiceDate),
      totalAmount: String(invoice.totalAmount),
    }
    : { ...EMPTY_FORM, invoiceDate: toInputDateTime() });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    const body = { ...form, totalAmount: Number(form.totalAmount) };
    try {
      const saved = isNew ? await api.createInvoice(body) : await api.updateInvoice(invoice.invoiceId, body);
      onSaved(saved, isNew);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="panel form-panel" onSubmit={handleSubmit}>
      <h2>{isNew ? 'New invoice' : `Edit ${invoice.invoiceNumber}`}</h2>
      {!isNew && (
        <p className="form-note">
          Already paid: <strong className="num">{money(invoice.paidAmount)}</strong>. The total can't go below this.
        </p>
      )}
      <Notice tone="error">{error}</Notice>

      <div className="form-grid">
        <Field label="Invoice number" hint="Must be unique, e.g. INV-0004.">
          {(p) => <input {...p} value={form.invoiceNumber} onChange={set('invoiceNumber')} required maxLength={30} autoFocus />}
        </Field>
        <Field label="Customer name">
          {(p) => <input {...p} value={form.customerName} onChange={set('customerName')} required maxLength={100} />}
        </Field>
        <Field label="Invoice date">
          {(p) => <input {...p} type="datetime-local" value={form.invoiceDate} onChange={set('invoiceDate')} required />}
        </Field>
        <Field label="Total amount (LKR)">
          {(p) => (
            <input {...p} type="number" inputMode="decimal" step="0.01" min="0.01"
              value={form.totalAmount} onChange={set('totalAmount')} required />
          )}
        </Field>
      </div>

      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? 'Saving…' : isNew ? 'Create invoice' : 'Save changes'}
        </button>
        <button type="button" className="btn" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}
