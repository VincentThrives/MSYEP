export const staff = {
  title: 'MSYEP User Manual — Staff Portal',
  portal: 'Staff Portal',
  audience: 'For MSYEP staff / coordinators',
  version: '1.1',
  date: '23 October 2026',
  intro: 'The Staff Portal gives you the full working view of MSYEP - zones, centers, students, mail, SOW, '
       + 'locations and the finance wing. It is the same as the Admin Console except that you cannot manage '
       + 'logins or change the admin signature.',
  sections: [

    { h1: 'Signing in', blocks: [
      { shot: '00-login.jpg', caption: 'The sign-in screen. Staff use the Staff tab.' },
      { steps: [
        'Leave the selector on Staff.',
        'Enter your email and password. The eye icon shows what you typed.',
        'Click Sign in. You land on the Dashboard.',
      ]},
      { note: 'If you see "Waking up the server...", wait up to a minute - the server sleeps when idle.' },
      { p: 'Forgotten your password? An Admin or Super Admin resets it from the Logins page.' },
    ]},

    { h1: 'Your portal at a glance', blocks: [
      { shot: 'T01-dashboard.jpg', caption: 'The Staff Portal dashboard (blue theme).' },
      { bullets: [
        'Zones - create and view zones, and send them their Certificate and MOU.',
        'Centers - create and view colleges, see them on a map, and send Batch Approval mail.',
        'Students - create, view, download documents and send student mail.',
        'KPMSYEP SOW - the eight training programmes per center.',
        'Resource Person - trainers attached to a center.',
        'Location - add districts, taluks and villages.',
        'Finance - the finance wing and Gram Panchayat mail IDs.',
        'Staff Management - add and view staff.',
      ]},
      { note: 'Two things you will not see, because they are Admin-only: the Logins page and the Admin Signature '
            + 'page. If you need a password reset or a new account, ask an administrator.' },
    ]},

    { h1: 'Zones', blocks: [
      { p: 'Create a zone with Franchise Sign-up, then work through its six tabs. View Zones lists them with '
         + 'District / Taluk / GP filters and a search box.' },
      { fields: [
        ['Tab 1 - Login & Organization', 'User ID, Password, Organization Name, Zone / University Name, Building (Own / Rent), website and budget, plus tickboxes for Registered Copy, MSME, GST, Niti Aayog, NGO Darpan and 12A / 80G.'],
        ['Tab 2 - Owner Details', 'Owner name, date of birth, gender, contact and alternate numbers, email, full address, city, state, pincode, occupation, qualification, and district / taluk / village.'],
        ['Tab 3 - KYC & Documents', 'Aadhaar number, PAN number, bank account details, and 12 document upload slots.'],
        ['Tab 4 - Business Fit', 'Investment capacity, preferred location, own or rent, space in sq.ft, and how soon they plan to start.'],
        ['Tab 5 - Franchise Certificate', 'Franchisee / trade name, registration number, issue date, valid-till (automatic, +2 years) and territory.'],
        ['Tab 6 - Membership & Submit', 'Membership tier and the Terms & Conditions tickbox.'],
      ]},
      { h2: 'Row actions on View Zones' },
      { fields: [
        ['Edit', 'Reopen and change the zone.'],
        ['Certificate', 'Download the Franchise Certificate PDF.'],
        ['MOU', 'Download the 42-page MOU.'],
        ['Mail', 'Email the Certificate and MOU to that zone.'],
        ['Delete', 'Remove the zone permanently.'],
      ]},
      { note: 'The first MOU download for a zone takes a few seconds to generate; after that it is cached and '
            + 'downloads instantly until the zone changes.' },
    ]},

    { h1: 'Sending mail', blocks: [
      { shot: 'T02-zone-mail.jpg', caption: 'Zone Mail. The same pattern is used for Center and Student mail.' },
      { steps: [
        'Open the Mail page under Zones, Centers or Students.',
        'Search for who you want - the box matches name, organization, code or email.',
        'Tick one or more rows, or click the blue arrow on a single row.',
        'Click the send button, check the subject and message, then confirm.',
        'The screen returns immediately. Scroll to Sent Mail History for the outcome.',
      ]},
      { h2: 'Reading the history' },
      { fields: [
        ['1/1 sent', 'Delivered.'],
        ['0/1 sent', 'Failed - the mail server could not be reached. Try again.'],
        ['stub - SMTP not configured', 'The server has no mail credentials, so nothing was sent. Tell an administrator.'],
        ['Empty recipients', 'That record has no email address saved.'],
      ]},
      { note: 'Sending can take up to a minute because the attachments are large - the MOU alone is about 10 MB. '
            + 'The page does not wait; refresh the history to see the result.' },
    ]},

    { h1: 'Centers and Students', blocks: [
      { h2: 'Centers' },
      { p: 'Six tabs: Academic & Type; Center Location; Center Details (mail-id, office number, principal, UUCMS '
         + 'coordinator, SC-ST cell coordinator, placement coordinator); Course Details (courses, total strength, '
         + 'and 3rd/4th sem numbers split Total / SC / ST / General); MOU Details; and Documents with 10 upload slots.' },
      { h2: 'Students' },
      { p: 'Seven tabs: Account; Personal Details; Education; Job (including the long technical-skills checklist); '
         + 'Address (zone and center required); MSYEP Allotments; and Documents with 7 upload slots.' },
      { bullets: [
        'View Students - filter by district, taluk, GP, center or caste; search by name, registration number or email; Export to Excel.',
        'Download Documents - pick a Doc Type, filter, tick students (or Select all) and download in bulk.',
      ]},
      { note: 'The Password boxes on the Account tab are optional. Fill them in and the student can sign in immediately with their mobile number or email. Leave them blank and an administrator must set a password later from the Logins page before the student can get in.' },
    ]},

    { h1: 'SOW, Resource Person and Location', blocks: [
      { h2: 'KPMSYEP SOW' },
      { p: 'Choose a zone, then a center, to open its eight programmes. Each records the inauguration date, the '
         + 'guest (name, phone, designation), the eight group names, who did each part - anchoring, welcome speech, '
         + 'Preamble reading, three queries, vote of thanks, honouring, guest opinion - and a photo for each. '
         + 'Download one programme or all eight as KP-MSYEP-Training-Programs.pdf.' },
      { h2: 'Resource Person' },
      { p: 'Choose a zone and center to manage the trainers attached to that center.' },
      { h2: 'Location' },
      { p: 'Three tabs - Add District, Add Taluk, Add Village / GP. Anything added here appears in every location '
         + 'dropdown across the portal, so this is where you go when someone cannot find their village.' },
      { fields: [
        ['Add District', 'State (Karnataka) and District Name.'],
        ['Add Taluk', 'State, District, Taluk Name.'],
        ['Add Village / GP', 'State, District, Taluk, Village / GP Name.'],
      ]},
    ]},

    { h1: 'Finance', blocks: [
      { shot: 'T03-finance.jpg', caption: 'Finance Wing - sending student FW documents to the Gram Panchayat.' },
      { steps: [
        'Filter by District, Taluk, Village / GP and Center.',
        'Tick the students.',
        'Check the Gram Panchayat Mails - filled in automatically, and you can change them.',
        'Optionally click GP Blue Print to attach the GP packet.',
        'Click Send documents to GP.',
      ]},
      { p: 'Finance -> Mail holds the Gram Panchayat mail IDs: Gram Panchayat Name and Mail ID are required, with '
         + 'Taluk, District and Contact Person optional. A student shows NO_GP_EMAIL when their GP is missing here.' },
    ]},

    { h1: 'Staff Management', blocks: [
      { fields: [
        ['Name / Designation / Phone / Email', 'Staff Details tab.'],
        ['Zone (University) / Center (College)', 'Where this person works.'],
        ['District / Taluk / Village or GP', 'Their location.'],
      ]},
      { p: 'View Staff lists everyone, with location filters and a search by name or phone.' },
    ]},

    { h1: 'If something goes wrong', blocks: [
      { fields: [
        ['"Waking up the server..."', 'Normal. Wait up to a minute.'],
        ['Mail shows 0/1 sent', 'The mail server could not be reached. Try again.'],
        ['Mail shows "stub - SMTP not configured"', 'The server has no mail credentials. Tell an administrator.'],
        ['A village is missing from a dropdown', 'Add it on the Location page.'],
        ['You cannot see Logins or Admin Signature', 'Correct - those are Admin-only.'],
        ['The MOU takes a long time', 'Only the first download per zone is slow; after that it is cached.'],
      ]},
    ]},
  ],
};
