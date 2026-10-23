export const admin = {
  title: 'MSYEP User Manual — Admin Console',
  portal: 'Admin Console',
  audience: 'For Super Admin and Admin logins',
  version: '1.1',
  date: '23 October 2026',
  intro: 'This manual explains every screen in the Admin Console: what each page is for, every field you '
       + 'can fill in, and the step-by-step workflows for the tasks you do most often. The Admin Console has '
       + 'full access to the system, including the Logins page where you create accounts and reset passwords.',
  sections: [

    // ───────────────────────────── 1
    { h1: 'Signing in', blocks: [
      { p: 'Open the portal in any browser. On the live site use https://msyep.in; on a test machine use http://localhost:4200.' },
      { shot: '00-login.jpg', caption: 'The sign-in screen. Admin and Super Admin both use the Staff tab.' },
      { h2: 'How to sign in' },
      { steps: [
        'Leave the role selector on Staff. (The four buttons only change the hint text — your account\'s role decides which portal opens.)',
        'Type your Email — for the Super Admin this is superadmin@msyep.in.',
        'Type your Password. Click the eye icon on the right to check what you typed.',
        'Click Sign in. You land on the Dashboard.',
      ] },
      { note: 'If the page sits on "Waking up the server…", that is normal. The hosted server sleeps when nobody '
            + 'has used it for a while and takes up to a minute to start. Wait rather than clicking Sign in again.' },
      { h2: 'If you cannot get in' },
      { bullets: [
        'Wrong username or password — check capitals; passwords are case-sensitive.',
        'Account is disabled — another admin has deactivated your login. Ask them to reactivate it on the Logins page.',
        'Forgot your password — only another Admin or Super Admin can reset it, from the Logins page.',
      ] },
    ]},

    // ───────────────────────────── 2
    { h1: 'Finding your way around', blocks: [
      { shot: 'A01-dashboard.jpg', caption: 'The Dashboard, with the menu down the left side.' },
      { bullets: [
        'The coloured tiles are live counts — zones, centers and students currently in the system.',
        'The menu on the left is grouped (Zones, Centers, Students, Staff). Click a group to expand or collapse it.',
        'The ☰ button at the top-left hides or shows the menu. On a phone the menu slides over the page; tap a link or the dark area to close it.',
        'Your name is at the top-right. Click it to Log out.',
      ] },
      { note: 'The portal works on a phone. The menu collapses behind the ☰ button and wide tables scroll sideways '
            + 'instead of squashing, so you can use it from the field.' },
    ]},

    // ───────────────────────────── 3
    { h1: 'Zones (Franchise)', blocks: [
      { p: 'A Zone is a franchise partner — usually a university or a trust that runs centers under it. Everything '
         + 'about a zone lives here: its sign-up form, its documents, its certificate and its MOU.' },
      { shot: 'A02-zones-list.jpg', caption: 'View Zones. Filter by location, search, then act on a row.' },
      { h2: 'What the row buttons do' },
      { fields: [
        ['✏️  Edit', 'Reopen the zone and change any detail.'],
        ['🏅  Certificate', 'Download the Franchise Certificate PDF for that zone.'],
        ['📄  MOU', 'Download the full MOU (42 pages) with logos and signatures applied.'],
        ['✉️  Mail', 'Email the Certificate and MOU to that zone.'],
        ['🗑️  Delete', 'Remove the zone permanently.'],
      ]},
      { note: 'The first MOU download for a zone takes a few seconds while it is generated. After that it is cached, '
            + 'so the same MOU downloads instantly until you change something about the zone.' },

      { pagebreak: true },
      { h2: 'Creating a zone — the six tabs' },
      { shot: 'A04-zone-create.jpg', caption: 'Create Zone. The bar shows how much of the form is complete.' },
      { p: 'Click Franchise Sign-up, then work through the tabs. The percentage next to each tab tells you how much '
         + 'of that tab is filled. You can save and come back — nothing is lost between tabs.' },

      { fields: [
        ['User ID', 'The login the zone will use. Defaults to the organization name if left blank.'],
        ['Password', 'The zone\'s password. Use the eye icon to check it.'],
        ['Organization Name', 'Registered name of the trust / institution.'],
        ['Zone / University Name', 'The university or zone this franchise covers.'],
        ['Building (Own / Rent)', 'Whether they own or rent their premises.'],
        ['Organization has a website?', 'Tick if yes.'],
        ['Budget to create the website', 'Only if they do not have one.'],
        ['Documents available', 'Tick each they hold: Registered Copy, MSME, GST, Niti Aayog, NGO Darpan, 12A / 80G.'],
        ['Willing to comply?', 'Tick if they will obtain the missing documents.'],
      ], title: 'Tab 1 — Login & Organization' },

      { fields: [
        ['Owner Full Name', 'Name of the franchise owner.'],
        ['Date of Birth', 'Owner\'s date of birth.'],
        ['Gender', 'Select from the list.'],
        ['Contact Number', 'Primary mobile number.'],
        ['Alternate Number', 'A second contact number.'],
        ['Email Address', 'Used for all mail sent to this zone.'],
        ['Full Address / City / State / Pincode', 'Owner\'s postal address.'],
        ['Occupation / Business Background', 'What the owner does.'],
        ['Educational Qualification', 'Owner\'s highest qualification.'],
        ['District / Taluk / Village or GP', 'Pick from the lists. If a place is missing, add it on the Location page first.'],
      ], title: 'Tab 2 — Owner Details' },

      { fields: [
        ['Aadhaar Card Number', 'Owner\'s Aadhaar number.'],
        ['PAN Number', 'Owner\'s PAN.'],
        ['Bank Account Details', 'Account number, IFSC, bank and branch.'],
        ['Document uploads (12 slots)', 'Upload the registration copy, MSME, GST, Niti Aayog, NGO Darpan, 12A / 80G and the owner\'s ID proofs.'],
      ], title: 'Tab 3 — KYC & Documents' },
      { note: 'This tab holds Aadhaar, PAN and bank details. Only Admin, Staff, Zone, Center and Finance logins can '
            + 'open zone records — students can never see them.', kind: 'warn' },

      { fields: [
        ['Investment Capacity (₹)', 'How much the partner can invest.'],
        ['Preferred Location for Franchise', 'Where they want to operate.'],
        ['Own or Rent the proposed space?', 'Ownership of the proposed premises.'],
        ['Space Available (sq.ft)', 'Size of the premises.'],
        ['How soon do you plan to start?', 'Expected start time.'],
      ], title: 'Tab 4 — Business Fit' },

      { fields: [
        ['Franchisee / Trade Name', 'The name printed on the certificate.'],
        ['Registration No.', 'Certificate registration number.'],
        ['Issue Date', 'Date the certificate is issued.'],
        ['Valid Till', 'Filled in automatically — two years after the issue date.'],
        ['Territory', 'Taken from the membership tier.'],
      ], title: 'Tab 5 — Franchise Certificate' },

      { fields: [
        ['Membership tier', 'Silver, Gold or Platinum. This sets the territory on the certificate.'],
        ['Terms & Conditions', 'Must be ticked before the form can be submitted.'],
      ], title: 'Tab 6 — Membership & Submit' },
    ]},

    // ───────────────────────────── 4
    { h1: 'Zone Mail — sending Certificate & MOU', blocks: [
      { shot: 'A03-zone-mail.jpg', caption: 'Zone Mail. Tick zones, then send — or use the ➤ on a single row.' },
      { steps: [
        'Open Zones → Mail.',
        'Find the zone with the search box (it matches name, organization or email).',
        'Tick one or more zones, or click the blue ➤ on a single row to send to just that zone.',
        'Click Send Certificate + MOU. Check the subject and message in the box that appears, then click Send mail.',
        'The screen comes back straight away. Scroll down to Sent Mail History to see the result.',
      ]},
      { note: 'Sending takes up to a minute because each mail carries the Certificate and the 42-page MOU (about 10 MB). '
            + 'The page does not wait for it — refresh Sent Mail History after a minute to see "1/1 sent".' },
      { h2: 'Reading the Sent Mail History' },
      { fields: [
        ['1/1 sent', 'Delivered successfully.'],
        ['0/1 sent', 'Failed — usually the mail server could not be reached. Try again.'],
        ['stub — SMTP not configured', 'The server has no mail credentials, so nothing was actually sent.'],
        ['(empty recipients)', 'That zone has no email address saved. Add one on the zone record.'],
      ]},
      { p: 'Click any history row to reopen the full mail, including its attachments.' },
    ]},

    // ───────────────────────────── 5
    { h1: 'Centers (Colleges)', blocks: [
      { p: 'A Center is a college or hostel delivering the programme under a zone.' },
      { shot: 'A05-centers-list.jpg', caption: 'View Centers, with District / Taluk / GP filters and a search box.' },
      { h2: 'Creating a center — the six tabs' },
      { shot: 'A06-center-create.jpg', caption: 'Create Center.' },
      { fields: [
        ['Academic Year', 'The year this center is being set up for.'],
        ['Center Academic Start / End Month', 'The academic calendar.'],
        ['Select Center Type', 'College, hostel, or other type.'],
        ['College / Center Name', 'Pick an existing college or type a new one.'],
        ['User ID', 'The login for this center (normally its email).'],
        ['Password', 'The center\'s password.'],
      ], title: 'Tab 1 — Academic & Type' },
      { fields: [
        ['Center District / Taluk / Village or GP', 'Where the center is.'],
        ['MSYEP Center Address', 'Street address.'],
        ['MSYEP Center Locality', 'Locality or area.'],
        ['Pincode', 'Postal code.'],
        ['Zone (University)', 'Which zone this center belongs to.'],
      ], title: 'Tab 2 — Center Location' },
      { fields: [
        ['Batch Year', 'The batch this center runs.'],
        ['Center Mail-ID', 'Where all center mail is sent.'],
        ['Center Office Number', 'Office phone.'],
        ['Principal Name / Number', 'Head of the institution.'],
        ['UUCMS / Computer Operator Coordinator', 'Name and number.'],
        ['SC-ST Cell Coordinator', 'Name and number.'],
        ['Placement (Kaushalya Patha) Coordinator', 'Name and phone.'],
        ['Center has a website?', 'Tick if yes.'],
      ], title: 'Tab 3 — Center Details' },
      { fields: [
        ['Enter your Center Courses', 'Type a course and press Enter. Repeat for each course.'],
        ['Center Total Strength', 'Total students at the center.'],
        ['3rd/4th Sem or 1yr — Total / SC / ST / General', 'Student numbers split by category.'],
      ], title: 'Tab 4 — Course Details' },
      { fields: [
        ['MOU Date (from) / (to)', 'The period the MOU covers.'],
        ['Year of Contract', 'Contract year.'],
      ], title: 'Tab 5 — MOU Details' },
      { fields: [
        ['Document uploads (10 slots)', 'Upload the center\'s supporting documents.'],
      ], title: 'Tab 6 — Documents' },
      { h2: 'Center Mail — Batch Approval' },
      { shot: 'A07-center-mail.jpg', caption: 'Center Mail sends the Batch Approval PDF to selected centers.' },
      { steps: [
        'Open Centers → Mail.',
        'Search for the center, then tick it (or click the ➤ on its row).',
        'Click Send Batch Approval PDF and confirm the message.',
        'Check Sent Mail History below for the result.',
      ]},
    ]},

    // ───────────────────────────── 6
    { h1: 'Students', blocks: [
      { shot: 'A08-student-create.jpg', caption: 'Create Student — seven tabs, with a completion bar.' },
      { p: 'Students normally register themselves from the login page. Use Create Student when you need to add '
         + 'someone centrally.' },
      { fields: [
        ['Student Name / Student Email / Mobile No', 'Account tab — the basics. Email or mobile becomes their username.'],
        ['Gender, Date of Birth, Caste, Father Name, Mother Name', 'Personal Details tab.'],
        ['School / College studied, Percentage or CGPA, Year of Passing, Stream / Course', 'Education tab — SSLC, PU/Diploma and Degree.'],
        ['Educational Qualification, Internship interest, Technical Skills, Career Goal, Hobbies', 'Job tab. Technical skills is a long checklist (programming, networking, design, AI and more).'],
        ['Zone, Center, State, District, Taluk, Village/GP, Pincode, Native Place, Hostel, Postal Address', 'Address tab. Zone and Center are required.'],
        ['Course Joining Date, Choose College / Hostel', 'MSYEP Allotments tab.'],
        ['Document uploads (7 slots)', 'Documents tab — photo, Aadhaar, marks cards and so on.'],
      ]},
      { fields: [
        ['Password (optional)', 'Set the login password here so the student can sign in straight away. At least 6 characters.'],
        ['Confirm Password', 'Type the same password again.'],
      ], title: 'Giving the student a password' },
      { note: 'Both password boxes are optional. Leave them blank and the student has no way in until you set a password from the Logins page. Fill them in and they can sign in immediately with their mobile number or email.' },
      { h2: 'View Students' },
      { shot: 'A09-students-view.jpg', caption: 'View Students, with filters and Export to Excel.' },
      { bullets: [
        'Filter by District, Taluk, Gram Panchayat, Center or Caste, or search by name, registration number or email.',
        'The pencil opens the record for editing; the eye shows the full profile; the PDF icon downloads their documents.',
        'Export to Excel downloads everything currently shown by your filters.',
      ]},
      { h2: 'Download Student Documents' },
      { shot: 'A10-download-docs.jpg', caption: 'Bulk-download documents for many students at once.' },
      { steps: [
        'Choose a Doc Type, or leave it on All.',
        'Narrow the list with the District / Taluk / GP / Center filters, or the search box.',
        'Tick the students you need, or use Select all.',
        'Click Download. The button shows how many are selected.',
      ]},
    ]},

    // ───────────────────────────── 7
    { h1: 'Staff, SOW and Resource Persons', blocks: [
      { h2: 'Staff Management' },
      { shot: 'A11-staff.jpg', caption: 'Staff Management.' },
      { fields: [
        ['Name / Designation / Phone / Email', 'Staff Details tab.'],
        ['Zone (University) / Center (College)', 'Assignment tab — where this person works.'],
        ['District / Taluk / Village or GP', 'Their location.'],
      ]},
      { h2: 'KP-MSYEP SOW' },
      { shot: 'A12-sow.jpg', caption: 'SOW — pick a zone and center to open its 8 programmes.' },
      { steps: [
        'Choose Select Zone (University), then Select Center (College).',
        'The center\'s 8 training programmes open. Fill in the details and upload the photos for each.',
        'Use Download to get the PDF, which is also emailed to the college.',
      ]},
      { p: 'The generated file is named KP-MSYEP-Training-Programs.pdf and contains one page per programme, '
         + 'each with its photo gallery.' },
      { h2: 'Resource Persons' },
      { p: 'Choose a zone and center to view and manage the resource persons attached to that center.' },
    ]},

    // ───────────────────────────── 8
    { h1: 'Location Management', blocks: [
      { shot: 'A13-location.jpg', caption: 'Location Management — districts, taluks and villages / GPs.' },
      { p: 'Everything you add here appears in every District / Taluk / Gram Panchayat dropdown across the portal. '
         + 'If someone cannot find their village when filling a form, add it here first.' },
      { fields: [
        ['Add District', 'State (fixed to Karnataka) and District Name.'],
        ['Add Taluk', 'State, District, then Taluk Name.'],
        ['Add Village / GP', 'State, District, Taluk, then Village / GP Name.'],
      ]},
      { p: 'Each list below the form shows what already exists, with a pencil to rename and a bin to delete.' },
    ]},

    // ───────────────────────────── 9
    { h1: 'Finance Wing', blocks: [
      { shot: 'A14-finance.jpg', caption: 'Finance Wing — send student FW documents to the Gram Panchayat.' },
      { steps: [
        'Filter by District, Taluk, Village / GP and Center to find the students you need.',
        'Tick the students in the table.',
        'Check Gram Panchayat Mails — the GP email is filled in automatically, and you can pick different recipients.',
        'Optionally click GP Blue Print to attach the GP packet.',
        'Click Send documents to GP.',
      ]},
      { note: 'Each student is mailed separately, so one failure does not stop the rest. The per-student result is '
            + 'recorded in Finance → Mail.' },
    ]},

    // ───────────────────────────── 10
    { h1: 'Logins — creating and managing accounts', blocks: [
      { shot: 'A15-logins.jpg', caption: 'Logins. Search by name or email and filter by role.' },
      { p: 'This page is the only place accounts are created, suspended or given new passwords. It is available to '
         + 'Admin and Super Admin only.' },
      { h2: 'Creating a login' },
      { steps: [
        'Click Create Login.',
        'Fill in Name, Email, Password and Role.',
        'For a Zone role pick the Zone; for a Center role pick the Center.',
        'Click Create. The person can sign in straight away.',
      ]},
      { fields: [
        ['Name', 'Person or institution the login belongs to.'],
        ['Email', 'This is the username they sign in with.'],
        ['Password', 'Their first password — use the eye icon to check it.'],
        ['Role', 'Admin, Zone, Center, Staff or Finance.'],
        ['Zone / Center', 'Shown only for Zone and Center roles; ties the login to that record.'],
      ]},
      { h2: 'Finding a login' },
      { bullets: [
        'Search matches both name and email — typing part of either narrows the list.',
        'The Role dropdown filters to one role, including Student and Super Admin.',
        'The counter shows how many of the total are currently listed.',
      ]},
      { h2: 'The three row actions' },
      { fields: [
        ['🔑  Reset password', 'Set a new password without needing the old one. Type it twice; each box has an eye icon. Minimum 6 characters.'],
        ['🔘  Activate / Deactivate', 'Deactivating blocks sign-in but keeps the account and its history. Reversible at any time.'],
        ['🗑️  Delete', 'Removes the login permanently. Use Deactivate instead if you may need it later.'],
      ]},
      { note: 'Two safeguards: you cannot deactivate your own login, and the last active Super Admin cannot be '
            + 'deactivated. Both would lock everyone out of this page.' },
      { h2: 'Common tasks' },
      { bullets: [
        'Someone forgot their password — find them, click the key, set a new one, tell them.',
        'Someone has left — click the toggle to deactivate. Their records stay intact.',
        'A student cannot sign in — they may have been created by an admin and never given a password. Set one with the key.',
      ]},
    ]},

    // ───────────────────────────── 11
    { h1: 'Admin Signature', blocks: [
      { shot: 'A16-admin-signature.jpg', caption: 'Admin Signature — uploaded once, used on every PDF.' },
      { p: 'Upload the authorised signature once and it appears automatically as the YKTK / giver signature on every '
         + 'document the system generates: the Franchise Certificate, the MOU, the Center Batch Approval, and the GP '
         + 'requisition and invoice.' },
      { steps: [
        'Click Upload signature.',
        'Choose a PNG with a transparent background, or a clear photo of a signature on white paper.',
        'It takes effect immediately — the next PDF generated uses the new signature. No redeploy is needed.',
      ]},
      { bullets: [
        'Maximum file size 2 MB.',
        'A photo on white paper is fine — the background is removed automatically so the signature never covers the text underneath.',
        'A scanned PDF of a signature also works.',
      ]},
    ]},

    // ───────────────────────────── 12
    { h1: 'If something goes wrong', blocks: [
      { fields: [
        ['"Waking up the server…"', 'Normal. The server sleeps when idle and takes up to a minute to start. Wait.'],
        ['Could not reach the server', 'The server is starting or the internet dropped. Try again in a moment.'],
        ['Mail shows 0/1 sent', 'The mail server could not be reached. Try sending again.'],
        ['Mail shows "stub — SMTP not configured"', 'The server has no mail credentials — nothing was sent. Contact whoever manages the hosting.'],
        ['A village or taluk is missing from a dropdown', 'Add it on the Location page; it then appears everywhere.'],
        ['A login does not work', 'Check on the Logins page whether the account shows Deactivated, and reset the password if needed.'],
        ['The MOU takes a long time', 'Only the first download per zone is slow. After that it is cached and instant.'],
      ]},
    ]},
  ],
};
