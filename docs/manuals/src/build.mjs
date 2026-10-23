// MSYEP user-manual generator.
// Renders a manual definition (see manuals/*.mjs) to a paginated A4 PDF with
// headings, numbered steps, field tables and real screenshots.

import { PDFDocument, StandardFonts, rgb } from 'pdf-lib';
import fs from 'fs';
import path from 'path';

const A4 = [595.28, 841.89];
const M = { l: 52, r: 52, t: 56, b: 56 };
const GREEN = rgb(0.055, 0.318, 0.196);      // #0E5132
const GREEN2 = rgb(0.118, 0.478, 0.275);     // #1E7A46
const GOLD = rgb(0.788, 0.635, 0.153);       // #C9A227
const INK = rgb(0.13, 0.19, 0.16);
const MUTED = rgb(0.42, 0.49, 0.45);
const RULE = rgb(0.87, 0.90, 0.88);
const SOFT = rgb(0.906, 0.953, 0.925);       // light green fill

/**
 * The built-in PDF fonts only cover WinAnsi, so emoji and other non-Latin-1 characters
 * would throw. Swap the ones we use for words, then drop anything still out of range.
 */
const ICONS = {
  '✏': '[Edit]', '🏅': '[Certificate]', '📄': '[MOU]',
  '✉': '[Mail]', '🗑': '[Delete]', '🔑': '[Key]',
  '🔘': '[Toggle]', '☰': '[Menu]', '➤': '>', '✅': '',
  '⚠': '!', '📍': '', '📎': '',
};
function sanitize(s) {
  let t = String(s);
  for (const [k, v] of Object.entries(ICONS)) t = t.split(k).join(v);
  t = t.replace(/️/g, '');                 // emoji variation selectors
  t = t.replace(/[‘’]/g, "'").replace(/[“”]/g, '"');
  t = t.replace(/…/g, '...');
  t = t.replace(/[—–]/g, '-');        // em / en dash
  t = t.replace(/₹/g, 'Rs.');              // rupee sign
  t = t.replace(/→/g, '->').replace(/✓/g, 'v');
  t = t.replace(/•/g, '-').replace(/·/g, '-');   // bullet / middle dot
  // anything still outside Latin-1 becomes a space rather than crashing the build
  t = Array.from(t).map(ch => ch.codePointAt(0) > 0xFF ? ' ' : ch).join('');
  return t.replace(/ {2,}/g, ' ').trim();
}

