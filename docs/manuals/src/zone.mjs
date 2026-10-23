export const zone = {
  title: 'MSYEP User Manual — Zone Portal',
  portal: 'Zone Portal',
  audience: 'For zone / franchise partners',
  version: '1.1',
  date: '23 October 2026',
  intro: 'A zone is a franchise partner - usually a university or trust - running MSYEP through a number of '
       + 'colleges. This manual covers creating and managing those centers, their students, staff and training '
       + 'programmes.',
  sections: [

    { h1: 'Signing in', blocks: [
      { shot: '00-login.jpg', caption: 'The sign-in screen. Zones use the Zone tab.' },
      { steps: [
        'Click the Zone button.',
        'Enter the User ID set up for your zone (usually your email).',
        'Enter your password. The eye icon shows what you typed.',
        'Click Sign in.',
      ]},
      { note: 'If you see "Waking up the server...", wait up to a minute - the server sleeps when idle.' },
      { p: 'Forgotten your password? Only an MSYEP administrator can reset it, from the Logins page.' },
    ]},

    { h1: 'Your portal at a glance', blocks: [
      { shot: 'Z01-dashboard.jpg', caption: 'The Zone Portal dashboard (orange theme).' },
      { bullets: [
        'Center Management - create and view your colleges, and see them on a map.',
        'Students Management - create and view students across your centers.',
        'Fund Approval - see the note below.',
        'KPMSYEP SOW - the eight training programmes for each of your centers.',
        'Resource Person - trainers attached to a center.',
        'KPMSYEP Kit Details - see the note below.',
        'Staff Management - the people working in your zone.',
      ]},
      { note: 'A zone cannot create another zone, and cannot see other zones\' data. Only MSYEP administrators '
            + 'create zones.' },
    ]},

    { h1: 'Center Management', blocks: [
      { shot: 'Z02-center-create.jpg', caption: 'Center Create - six tabs with a completion bar.' },
      { p: 'Add each college or hostel running the programme under your zone. The form has six tabs; you can save '
         + 'and come back.' },
      { fields: [
        ['Academic Year', 'The year this center is set up for.'],
        ['Center Academic Start / End Month', 'Their academic calendar.'],
        ['Select Center Type', 'College, hostel or other.'],
        ['College / Center Name', 'Pick an existing college or type a new one.'],
        ['User ID', 'The login for this center - normally its email.'],
        ['Password', 'The center\'s first password. Pass it on to them.'],
      ], title: 'Tab 1 - Academic & Type' },
      { fields: [
        ['Center District / Taluk / Village or GP', 'Where the center is.'],
        ['MSYEP Center Address / Locality / Pincode', 'Full address.'],
        ['Zone (University)', 'Your zone.'],
      ], title: 'Tab 2 - Center Location' },
      { fields: [
        ['Batch Year', 'The batch this center runs.'],
        ['Center Mail-ID', 'Where all mail to this center is sent.'],
        ['Center Office Number', 'Office phone.'],
        ['Principal Name / Number', 'Head of the institution.'],
        ['UUCMS / Computer Operator Coordinator', 'Name and number.'],
        ['SC-ST Cell Coordinator', 'Name and number.'],
        ['Placement (Kaushalya Patha) Coordinator', 'Name and phone.'],
        ['Center has a website?', 'Tick if yes.'],
      ], title: 'Tab 3 - Center Details' },
      { fields: [
        ['Enter your Center Courses', 'Type a course and press Enter, once per course.'],
        ['Center Total Strength', 'Total students at the center.'],
        ['3rd/4th Sem or 1yr - Total / SC / ST / General', 'Student numbers by category.'],
      ], title: 'Tab 4 - Course Details' },
      { fields: [
        ['MOU Date (from) / (to)', 'Period the MOU covers.'],
        ['Year of Contract', 'Contract year.'],
      ], title: 'Tab 5 - MOU Details' },
      { fields: [
        ['Document uploads (10 slots)', 'The center\'s supporting documents.'],
      ], title: 'Tab 6 - Documents' },
      { h2: 'Center View and Center Map' },
      { bullets: [
        'Center View lists your centers, with District / Taluk / GP filters and a search box.',
        'The pencil edits a center; the other icons open its documents.',
        'Center Map plots your centers using the location fields you entered.',
      ]},
    ]},

    { h1: 'Students Management', blocks: [
      { h2: 'Students Create' },
      { p: 'Seven tabs, the same form centers use.' },
      { fields: [
        ['Account', 'Student Name, Student Email, Mobile No.'],
        ['Personal Details', 'Gender, Date of Birth, Caste, Father Name, Mother Name.'],
        ['Education', 'School / college studied, percentage or CGPA, year of passing, stream or course.'],
        ['Job', 'Current qualification, internship interest, hobbies, technical-skills checklist, career goal.'],
        ['Address', 'Zone and Center (required), plus full address.'],
        ['MSYEP Allotments', 'Course joining date and placement.'],
        ['Documents', 'Seven upload slots.'],
      ]},
      { note: 'The Password boxes on the Account tab are optional. Fill them in and the student can sign in immediately with their mobile number or email. Leave them blank and an administrator must set a password later from the Logins page before the student can get in.' },
      { h2: 'Students View' },
      { bullets: [
        'Filter by District, Taluk, Gram Panchayat, Center or Caste, or search by name, registration number or email.',
        'Export to Excel downloads whatever the filters are showing.',
      ]},
    ]},

    { h1: 'SOW, Resource Person and Staff', blocks: [
      { h2: 'KPMSYEP SOW' },
      { steps: [
        'Open KPMSYEP SOW.',
        'Choose one of your centers.',
        'Its eight training programmes open. Fill in the details and photos for each.',
        'Download a single programme, or all eight as KP-MSYEP-Training-Programs.pdf.',
      ]},
      { p: 'Each programme records the inauguration date, the guest (name, phone, designation), the eight group '
         + 'names, who did each part of the programme - anchoring, welcome speech, Preamble reading, three queries, '
         + 'vote of thanks, honouring, guest opinion - and a photograph for each.' },
      { h2: 'Resource Person' },
      { p: 'Choose a center to see and manage the trainers attached to it.' },
      { h2: 'Staff Management' },
      { fields: [
        ['Name / Designation / Phone / Email', 'Staff Details tab.'],
        ['Zone (University) / Center (College)', 'Where this person works.'],
        ['District / Taluk / Village or GP', 'Their location.'],
      ]},
    ]},

    { h1: 'Modules not yet available', blocks: [
      { shot: 'Z03-fund-approval.jpg', caption: 'Fund Approval is scaffolded but not yet built.' },
      { p: 'Two menu items are placeholders. Opening them shows a notice rather than a working screen:' },
      { fields: [
        ['Fund Approval', 'Waiting on its specification - form fields, the 16 VBSOW programmes and GP mapping.'],
        ['KPMSYEP Kit Details', 'Waiting on its specification.'],
      ]},
      { note: 'These will be wired up once the details are provided. Until then, handle fund approvals through your '
            + 'usual offline process.' },
    ]},

    { h1: 'If something goes wrong', blocks: [
      { fields: [
        ['"Waking up the server..."', 'Normal. Wait up to a minute.'],
        ['A center cannot sign in', 'Check the User ID in Tab 1 of their record, and ask an administrator to reset the password.'],
        ['A village or taluk is missing', 'An administrator adds it on the Location page; it then appears everywhere.'],
        ['You cannot create a zone', 'Correct - only administrators create zones.'],
        ['A student cannot sign in', 'They probably have no password yet. An administrator can set one.'],
      ]},
    ]},
  ],
};
