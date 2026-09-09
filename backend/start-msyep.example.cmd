@echo off
REM ============================================================
REM  TEMPLATE  -  copy this to  start-msyep.cmd  and paste your
REM  real mailbox credentials there (start-msyep.cmd is gitignored).
REM
REM  Turns on REAL email for MSYEP via Namecheap Private Email.
REM  Runs the backend on port 8080.
REM ============================================================

REM ---- SMTP: Namecheap Private Email ----
set "MAIL_HOST=mail.privateemail.com"
set "MAIL_PORT=587"

REM ---- Your mailbox (also the "from" address) ----
set "MAIL_USERNAME=no-reply@msyep.in"
set "MAIL_PASSWORD=your-mailbox-password"

REM ---- (optional) MongoDB Atlas; leave commented for local MongoDB ----
REM set "MONGODB_URI=mongodb+srv://USER:PASS@cluster.mongodb.net/msyep"

cd /d "%~dp0"
call mvnw.cmd -o spring-boot:run
