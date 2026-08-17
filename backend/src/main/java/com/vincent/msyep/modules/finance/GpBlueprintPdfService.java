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
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
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

import javax.imageio.ImageIO;

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

    private final com.vincent.msyep.modules.admin.AdminSignatureService adminSignature;

    public GpBlueprintPdfService(MongoTemplate mongo, CenterRepository centers,
                                 com.vincent.msyep.modules.sow.SowService sowService,
                                 com.vincent.msyep.modules.entrance.EntranceAttemptRepository entranceAttempts,
                                 com.vincent.msyep.modules.admin.AdminSignatureService adminSignature,
                                 @Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.mongo = mongo;
        this.centers = centers;
        this.sowService = sowService;
        this.entranceAttempts = entranceAttempts;
        this.adminSignature = adminSignature;
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

            PdfImageXObject letterhead = img("cba-letterhead.png");   // flat opaque letterhead — arc never composites over content
            PdfFont bold, reg;
            try {   // embed real TrueType so overlays render tight (standard-14 fonts can space oddly in some viewers)
                bold = PdfFontFactory.createFont("C:/Windows/Fonts/timesbd.ttf", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                reg = PdfFontFactory.createFont("C:/Windows/Fonts/times.ttf", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception fe) {
                bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
                reg = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            }

            Rectangle ref = lh.getPage(1).getPageSize();
            float w = ref.getWidth(), h = ref.getHeight();
            float s = (h - HEADER_H - FOOTER_H) / h, tx = (w - w * s) / 2f, ty = FOOTER_H;

            // ---- Page 1: cover — rebuilt as native text on the letterhead ----
            byte[] coverPdf = coverPdf(nz(gramPanchayat), nz(tk), nz(dt), today);
            copyIn(dst, coverPdf);

            // ---- Page 2: GP requisition letter — rebuilt as native text (auto-paginates the student list) ----
            copyIn(dst, requisitionPdf(nz(gramPanchayat), tk, dt, refNo, students, listTotal));

            // ---- Reference letters p8-p12: kept as the ORIGINAL government-issued scans (not rewritten) ----
            for (int rp : new int[]{8, 9, 10, 11, 12}) {
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
                    // Training period: FROM = the student's registered date, TO = +45 calendar days
                    // (Sundays/holidays included).
                    LocalDate regDate = stu.getCreatedAt() != null
                            ? stu.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                            : LocalDate.now();
                    addCertificatePage(dst, certImg, stu, regDate.toString(), regDate.plusDays(45).toString());
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

            // ---- Invoice (LAST page): rebuilt as native text on the letterhead (no scanned image).
            String billNo = String.format("%05d", Math.abs(("bill" + nz(gramPanchayat) + nz(tk)).hashCode()) % 100000);
            byte[] invoicePdf = invoicePdf(tk, dt, sc, st, others, payable, billNo);
            if (invoicePdf != null) {
                try (PdfDocument iv = new PdfDocument(new PdfReader(new ByteArrayInputStream(invoicePdf)))) {
                    iv.copyPagesTo(1, iv.getNumberOfPages(), dst);
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Failed to build GP blueprint: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    // ---------------------------------------------------------------- helpers

    private PdfCanvas newPage(PdfDocument dst, float w, float h, com.itextpdf.kernel.pdf.xobject.PdfXObject letterhead) {
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

    // ---------------------------------------------------------------- Kannada (Java2D-shaped)

    /**
     * iText core cannot shape Kannada conjuncts, so render the (fixed) Kannada string with Java2D —
     * which uses the OS complex-script layout engine and shapes it correctly — into a crisp
     * transparent image, scaled so its visual height ≈ {@code ptSize} points for inline use.
     * Everything else on the page stays true native text.
     */
    private Image kn(String text, float ptSize, DeviceRgb color) {
        try {
            int px = Math.round(ptSize * 4);   // 4× oversample → ~288 DPI when placed at ptSize
            Font font = new Font("Nirmala UI", Font.PLAIN, px);
            FontRenderContext frc = new FontRenderContext(null, true, true);
            TextLayout tl = new TextLayout(text, font, frc);
            Rectangle2D b = tl.getBounds();
            int pad = Math.max(2, px / 12);
            int w = (int) Math.ceil(b.getWidth()) + pad * 2 + 4;
            int h = (int) Math.ceil(tl.getAscent() + tl.getDescent()) + pad * 2;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color == null ? Color.BLACK
                    : new Color((int) (color.getColorValue()[0] * 255),
                                (int) (color.getColorValue()[1] * 255),
                                (int) (color.getColorValue()[2] * 255)));
            tl.draw(g, (float) (pad - b.getX()), pad + tl.getAscent());
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            Image im = new Image(ImageDataFactory.create(out.toByteArray()));
            float scale = (h > 0) ? (ptSize * 1.33f) / h : 1f;   // px→pt (72/54 ≈ 1.33 at 4× with padding)
            im.scale(scale, scale);
            return im;
        } catch (Exception e) {
            log.warn("kannada render failed [{}]: {}", text, e.getMessage());
            return null;
        }
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
            PdfFont boldItalic = PdfFontFactory.createFont(StandardFonts.TIMES_BOLDITALIC);
            // Institute credential tagline — centered in the band above "Certificate of Completion".
            drawCenter(cv, boldItalic, "An ISO 9001:2001 Certified Training Institute", 421, 345, 10, 1, 0, 0);
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

    // ---------------------------------------------------------------- native invoice page

    /** A Kannada string as a right-alignable block image at the given point size. */
    private Image knBlock(String text, float pt) {
        Image im = kn(text, pt, null);
        return im;
    }

    /** Borderless cell holding a block element (Kannada image or native paragraph), vertically centred. */
    private Cell plain(com.itextpdf.layout.element.IBlockElement e) {
        return new Cell().add(e).setBorder(Border.NO_BORDER).setPadding(0)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell plainImg(Image e) {
        Cell c = new Cell().setBorder(Border.NO_BORDER).setPadding(0)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (e != null) c.add(e);
        return c;
    }

    /** A signatory's handwritten signature (transparent PNG asset), right-aligned, up to widthPt wide. */
    /** The central admin/giver signature (uploaded by the admin, else the bundled default), right-aligned. */
    private Image signature(float widthPt) {
        try {
            byte[] bytes = com.vincent.msyep.common.SignatureImage.clean(adminSignature.get());
            if (bytes == null) return null;
            Image im = new Image(ImageDataFactory.create(bytes));
            im.scaleToFit(widthPt, widthPt * 0.62f);
            im.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            return im;
        } catch (Exception e) {
            log.warn("admin signature render failed: {}", e.getMessage());
            return null;
        }
    }

    /** Invoice page rebuilt as native text (English + all data) with Java2D-shaped Kannada, on the letterhead. */
    private byte[] invoicePdf(String taluk, String district, int sc, int st, int others, int payable, String billNo) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
            PdfImageXObject letterhead = img("cba-letterhead.png");   // flat opaque letterhead — arc never composites over content
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, ev -> {
                PdfPage page = ((PdfDocumentEvent) ev).getPage();
                Rectangle p = page.getPageSize();
                new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf)
                        .addXObjectFittedIntoRectangle(letterhead, new Rectangle(0, 0, p.getWidth(), p.getHeight()));
            });
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            PdfFont reg = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            PdfFont ital = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);
            DeviceRgb navy = new DeviceRgb(31, 59, 95);

            try (Document doc = new Document(pdf)) {
                doc.setFont(reg);
                doc.setMargins(HEADER_H + 20, 48, FOOTER_H + 16, 48);

                doc.add(new Paragraph("Invoice").setFont(bold).setFontSize(19)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

                // Top box: INVOICE | bill no
                Table top = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
                top.addCell(new Cell().add(new Paragraph("INVOICE").setFont(bold).setFontSize(14))
                        .setBorder(new SolidBorder(0.8f)).setPadding(8));
                top.addCell(new Cell().add(new Paragraph()
                                .add(new Text("INVOICE  bill no :  ").setFont(bold).setFontSize(12))
                                .add(new Text(billNo).setFont(bold).setFontSize(12).setUnderline())
                                .setTextAlignment(TextAlignment.RIGHT))
                        .setBorder(new SolidBorder(0.8f)).setPadding(8)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE));
                doc.add(top);

                // Addressee (left) + Bank Details (right)
                Table mid = new Table(UnitValue.createPercentArray(new float[]{1f, 1.2f})).useAllAvailableWidth();
                Cell addr = new Cell().setBorder(new SolidBorder(0.8f)).setPadding(8);
                addr.add(knBlock("ಇವರಿಗೆ,", 11));
                addr.add(knBlock("ಕಾರ್ಯನಿರ್ವಹಣಾಧಿಕಾರಿಗಳು", 11));
                Table trow = new Table(UnitValue.createPercentArray(new float[]{1.6f, 1})).useAllAvailableWidth();
                trow.addCell(plainImg(knBlock("ತಾಲ್ಲೂಕು ಪಂಚಾಯಿತಿ", 11)));
                trow.addCell(plain(new Paragraph(nz(taluk)).setFont(bold).setFontSize(11)));
                addr.add(trow);
                Table drow = new Table(UnitValue.createPercentArray(new float[]{1, 1.4f})).useAllAvailableWidth();
                drow.addCell(plain(new Paragraph(nz(district)).setFont(bold).setFontSize(11)));
                drow.addCell(plainImg(knBlock("ಜಿಲ್ಲೆ.", 11)));
                addr.add(drow);
                mid.addCell(addr);

                Cell bank = new Cell().setBorder(new SolidBorder(0.8f)).setPadding(8);
                bank.add(new Paragraph("Bank Details").setFont(ital).setBold().setFontSize(13).setMarginBottom(4));
                bank.add(bankLine(ital, bold, "Bank Name :- ", "State Bank Of India"));
                Table anRow = new Table(UnitValue.createPercentArray(new float[]{1.15f, 1.85f})).useAllAvailableWidth();
                anRow.addCell(plain(new Paragraph("Account Name :- ").setFont(ital).setFontSize(11)));
                anRow.addCell(plainImg(knBlock("ಯುಕ್ತ ಕೌಶಲ್ಯ ತರಬೇತಿ ಕೇಂದ್ರ", 11)));
                bank.add(anRow);
                bank.add(bankLine(ital, bold, "Account No :- ", "40206931529"));
                bank.add(bankLine(ital, bold, "IFSC CODE :- ", "SBIN0018222"));
                bank.add(bankLine(ital, bold, "Branch Name :- ", "MADHUGIRI"));
                mid.addCell(bank);
                doc.add(mid);

                // Funding table — serial-number column + only the caste rows that have students.
                Table ft = new Table(UnitValue.createPercentArray(new float[]{0.8f, 2.1f, 2.1f, 1.5f, 2.1f})).useAllAvailableWidth();
                ft.addHeaderCell(knHeader("ಕ್ರ. ಸಂಖ್ಯೆ", navy));
                ft.addHeaderCell(knHeader("ಒಬ್ಬ ವಿದ್ಯಾರ್ಥಿಗೆ ಸಹಾಯಧನ", navy));
                ft.addHeaderCell(knHeader("ಒಟ್ಟು ತರಬೇತಿ ಪಡೆದ ವಿದ್ಯಾರ್ಥಿ", navy));
                ft.addHeaderCell(new Cell().add(new Paragraph("caste").setFont(reg).setFontSize(11).setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(navy).setBorder(new SolidBorder(0.8f)).setPadding(6)
                        .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
                ft.addHeaderCell(knHeader("ಒಟ್ಟು ಸಹಾಯಧನದ ಮೊತ್ತ", navy));
                String[][] all = {
                        {String.valueOf(sc), "SC", String.valueOf(sc * AMOUNT)},
                        {String.valueOf(st), "ST", String.valueOf(st * AMOUNT)},
                        {String.valueOf(others), "Others", "0"},
                };
                int sn = 1;
                for (String[] r : all) {
                    if (Integer.parseInt(r[0]) <= 0) continue;   // skip castes with no students — no empty rows
                    ft.addCell(invCell(String.valueOf(sn++), reg));
                    ft.addCell(invCell("1500/-", reg));
                    ft.addCell(invCell(r[0], bold));
                    ft.addCell(invCell(r[1], reg));
                    ft.addCell(invCell(r[2], bold));
                }
                doc.add(ft);

                Image inti = knBlock("ಇಂತಿ", 12);
                inti.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                doc.add(inti);

                doc.add(new Paragraph("Subtotal  -  " + payable).setFont(bold).setFontSize(12).setMarginTop(8));
                doc.add(new Paragraph("TOTAL AMOUNT  -  " + payable).setFont(bold).setFontSize(13).setMarginTop(0));

                Image f1 = knBlock("ಜಿಲ್ಲಾ ಮುಖ್ಯ ಕಾರ್ಯಕ್ರಮ ಆಯೋಜಕರ", 12);
                Image f2 = knBlock("ಯುಕ್ತ ಕೌಶಲ್ಯ ತರಬೇತಿ ಕೇಂದ್ರ", 12);
                f1.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                f2.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                Div sig = new Div().setMarginTop(8);
                Image sgn = signature(105);
                if (sgn != null) sig.add(sgn);
                sig.add(f1);
                sig.add(f2);
                doc.add(sig);
            }
        } catch (Exception e) {
            log.warn("invoice page failed: {}", e.getMessage());
            return null;
        }
        return out.toByteArray();
    }

    private Paragraph bankLine(PdfFont label, PdfFont val, String l, String v) {
        return new Paragraph().add(new Text(l).setFont(label).setFontSize(11))
                .add(new Text(v).setFont(val).setFontSize(11)).setMarginBottom(1);
    }

    private Cell knHeader(String kannada, DeviceRgb bg) {
        Image im = kn(kannada, 10, new DeviceRgb(255, 255, 255));
        Cell c = new Cell().setBackgroundColor(bg).setBorder(new SolidBorder(0.8f)).setPadding(6)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (im != null) { im.setHorizontalAlignment(HorizontalAlignment.CENTER); c.add(im); }
        return c;
    }

    private Cell invCell(String s, PdfFont f) {
        return new Cell().add(new Paragraph(nz(s)).setFont(f).setFontSize(12).setMargin(0))
                .setBorder(new SolidBorder(0.8f)).setPadding(6)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHeight(32);
    }

    // ---------------------------------------------------------------- framed native pages

    @FunctionalInterface
    private interface DocBody { void build(Document doc, PdfFont bold, PdfFont reg, PdfFont ital) throws Exception; }

    /** Build a single letterhead-framed page and run {@code body} to fill it (native text). */
    private byte[] framed(DocBody body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
            PdfImageXObject letterhead = img("cba-letterhead.png");   // flat opaque letterhead — arc never composites over content
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, ev -> {
                PdfPage page = ((PdfDocumentEvent) ev).getPage();
                Rectangle p = page.getPageSize();
                new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf)
                        .addXObjectFittedIntoRectangle(letterhead, new Rectangle(0, 0, p.getWidth(), p.getHeight()));
            });
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
            PdfFont reg = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            PdfFont ital = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);
            try (Document doc = new Document(pdf)) {
                doc.setFont(reg);
                doc.setMargins(HEADER_H + 18, 48, FOOTER_H + 16, 48);
                body.build(doc, bold, reg, ital);
            }
        } catch (Exception e) {
            log.warn("framed page failed: {}", e.getMessage());
            return null;
        }
        return out.toByteArray();
    }

    /** Copy every page of a generated sub-PDF into the destination document. */
    private void copyIn(PdfDocument dst, byte[] pdf) {
        if (pdf == null) return;
        try (PdfDocument src = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            src.copyPagesTo(1, src.getNumberOfPages(), dst);
        } catch (Exception e) {
            log.warn("copyIn failed: {}", e.getMessage());
        }
    }

    /** Wrap a Kannada string to {@code maxWidthPt} and render (Java2D-shaped) as one crisp image. */
    private Image knWrap(String text, float ptSize, float maxWidthPt) {
        try {
            int px = Math.round(ptSize * 4);
            float maxPx = maxWidthPt * 4;
            Font font = new Font("Nirmala UI", Font.PLAIN, px);
            FontRenderContext frc = new FontRenderContext(null, true, true);
            java.text.AttributedString as = new java.text.AttributedString(text);
            as.addAttribute(java.awt.font.TextAttribute.FONT, font);
            java.awt.font.LineBreakMeasurer lbm =
                    new java.awt.font.LineBreakMeasurer(as.getIterator(), frc);
            java.util.List<TextLayout> lines = new ArrayList<>();
            float total = 0, wMax = 0;
            while (lbm.getPosition() < text.length()) {
                TextLayout tl = lbm.nextLayout(maxPx);
                lines.add(tl);
                total += tl.getAscent() + tl.getDescent() + tl.getLeading();
                wMax = Math.max(wMax, tl.getAdvance());
            }
            int pad = Math.max(2, px / 12);
            int w = (int) Math.ceil(wMax) + pad * 2 + 4;
            int h = (int) Math.ceil(total) + pad * 2;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.BLACK);
            float y = pad;
            for (TextLayout tl : lines) {
                y += tl.getAscent();
                tl.draw(g, pad, y);
                y += tl.getDescent() + tl.getLeading();
            }
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            Image im = new Image(ImageDataFactory.create(out.toByteArray()));
            im.scaleToFit(maxWidthPt, h);   // fit full-width paragraphs to the target column width
            return im;
        } catch (Exception e) {
            log.warn("kannada wrap failed [{}]: {}", text, e.getMessage());
            return null;
        }
    }

    /** A titled, bordered section card with an accent header and label : value rows. */
    private Table sectionBox(PdfFont bold, PdfFont reg, DeviceRgb accent, String title, String[][] rows) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
        t.addCell(new Cell().add(new Paragraph(title).setFont(bold).setFontSize(14).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(accent).setPadding(8).setBorder(new SolidBorder(accent, 0.8f)));
        Cell body = new Cell().setPadding(12).setBorder(new SolidBorder(0.8f));
        for (String[] r : rows) {
            body.add(new Paragraph().add(new Text(r[0] + " : ").setFont(bold).setFontSize(12.5f))
                    .add(new Text(r[1]).setFont(reg).setFontSize(12.5f)).setMarginBottom(7));
        }
        t.addCell(body);
        return t;
    }

    /** Cover page — styled title block + two section cards, native English on the letterhead. */
    private byte[] coverPdf(String gp, String taluk, String district, String today) {
        DeviceRgb green = new DeviceRgb(14, 81, 50);
        return framed((doc, bold, reg, ital) -> {
            doc.add(new Paragraph("MULTI SKILLS YOUTH EMPOWERMENT PROGRAMME")
                    .setFont(bold).setFontSize(19).setFontColor(green)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(8).setMarginBottom(2));
            doc.add(new Paragraph("Kaushalya Patha  ·  KP-MSYEP")
                    .setFont(ital).setFontSize(14).setTextAlignment(TextAlignment.CENTER)
                    .setBorderBottom(new SolidBorder(green, 1.2f)).setPaddingBottom(8).setMarginBottom(46));

            doc.add(sectionBox(bold, reg, green, "Submitted By", new String[][]{
                    {"Name", "Kaushalya-Patha MSYEP"},
                    {"Department", "Rural Development & Panchayat Raj (RDPR)"},
                    {"Date of Submission", today},
            }));
            doc.add(new Paragraph().setMarginBottom(26));
            doc.add(sectionBox(bold, reg, green, "Submitted To", new String[][]{
                    {"Panchayat Development Officer", nz(gp) + "  Grama Panchayath (Students GP)"},
                    {"Taluk", nz(taluk)},
                    {"District", nz(district)},
            }));
        });
    }

    /** Addressee line: native value on the left, Kannada label on the right (borderless). */
    private Table addrRow(String value, String knLabel, PdfFont bold) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1.3f, 2.7f})).useAllAvailableWidth();
        t.addCell(new Cell().add(new Paragraph(value).setFont(bold).setFontSize(11)
                .setBorderBottom(new SolidBorder(0.6f))).setBorder(Border.NO_BORDER).setPadding(0));
        Cell l = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingLeft(4)
                .setVerticalAlignment(VerticalAlignment.BOTTOM);
        Image im = kn(knLabel, 11, null);
        if (im != null) l.add(im);
        t.addCell(l);
        return t;
    }

    /** Plain (light-grey) Kannada table header cell. */
    private Cell knHdrPlain(String kannada) {
        Image im = kn(kannada, 9.5f, null);
        Cell c = new Cell().setBackgroundColor(new DeviceRgb(230, 238, 233))
                .setBorder(new SolidBorder(0.7f)).setPadding(5)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (im != null) { im.setHorizontalAlignment(HorizontalAlignment.CENTER); c.add(im); }
        return c;
    }

    private Cell reqCell(String s, PdfFont f) {
        return new Cell().add(new Paragraph(nz(s)).setFont(f).setFontSize(10).setMargin(0))
                .setBorder(new SolidBorder(0.7f)).setPadding(5)
                .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    /**
     * GP requisition letter (blueprint p7) rebuilt: native English/data + Java2D-shaped Kannada, on
     * the letterhead; the student list auto-paginates. Kannada body is a best-effort transcription.
     */
    private byte[] requisitionPdf(String gp, String taluk, String district, String refNo,
                                  List<Student> students, int listTotal) {
        String subject = "ವಿಷಯ:- \"ಕೌಶಲ್ಯ ಪಥ\" MSYEP ತರಬೇತಿಗಳನ್ನು SCP/TSP ಯೋಜನೆ ತರಬೇತಿ ಕಾರ್ಯಕ್ರಮದ ಅಡಿಯಲ್ಲಿ ಪ.ಜಾತಿ/ಪ.ಪಂಗಡದ ವಿದ್ಯಾರ್ಥಿ ಫಲಾನುಭವಿಗಳಿಗೆ ಯುಕ್ತ ಕೌಶಲ್ಯ ತರಬೇತಿ ಕೇಂದ್ರದ ವತಿಯಿಂದ MSYEP ತರಬೇತಿಗಳು ಪೂರ್ಣಗೊಂಡಿದ್ದು ಅನುದಾನವನ್ನು ನೀಡುವ ಬಗ್ಗೆ.";
        String reference = "ಉಲ್ಲೇಖ:- 1. ಕರ್ನಾಟಕ ಸರ್ಕಾರ ಸುತ್ತೋಲೆ ಗ್ರಾ.ಅ.ಪಂ ಪತ್ರ ಸಂಖ್ಯೆ ಗ್ರಾ.ಅ.ಪಂ 2015.";
        String p1 = "ಮೇಲ್ಕಂಡ ವಿಷಯ ಹಾಗೂ ಉಲ್ಲೇಖ ಅನ್ವಯ SCP/TSP ಯೋಜನೆ ತರಬೇತಿ ಕಾರ್ಯಕ್ರಮ ಅಡಿಯಲ್ಲಿ ಪ.ಜಾತಿ/ಪ.ಪಂಗಡದ ವಿದ್ಯಾರ್ಥಿ ಫಲಾನುಭವಿಗಳಿಗೆ ವೃತ್ತಿಪರ ತರಬೇತಿಗಳನ್ನು ನೀಡಲು ಅವಕಾಶವಿದ್ದು ಹಾಗೂ ತಮ್ಮ ಗ್ರಾಮ ಪಂಚಾಯಿತಿಯ ಅನುಮೋದನೆ ಪತ್ರ ಮೇರೆಗೆ.";
        String p2 = "ವಿವಿಧ ಜಿಲ್ಲೆ/ತಾಲ್ಲೂಕುಗಳಲ್ಲಿ ಉನ್ನತ ಶಿಕ್ಷಣ ಹಾಗೂ ವಿದ್ಯಾಭ್ಯಾಸ ಮಾಡುತ್ತಿರುವ ವಿದ್ಯಾರ್ಥಿಗಳು MSYEP ತರಬೇತಿಗಳನ್ನು ಪಡೆಯಲು ಆನ್‌ಲೈನ್ ಮೂಲಕ ನೋಂದಣಿಯಾಗಿ, ತಮ್ಮ ಗ್ರಾಮ ಪಂಚಾಯಿತಿ ವತಿಯಿಂದ ತರಬೇತಿಗಳನ್ನು ಪಡೆಯಲು ಅನುಮೋದನೆ ಪಡೆದ ವಿದ್ಯಾರ್ಥಿ ಫಲಾನುಭವಿಗಳಿಗೆ ಯುಕ್ತ ಕೌಶಲ್ಯ ತರಬೇತಿ ಕೇಂದ್ರದ ವತಿಯಿಂದ MSYEP (Multi Skills Youth Empowerment Program) ವಿವಿಧ ಕೌಶಲ್ಯಗಳ ಯುವ ಸಬಲೀಕರಣ ಕಾರ್ಯಕ್ರಮ ಅಡಿಯಲ್ಲಿ (ಕಂಪ್ಯೂಟರ್, ಸ್ಪೋಕನ್ ಇಂಗ್ಲೀಷ್, ವೃತ್ತಿ ವಿಕಸನ, ಜೀವನ ಕೌಶಲ್ಯ, ವೃತ್ತಿಪರ ತರಬೇತಿಗಳನ್ನು) ಪ.ಜಾತಿ/ಪ.ಪಂಗಡದ ವಿದ್ಯಾರ್ಥಿ ಫಲಾನುಭವಿಗಳಿಗೆ ತರಬೇತಿಗಳು ಪೂರ್ಣಗೊಂಡಿದ್ದು ಅನುದಾನವನ್ನು ಬಿಡುಗಡೆ ಮಾಡಬೇಕೆಂದು ಈ ಮೂಲಕ ತಮ್ಮಲ್ಲಿ ಮನವಿ.";
        String p3 = "ಈ ಪತ್ರದೊಂದಿಗೆ ತಮ್ಮ ಗ್ರಾಮ ಪಂಚಾಯಿತಿಯ ಅನುಮೋದನೆ ಪತ್ರ ಹಾಗೂ ವಿದ್ಯಾರ್ಥಿ ಫಲಾನುಭವಿಗಳ ಎಲ್ಲಾ ದಾಖಲಾತಿಗಳನ್ನು ಲಗತ್ತಿಸಿದೆ.";
        return framed((doc, bold, reg, ital) -> {
            doc.add(new Paragraph(refNo).setFont(bold).setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(2));
            doc.add(knLine("ಇವರಿಗೆ,", 11));
            doc.add(knLine("ಅಧ್ಯಕ್ಷರು / ಪಂಚಾಯಿತಿ ಅಭಿವೃದ್ಧಿ ಅಧಿಕಾರಿಗಳು", 11));
            doc.add(addrRow(gp, "ಪಂಚಾಯಿತಿ", bold));
            doc.add(addrRow(nz(taluk), "ತಾಲ್ಲೂಕು", bold));
            doc.add(addrRow(nz(district), "ಜಿಲ್ಲೆ", bold));

            Image subj = knWrap(subject, 10, 485);
            if (subj != null) doc.add(new Div().add(subj).setMarginTop(4).setMarginBottom(2));
            Image refr = knWrap(reference, 10, 485);
            if (refr != null) doc.add(new Div().add(refr).setMarginBottom(3));
            for (String p : new String[]{p1, p2, p3}) {
                Image pi = knWrap(p, 10, 490);
                if (pi != null) doc.add(new Div().add(pi).setMarginBottom(2));
            }

            Table t = new Table(UnitValue.createPercentArray(new float[]{1, 3f, 2.4f, 1.3f, 1.6f}))
                    .useAllAvailableWidth().setMarginTop(3);
            t.addHeaderCell(knHdrPlain("ಕ್ರ. ಸಂಖ್ಯೆ"));
            t.addHeaderCell(knHdrPlain("ವಿದ್ಯಾರ್ಥಿಯ ಹೆಸರು"));
            t.addHeaderCell(knHdrPlain("ಒಬ್ಬ ವಿದ್ಯಾರ್ಥಿಗೆ ತಗಲುವ ವೆಚ್ಚ"));
            t.addHeaderCell(knHdrPlain("ಜಾತಿ"));
            t.addHeaderCell(knHdrPlain("ಒಟ್ಟು"));
            for (int i = 0; i < students.size(); i++) {
                Student stu = students.get(i);
                t.addCell(reqCell(String.valueOf(i + 1), reg));
                t.addCell(reqCell(nz(stu.getName()), reg));
                t.addCell(reqCell("1500", reg));
                t.addCell(reqCell(caste(stu), reg));
                if (i == 0) {
                    Cell tot = new Cell(students.size(), 1).add(new Paragraph(String.valueOf(listTotal))
                                    .setFont(bold).setFontSize(12).setMargin(0))
                            .setBorder(new SolidBorder(0.7f)).setPadding(5)
                            .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
                    t.addCell(tot);
                }
            }
            doc.add(t);

            Image inti = kn("ಇಂತಿ", 11, null);
            if (inti != null) { inti.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                doc.add(new Div().add(inti).setMarginTop(1)); }
            Image f1 = kn("ಜಿಲ್ಲಾ ಮುಖ್ಯ ಕಾರ್ಯಕ್ರಮ ಆಯೋಜಕರು", 11, null);
            Image f2 = kn("ಯುಕ್ತ ಕೌಶಲ್ಯ ತರಬೇತಿ ಕೇಂದ್ರ", 11, null);
            Div sig = new Div().setMarginTop(2);
            Image sgn = signature(100);
            if (sgn != null) sig.add(sgn);
            if (f1 != null) { f1.setHorizontalAlignment(HorizontalAlignment.RIGHT); sig.add(f1); }
            if (f2 != null) { f2.setHorizontalAlignment(HorizontalAlignment.RIGHT); sig.add(f2); }
            doc.add(sig);
        });
    }

    /** A single Kannada line as a left-aligned block. */
    private Div knLine(String text, float pt) {
        Div d = new Div();
        Image im = kn(text, pt, null);
        if (im != null) d.add(im);
        return d;
    }

    /** Continuation table (students 4..N) as a letterhead-framed, auto-paginating table. */
    private byte[] continuationPdf(List<Student> students, int listTotal) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
            PdfImageXObject letterhead = img("cba-letterhead.png");   // flat opaque letterhead — arc never composites over content
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
        byte[] adminSig = com.vincent.msyep.common.SignatureImage.clean(readAdminSignature());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
            PdfImageXObject letterhead = img("cba-letterhead.png");   // flat opaque letterhead — arc never composites over content
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
