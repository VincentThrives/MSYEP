// MSYEP live-email test.
// Fires ONE real send to the target address via the Finance recipient-override,
// then reads the MailLog and reports whether it actually went out (stub:false) or
// was simulated (stub:true = SMTP creds not loaded).
//
// Run:  node test-live-email.mjs
// Override target:  TO=someone@example.com node test-live-email.mjs

const BASE  = process.env.BASE  || 'http://127.0.0.1:8080/api/v1';
const TO    = process.env.TO    || 'vincentthrives@gmail.com';
const EMAIL = process.env.ADMIN_EMAIL || 'superadmin@msyep.in';
const PASS  = process.env.ADMIN_PASS  || 'Admin@12345';

const j = async (r) => { const t = await r.text(); try { return JSON.parse(t); } catch { return t; } };
const unwrap = (x) => (x && typeof x === 'object' && 'data' in x) ? x.data : x;

try {
  // 1) Login
  const login = await fetch(`${BASE}/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: EMAIL, password: PASS }),
  }).then(j);
  const token = unwrap(login)?.token || login?.token;
  if (!token) { console.error('LOGIN FAILED:', JSON.stringify(login)); process.exit(1); }
  const H = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
  console.log('1) Logged in as', EMAIL);

  // 2) Grab any student id (the packet is addressed per-student; recipient is overridden below)
  const students = unwrap(await fetch(`${BASE}/students`, { headers: H }).then(j));
  const sid = Array.isArray(students) ? students[0]?.id : null;
  if (!sid) { console.error('NO STUDENTS FOUND — cannot build a test packet.'); process.exit(1); }
  console.log('2) Using studentId', sid);

  // 3) Send the real mail (recipientEmails overrides the auto-resolved GP email)
  const send = unwrap(await fetch(`${BASE}/finance/send-mail`, {
    method: 'POST', headers: H,
    body: JSON.stringify({
      studentIds: [sid],
      recipientEmails: [TO],
      subject: 'MSYEP live email test',
      body: 'This is a real SMTP test from MSYEP via Namecheap Private Email. '
          + 'If this landed in your inbox, real email delivery is working.',
    }),
  }).then(j));
  console.log('3) Send result:', JSON.stringify(send));

  // 4) Read the MailLog (NEWEST entry by timestamp — not an old match) and report
  const logs = unwrap(await fetch(`${BASE}/finance/mail-history`, { headers: H }).then(j));
  const rec = Array.isArray(logs) && logs.length
    ? [...logs].sort((a, b) => new Date(b.sentAt) - new Date(a.sentAt))[0]
    : null;
  const outcome = String(Object.values(send || {})[0] || '');   // "SENT:..." / "FAILED:..." from THIS send

  console.log('\n==================== VERDICT ====================');
  console.log('send outcome :', outcome);
  if (rec) {
    console.log('log status   :', rec.status);
    console.log('log stub     :', rec.stub);
  }
  console.log('------------------------------------------------');
  if (outcome.startsWith('FAILED')) {
    console.log('❌ SEND FAILED — the SMTP server rejected it:');
    console.log('   ' + outcome.replace(/^FAILED:/, '').trim());
  } else if (rec && rec.stub === false && outcome.startsWith('SENT')) {
    console.log('✅ REAL EMAIL SENT to ' + TO + ' — check the inbox (and Spam on first send).');
  } else if (rec && rec.stub === true) {
    console.log('⚠️  STILL SIMULATED — SMTP creds not loaded. Restart via start-msyep.cmd.');
  } else {
    console.log('⚠️  Unclear — outcome:', outcome, '| newest log:', rec && rec.status);
  }
  console.log('================================================');
} catch (e) {
  console.error('TEST ERROR:', e.message);
  console.error('Is the backend running on ' + BASE + ' ?');
  process.exit(1);
}
