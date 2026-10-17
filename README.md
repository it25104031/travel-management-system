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

