package com.vincent.msyep.modules.sow;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.vincent.msyep.common.exception.ResourceNotFoundException;
import com.vincent.msyep.modules.center.Center;
import com.vincent.msyep.modules.center.CenterRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** KP-MSYEP SOW: save, fetch, PDF-generate and email the 8 programme forms per center. */
@Service
public class SowService {

    private static final Logger log = LoggerFactory.getLogger(SowService.class);
    private static final String LETTERHEAD = "cba-letterhead.pdf";
    private static final float HEADER_H = 156f;
    private static final float FOOTER_H = 66f;
    // Content margins for the gallery page. The letterhead header ink ends ~133pt from the top, so the
    // heading sits just below it; side margins are tight so the cards use the full page width.
    private static final float MARGIN_TOP = 140f;
    private static final float MARGIN_SIDE = 28f;
    private static final float MARGIN_BOTTOM = FOOTER_H + 10f;

    /** Ordered text fields → labels for the PDF. */
    private static final Map<String, String> TEXT_LABELS = new LinkedHashMap<>();
    /** Ordered photo fields → labels for the PDF. */
    private static final Map<String, String> PHOTO_LABELS = new LinkedHashMap<>();
    static {
        TEXT_LABELS.put("programNumber", "Program");
        TEXT_LABELS.put("inaugurationDate", "Program Inauguration Date");
        TEXT_LABELS.put("guestName", "Guest Name");
        TEXT_LABELS.put("guestPhone", "Guest Phone Number");
        TEXT_LABELS.put("guestDesignation", "Guest Designation");
        TEXT_LABELS.put("groupsDate", "Groups Date");
        for (int i = 1; i <= 8; i++) TEXT_LABELS.put("group" + i, "Group " + i + " Name");
        TEXT_LABELS.put("letterGuestName", "Guest Name (Letter)");
        TEXT_LABELS.put("letterGuestPhone", "Guest Phone (Letter)");
        TEXT_LABELS.put("letterGuestDesignation", "Guest Designation (Letter)");
        TEXT_LABELS.put("anchoring", "Program Anchoring by");
        TEXT_LABELS.put("welcomeSpeech", "Welcome Speech by");
        TEXT_LABELS.put("preambleReading", "Indian Constitution Preamble Reading by");
        TEXT_LABELS.put("query1", "Query 1 by");
        TEXT_LABELS.put("query1Question", "Query 1 Question");
        TEXT_LABELS.put("query2", "Query 2 by");
        TEXT_LABELS.put("query2Question", "Query 2 Question");
        TEXT_LABELS.put("query3", "Query 3 by");
        TEXT_LABELS.put("query3Question", "Query 3 Question");
        TEXT_LABELS.put("voteOfThanks", "Vote of Thanks by");
        TEXT_LABELS.put("honouring", "Honouring by");
        TEXT_LABELS.put("guestOpinionBy", "Taking Guest Opinion by");
        TEXT_LABELS.put("guestOpinion", "Guest Opinion");

        PHOTO_LABELS.put("programLetterPhoto", "Program Letter Photo");
        PHOTO_LABELS.put("anchoring", "Program Anchoring");
        PHOTO_LABELS.put("welcomeSpeech", "Welcome Speech");
        PHOTO_LABELS.put("preambleReading", "Preamble Reading");
        PHOTO_LABELS.put("query1", "Query 1");
        PHOTO_LABELS.put("query2", "Query 2");
        PHOTO_LABELS.put("query3", "Query 3");
        PHOTO_LABELS.put("voteOfThanks", "Vote of Thanks");
        PHOTO_LABELS.put("honouring", "Honouring");
        PHOTO_LABELS.put("guestOpinionBy", "Taking Guest Opinion");
        PHOTO_LABELS.put("guestOpinion", "Guest Opinion");
    }

    private final SowSubmissionRepository repo;
    private final CenterRepository centers;
    private final Optional<JavaMailSender> mailSender;
    private final String mailFrom;

