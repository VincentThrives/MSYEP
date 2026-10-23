import { buildManual } from './build.mjs';
import { admin }   from './admin.mjs';
import { staff }   from './staff.mjs';
import { zone }    from './zone.mjs';
import { center }  from './center.mjs';
import { finance } from './finance.mjs';
import { student } from './student.mjs';

const all = [
  [admin,   'MSYEP-User-Manual-Admin-Console.pdf'],
  [staff,   'MSYEP-User-Manual-Staff-Portal.pdf'],
  [zone,    'MSYEP-User-Manual-Zone-Portal.pdf'],
  [center,  'MSYEP-User-Manual-Center-Portal.pdf'],
  [finance, 'MSYEP-User-Manual-Finance-Wing.pdf'],
  [student, 'MSYEP-User-Manual-Student-Portal.pdf'],
];
for (const [def, file] of all) {
  const r = await buildManual(def, './out/' + file, './shots');
  console.log(String(r.pages).padStart(3), 'pages  ', file);
}
