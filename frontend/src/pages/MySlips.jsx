import { useRef, useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { date, fileSize, money, toInputDate } from '../format.js';
import Field from '../components/Field.jsx';
import Notice from '../components/Notice.jsx';
import PageHeader from '../components/PageHeader.jsx';
import StatusStamp from '../components/StatusStamp.jsx';
import { Empty, Loading } from '../components/Loading.jsx';
import useLoad from '../components/useLoad.js';

const MAX_BYTES = 5 * 1024 * 1024;
const ALLOWED = ['.jpg', '.jpeg', '.png', '.pdf'];

const STATUS_HELP = {
  PENDING: 'We are checking this transfer.',
  APPROVED: 'Payment received and added to your invoice.',
  REJECTED: 'We could not match this transfer.',
};

export default function MySlips() {
  const { user } = useAuth();
  const { data: slips, error: loadError, loading, reload } = useLoad(() => api.mySlips());
  const [message, setMessage] = useState({});

  return (
    <div className="page">
      <PageHeader
        title={`Hello, ${user?.fullName?.split(' ')[0] ?? ''}`}
        intro="Paid by bank transfer? Upload the slip and we'll add the payment to your invoice once it's checked."
      />

      <Notice tone={message.tone} onDismiss={() => setMessage({})}>{message.text}</Notice>

      <div className="two-col">
        <UploadForm
          onUploaded={(slip) => {
            setMessage({ tone: 'success', text: `Slip for ${slip.invoiceNumber} uploaded. You'll see the result here.` });
            reload();
          }}
        />

        <section aria-labelledby="history-title">
          <h2 id="history-title" className="section-title">Your uploads</h2>
          <Notice tone="error">{loadError}</Notice>
          {loading && !slips && <Loading />}
          {slips && (slips.length === 0 ? (
            <Empty title="Nothing uploaded yet">Your bank slips and their status will be listed here.</Empty>
          ) : (
            <ul className="slip-list">
              {slips.map((slip) => (
                <li key={slip.slipId} className="slip panel">
                  <div className="slip-main">
                    <div className="slip-amount num">{money(slip.amountPaid)}</div>
                    <div className="slip-meta">
                      <span>For <strong>{slip.invoiceNumber}</strong>, paid {date(slip.paymentDate)}</span>
                      <span className="muted">{STATUS_HELP[slip.status]}</span>
                    </div>
                    <StatusStamp status={slip.status} />
                  </div>
                  {slip.adminRemark && <p className="slip-remark">Note from us: {slip.adminRemark}</p>}
                  <div className="slip-file">
                    <a href={slip.fileUrl} target="_blank" rel="noreferrer">View {slip.originalFileName}</a>
                    <span className="muted"> ({fileSize(slip.fileSize)})</span>
                  </div>
                </li>
              ))}
            </ul>
          ))}
        </section>
      </div>
    </div>
  );
}

function UploadForm({ onUploaded }) {
  const fileInput = useRef(null);
  const [form, setForm] = useState({ invoiceNumber: '', amountPaid: '', paymentDate: toInputDate(), bankReference: '' });
  const [file, setFile] = useState(null);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const pickFile = (f) => {
    setError('');
    if (!f) return;
    const ext = f.name.slice(f.name.lastIndexOf('.')).toLowerCase();
    if (!ALLOWED.includes(ext)) {
      setError('Choose a JPG, PNG or PDF file.');
      return;
    }
    if (f.size > MAX_BYTES) {
      setError(`That file is ${fileSize(f.size)}. The limit is 5 MB.`);
      return;
    }
    setFile(f);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!file) {
      setError('Attach a photo or PDF of your bank slip.');
      return;
    }
    const data = new FormData();
    Object.entries(form).forEach(([k, v]) => data.append(k, v));
    data.append('file', file);

    setBusy(true);
    try {
      const slip = await api.uploadSlip(data);
      setForm({ invoiceNumber: '', amountPaid: '', paymentDate: toInputDate(), bankReference: '' });
      setFile(null);
      if (fileInput.current) fileInput.current.value = '';
      onUploaded(slip);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="panel form-panel" onSubmit={handleSubmit}>
      <h2>Upload a bank slip</h2>
      <Notice tone="error">{error}</Notice>

      <Field label="Invoice number" hint="It's printed at the top of your invoice, e.g. INV-0001.">
        {(p) => <input {...p} value={form.invoiceNumber} onChange={set('invoiceNumber')} required maxLength={30} />}
      </Field>
      <div className="form-grid">
        <Field label="Amount paid (LKR)">
          {(p) => (
            <input {...p} type="number" inputMode="decimal" step="0.01" min="0.01"
              value={form.amountPaid} onChange={set('amountPaid')} required />
          )}
        </Field>
        <Field label="Date paid">
          {(p) => <input {...p} type="date" max={toInputDate()} value={form.paymentDate} onChange={set('paymentDate')} required />}
        </Field>
      </div>
      <Field label="Bank reference (optional)">
        {(p) => <input {...p} value={form.bankReference} onChange={set('bankReference')} maxLength={100} />}
      </Field>

      <div
        className={`dropzone ${dragging ? 'is-dragging' : ''} ${file ? 'has-file' : ''}`}
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => { e.preventDefault(); setDragging(false); pickFile(e.dataTransfer.files[0]); }}
      >
        <input
          ref={fileInput}
          id="slip-file"
          type="file"
          accept=".jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf"
          className="sr-only"
          onChange={(e) => pickFile(e.target.files[0])}
        />
        <label htmlFor="slip-file" className="dropzone-label">
          {file ? (
            <>
              <strong>{file.name}</strong>
              <span className="muted">{fileSize(file.size)}. Choose a different file</span>
            </>
          ) : (
            <>
              <strong>Choose the slip file</strong>
              <span className="muted">or drop it here. JPG, PNG or PDF, up to 5 MB.</span>
            </>
          )}
        </label>
      </div>

      <div className="form-actions">
        <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
          {busy ? 'Uploading…' : 'Upload slip'}
        </button>
      </div>
    </form>
  );
}
