export const center = {
  title: 'MSYEP User Manual — Center Portal',
  portal: 'Center Portal',
  audience: 'For colleges and hostels running the MSYEP programme',
  version: '1.1',
  date: '23 October 2026',
  intro: 'This manual covers everything a center does in MSYEP: keeping your own details up to date, adding and '
       + 'managing your students, filling in your eight training programmes, and downloading documents.',
  sections: [

    { h1: 'Signing in', blocks: [
      { shot: '00-login.jpg', caption: 'The sign-in screen. Centers use the Center tab.' },
      { steps: [
        'Click the Center button.',
        'Enter the Center Mail-ID that was registered as your User ID.',
        'Enter your password. The eye icon shows what you typed.',
        'Click Sign in.',
      ]},
      { note: 'If you see "Waking up the server...", wait up to a minute. The server sleeps when it has not been '
            + 'used and has to start up again.' },
      { h2: 'New center?' },
      { p: 'A college can register itself. On the Center tab click "New center? Register as a Center" and complete '
         + 'the form. You choose your own Mail-ID and password, and can sign in as soon as it is submitted.' },
      { h2: 'Forgotten your password?' },
      { p: 'Only an MSYEP administrator can reset it. Contact them and they will set a new one from the Logins page.' },
    ]},

    { h1: 'Your portal at a glance', blocks: [
      { shot: 'C01-dashboard.jpg', caption: 'The Center Portal dashboard (pink theme).' },
      { bullets: [
        'The tiles show how many zones, centers and students you can see. Your student count covers your center only.',
        'My Center - view your own details and your location on the map.',
        'Students - create students, view them, and download their documents.',
        'KPMSYEP SOW - your eight training programmes.',
        'Resource Person - the resource people attached to your center.',
      ]},
      { note: 'A center can see and edit only its own record and its own students. You cannot create another center '
            + 'or see other colleges\' data.' },
    ]},

    { h1: 'My Center', blocks: [
      { shot: 'C02-my-center.jpg', caption: 'View Center shows your own record.' },
      { p: 'Your center record holds everything about your institution. Click the pencil to edit. The form has six '
         + 'tabs and a completion bar - you can save and come back.' },
      { fields: [
        ['Academic Year', 'The year this record covers.'],
        ['Center Academic Start / End Month', 'Your academic calendar.'],
        ['Select Center Type', 'College, hostel or other.'],
        ['College / Center Name', 'Your institution name.'],
        ['User ID / Password', 'The login for this center.'],
      ], title: 'Tab 1 - Academic & Type' },
      { fields: [
        ['Center District / Taluk / Village or GP', 'Where you are.'],
        ['MSYEP Center Address / Locality / Pincode', 'Your full address.'],
        ['Zone (University)', 'The zone you come under.'],
      ], title: 'Tab 2 - Center Location' },
      { fields: [
        ['Batch Year', 'The batch you are running.'],
        ['Center Mail-ID', 'Where MSYEP sends your mail. Keep this current.'],
        ['Center Office Number', 'Office phone.'],
        ['Principal Name / Number', 'Head of the institution.'],
        ['UUCMS / Computer Operator Coordinator', 'Name and number.'],
        ['SC-ST Cell Coordinator', 'Name and number.'],
        ['Placement (Kaushalya Patha) Coordinator', 'Name and phone.'],
        ['Center has a website?', 'Tick if yes.'],
      ], title: 'Tab 3 - Center Details' },
      { fields: [
        ['Enter your Center Courses', 'Type a course and press Enter. Repeat for each one.'],
        ['Center Total Strength', 'Total students at your institution.'],
        ['3rd/4th Sem or 1yr - Total / SC / ST / General', 'Student numbers by category.'],
      ], title: 'Tab 4 - Course Details' },
      { fields: [
        ['MOU Date (from) / (to)', 'The period your MOU covers.'],
        ['Year of Contract', 'Contract year.'],
      ], title: 'Tab 5 - MOU Details' },
      { fields: [
        ['Document uploads (10 slots)', 'Upload your supporting documents.'],
      ], title: 'Tab 6 - Documents' },
      { h2: 'Center Map' },
      { p: 'Shows your center on a map, based on the district, taluk and village you entered. If the pin looks wrong, '
         + 'check those fields.' },
    ]},

    { h1: 'Students', blocks: [
      { h2: 'Create Student' },
      { p: 'Students usually register themselves from the login page. Use Create Student when you need to add '
         + 'someone yourself. The form has seven tabs.' },
      { fields: [
        ['Account', 'Student Name, Student Email, Mobile No.'],
        ['Personal Details', 'Gender, Date of Birth, Caste, Father Name, Mother Name.'],
        ['Education', 'School and college studied, percentage or CGPA, year of passing, stream or course for SSLC, PU/Diploma and Degree.'],
        ['Job', 'Current qualification, internship interest, hobbies, a long technical-skills checklist, and career goal.'],
        ['Address', 'Zone and Center (required), state, district, taluk, village/GP, pincode, native place, hostel, postal address.'],
        ['MSYEP Allotments', 'Course joining date and the college / hostel they are placed at.'],
        ['Documents', 'Seven upload slots - photo, Aadhaar, marks cards and so on.'],
      ]},
      { note: 'The Password boxes on the Account tab are optional. Fill them in and the student can sign in immediately with their mobile number or email. Leave them blank and an administrator must set a password later from the Logins page before the student can get in.' },
      { h2: 'View Students' },
      { bullets: [
        'Filter by District, Taluk, Gram Panchayat or Caste, or search by name, registration number or email.',
        'The pencil edits a record; the eye opens the full profile; the PDF icon downloads their documents.',
        'Export to Excel downloads whatever your filters are currently showing.',
      ]},
      { h2: 'Download Documents' },
      { steps: [
        'Choose a Doc Type, or leave it on All.',
        'Narrow the list with the filters or the search box.',
        'Tick the students you want, or use Select all.',
        'Click Download - the button shows how many are selected.',
      ]},
    ]},

    { h1: 'KP-MSYEP SOW — your eight programmes', blocks: [
      { shot: 'C03-sow-programs.jpg', caption: 'The eight programmes. A green tick means that one is saved.' },
      { p: 'Each programme is a Statement of Work describing one training event you ran: who spoke, who took part, '
         + 'and photographs of the day. Tap any tile to fill it in; tiles you have saved show "Saved - tap to edit".' },
      { shot: 'C04-sow-program-form.jpg', caption: 'Inside a programme.' },

      { fields: [
        ['Program', 'Which of the eight programmes this is.'],
        ['Program Inauguration Date', 'Date of the event.'],
        ['Guest Name', 'The chief guest.'],
        ['Guest Phone Number', "Guest's contact number."],
        ['Guest Designation', 'Their role or title.'],
      ], title: 'Inauguration & Guest' },

      { fields: [
        ['Groups Date', 'Date the groups met.'],
        ['Group 1 to Group 8 Name', 'Name each of the eight student groups.'],
      ], title: '08 Groups' },

      { fields: [
        ['Program Anchoring by', 'Who anchored the programme.'],
        ['Welcome Speech by', 'Who gave the welcome speech.'],
        ['Indian Constitution - Preamble Reading by', 'Who read the Preamble.'],
        ['Query 1 by / Question', 'Who asked the first question, and what it was.'],
        ['Query 2 by / Query 3 by', 'The second and third questioners.'],
        ['Vote of Thanks by', 'Who gave the vote of thanks.'],
        ['Honouring by', 'Who honoured the guest.'],
        ['Taking Guest Opinion by', 'Who collected the guest\'s opinion.'],
        ['Guest Opinion', 'What the guest said.'],
      ], title: 'Who did what' },

      { h2: 'Photographs' },
      { p: 'Each part of the programme has a photo slot - the programme letter, anchoring, welcome speech, preamble '
         + 'reading, the three queries, vote of thanks, honouring, taking the guest opinion and the guest opinion '
         + 'itself. Upload one photo per slot.' },
      { bullets: [
        'Upload the photo at whatever size you have - the whole picture is kept, nothing is cropped.',
        'Click any photo to open it full size and check it.',
        'Photos appear in the generated PDF as a single-page gallery with captions.',
      ]},
      { h2: 'Downloading the PDF' },
      { steps: [
        'Fill in a programme and click Save.',
        'Use Download on that programme for just that one, or "Download all programs (PDF)" at the top for all eight.',
        'The file is named KP-MSYEP-Training-Programs.pdf.',
        'A copy is also emailed automatically to your center mail-id.',
      ]},
      { note: 'Each page is headed "KP-MSYEP Training Program 1" through 8, with your college name underneath.' },
    ]},

    { h1: 'Resource Person', blocks: [
      { p: 'Lists the resource people attached to your center - the trainers and specialists who deliver sessions. '
         + 'Your zone and center are already selected for you.' },
    ]},

    { h1: 'If something goes wrong', blocks: [
      { fields: [
        ['"Waking up the server..."', 'Normal. Wait up to a minute for the server to start.'],
        ['A student cannot sign in', 'They may have no password. Ask an administrator to set one on the Logins page.'],
        ['Your village is missing from a dropdown', 'An administrator has to add it on the Location page.'],
        ['The SOW PDF did not arrive by email', 'Check that your Center Mail-ID is correct in Tab 3 of your center record.'],
        ['A photo looks cut off', 'Click it to open it full size - the full photo is stored and used in the PDF.'],
      ]},
    ]},
  ],
};
