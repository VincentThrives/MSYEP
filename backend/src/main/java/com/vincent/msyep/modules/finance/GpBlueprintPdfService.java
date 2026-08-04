package com.vincent.msyep.modules.finance;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.vincent.msyep.modules.center.Center;
import com.vincent.msyep.modules.center.CenterDocument;
import com.vincent.msyep.modules.center.CenterRepository;
import com.vincent.msyep.modules.student.Student;
import com.vincent.msyep.modules.student.StudentDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gram-Panchayat "Blue Print" report that Finance attaches to the GP mail. Stage 1 covers the
 * financial packet — cover, the Kannada GP requisition letter with the student amount table, and the
 * invoice summary — all printed on the YKTK letterhead. The per-student document/certificate pages
 * are added in a later stage.
 */
@Service
public class GpBlueprintPdfService {

    private static final Logger log = LoggerFactory.getLogger(GpBlueprintPdfService.class);
    private static final String LETTERHEAD = "cba-letterhead.pdf";
    private static final float HEADER_H = 156f;
    private static final float FOOTER_H = 66f;
    private static final int AMOUNT = 1500;

    /** Center program-photo slots appended after each student's certificate. */
    private static final String[] PROGRAM_PHOTOS = {
            "theoryClassPhoto", "practicalClassPhoto", "sowFilledCopy", "studentOpinion",
            "guestOpinion", "batchOpinion", "residentialProof"
    };

    private final MongoTemplate mongo;
    private final CenterRepository centers;
    private final com.vincent.msyep.modules.sow.SowService sowService;
    private final com.vincent.msyep.modules.entrance.EntranceAttemptRepository entranceAttempts;
    private final String uploadsDir;

    public GpBlueprintPdfService(MongoTemplate mongo, CenterRepository centers,
                                 com.vincent.msyep.modules.sow.SowService sowService,
                                 com.vincent.msyep.modules.entrance.EntranceAttemptRepository entranceAttempts,
                                 @Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.mongo = mongo;
        this.centers = centers;
        this.sowService = sowService;
        this.entranceAttempts = entranceAttempts;
        this.uploadsDir = uploadsDir;
    }

    /** Entrance-test marks (best SUBMITTED attempt) as "score/total", or "-" if not taken. */
    private String entranceMarks(String studentId) {
        if (!StringUtils.hasText(studentId)) return "-";
        return entranceAttempts.findByStudentIdOrderByStartedAtDesc(studentId).stream()
                .filter(a -> "SUBMITTED".equals(a.getStatus()))
                .findFirst()
                .map(a -> a.getScore() + "/" + a.getTotal())
                .orElse("-");
    }