    public SowService(SowSubmissionRepository repo, CenterRepository centers,
                      Optional<JavaMailSender> mailSender,
                      @Value("${spring.mail.username:}") String mailFrom) {
        this.repo = repo;
        this.centers = centers;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    public List<SowSubmission> listForCenter(String centerId) {
        return repo.findByCenterId(centerId);
    }

    public SowSubmission get(String centerId, int programIndex) {
        return repo.findByCenterIdAndProgramIndex(centerId, programIndex).orElse(null);
    }

    public SowSubmission save(String centerId, int programIndex,
                              Map<String, String> fields, Map<String, String> photos) {
        SowSubmission s = repo.findByCenterIdAndProgramIndex(centerId, programIndex)
                .orElseGet(() -> SowSubmission.builder()
                        .centerId(centerId).programIndex(programIndex)
                        .createdAt(Instant.now()).build());
        if (fields != null) s.setFields(fields);
        if (photos != null) s.setPhotos(photos);
        s.setUpdatedAt(Instant.now());
        return repo.save(s);
    }

    /** Build the one-page PDF, email it to the center's mail-id, and return the bytes. */
    public DownloadResult downloadAndEmail(String centerId, int programIndex) {
        SowSubmission s = repo.findByCenterIdAndProgramIndex(centerId, programIndex)
                .orElseThrow(() -> new ResourceNotFoundException("No SOW saved for program " + programIndex));
        Center center = centers.findById(centerId).orElse(null);
        byte[] pdf = buildPdf(s, center);

        String email = center == null ? null : firstNonBlank(center.getEmail(), center.getContactEmail());
        boolean emailed = false;
        String note;
        if (!StringUtils.hasText(email)) {
            note = "No college mail-id on the center — PDF downloaded only.";
        } else if (mailSender.isEmpty() || !StringUtils.hasText(mailFrom)) {
            note = "Mail not configured — PDF downloaded (would email " + email + ").";
        } else {
            try {
                MimeMessage msg = mailSender.get().createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true);
                helper.setFrom(mailFrom);
                helper.setTo(email);
                helper.setSubject("KP-MSYEP SOW — Program " + programIndex
                        + (center != null ? " — " + nz(center.getName()) : ""));
                helper.setText("Please find attached the KP-MSYEP SOW form for Program " + programIndex + ".\n\nRegards,\nMSYEP");
                helper.addAttachment("SOW-Program-" + programIndex + ".pdf", new ByteArrayResource(pdf));
                mailSender.get().send(msg);
                emailed = true;
                note = "PDF emailed to " + email;
            } catch (Exception ex) {
                note = "Email failed: " + ex.getMessage() + " — PDF downloaded.";
                log.warn("SOW email failed for center {}", centerId, ex);
            }
        }
        return new DownloadResult(pdf, emailed, note);
    }

    /** The inauguration date (yyyy-MM-dd) of a given program for a center, or null if not saved. */
    public String programInaugurationDate(String centerId, int programIndex) {
        return repo.findByCenterIdAndProgramIndex(centerId, programIndex)
                .map(s -> s.getFields() != null ? s.getFields().get("inaugurationDate") : null)
                .orElse(null);
    }

