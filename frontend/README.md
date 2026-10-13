# Billing desk frontend (React + Vite)

A separate frontend for the Spring Boot backend in `../travelmanagement`.
The old Thymeleaf pages on http://localhost:8081 still work. This app is a newer UI for the same data.

## Run it

You need Node.js 18 or newer.

1. Start the backend first (it must be on port **8081**):
   ```
   cd travelmanagement
   ./mvnw spring-boot:run
   ```
2. In a second terminal:
   ```
   cd frontend
   npm install      # first time only
   npm run dev
   ```
3. Open **http://localhost:5173** and log in with `admin` / `admin123` or `user1` / `user123`.

Vite forwards every `/api/...` and `/payment-slips/...` request to `http://localhost:8081`
(see `vite.config.js`), so the login session works without any CORS setup.

## Screens

| Who      | Page | What it does |
|----------|------|--------------|
| Admin    | Overview | How much is still owed, totals, slips waiting, largest open balances |
| Admin    | Invoices | Create, edit, print and delete invoices; filter by status; search |
| Admin    | Payments | Record, edit and delete payments; "Full balance" fills the amount |
| Admin    | Take a payment | Counter payments against an invoice, or a new invoice for a walk-in customer, with a receipt |
| Admin    | Bank slips | Approve or reject slips customers uploaded, with an optional note |
| Customer | My bank slips | Upload a slip (JPG, PNG or PDF, max 5 MB) and see whether it was approved |

## Project layout

```
src/
  main.jsx          entry point (fonts, styles, providers)
  App.jsx           routes and role checks
  router.jsx        tiny router (no react-router needed)
  auth.jsx          login state (calls /api/auth/*)
  api.js            every backend call in one place
  format.js         money / date formatting (LKR)
  styles.css        all styles (colours defined at the top)
  components/       shared pieces: sidebar, status stamp, balance bar, notices
  pages/            one file per screen
```

## Backend endpoints used

| Endpoint | Access |
|----------|--------|
| `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/logout`, `GET /api/auth/me` | public |
| `GET/POST /api/my/slips` | any logged-in user (their own slips) |
| `/api/billing/**` (dashboard, invoices, payments, gateway) | ADMIN |
| `/api/admin/slips/**` | ADMIN |

## Build for production

`npm run build` creates `dist/`. `npm run preview` serves that build on http://localhost:4173
with the same proxy to the backend.
