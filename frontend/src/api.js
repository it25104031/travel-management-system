// All calls go to the Spring Boot backend through the Vite proxy (see vite.config.js).

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

let onUnauthorized = null;
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

async function request(path, { method = 'GET', body, form } = {}) {
  const options = { method, headers: { Accept: 'application/json' }, credentials: 'same-origin' };

  if (form) {
    options.body = form; // browser sets the multipart boundary itself
  } else if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(path, options);
  } catch {
    throw new ApiError("Can't reach the server. Check that the Spring Boot app is running on port 8081.", 0);
  }

  if (response.status === 204) return null;

  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }

  if (!response.ok) {
    if (response.status === 401 && onUnauthorized && !path.startsWith('/api/auth/')) onUnauthorized();
    let message = data?.error || data?.message;
    if (!message) {
      message = response.status >= 500
        ? "The server couldn't finish that request. Check the Spring Boot console for the error."
        : `Request failed (${response.status}).`;
    }
    throw new ApiError(message, response.status);
  }
  return data;
}

export const api = {
  // auth
  me: () => request('/api/auth/me'),
  login: (username, password) => request('/api/auth/login', { method: 'POST', body: { username, password } }),
  register: (data) => request('/api/auth/register', { method: 'POST', body: data }),
  logout: () => request('/api/auth/logout', { method: 'POST' }),

  // admin billing
  dashboard: () => request('/api/billing/dashboard'),
  invoices: () => request('/api/billing/invoices'),
  invoice: (id) => request(`/api/billing/invoices/${id}`),
  createInvoice: (data) => request('/api/billing/invoices', { method: 'POST', body: data }),
  updateInvoice: (id, data) => request(`/api/billing/invoices/${id}`, { method: 'PUT', body: data }),
  deleteInvoice: (id) => request(`/api/billing/invoices/${id}`, { method: 'DELETE' }),

  payments: () => request('/api/billing/payments'),
  createPayment: (data) => request('/api/billing/payments', { method: 'POST', body: data }),
  updatePayment: (id, data) => request(`/api/billing/payments/${id}`, { method: 'PUT', body: data }),
  deletePayment: (id) => request(`/api/billing/payments/${id}`, { method: 'DELETE' }),

  gatewayPay: (data) => request('/api/billing/gateway', { method: 'POST', body: data }),

  // slips
  allSlips: (status = 'ALL') => request(`/api/admin/slips?status=${encodeURIComponent(status)}`),
  approveSlip: (id, adminRemark) => request(`/api/admin/slips/${id}/approve`, { method: 'POST', body: { adminRemark } }),
  rejectSlip: (id, adminRemark) => request(`/api/admin/slips/${id}/reject`, { method: 'POST', body: { adminRemark } }),
  mySlips: () => request('/api/my/slips'),
  uploadSlip: (form) => request('/api/my/slips', { method: 'POST', form }),
};