    /**
     * Merge every saved SOW program for a center into a single PDF (each program already on the
     * letterhead, with its fields + photos). Returns null if the center has no saved programs.
     */
    public byte[] allProgramsPdf(String centerId) {
        Center center = centers.findById(centerId).orElse(null);
        List<SowSubmission> list = repo.findByCenterId(centerId);
        list.sort(java.util.Comparator.comparingInt(SowSubmission::getProgramIndex));
        if (list.isEmpty()) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out))) {
            applyLetterhead(pdf);
            try (Document doc = new Document(pdf)) {
                doc.setFont(PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN));
                doc.setMargins(HEADER_H + 12, 40, FOOTER_H + 12, 40);
                doc.add(new Paragraph("KP-MSYEP — Statement of Work  ·  Program Photos")
                        .setBold().setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
                if (center != null) doc.add(new Paragraph(nz(center.getName()))
                        .setFontSize(11).setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));
                // 4 programs per page: a 2-column grid of fixed-height cells auto-paginates 2 rows/page.
                Table grid = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
                for (SowSubmission s : list) grid.addCell(programCell(s));
                if (list.size() % 2 == 1) grid.addCell(new Cell().setBorder(Border.NO_BORDER).setHeight(250));
                doc.add(grid);
            }
        } catch (Exception e) {
            log.warn("SOW all-programs compact failed: {}", e.getMessage());
            return null;
        }
        return out.toByteArray();
    }

    /** One program as a compact bordered cell: title + a small grid of its photos (4 programs fit a page). */
    private Cell programCell(SowSubmission s) {
        Cell cell = new Cell().setBorder(new SolidBorder(0.8f)).setPadding(6).setHeight(250);
        cell.add(new Paragraph("Program " + s.getProgramIndex())
                .setBold().setFontSize(11).setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
        Map<String, String> ph = s.getPhotos() == null ? Map.of() : s.getPhotos();
        Table pg = new Table(2).useAllAvailableWidth();
        int shown = 0;
        for (Map.Entry<String, String> e : PHOTO_LABELS.entrySet()) {
            if (shown >= 6) break;   // keep each cell compact so 4 programs fit one page
            byte[] img = decode(ph.get(e.getKey()));
            if (img == null) continue;
            Cell pc = new Cell().setBorder(new SolidBorder(0.4f)).setPadding(2);
            pc.add(new Paragraph(e.getValue()).setFontSize(6f).setMarginBottom(1));
            try { Image im = new Image(ImageDataFactory.create(img)); im.setAutoScale(true); pc.add(im); }
            catch (Exception ex) { continue; }
            pg.addCell(pc);
            shown++;
        }
        if (shown == 0) cell.add(new Paragraph("(no photos uploaded)").setFontSize(8).setItalic());
        else { if (shown % 2 == 1) pg.addCell(new Cell().setBorder(Border.NO_BORDER)); cell.add(pg); }
        return cell;
    }

    /** Every saved SOW program merged into ONE PDF — one program per page, with the full filled
     *  details and photos (same layout as the single-program download). */
    public byte[] allProgramsFullPdf(String centerId) {
        Center center = centers.findById(centerId).orElse(null);
        List<SowSubmission> list = repo.findByCenterId(centerId);
        list.sort(java.util.Comparator.comparingInt(SowSubmission::getProgramIndex));
        if (list.isEmpty())
            throw new ResourceNotFoundException("No saved SOW programs to download for this center");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int added = 0;
        try (PdfDocument dst = new PdfDocument(new PdfWriter(out))) {
            for (SowSubmission s : list) {
                try {
                    byte[] one = buildPdf(s, center);
                    try (PdfDocument src = new PdfDocument(new PdfReader(new java.io.ByteArrayInputStream(one)))) {
                        src.copyPagesTo(1, src.getNumberOfPages(), dst);
                    }
                    added++;
                } catch (Exception ex) {
                    log.warn("SOW full-merge: program {} skipped: {}", s.getProgramIndex(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to merge SOW programs: " + e.getMessage());
        }
        if (added == 0)
            throw new ResourceNotFoundException("No saved SOW programs to download for this center");
        return out.toByteArray();
    }

    /** Build every saved SOW program for the center into a single ZIP. */
    public byte[] downloadAllZip(String centerId) {
        Center center = centers.findById(centerId).orElse(null);
        List<SowSubmission> list = repo.findByCenterId(centerId);
        list.sort(java.util.Comparator.comparingInt(SowSubmission::getProgramIndex));
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int added = 0;
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
            for (SowSubmission s : list) {
                try {
                    byte[] pdf = buildPdf(s, center);
                    zip.putNextEntry(new java.util.zip.ZipEntry("SOW-Program-" + s.getProgramIndex() + ".pdf"));
                    zip.write(pdf);
                    zip.closeEntry();
                    added++;
                } catch (Exception ex) {
                    log.warn("SOW zip: program {} skipped: {}", s.getProgramIndex(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build SOW zip: " + e.getMessage());
        }
        if (added == 0) throw new ResourceNotFoundException("No saved SOW programs to download for this center");
        return out.toByteArray();
    }

    public record DownloadResult(byte[] pdf, boolean emailed, String note) {}

    private byte[] buildPdf(SowSubmission s, Center center) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer)) {

            // Print the SOW on the YKTK letterhead (header + grey arc + footer) on every page.
            applyLetterhead(pdf);

            try (Document doc = new Document(pdf)) {
                doc.setFont(PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN));
                doc.setMargins(MARGIN_TOP, MARGIN_SIDE, MARGIN_BOTTOM, MARGIN_SIDE);

                // Solid green title banner.
                com.itextpdf.layout.element.Div banner = new com.itextpdf.layout.element.Div()
                        .setBackgroundColor(GREEN)
                        .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(UnitValue.createPointValue(6)))
                        .setPaddingTop(7).setPaddingBottom(7).setMarginBottom(9);
                banner.add(new Paragraph("KP-MSYEP  ·  Statement of Work")
                        .setBold().setFontSize(15).setFontColor(WHITE)
                        .setTextAlignment(TextAlignment.CENTER).setMargin(0).setMultipliedLeading(1f));
                banner.add(new Paragraph("Program " + s.getProgramIndex()
                        + (center != null ? "   ·   " + nz(center.getName()) : ""))
                        .setFontSize(10.5f).setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(224, 240, 230))
                        .setTextAlignment(TextAlignment.CENTER).setMargin(0).setMarginTop(2).setMultipliedLeading(1f));
                doc.add(banner);

                // ---- One-page photo gallery: every uploaded photo as a captioned card ----
                Map<String, String> f = s.getFields() == null ? Map.of() : s.getFields();
                Map<String, String> ph = s.getPhotos() == null ? Map.of() : s.getPhotos();
                addPhotoGallery(doc, pdf, f, ph);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build SOW PDF: " + e.getMessage());
        }
        return out.toByteArray();
    }

    /** Draw the full YKTK letterhead page as the background of every page. */
    private void applyLetterhead(PdfDocument pdf) {
        try (InputStream in = new ClassPathResource("cba-letterhead.png").getInputStream()) {
            // Flat, opaque raster of the letterhead — draws as a plain background so its grey arc never
            // composites OVER the page content (photos). Page content is drawn on top of it. Built once
            // and reused on every page so the file doesn't balloon.
            com.itextpdf.kernel.pdf.xobject.PdfImageXObject letterhead =
                    new com.itextpdf.kernel.pdf.xobject.PdfImageXObject(ImageDataFactory.create(in.readAllBytes()));
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, ev -> {
                PdfPage page = ((PdfDocumentEvent) ev).getPage();
                Rectangle p = page.getPageSize();
                new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf)
                        .addXObjectFittedIntoRectangle(letterhead, new Rectangle(0, 0, p.getWidth(), p.getHeight()));
            });
        } catch (Exception e) {
            log.warn("SOW letterhead missing: {}", e.getMessage());
        }
    }

    private static final com.itextpdf.kernel.colors.DeviceRgb GREEN = new com.itextpdf.kernel.colors.DeviceRgb(31, 122, 74);
    private static final com.itextpdf.kernel.colors.DeviceRgb GREY = new com.itextpdf.kernel.colors.DeviceRgb(95, 95, 95);
    private static final com.itextpdf.kernel.colors.DeviceRgb DARK = new com.itextpdf.kernel.colors.DeviceRgb(55, 60, 58);
    private static final com.itextpdf.kernel.colors.DeviceRgb WHITE = new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255);
    /** Vibrant but tasteful accents cycled across the photo cards for a colourful, magazine look. */
    private static final com.itextpdf.kernel.colors.DeviceRgb[] PALETTE = {
            new com.itextpdf.kernel.colors.DeviceRgb(14, 124, 123),   // teal
            new com.itextpdf.kernel.colors.DeviceRgb(63, 81, 181),    // indigo
            new com.itextpdf.kernel.colors.DeviceRgb(231, 111, 81),   // coral
            new com.itextpdf.kernel.colors.DeviceRgb(46, 125, 79),    // green
            new com.itextpdf.kernel.colors.DeviceRgb(217, 154, 51),   // amber
            new com.itextpdf.kernel.colors.DeviceRgb(123, 76, 160),   // purple
            new com.itextpdf.kernel.colors.DeviceRgb(194, 69, 110),   // rose
            new com.itextpdf.kernel.colors.DeviceRgb(38, 132, 176),   // blue
    };

    /**
     * Lay out all of a program's photos on a single page: the Program Letter Photo as a wide hero, then
     * every other uploaded photo as a captioned card in an adaptive grid. Each caption shows the activity
     * name and who performed it. Sizes are computed from the photo count so everything fits one page.
     */
    private void addPhotoGallery(Document doc, PdfDocument pdf, Map<String, String> f, Map<String, String> ph) {
        float pageW = pdf.getDefaultPageSize().getWidth();
        float pageH = pdf.getDefaultPageSize().getHeight();
        float usableW = pageW - MARGIN_SIDE - MARGIN_SIDE;
        float usableH = pageH - MARGIN_TOP - MARGIN_BOTTOM;
        float used = 52f;   // the title block already added above

        // Hero — the Program Letter Photo, wide across the top.
        byte[] hero = decode(ph.get("programLetterPhoto"));
        float heroH = 0;
        if (hero != null) {
            String guest = firstNonBlank(f.get("guestName"), f.get("letterGuestName"), f.get("guestDesignation"));
            String date = firstNonBlank(f.get("inaugurationDate"), f.get("groupsDate"));
            String sub = "";
            if (StringUtils.hasText(guest)) sub += "Chief Guest: " + guest;
            if (StringUtils.hasText(date)) sub += (sub.isEmpty() ? "" : "   ·   ") + date;
            heroH = 150;
            Table heroT = new Table(1).useAllAvailableWidth().setMarginBottom(8);
            heroT.addCell(photoCard(hero, PHOTO_LABELS.get("programLetterPhoto"), sub, usableW - 24, 108, heroH - 8, GREEN));
            doc.add(heroT);
        }

        // Every other uploaded photo, in the defined order, with its caption.
        List<String[]> cards = new java.util.ArrayList<>();   // {key, title, subtitle}
        for (Map.Entry<String, String> e : PHOTO_LABELS.entrySet()) {
            String key = e.getKey();
            if (key.equals("programLetterPhoto")) continue;
            if (decode(ph.get(key)) == null) continue;
            cards.add(new String[]{key, e.getValue(), performedBy(key, f)});
        }
        int count = cards.size();
        if (count == 0) {
            if (hero == null) doc.add(new Paragraph("No photos uploaded for this program yet.")
                    .setFontSize(11).setFontColor(GREY).setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
            return;
        }

        int cols = count <= 4 ? 2 : 3;
        int rows = (int) Math.ceil(count / (double) cols);
        // Fit the grid into the remaining band with a firm safety factor so it never spills to a 2nd page.
        float gridH = (usableH - used - heroH) * 0.82f;
        float rowH = Math.min(gridH / rows, 190f);
        float colW = usableW / cols;
        float photoBoxW = colW - 20;
        float photoBoxH = rowH - 36;   // room for the coloured header bar + performer line

        Table grid = new Table(cols).useAllAvailableWidth();
        grid.setBorderCollapse(com.itextpdf.layout.properties.BorderCollapsePropertyValue.SEPARATE);
        grid.setHorizontalBorderSpacing(6).setVerticalBorderSpacing(6);
        int idx = 0;
        for (String[] c : cards) {
            com.itextpdf.kernel.colors.DeviceRgb accent = PALETTE[idx++ % PALETTE.length];
            grid.addCell(photoCard(decode(ph.get(c[0])), c[1], c[2], photoBoxW, photoBoxH, rowH, accent));
        }
        for (int i = count; i < rows * cols; i++) grid.addCell(new Cell().setBorder(Border.NO_BORDER));
        doc.add(grid);
    }

    /** Short "by <person>" (or a trimmed opinion) for a photo caption — kept to one line for the grid. */
    private String performedBy(String key, Map<String, String> f) {
        if (key.equals("guestOpinion")) return ellipsis(nz(f.get("guestOpinion")), 42);
        String person = f.get(key);
        return StringUtils.hasText(person) ? ellipsis("by " + person, 34) : "";
    }

    private static String ellipsis(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1).trim() + "…" : s;
    }

    /**
     * One photo card: a solid coloured header bar with the activity name (white), the whole photo fitted
     * below it, and the performer underneath — framed by a matching coloured border with rounded corners.
     */
    private Cell photoCard(byte[] img, String title, String subtitle, float boxW, float boxH, float cellH,
                           com.itextpdf.kernel.colors.DeviceRgb accent) {
        Cell c = new Cell().setPadding(0).setHeight(cellH)
                .setBorder(new SolidBorder(accent, 1.3f))
                .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(UnitValue.createPointValue(5)))
                .setBackgroundColor(WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.TOP);
        // Solid coloured header bar.
        c.add(new Paragraph(nz(title)).setBold().setFontSize(8f).setFontColor(WHITE).setBackgroundColor(accent)
                .setTextAlignment(TextAlignment.CENTER).setPaddingTop(2.5f).setPaddingBottom(2.5f)
                .setMargin(0).setMultipliedLeading(1f));
        // Photo, inset and centred.
        try {
            Image im = new Image(ImageDataFactory.create(img));
            im.scaleToFit(boxW, boxH);
            im.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            c.add(new com.itextpdf.layout.element.Div().setMarginTop(4).setMarginBottom(2)
                    .setTextAlignment(TextAlignment.CENTER).add(im));
        } catch (Exception ignore) { /* skip an unreadable image */ }
        // Performer / caption.
        if (StringUtils.hasText(subtitle))
            c.add(new Paragraph(subtitle).setBold().setFontSize(7.5f).setFontColor(DARK)
                    .setMargin(0).setMarginBottom(4).setMultipliedLeading(1f));
        return c;
    }

    private static byte[] decode(String dataUrl) {
        if (!StringUtils.hasText(dataUrl)) return null;
        try {
            String b64 = dataUrl.contains(",") ? dataUrl.substring(dataUrl.indexOf(',') + 1) : dataUrl;
            return Base64.getDecoder().decode(b64);
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (StringUtils.hasText(v)) return v;
        return null;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
