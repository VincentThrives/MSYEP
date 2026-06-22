# MSYEP

**Yukta Kaushalya Tarabethi** skilling-program portal — Karnataka.

A multi-tier portal (Super Admin · Admin · Zone · Center · Staff · Finance · Student)
for managing universities (Zones), colleges (Centers), and students under the MSYEP program.

## Stack
- **Backend:** Java 21 · Spring Boot 3.3 · MongoDB · Spring Security (JWT) · Apache POI (Excel) · iText (PDF) · Spring Mail
- **Frontend:** Angular 18 · Angular Material · ngx-mat-select-search

## Features
- Role-based, themed portals (Zone = orange, Center = pink, …) with a gold + green brand.
- **Zone** management: CRUD, Excel import, PDF export, courses (PU/SSLC/ITI/Diploma/Degree).
- **Center** registration: auto `CENTER-{year}-{NNNN}` code + `CENENR…` enrollment number,
  6+7 document uploads, auto-created CENTER login, emailed registration PDF.
- **Student** & **Staff** management; **Finance** wing with GP-email send.
- Cascading, searchable **District → Taluk → Gram Panchayat** dropdowns seeded with the full
  Karnataka master (30 districts · 226 taluks · 6,009 gram panchayats, from the LGD open data).

## Run locally
Prereqs: Java 21, Node 18+, MongoDB running on `localhost:27017`.

```bash
# Backend  → http://localhost:8080
cd backend
MONGODB_URI=mongodb://127.0.0.1:27017/msyep ./mvnw spring-boot:run

# Frontend → http://localhost:4200
cd frontend
npm install
npm start
```

First boot seeds a Super Admin: **superadmin@msyep.in / Admin@12345** (override via
`SEED_SUPERADMIN_EMAIL` / `SEED_SUPERADMIN_PASSWORD`). Registration email needs
`MAIL_USERNAME` / `MAIL_PASSWORD` (a Gmail App Password).

See `../MSYEP_SPEC.md` for the full living spec.
