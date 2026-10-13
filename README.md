# Travel Agency ERP - Group 09 (all five modules in one app)

One Spring Boot application, one MySQL database (`travel_erp`), one design for the public website and the staff workspace.

| Module | Owner | Java package | Staff pages | Website pages |
|---|---|---|---|---|
| 1. Authentication & identity | IT25103339 | `com.travel.module1_auth` | `/admin/users`, `/admin/customers`, `/admin/audit` | `/login`, `/signup` |
| 2. Visa & travel compliance | IT25103339 | `com.travel.module2_visa` | `/visas/dashboard`, `/visas/departures`, `/visas/incidents`, `/admin/visa-rules`, `/admin/countries`, `/admin/reports` | `/visas/select-country`, `/portal/login` |
| 3. Billing & payments | IT25104031 | `com.travel.module3_billing` | `/billing`, `/billing/invoices`, `/billing/payments`, `/billing/slips`, `/billing/pay` | pay an invoice at `/my/invoices/{id}` |
| 4. Hotel & transport logistics | IT25104030 | `com.travel.module4_logistics` | `/logistics/departures` ... `/logistics/costs` | - |
| 5. Tour packages | IT25104033 | `com.travel.module5_packages` | `/packages`, `/itineraries/{id}`, `/packages/bookings` | `/tours`, `/tours/{id}`, `/cart`, `/checkout` |
| Website + staff home | shared | `com.travel.web` | `/staff` | `/`, `/about`, `/my` |
| Shared helpers | shared | `com.travel.common` | | |

Templates follow the same folders: `src/main/resources/templates/module1_auth`, `module2_visa`, ... `site`, `web`, `layout`.
One stylesheet pair (`static/css/app.css` + `site.css`) styles every page.

## Run it
1. **MySQL running.** First time only: run `travel_erp.sql` in MySQL Workbench.
   *If you already have the `travel_erp` database from the 3-module version, skip this* - the new tables
   (logistics, packages, bookings, traveller accounts) are added automatically on start.
2. If your MySQL `root` user has a password, set `spring.datasource.password` in `src/main/resources/application.yml`.
3. Open the folder in IntelliJ (Maven project) and run `com.travel.TravelApplication`.
4. Open **http://localhost:8082**

On the first start with an empty `tour_package` table, six sample tour packages with day-by-day itineraries are
added so the website has something to show (`app.demo-data: true`; edit or delete them under Staff > Tour packages).

## Logins
| Who | Where | Login |
|---|---|---|
| Admin | http://localhost:8082/login?staff | `admin` / `admin123` |
| Office staff | same | `operator` / `operator123` |
| Traveller | http://localhost:8082/signup | create an account with your email |
| Visa portal (reference + passkey) | http://localhost:8082/portal/login | shown after a visa application is submitted |

If `admin123` is refused, your `admin` row came from the team's original INSERT. Reset it in MySQL Workbench:
```sql
UPDATE travel_erp.admin_users SET password = '$2a$12$lbMNkL24TL3RYlMVwOPFGuuB.sc33c2oWu7joPvuzpTRR7KDiWtTe' WHERE username = 'admin';
```

Staff and travellers use the same sign-in page: staff type their username, travellers their email.
Admins can create more staff accounts, including other admins, at **Staff accounts** (`/admin/users`).

## The website journey
1. **Home** (`/`) - the big logo fades as you scroll; About us, services, tours and contact/social links.
2. **Tours** (`/tours`) - search and filter, open a tour, choose date + travellers, **Add to cart** (no account needed).
3. **Checkout** (`/checkout`) - asks the visitor to sign in or sign up, then comes back with the cart kept.
   Placing the booking creates **one invoice in Billing** and one booking per tour.
4. **Pay** (`/my/invoices/{id}`) - by **bank transfer** with a slip upload. Staff accept or reject slips under
   Billing > Bank slips; the booking then shows *Confirmed*. Card payments show a "coming soon" placeholder until the
   LankaPay merchant account is approved (staff can still record cash/bank payments under Billing > Payments).
5. **Profile** (`/my`) - My trips, Payments (invoices + slips), Visa applications, Account settings.
   Trips with nothing paid yet have a **Delete trip** button; the trip's amount comes off its invoice (and the invoice
   is removed when it was the only trip). Paid, part-paid and in-review trips can't be deleted.
6. **Visas** - applying needs a signed-in traveller (or a staff member entering one for a walk-in). The application then
   appears under the traveller's profile.

## How the modules talk to each other
- Booking (M5) -> Billing (M3): checkout calls `InvoiceService.createInvoice`; a booking's status is always read from its
  invoice (UNPAID / PARTIAL / PAID, or "in review" while a slip is pending).
- Billing can't delete an invoice that still has active bookings (`InvoiceDeletionGuard`, implemented by `BookingService`).
- Packages with bookings can't be deleted - set them to UNAVAILABLE to hide them from the website.
- Logistics departures (M4) suggest package names from M5.
- Payment slips can belong to a staff member or a website traveller (`payment_slip.customer_id`).
- Visa applications keep the traveller's account id (`site_customer_id`).

## Change the company details
Name, phone, WhatsApp, email, address and Facebook page are the client's real details and live in
`application.yml` under `app.site`. Still to fill in there: the **bank account** shown to travellers who pay by
transfer (currently `[Bank name]` / `[Account number]`), and optionally opening hours, Instagram, YouTube or
TikTok (blank ones are hidden on the site).

## Tests
`./mvnw test` - 47 tests on an in-memory database (no MySQL needed), including a full website journey
(sign up -> cart -> checkout -> bank slip -> staff approval) in `module5_packages/WebsiteBookingFlowTest`.

## What was merged and what was left out
- Module 4 came from `LogisticsProject-Final-FINAL` (was `com.dahamdi.travelmanagement`, database `travel_logistics_db`).
- Module 5 came from `travel-management-system-completed` (tour packages + itineraries). That project also carried an
  older copy of invoices/payments and an airline booking module: those are not included - Billing is Module 3, and
  flights were removed from the agreed EER.
- Old stylesheets (`logistics.css`, `travel.css`, the per-page billing styles) were replaced by the shared theme.
- `all_modules_schema.sql` lists every table the app uses (generated from the entities) for the report/EER.
- `docs/module1-2-auth-visa-notes.md` keeps the detailed notes for Modules 1 and 2.
