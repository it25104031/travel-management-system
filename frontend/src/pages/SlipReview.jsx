import { useState } from 'react';
import { api } from '../api.js';
import { date, dateTime, fileSize, money } from '../format.js';
import Notice from '../components/Notice.jsx';
import PageHeader from '../components/PageHeader.jsx';
import StatusStamp from '../components/StatusStamp.jsx';
import { Empty, Loading } from '../components/Loading.jsx';
import useLoad from '../components/useLoad.js';

const FILTERS = [
  { key: 'PENDING', label: 'Waiting' },
  { key: 'APPROVED', label: 'Approved' },
  { key: 'REJECTED', label: 'Rejected' },
  { key: 'ALL', label: 'All' },
];

export default function SlipReview() {
  const [filter, setFilter] = useState('PENDING');
  const { data: slips, error: loadError, loading, reload } = useLoad(() => api.allSlips(filter), [filter]);
  const [message, setMessage] = useState({});
  const [justDecided, setJustDecided] = useState(null);

  const decide = async (slip, action, remark) => {
    setMessage({});
    try {
      const updated = action === 'approve'
        ? await api.approveSlip(slip.slipId, remark)
        : await api.rejectSlip(slip.slipId, remark);
      setJustDecided(updated);
      setMessage({
        tone: 'success',
        text: action === 'approve'
          ? `Approved. ${money(slip.amountPaid)} is now recorded as a payment on ${slip.invoiceNumber}.`
          : `Rejected the slip for ${slip.invoiceNumber}. The customer will see your note.`,
      });
      reload();
    } catch (err) {
      setMessage({ tone: 'error', text: err.message });
    }
  };

  return (
    <div className="page">
      <PageHeader
        title="Bank slips"
        intro="Customers upload proof of bank transfers here. Approving a slip records the payment on its invoice."
      />

      <div className="toolbar">
        <div className="tabs" role="group" aria-label="Filter slips">
          {FILTERS.map((f) => (
            <button key={f.key} type="button" className={filter === f.key ? 'tab active' : 'tab'}
              aria-pressed={filter === f.key} onClick={() => { setFilter(f.key); setJustDecided(null); }}>
              {f.label}
            </button>
          ))}
        </div>
      </div>

      <Notice tone={message.tone} onDismiss={() => setMessage({})}>{message.text}</Notice>
      <Notice tone="error">{loadError}</Notice>
      {loading && !slips && <Loading />}

      {slips && (slips.length === 0 ? (
        <Empty title={filter === 'PENDING' ? 'No slips waiting' : 'No slips here'}>
          {filter === 'PENDING' ? 'New uploads from customers will appear here.' : 'Try another filter.'}
        </Empty>
      ) : (
        <ul className="slip-list">
          {slips.map((slip) => (
            <SlipRow
              key={slip.slipId}
              slip={slip}
              fresh={justDecided?.slipId === slip.slipId}
              onDecide={decide}
            />
          ))}
        </ul>
      ))}
    </div>
  );
}

function SlipRow({ slip, fresh, onDecide }) {
  const [remark, setRemark] = useState('');
  const [busy, setBusy] = useState(false);
  const pending = slip.status === 'PENDING';

  const run = async (action) => {
    setBusy(true);
    await onDecide(slip, action, remark);
    setBusy(false);
  };

  return (
    <li className="slip panel">
      <div className="slip-main">
        <div className="slip-amount num">{money(slip.amountPaid)}</div>
        <div className="slip-meta">
          <span>
            For <strong>{slip.invoiceNumber}</strong>, from {slip.uploadedBy}
          </span>
          <span className="muted">
            Paid {date(slip.paymentDate)}
            {slip.bankReference ? `, bank ref ${slip.bankReference}` : ''}. Uploaded {dateTime(slip.createdAt)}.
          </span>
        </div>
        <StatusStamp status={slip.status} fresh={fresh} />
      </div>

      <div className="slip-file">
        <a href={slip.fileUrl} target="_blank" rel="noreferrer">View {slip.originalFileName}</a>
        <span className="muted"> ({fileSize(slip.fileSize)})</span>
        <a href={slip.downloadUrl} className="slip-download">Download</a>
      </div>

      {slip.adminRemark && !pending && <p className="slip-remark">Note: {slip.adminRemark}</p>}

      {pending && (
        <div className="slip-decide">
          <label className="sr-only" htmlFor={`remark-${slip.slipId}`}>Note to customer (optional)</label>
          <input
            id={`remark-${slip.slipId}`}
            placeholder="Note to customer (optional)"
            value={remark}
            onChange={(e) => setRemark(e.target.value)}
            maxLength={500}
          />
          <button type="button" className="btn btn-good" disabled={busy} onClick={() => run('approve')}>
            Approve
          </button>
          <button type="button" className="btn btn-danger" disabled={busy} onClick={() => run('reject')}>
            Reject
          </button>
        </div>
      )}
    </li>
  );
}
