export const student = {
  title: 'MSYEP User Manual — Student Portal',
  portal: 'Student Portal',
  audience: 'For students enrolled in the MSYEP programme',
  version: '1.1',
  date: '23 October 2026',
  intro: 'This manual shows you how to register, sign in, complete your profile, take the entrance test and '
       + 'download your MSYEP resume. Follow it in order — each step unlocks the next.',
  sections: [

    { h1: 'Registering for the first time', blocks: [
      { p: 'You create your own account. Open the portal, click the Student tab, then "New student? Create an account".' },
      { shot: 'S01-register.jpg', caption: 'The student registration form.' },
      { h2: 'What to fill in' },
      { fields: [
        ['Full Name', 'Your name as it appears on your records. Required.'],
        ['Mobile Number', 'Your 10-digit mobile number. Required — this becomes your username.'],
        ['Email (optional)', 'If you give one you can sign in with it instead of your mobile number.'],
        ['Create Password', 'Choose your own password. At least 6 characters. Click the eye to check it.'],
        ['Confirm Password', 'Type the same password again. Both must match.'],
        ['Zone', 'Your university / zone. Pick from the list.'],
        ['Center (College)', 'Your college. The list fills in after you choose a zone.'],
        ['District / Taluk or Town / Village or Gram Panchayat', 'Where you live.'],
        ['Gender', 'Select from the list.'],
        ['Date of Birth', 'Pick from the calendar.'],
        ['Educational Qualification (optional)', 'Your current or highest qualification.'],
      ]},
      { steps: [
        'Fill in every field marked with a star.',
        'Choose a password you will remember, and type it the same way twice.',
        'Click Register.',
        'You can now sign in straight away with your mobile number (or email) and that password.',
      ]},
      { note: 'Write your password down somewhere safe. If you forget it you cannot reset it yourself — you must '
            + 'ask your college or an MSYEP administrator to set a new one for you.' },
    ]},

    { h1: 'Signing in', blocks: [
      { shot: '00-login.jpg', caption: 'The sign-in screen. Choose the Student tab.' },
      { steps: [
        'Click the Student button.',
        'In the first box type your registered mobile number OR your email — either works.',
        'Type your password. Use the eye icon to check it.',
        'Click Sign in. Your profile opens.',
      ]},
      { note: 'If you see "Waking up the server...", just wait. The server sleeps when nobody is using it and can '
            + 'take up to a minute to start. Do not keep clicking Sign in.' },
      { h2: 'Problems signing in' },
      { fields: [
        ['Invalid username or password', 'Check your mobile number and that capital letters are right.'],
        ['Account is disabled', 'Your login was deactivated. Ask your college or an administrator.'],
        ['You never set a password', 'If your college created your record for you, ask them to set a password for you on the Logins page.'],
      ]},
    ]},

    { h1: 'Your profile — the seven tabs', blocks: [
      { shot: 'S02-profile-account.jpg', caption: 'Your profile. The bar at the top shows how complete it is.' },
      { p: 'Your profile is split into seven tabs. The percentage on each tab shows how much of it you have filled. '
         + 'Click Save at the bottom — you can come back and finish later.' },
      { p: 'Fill in as much as you can: your profile has to be reasonably complete before you can generate your CV.' },

      { fields: [
        ['Student Name', 'Your full name.'],
        ['Student Email', 'Your email address.'],
        ['Mobile No', 'Your mobile number.'],
      ], title: 'Tab 1 - Account' },

      { fields: [
        ['Gender', 'Select from the list.'],
        ['Date of Birth', 'Pick from the calendar. Needed for your CV.'],
        ['Caste', 'Select your category.'],
        ['Father Name', "Your father's name."],
        ['Mother Name', "Your mother's name."],
      ], title: 'Tab 2 - Personal Details' },

      { fields: [
        ['School Studied', 'Your SSLC / 10th school.'],
        ['Percentage / CGPA', 'Choose which you are entering, then the value.'],
        ['Year of Passing', 'The year you finished.'],
        ['College / School Studied', 'Your PU or Diploma institution.'],
        ['Stream / Course (PU / Diploma)', 'What you studied.'],
        ['College Studied', 'Your degree college, if you have one.'],
        ['Course / Stream (Degree)', 'Your degree course.'],
      ], title: 'Tab 3 - Education' },

      { fields: [
        ['Educational Qualification (Current Class)', 'What you are studying now.'],
        ['Interested for Internship?', 'Yes or No.'],
        ['Hobbies', 'Tick any that apply, such as Music, Classical Dancing or Acting.'],
        ['Technical Skills', 'A long checklist - tick everything you know, from MS Office and typing through to programming, networking, design, AutoCAD, AI and cloud. These appear on your CV.'],
        ['Career Goal', 'A sentence about the work you want. This is printed on your CV.'],
      ], title: 'Tab 4 - Job' },

      { fields: [
        ['Zone (University)', 'Required.'],
        ['Center (College)', 'Required.'],
        ['State / District / Taluk or Town / Village or GP', 'Where you live.'],
        ['Pincode', 'Your postal code.'],
        ['Native Place (if migrant)', 'Only if you have moved.'],
        ['Hostel Name', 'If you stay in a hostel.'],
        ['Postal Address', 'Your full address.'],
      ], title: 'Tab 5 - Address' },

      { fields: [
        ['Course Joining Date', 'When you joined the programme.'],
        ['Choose College / Hostel Name', 'Where you are placed.'],
      ], title: 'Tab 6 - MSYEP Allotments' },

      { fields: [
        ['Document uploads (7 slots)', 'Upload your passport-size photo, Aadhaar, marks cards and any other documents your college asks for.'],
      ], title: 'Tab 7 - Documents' },
      { note: 'Your passport-size photo is needed for your CV, so upload it even if you leave other documents '
            + 'for later.' },
    ]},

    { h1: 'My Resume / CV', blocks: [
      { shot: 'S03-resume.jpg', caption: 'The CV page lists exactly which fields are still empty.' },
      { p: 'The portal builds a formal MSYEP resume from your profile. There is a one-time fee of Rs.90; once you '
         + 'have paid you can download it as many times as you like.' },
      { h2: 'Step 1 - complete your profile' },
      { p: 'If anything is missing the page lists it as buttons, as in the picture above. Click any one of them to '
         + 'jump straight to that field, fill it in, then come back.' },
      { bullets: [
        'Date of Birth', 'Gender', 'Educational Qualification', 'Career Objective',
        'Technical Skills', 'District', 'Passport Size Photo',
      ]},
      { h2: 'Step 2 - pay and download' },
      { steps: [
        'When no fields are listed as missing, the payment button becomes active.',
        'Pay the one-time Rs.90 fee.',
        'Your CV is generated and downloads as a PDF.',
        'You can download it again at any time at no extra cost.',
      ]},
      { note: 'The fee is charged once per student, not per download.' },
    ]},

    { h1: 'Entrance Test', blocks: [
      { shot: 'S04-entrance-test.jpg', caption: 'The entrance test screen.' },
      { h2: 'Before you start' },
      { fields: [
        ['Questions', '10 questions.'],
        ['Time limit', '10 minutes.'],
        ['Pass mark', '5 out of 10.'],
        ['Selfie', 'Required. You must allow the camera before the test will start.'],
      ]},
      { steps: [
        'Click Enable camera and allow your browser to use it.',
        'Make sure your face is visible in the box on the left.',
        'Click Start Test.',
        'Answer all 10 questions within 10 minutes.',
        'Submit. Your result is shown and saved to your record.',
      ]},
      { note: 'Find a quiet place with a working camera and a steady internet connection before you begin - the '
            + '10-minute timer keeps running once the test has started.', kind: 'warn' },
    ]},

    { h1: 'Everyday tips', blocks: [
      { bullets: [
        'The menu is on the left. On a phone, tap the button at the top-left to open it.',
        'Click your name at the top-right to log out.',
        'Always click Save before leaving a profile tab.',
        'The portal works on a phone, so you can fill in your profile and upload photos from your mobile.',
      ]},
      { h2: 'Who to ask for help' },
      { fields: [
        ['Forgot your password', 'Your college or an MSYEP administrator can set a new one.'],
        ['Your college is missing from the list', 'Tell your college - an administrator has to add it.'],
        ['Your village is missing', 'An administrator can add it on the Location page.'],
        ['CV will not generate', 'Check the list of missing fields on the My Resume / CV page.'],
      ]},
    ]},
  ],
};
