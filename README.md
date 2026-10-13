# Travel Management System – Billing & Payments

Spring Boot + Thymeleaf + MySQL application for invoices, payments, the payment gateway and payment-slip approval.

## Running it

1. Start MySQL (e.g. XAMPP) and run `Database/Database.sql` (safe to run more than once).
2. If your MySQL user/password isn't `root` / empty, set the environment variables
   `DB_USERNAME` and `DB_PASSWORD` (or `DB_URL`) before starting.
3. Run from the project root folder (so slips are saved to `uploads/payment-slips`):
   ```
   cd travelmanagement
   ./mvnw spring-boot:run
   ```
4. Open http://localhost:8081

## New frontend (React + Vite)

There is also a newer UI in the `frontend/` folder. Start the backend as above, then:

```
cd frontend
npm install
npm run dev
```

and open http://localhost:5173. See `frontend/README.md` for details. The Thymeleaf pages keep working.

## Demo accounts (created automatically on first start)

| Role  | Username | Password  |
|-------|----------|-----------|
| ADMIN | admin    | admin123  |
| USER  | user1    | user123   |

Passwords are stored as BCrypt hashes. Old plain-text passwords already in the database are
converted to hashes automatically the first time that user logs in.

## Who can access what

| Area | Access |
|------|--------|
| `/login`, `/register` | everyone |
| `/user/**`, `/payment-gateway`, own slip files | any logged-in user |
| `/api/auth/**` (login for the Vite frontend) | everyone |
| `/api/my/**` (own bank slips) | any logged-in user |
| `/admin/**`, `/invoices/**`, `/payments/**`, other `/api/**` | ADMIN only |

This is enforced in one place: `config/AuthInterceptor.java`.

## Billing rules

- Amounts are `BigDecimal` (exact money values, matches `DECIMAL(10,2)` in MySQL).
- A payment must be greater than 0 and can't exceed the invoice's outstanding balance —
  checked on the server for the payments page, the REST API, the gateway and slip approval.
- Invoice status (UNPAID / PARTIAL / PAID) is calculated from COMPLETED payments and
  updated after every payment change. It can't be typed in by hand.
- Only PENDING slips can be approved or rejected, so a slip can't be counted twice.
- An invoice with payments or slips can't be deleted.
- Uploaded slips must be real JPG / PNG / PDF files, max 5 MB.