export async function buildManual(def, outFile, shotsDir) {
  const pdf = await PDFDocument.create();
  const F = await pdf.embedFont(StandardFonts.Helvetica);
  const B = await pdf.embedFont(StandardFonts.HelveticaBold);
  const I = await pdf.embedFont(StandardFonts.HelveticaOblique);

  let page, y;
  let pageNo = 0;
  const pages = [];

  const newPage = () => {
    page = pdf.addPage(A4);
    pages.push(page);
    pageNo++;
    y = A4[1] - M.t;
    // running header
    page.drawText(sanitize(def.title), { x: M.l, y: A4[1] - 32, size: 8, font: F, color: MUTED });
    page.drawText(sanitize('MSYEP'), { x: A4[0] - M.r - B.widthOfTextAtSize('MSYEP', 8), y: A4[1] - 32, size: 8, font: B, color: GOLD });
    page.drawLine({ start: { x: M.l, y: A4[1] - 40 }, end: { x: A4[0] - M.r, y: A4[1] - 40 }, thickness: 0.5, color: RULE });
    return page;
  };

  const need = (h) => { if (y - h < M.b + 16) newPage(); };

  const wrap = (text, font, size, width) => {
    const words = sanitize(text).split(/\s+/);
    const lines = [];
    let line = '';
    for (const w of words) {
      const t = line ? line + ' ' + w : w;
      if (font.widthOfTextAtSize(t, size) <= width) line = t;
      else { if (line) lines.push(line); line = w; }
    }
    if (line) lines.push(line);
    return lines;
  };

  const para = (text, { size = 10, font = F, color = INK, gap = 5, indent = 0 } = {}) => {
    const width = A4[0] - M.l - M.r - indent;
    for (const ln of wrap(text, font, size, width)) {
      need(size + 4);
      page.drawText(sanitize(ln), { x: M.l + indent, y, size, font, color });
      y -= size + 3.2;
    }
    y -= gap;
  };

  const h1 = (t) => {
    need(46);
    y -= 6;
    page.drawText(sanitize(t), { x: M.l, y, size: 17, font: B, color: GREEN });
    y -= 7;
    page.drawLine({ start: { x: M.l, y }, end: { x: M.l + 46, y }, thickness: 2.5, color: GOLD });
    y -= 16;
  };

  const h2 = (t) => {
    need(32);
    y -= 4;
    page.drawText(sanitize(t), { x: M.l, y, size: 12, font: B, color: GREEN2 });
    y -= 16;
  };

  const steps = (list) => {
    list.forEach((s, i) => {
      const n = `${i + 1}.`;
      need(16);
      page.drawText(sanitize(n), { x: M.l + 2, y, size: 10, font: B, color: GREEN2 });
      const width = A4[0] - M.l - M.r - 22;
      const lines = wrap(s, F, 10, width);
      lines.forEach((ln, j) => {
        if (j > 0) need(14);
        page.drawText(sanitize(ln), { x: M.l + 22, y, size: 10, font: F, color: INK });
        y -= 13.5;
      });
      y -= 3;
    });
    y -= 4;
  };

  const bullets = (list, { icon = '•' } = {}) => {
    for (const s of list) {
      need(16);
      page.drawText(sanitize(icon), { x: M.l + 4, y, size: 10, font: F, color: GOLD });
      const width = A4[0] - M.l - M.r - 20;
      wrap(s, F, 10, width).forEach((ln, j) => {
        if (j > 0) need(14);
        page.drawText(sanitize(ln), { x: M.l + 18, y, size: 10, font: F, color: INK });
        y -= 13.5;
      });
      y -= 2.5;
    }
    y -= 4;
  };

  /** Field table: rows of [name, note] rendered as a two-column striped list. */
  const fieldTable = (title, rows) => {
    if (!rows || !rows.length) return;
    need(40);
    if (title) { h2(title); }
    const w = A4[0] - M.l - M.r;
    const c1 = Math.min(210, w * 0.42);
    // header
    need(22);
    page.drawRectangle({ x: M.l, y: y - 4, width: w, height: 18, color: SOFT });
    page.drawText(sanitize('Field'), { x: M.l + 6, y, size: 9, font: B, color: GREEN });
    page.drawText(sanitize('What to enter'), { x: M.l + c1 + 6, y, size: 9, font: B, color: GREEN });
    y -= 20;
    rows.forEach((r, idx) => {
      const name = Array.isArray(r) ? r[0] : r;
      const note = Array.isArray(r) ? (r[1] || '') : '';
      const nameLines = wrap(name, B, 9, c1 - 12);
      const noteLines = wrap(note, F, 9, w - c1 - 12);
      const h = Math.max(nameLines.length, noteLines.length) * 11.5 + 6;
      need(h + 2);
      if (idx % 2 === 0) page.drawRectangle({ x: M.l, y: y - h + 10, width: w, height: h, color: rgb(0.985, 0.99, 0.985) });
      let yy = y;
      nameLines.forEach(ln => { page.drawText(sanitize(ln), { x: M.l + 6, y: yy, size: 9, font: B, color: INK }); yy -= 11.5; });
      yy = y;
      noteLines.forEach(ln => { page.drawText(sanitize(ln), { x: M.l + c1 + 6, y: yy, size: 9, font: F, color: MUTED }); yy -= 11.5; });
      y -= h;
    });
    y -= 8;
  };

  const note = (text, kind = 'tip') => {
    const w = A4[0] - M.l - M.r;
    const lines = wrap(text, F, 9.5, w - 26);
    const h = lines.length * 12 + 14;
    need(h + 6);
    const bg = kind === 'warn' ? rgb(0.996, 0.945, 0.945) : rgb(1, 0.973, 0.863);
    const bar = kind === 'warn' ? rgb(0.70, 0.15, 0.12) : GOLD;
    page.drawRectangle({ x: M.l, y: y - h + 12, width: w, height: h, color: bg });
    page.drawRectangle({ x: M.l, y: y - h + 12, width: 3, height: h, color: bar });
    let yy = y;
    lines.forEach(ln => { page.drawText(sanitize(ln), { x: M.l + 14, y: yy, size: 9.5, font: F, color: INK }); yy -= 12; });
    y -= h + 6;
  };

  const shot = async (file, caption) => {
    const p = path.join(shotsDir, file);
    if (!fs.existsSync(p)) { console.warn('missing screenshot:', file); return; }
    const img = await pdf.embedJpg(fs.readFileSync(p));
    const maxW = A4[0] - M.l - M.r;
    const scale = Math.min(maxW / img.width, 1);
    const w = img.width * scale, h = img.height * scale;
    need(h + (caption ? 26 : 14));
    page.drawRectangle({ x: M.l - 1, y: y - h - 1, width: w + 2, height: h + 2, borderColor: RULE, borderWidth: 1 });
    page.drawImage(img, { x: M.l, y: y - h, width: w, height: h });
    y -= h + 6;
    if (caption) {
      page.drawText(sanitize(caption), { x: M.l, y, size: 8.5, font: I, color: MUTED });
      y -= 14;
    }
    y -= 4;
  };

  // ---------------- cover ----------------
  page = pdf.addPage(A4); pages.push(page); pageNo++;
  page.drawRectangle({ x: 0, y: A4[1] - 232, width: A4[0], height: 232, color: GREEN });
  page.drawRectangle({ x: 0, y: A4[1] - 238, width: A4[0], height: 6, color: GOLD });
  page.drawText(sanitize('MSYEP'), { x: M.l, y: A4[1] - 118, size: 40, font: B, color: rgb(1, 1, 1) });
  page.drawText(sanitize('Yuktha Kaushalya Tarabethi'), { x: M.l, y: A4[1] - 144, size: 12, font: F, color: rgb(0.88, 0.94, 0.90) });
  page.drawText(sanitize('USER MANUAL'), { x: M.l, y: A4[1] - 196, size: 13, font: B, color: GOLD });
  y = A4[1] - 300;
  page.drawText(sanitize(def.portal), { x: M.l, y, size: 30, font: B, color: GREEN });
  y -= 26;
  page.drawText(sanitize(def.audience), { x: M.l, y, size: 12, font: F, color: MUTED });
  y -= 44;
  for (const ln of wrap(def.intro, F, 11, A4[0] - M.l - M.r)) {
    page.drawText(sanitize(ln), { x: M.l, y, size: 11, font: F, color: INK }); y -= 15;
  }
  y -= 20;
  page.drawText(sanitize('In this manual'), { x: M.l, y, size: 11, font: B, color: GREEN2 }); y -= 18;
  def.sections.forEach((s, i) => {
    page.drawText(sanitize(`${i + 1}.  ${s.h1}`), { x: M.l + 6, y, size: 10, font: F, color: INK }); y -= 15;
  });
  page.drawText(`Version ${def.version}  ·  ${def.date}`, { x: M.l, y: M.b + 10, size: 9, font: F, color: MUTED });

  // ---------------- body ----------------
  newPage();
  for (const s of def.sections) {
    h1(s.h1);
    for (const blk of s.blocks) {
      if (blk.p) para(blk.p);
      else if (blk.h2) h2(blk.h2);
      else if (blk.steps) steps(blk.steps);
      else if (blk.bullets) bullets(blk.bullets);
      else if (blk.fields) fieldTable(blk.title, blk.fields);
      else if (blk.note) note(blk.note, blk.kind || 'tip');
      else if (blk.shot) await shot(blk.shot, blk.caption);
      else if (blk.pagebreak) newPage();
    }
  }

  // page numbers (skip cover)
  pages.forEach((pg, i) => {
    if (i === 0) return;
    const t = `Page ${i} of ${pages.length - 1}`;
    pg.drawText(sanitize(t), { x: (A4[0] - F.widthOfTextAtSize(sanitize(t), 8)) / 2, y: 28, size: 8, font: F, color: MUTED });
  });

  fs.writeFileSync(outFile, await pdf.save());
  return { pages: pages.length, out: outFile };
}