    public byte[] build(String gramPanchayat, String taluk, String district) {
        Query q = new Query();
        if (StringUtils.hasText(gramPanchayat)) q.addCriteria(Criteria.where("gramPanchayat").is(gramPanchayat));
        if (StringUtils.hasText(taluk)) q.addCriteria(Criteria.where("taluk").is(taluk));
        if (StringUtils.hasText(district)) q.addCriteria(Criteria.where("district").is(district));
        List<Student> students = mongo.find(q, Student.class);

        // Fill taluk/district from the students if not supplied.
        String tk = StringUtils.hasText(taluk) ? taluk : first(students, Student::getTaluk);
        String dt = StringUtils.hasText(district) ? district : first(students, Student::getDistrict);
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String refNo = "MSYEP/GP/" + String.format("%05d", Math.abs((nz(gramPanchayat) + tk).hashCode()) % 100000)
                + "/" + academicYear();

        int sc = 0, st = 0, others = 0;
        for (Student s : students) {
            String cat = caste(s);
            if ("SC".equals(cat)) sc++;
            else if ("ST".equals(cat)) st++;
            else others++;
        }
        // Letter "student strength" list — every student counts ₹1500 (Others included).
        int listTotal = students.size() * AMOUNT;
        // Invoice funding claim — Others caste is not funded (₹0), so excluded from the payable total.
        int payable = (sc + st) * AMOUNT;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument lh = new PdfDocument(new PdfReader(new ClassPathResource(LETTERHEAD).getInputStream()));
             PdfDocument dst = new PdfDocument(new PdfWriter(out))) {

            PdfFormXObject letterhead = lh.getPage(1).copyAsFormXObject(dst);
            PdfFont bold, reg;
            try {   // embed real TrueType so overlays render tight (standard-14 fonts can space oddly in some viewers)
                bold = PdfFontFactory.createFont("C:/Windows/Fonts/timesbd.ttf", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                reg = PdfFontFactory.createFont("C:/Windows/Fonts/times.ttf", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception fe) {
                bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
                reg = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            }

            PdfImageXObject cover = img("gp-p3.png");
            PdfImageXObject letter = img("gp-p7.png");
            PdfImageXObject invoice = img("gp-p17.png");
            // Official reference letters (blueprint pages 8-12), added after the first two pages.
            int[] refPages = {8, 9, 10, 11, 12};

            Rectangle ref = lh.getPage(1).getPageSize();
            float w = ref.getWidth(), h = ref.getHeight();
            float s = (h - HEADER_H - FOOTER_H) / h, tx = (w - w * s) / 2f, ty = FOOTER_H;

            // ---- Page 1: cover (blueprint p3) ----
            PdfCanvas cv = newPage(dst, w, h, letterhead);
            place(cv, cover, s, tx, ty, w, h);
            drawCenter(cv, bold, "Kaushalya Patha MSYEP", 298, 674, 15, s, tx, ty);
            drawCenter(cv, reg, "Multi Skill Youth Empowerment Program", 298, 644, 11, s, tx, ty);
            drawT(cv, reg, today, 158, 409, 11, 160, s, tx, ty);
            drawT(cv, bold, nz(gramPanchayat), 36, 251, 11, 190, s, tx, ty);
            drawT(cv, reg, nz(tk), 108, 217, 11, 200, s, tx, ty);
            drawT(cv, reg, nz(dt), 116, 187, 11, 200, s, tx, ty);

            // ---- Page 2: GP requisition letter (blueprint p7) ----
            cv = newPage(dst, w, h, letterhead);
            place(cv, letter, s, tx, ty, w, h);
            drawT(cv, bold, refNo, 355, 742, 10, 210, s, tx, ty);
            // Names sit on the (now-erased) blank lines, before the Kannada label.
            drawT(cv, bold, nz(gramPanchayat), 40, 693, 11, 82, s, tx, ty);
            drawT(cv, bold, nz(tk), 38, 678, 11, 90, s, tx, ty);
            drawT(cv, bold, nz(dt), 38, 663, 11, 98, s, tx, ty);
            // The template table holds 3 rows — fill the first 3 students here; the rest continue
            // on appended pages below (dynamic table).
            float rowY = 181;
            int onLetter = Math.min(students.size(), 3);
            for (int i = 0; i < onLetter; i++) {
                Student stu = students.get(i);
                float y = rowY - 36 * i;
                drawT(cv, reg, nz(stu.getName()), 118, y, 10, 175, s, tx, ty);                 // name
                drawT(cv, reg, caste(stu), 388, y, 10, 60, s, tx, ty);                          // caste
            }
            // Grand total (all students, Others included) in the merged ಒಟ್ಟು (Total) column.
            drawCenter(cv, bold, "Total", 532, 148, 9, s, tx, ty);
            drawCenter(cv, bold, String.valueOf(listTotal), 532, 130, 12, s, tx, ty);

            // ---- Continuation pages for students beyond the 3 template rows ----
            if (students.size() > 3) {
                byte[] cont = continuationPdf(students, listTotal);
                if (cont != null) {
                    try (PdfDocument cp = new PdfDocument(new PdfReader(new ByteArrayInputStream(cont)))) {
                        cp.copyPagesTo(1, cp.getNumberOfPages(), dst);
                    }
                }
            }

            // ---- Reference letters (blueprint pages 8-12) on the YKTK letterhead ----
            for (int rp : refPages) {
                PdfImageXObject pg = img("gp-p" + rp + ".png");
                PdfCanvas rc = newPage(dst, w, h, letterhead);
                if (pg != null) rc.addXObjectFittedIntoRectangle(pg, new Rectangle(tx, ty, w * s, h * s));
            }

            // ---- Per-student section: data + document & center photos ----
            byte[] perStudent = perStudentSection(students, today);
            if (perStudent != null) {
                try (PdfDocument ps = new PdfDocument(new PdfReader(new ByteArrayInputStream(perStudent)))) {
                    ps.copyPagesTo(1, ps.getNumberOfPages(), dst);
                }
            }

            // ---- Certificate of Completion — one designed landscape certificate per student ----
            PdfImageXObject certImg = img("cert-template.png");
            if (certImg != null) {
                for (Student stu : students) {
                    String cid = stu.getCenterId();
                    String from = StringUtils.hasText(cid) ? sowService.programInaugurationDate(cid, 1) : null;
                    String to = StringUtils.hasText(cid) ? sowService.programInaugurationDate(cid, 8) : null;
                    addCertificatePage(dst, certImg, stu, from, to);
                }
            }

            // ---- KP-MSYEP SOW: the 8 programs (names, details & photos) for the GP's center(s) ----
            for (String cid : students.stream().map(Student::getCenterId)
                    .filter(StringUtils::hasText).distinct().toList()) {
                try {
                    byte[] sow = sowService.allProgramsPdf(cid);
                    if (sow != null) {
                        try (PdfDocument sp = new PdfDocument(new PdfReader(new ByteArrayInputStream(sow)))) {
                            sp.copyPagesTo(1, sp.getNumberOfPages(), dst);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("SOW programs for center {} skipped: {}", cid, ex.getMessage());
                }
            }

            // ---- Invoice (LAST page): official cba-letterhead (same arc/header/footer as the other pages) + clean content overlay.
            PdfPage ip = dst.addNewPage(new PageSize(w, h));
            cv = new PdfCanvas(ip);
            cv.addXObjectAt(letterhead, 0, 0);   // real letterhead background — identical to every other page
            if (invoice != null) cv.addXObjectFittedIntoRectangle(invoice, new Rectangle(0, 0, w, h));  // transparent content overlay
            // Auto-generated 5-digit invoice bill number (deterministic per GP) + academic year.
            String billNo = String.format("%05d", Math.abs(("bill" + nz(gramPanchayat) + nz(tk)).hashCode()) % 100000);
            drawCenter(cv, bold, billNo, 528, 616, 13, 1, 0, 0);       // on the "bill no :" underline
            drawT(cv, reg, academicYear(), 42, 602, 11, 90, 1, 0, 0);  // under the INVOICE label
            // Address block — taluk / district on the dotted blanks.
            drawCenter(cv, bold, nz(tk), 214, 528, 11, 1, 0, 0);       // taluk blank
            drawCenter(cv, bold, nz(dt), 80, 506, 11, 1, 0, 0);        // district blank
            // ---- Funding table values (grid + Kannada header baked into the template) — centred per cell.
            float[] icx = {97.5f, 232.5f, 355f, 487.5f};
            float[] irY = {348f, 298f, 248f};   // SC, ST, Others text baselines
            String[][] invRows = {
                    {"1500/-", String.valueOf(sc),     "SC",     String.valueOf(sc * AMOUNT)},
                    {"1500/-", String.valueOf(st),     "ST",     String.valueOf(st * AMOUNT)},
                    {"1500/-", String.valueOf(others), "Others", "0"},
            };
            for (int r = 0; r < invRows.length; r++) {
                drawCenter(cv, reg,  invRows[r][0], icx[0], irY[r], 12, 1, 0, 0);
                drawCenter(cv, bold, invRows[r][1], icx[1], irY[r], 13, 1, 0, 0);
                drawCenter(cv, reg,  invRows[r][2], icx[2], irY[r], 12, 1, 0, 0);
                drawCenter(cv, bold, invRows[r][3], icx[3], irY[r], 13, 1, 0, 0);
            }
            // Subtotal + Total (Others excluded from the payable claim), left-aligned below the table.
            drawT(cv, bold, "Subtotal  -  " + payable, 40, 205, 12, 260, 1, 0, 0);
            drawT(cv, bold, "TOTAL AMOUNT  -  " + payable, 40, 176, 13, 320, 1, 0, 0);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to build GP blueprint: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    // ---------------------------------------------------------------- helpers

    private PdfCanvas newPage(PdfDocument dst, float w, float h, PdfFormXObject letterhead) {
        PdfPage op = dst.addNewPage(new PageSize(w, h));
        PdfCanvas cv = new PdfCanvas(op);
        cv.addXObjectFittedIntoRectangle(letterhead, new Rectangle(0, 0, w, h));
        return cv;
    }

    private void place(PdfCanvas cv, PdfImageXObject page, float s, float tx, float ty, float w, float h) {
        if (page != null) cv.addXObjectFittedIntoRectangle(page, new Rectangle(tx, ty, w * s, h * s));
    }

    private PdfImageXObject img(String name) {
        try (InputStream in = new ClassPathResource(name).getInputStream()) {
            return new PdfImageXObject(ImageDataFactory.create(in.readAllBytes()));
        } catch (Exception e) {
            log.warn("gp blueprint asset {} missing: {}", name, e.getMessage());
            return null;
        }
    }

    private static String caste(Student s) {
        String c = s.getCaste() == null ? "" : s.getCaste().trim().toUpperCase();
        if (c.contains("SC") || c.equals("SCHEDULED CASTE")) return "SC";
        if (c.contains("ST") || c.equals("SCHEDULED TRIBE")) return "ST";
        return "Others";
    }

    private String academicYear() {
        LocalDate now = LocalDate.now();
        int y = now.getMonthValue() >= 6 ? now.getYear() : now.getYear() - 1;
        return y + "-" + String.valueOf(y + 1).substring(2);
    }

    private PdfExtGState opaque() {
        return new PdfExtGState().setFillOpacity(1f).setStrokeOpacity(1f).setSoftMask(PdfName.None);
    }

    private void drawT(PdfCanvas cv, PdfFont f, String text, float x, float y, float size, float maxW,
                       float s, float tx, float ty) {
        if (!StringUtils.hasText(text)) return;
        float fs = size * s, mw = maxW * s;
        while (fs > 5 && f.getWidth(text, fs) > mw) fs -= 0.5f;
        cv.saveState().setExtGState(opaque()).setFillColor(ColorConstants.BLACK)
                .beginText().setFontAndSize(f, fs)
                .setTextRenderingMode(PdfCanvasConstants.TextRenderingMode.FILL)
                .moveText(tx + x * s, ty + y * s).showText(text).endText().restoreState();
    }

    /** Draw a single bordered table row (outer box + vertical column dividers) at identity scale. */
    private void drawTableRow(PdfCanvas cv, float x, float y, float wRow, float hRow, float[] dividers, DeviceRgb border) {
        cv.saveState().setExtGState(opaque()).setStrokeColor(border).setLineWidth(0.8f);
        cv.rectangle(x, y, wRow, hRow).stroke();
        for (float dx : dividers) {
            cv.moveTo(dx, y).lineTo(dx, y + hRow).stroke();
        }
        cv.restoreState();
    }

    private void drawCenter(PdfCanvas cv, PdfFont f, String text, float cx, float y, float size,
                            float s, float tx, float ty) {
        if (!StringUtils.hasText(text)) return;
        float fs = size * s;
        float x = cx - f.getWidth(text, fs) / 2f / s;
        drawT(cv, f, text, x, y, size, 600, s, tx, ty);
    }

    /** One designed landscape "Certificate of Completion" per student (name, dates, reg no overlaid). */
    private void addCertificatePage(PdfDocument dst, PdfImageXObject certImg, Student s, String from, String to) {
        try {
            PdfPage p = dst.addNewPage(new PageSize(842, 595));
            PdfCanvas cv = new PdfCanvas(p);
            cv.addXObjectFittedIntoRectangle(certImg, new Rectangle(0, 0, 842, 595));
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            PdfFont reg = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            // Student name (centered where "S T DIVAKARA" was).
            drawCenter(cv, bold, nz(s.getName()).toUpperCase(), 421, 213, 24, 1, 0, 0);
            // Training period: from = SOW program 1, to = SOW program 8 (centered on the two blanks).
            drawCenter(cv, reg, fmt(from), 381, 123, 12, 1, 0, 0);
            drawCenter(cv, reg, fmt(to), 509, 123, 12, 1, 0, 0);
            // Registration No: <student register number> (label + number redrawn on the cleared line).
            drawCenter(cv, reg, "Registration No: " + nz(s.getRegisterNo()), 421, 85, 13, 1, 0, 0);
        } catch (Exception e) {
            log.warn("certificate page failed for {}: {}", s.getName(), e.getMessage());
        }
    }

    /** Continuation table (students 4..N) as a letterhead-framed, auto-paginating table. */
    private byte[] continuationPdf(List<Student> students, int listTotal) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument lh = new PdfDocument(new PdfReader(new ClassPathResource(LETTERHEAD).getInputStream()));
             PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
            PdfFormXObject letterhead = lh.getPage(1).copyAsFormXObject(pdf);
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, ev -> {
                PdfPage page = ((PdfDocumentEvent) ev).getPage();
                Rectangle p = page.getPageSize();
                new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf)
                        .addXObjectFittedIntoRectangle(letterhead, new Rectangle(0, 0, p.getWidth(), p.getHeight()));
            });
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            PdfFont reg = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            try (Document doc = new Document(pdf)) {
                doc.setFont(reg);
                doc.setMargins(HEADER_H + 14, 42, FOOTER_H + 14, 42);
                doc.add(new Paragraph("Gram Panchayat Student List (continued)")
                        .setFont(bold).setFontSize(12).setMarginBottom(6));
                Table t = new Table(UnitValue.createPercentArray(new float[]{10, 40, 18, 14, 18}))
                        .useAllAvailableWidth();
                for (String hd : new String[]{"Sl. No", "Student Name", "Amount (Rs.)", "Caste", "Total"}) {
                    t.addHeaderCell(new Cell().add(new Paragraph(hd).setFont(bold).setFontSize(9).setMargin(0))
                            .setBackgroundColor(new DeviceRgb(230, 238, 233)).setPadding(4));
                }
                for (int i = 3; i < students.size(); i++) {
                    Student stu = students.get(i);
                    t.addCell(contCell(String.valueOf(i + 1), reg));
                    t.addCell(contCell(nz(stu.getName()), reg));
                    t.addCell(contCell("1500", reg));
                    t.addCell(contCell(caste(stu), reg));
                    t.addCell(contCell("1500", reg));
                }
                doc.add(t);
                doc.add(new Paragraph("Grand Total (all students): Rs. " + listTotal)
                        .setFont(bold).setFontSize(11).setTextAlignment(TextAlignment.RIGHT).setMarginTop(8));
            }
        } catch (Exception e) {
            log.warn("continuation table failed: {}", e.getMessage());
            return null;
        }
        return out.toByteArray();
    }

    private Cell contCell(String s, PdfFont f) {
        return new Cell().add(new Paragraph(nz(s)).setFont(f).setFontSize(9).setMargin(0)).setPadding(4);
    }

    // ---------------------------------------------------------------- per-student section

    /** A letterhead-framed, flowing section: one block per student (data + certificate + photos). */
    private byte[] perStudentSection(List<Student> students, String today) {
        if (students.isEmpty()) return null;
        byte[] adminSig = readAdminSignature();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument lh = new PdfDocument(new PdfReader(new ClassPathResource(LETTERHEAD).getInputStream()));
             PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
            PdfFormXObject letterhead = lh.getPage(1).copyAsFormXObject(pdf);
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, ev -> {
                PdfPage page = ((PdfDocumentEvent) ev).getPage();
                Rectangle p = page.getPageSize();
                new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf)
                        .addXObjectFittedIntoRectangle(letterhead, new Rectangle(0, 0, p.getWidth(), p.getHeight()));
            });
            try (Document doc = new Document(pdf)) {
                doc.setFont(PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN));
                doc.setMargins(HEADER_H + 14, 42, FOOTER_H + 14, 42);
                boolean first = true;
                for (Student s : students) {
                    if (!first) doc.add(new AreaBreak());
                    first = false;
                    addStudentBlock(doc, s, adminSig, today);
                }
            }
        } catch (Exception e) {
            log.warn("per-student section failed: {}", e.getMessage());
            return null;
        }
        return out.toByteArray();
    }

    private void addStudentBlock(Document doc, Student s, byte[] adminSig, String today) {
        Center center = StringUtils.hasText(s.getCenterId())
                ? centers.findById(s.getCenterId()).orElse(null) : null;

        doc.add(new Paragraph("Student Documents — " + nz(s.getName()))
                .setBold().setFontSize(13).setMarginBottom(4));

        // Data table with the passport photo in the last column.
        Table t = new Table(UnitValue.createPercentArray(new float[]{6, 22, 19, 12, 14, 27})).useAllAvailableWidth();
        for (String h : new String[]{"SNo", "Student Name", "Father Name", "Caste", "Entrance Marks", "Photo"})
            t.addHeaderCell(cell(h, true));
        t.addCell(cell("1", false));
        t.addCell(cell(nz(s.getName()), false));
        t.addCell(cell(nz(s.getFatherName()), false));
        t.addCell(cell(caste(s), false));
        t.addCell(cell(entranceMarks(s.getId()), false));
        Cell photoCell = new Cell().setBorder(new SolidBorder(0.5f)).setPadding(2);
        byte[] photo = readDoc(s.getDocuments(), "passportPhoto");
        if (photo != null) {
            try { photoCell.add(new Image(ImageDataFactory.create(photo)).setAutoScale(true)); } catch (Exception ignored) { }
        }
        t.addCell(photoCell);
        doc.add(t);
        doc.add(new Paragraph("Class / Qualification: " + nz(s.getEducationalQualification())
                + "     College: " + nz(s.getCollegeName())).setFontSize(9).setMarginTop(2));

        // (The Certificate of Completion is now a separate designed landscape page — see certificatePages.)

        // Document images: Aadhaar + Caste Certificate.
        imageRow(doc, "Aadhaar Card", readDoc(s.getDocuments(), "aadhaar"),
                "Caste Certificate", readDoc(s.getDocuments(), "casteCertificate"));

        // Center program photos (up to 8).
        if (center != null) {
            List<byte[]> photos = new ArrayList<>();
            for (String type : PROGRAM_PHOTOS) {
                byte[] b = readDoc(center.getDocuments(), type);
                if (b != null) photos.add(b);
            }
            if (!photos.isEmpty()) {
                doc.add(new Paragraph("Center Program Photos — " + nz(center.getName()))
                        .setBold().setFontSize(10).setMarginTop(8));
                Table grid = new Table(2).useAllAvailableWidth();
                for (byte[] b : photos) {
                    Cell c = new Cell().setBorder(new SolidBorder(0.5f)).setPadding(3);
                    try { c.add(new Image(ImageDataFactory.create(b)).setAutoScale(true)); } catch (Exception ex) { continue; }
                    grid.addCell(c);
                }
                if (photos.size() % 2 == 1) grid.addCell(new Cell().setBorder(Border.NO_BORDER));
                doc.add(grid);
            }
        }
    }

    private void imageRow(Document doc, String l1, byte[] i1, String l2, byte[] i2) {
        if (i1 == null && i2 == null) return;
        Table t = new Table(2).useAllAvailableWidth().setMarginTop(8);
        t.addCell(imgCell(l1, i1));
        t.addCell(imgCell(l2, i2));
        doc.add(t);
    }

    private Cell imgCell(String label, byte[] img) {
        Cell c = new Cell().setBorder(new SolidBorder(0.5f)).setPadding(4);
        c.add(new Paragraph(label).setBold().setFontSize(9).setMarginBottom(3));
        if (img != null) {
            try { c.add(new Image(ImageDataFactory.create(img)).setAutoScale(true)); }
            catch (Exception e) { c.add(new Paragraph("(not an image)").setFontSize(8).setItalic()); }
        } else {
            c.add(new Paragraph("(not uploaded)").setFontSize(8).setItalic());
        }
        return c;
    }

    private Cell cell(String s, boolean header) {
        Cell c = new Cell().add(new Paragraph(nz(s).isEmpty() ? "-" : s).setFontSize(9).setMargin(0))
                .setBorder(new SolidBorder(0.5f)).setPadding(3);
        if (header) c.setBold();
        return c;
    }

    private byte[] readDoc(List<?> documents, String type) {
        if (documents == null) return null;
        for (Object o : documents) {
            String dtype = null, dpath = null;
            if (o instanceof StudentDocument d) { dtype = d.getType(); dpath = d.getPath(); }
            else if (o instanceof CenterDocument d) { dtype = d.getType(); dpath = d.getPath(); }
            if (type.equals(dtype) && StringUtils.hasText(dpath)) {
                try {
                    Path p = Paths.get(uploadsDir).resolve(dpath.replace("uploads/", ""));
                    if (Files.exists(p)) return Files.readAllBytes(p);
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private byte[] readAdminSignature() {
        for (String name : new String[]{"approval-signature.png", "giver-signature.png"}) {
            try {
                Path p = Paths.get(uploadsDir, "system", name);
                if (Files.exists(p)) return Files.readAllBytes(p);
            } catch (Exception ignored) { }
        }
        return null;
    }

    /** Format an ISO yyyy-MM-dd date as dd-MM-yyyy for the certificate; blank if unavailable. */
    private static String fmt(String iso) {
        if (!StringUtils.hasText(iso)) return "";
        try {
            return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    private static String first(List<Student> list, java.util.function.Function<Student, String> f) {
        for (Student s : list) { String v = f.apply(s); if (StringUtils.hasText(v)) return v; }
        return "";
    }
}
