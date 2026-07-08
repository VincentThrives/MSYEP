package com.vincent.msyep.modules.sow;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.vincent.msyep.common.exception.ResourceNotFoundException;
import com.vincent.msyep.modules.center.Center;
import com.vincent.msyep.modules.center.CenterRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
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

    public record DownloadResult(byte[] pdf, boolean emailed, String note) {}

    private byte[] buildPdf(SowSubmission s, Center center) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {
            doc.add(new Paragraph("KP-MSYEP — Statement of Work")
                    .setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Program " + s.getProgramIndex()
                    + (center != null ? "  ·  " + nz(center.getName()) : ""))
                    .setFontSize(11).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(" ").setFontSize(4));

            Map<String, String> f = s.getFields() == null ? Map.of() : s.getFields();
            for (Map.Entry<String, String> e : TEXT_LABELS.entrySet()) {
                String v = f.get(e.getKey());
                if (StringUtils.hasText(v)) {
                    doc.add(new Paragraph()
                            .add(new com.itextpdf.layout.element.Text(e.getValue() + ": ").setBold())
                            .add(new com.itextpdf.layout.element.Text(v))
                            .setFontSize(10).setMarginBottom(2));
                }
            }

            Map<String, String> ph = s.getPhotos() == null ? Map.of() : s.getPhotos();
            boolean anyPhoto = ph.values().stream().anyMatch(StringUtils::hasText);
            if (anyPhoto) {
                doc.add(new Paragraph(" ").setFontSize(4));
                doc.add(new Paragraph("Photos").setBold().setFontSize(11));
                for (Map.Entry<String, String> e : PHOTO_LABELS.entrySet()) {
                    String data = ph.get(e.getKey());
                    byte[] img = decode(data);
                    if (img != null) {
                        try {
                            doc.add(new Paragraph(e.getValue()).setFontSize(9).setBold().setMarginBottom(1));
                            Image image = new Image(ImageDataFactory.create(img));
                            image.setAutoScale(false);
                            image.setWidth(120);
                            doc.add(image);
                        } catch (Exception ignored) { }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build SOW PDF: " + e.getMessage());
        }
        return out.toByteArray();
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
