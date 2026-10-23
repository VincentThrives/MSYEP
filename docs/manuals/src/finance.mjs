export const finance = {
  title: 'MSYEP User Manual — Finance Wing',
  portal: 'Finance Wing',
  audience: 'For the MSYEP finance team',
  version: '1.1',
  date: '23 October 2026',
  intro: 'The Finance Wing sends students\' FW documents to their Gram Panchayat and keeps the list of Gram '
       + 'Panchayat mail IDs up to date. This manual covers both, plus the student screens you can reach.',
  sections: [

    { h1: 'Signing in', blocks: [
      { shot: '00-login.jpg', caption: 'The sign-in screen. Use the Staff tab.' },
      { steps: [
        'Leave the selector on Staff - your account\'s role decides which portal opens.',
        'Enter your email and password. The eye icon shows what you typed.',
        'Click Sign in. You land straight on the Finance Wing, not a dashboard.',
      ]},
      { note: 'If you see "Waking up the server...", wait up to a minute - the server sleeps when idle.' },
    ]},

    { h1: 'Sending documents to a Gram Panchayat', blocks: [
      { shot: 'F01-finance-wing.jpg', caption: 'Finance Wing - Student FW Documents.' },
      { h2: 'The filters' },
      { fields: [
        ['District', 'Narrow to one district.'],
        ['Taluk / Town', 'Fills in once a district is chosen.'],
        ['Village / Gram Panchayat', 'Fills in once a taluk is chosen.'],
        ['Center', 'Narrow to one college.'],
        ['Clear', 'Resets every filter.'],
      ]},
      { h2: 'Sending' },
      { steps: [
        'Use the filters to bring up the students you need.',
        'Tick them in the table. The tickbox in the header selects everyone shown.',
        'Check Gram Panchayat Mails - the GP email is filled in automatically from the mapping table. You can pick different or extra recipients.',
        'If you want the GP packet attached, click GP Blue Print first.',
        'Click Send documents to GP.',
      ]},
      { note: 'Each student is sent as a separate mail, so one failure does not stop the rest. The outcome for every '
            + 'student is recorded - see the history under Finance -> Mail.' },
      { h2: 'What the results mean' },
      { fields: [
        ['SENT', 'Delivered to the listed address.'],
        ['NO_GP_EMAIL', 'That student\'s Gram Panchayat has no mail ID. Add it on the Mail page.'],
        ['FAILED', 'The mail server could not be reached. Try again.'],
        ['NOT_FOUND', 'The student record could not be read.'],
      ]},
      { p: 'The View / Download column lets you open a student\'s documents before you send them.' },
    ]},

    { h1: 'Gram Panchayat Mail IDs', blocks: [
      { shot: 'F02-gp-mail-ids.jpg', caption: 'The GP mail-ID list feeds the dropdown on the Finance Wing page.' },
      { p: 'Everything you add here appears in the "Gram Panchayat Mails" dropdown used for sending. Keep it '
         + 'current - a missing entry is why a student shows NO_GP_EMAIL.' },
      { fields: [
        ['Gram Panchayat Name', 'Required. The GP this address belongs to.'],
        ['Mail ID', 'Required. Where documents for that GP are sent.'],
        ['Taluk', 'The taluk it sits in.'],
        ['District', 'The district it sits in.'],
        ['Contact Person', 'Who to ask for at that GP.'],
      ]},
      { steps: [
        'Fill in at least the Gram Panchayat Name and Mail ID.',
        'Add the taluk, district and contact person so the entry is easy to find later.',
        'Click Add.',
        'Use the pencil on a row to correct it, or the bin to remove it.',
      ]},
    ]},

    { h1: 'Students', blocks: [
      { shot: 'F03-students-view.jpg', caption: 'View Students, with filters and Export to Excel.' },
      { h2: 'View Students' },
      { bullets: [
        'Filter by District, Taluk, Gram Panchayat, Center or Caste, or search by name, registration number or email.',
        'The pencil opens a record for editing.',
        'Export to Excel downloads whatever the filters are currently showing - useful for reconciliation.',
      ]},
      { h2: 'Create Student' },
      { p: 'The same seven-tab form used elsewhere: Account, Personal Details, Education, Job, Address, MSYEP '
         + 'Allotments and Documents. Zone and Center are required on the Address tab.' },
      { note: 'The Password boxes on the Account tab are optional. Fill them in and the student can sign in immediately with their mobile number or email. Leave them blank and an administrator must set a password later from the Logins page before the student can get in.' },
    ]},

    { h1: 'If something goes wrong', blocks: [
      { fields: [
        ['"Waking up the server..."', 'Normal. Wait up to a minute.'],
        ['A student shows NO_GP_EMAIL', 'Their Gram Panchayat has no mail ID. Add it on the Mail page and send again.'],
        ['The send result shows 0 sent', 'The mail server could not be reached. Try again shortly.'],
        ['A Gram Panchayat is missing from the dropdown', 'Add it on the Mail page.'],
        ['A village is missing from the filters', 'An administrator adds it on the Location page.'],
      ]},
    ]},
  ],
};
