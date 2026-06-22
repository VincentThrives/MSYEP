package com.vincent.msyep.modules.center;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.vincent.msyep.common.CounterService;
import com.vincent.msyep.common.IdGen;
import com.vincent.msyep.common.exception.ResourceNotFoundException;
import com.vincent.msyep.modules.center.dto.CenterRegistrationResult;
import com.vincent.msyep.modules.user.Role;
import com.vincent.msyep.modules.user.User;
import com.vincent.msyep.modules.user.UserRepository;
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
import java.time.LocalDate;
import java.util.Optional;

@Service
public class CenterRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(CenterRegistrationService.class);

    private final CenterRepository centers;
    private final UserRepository users;
    private final CounterService counters;
    private final Optional<JavaMailSender> mailSender;
    private final String mailFrom;

    public CenterRegistrationService(CenterRepository centers, UserRepository users,
                                     CounterService counters, Optional<JavaMailSender> mailSender,
                                     @Value("${spring.mail.username:}") String mailFrom) {
        this.centers = centers;
        this.users = users;
        this.counters = counters;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    /**
     * Register a new center: auto Center Code (CENTER-{year}-{NNNN}) and
     * Enrollment Number (CENENR{year}{NNNNNNN}), link the assigned head user,
     * then email the registration PDF to that head.
     */
    public CenterRegistrationResult register(Center input) {
        if (!StringUtils.hasText(input.getName())) {
            throw new IllegalArgumentException("Center name is required");
        }

        int year = LocalDate.now().getYear();
        long seq = counters.next("center");
        String code = String.format("CENTER-%d-%04d", year, seq);
        String enrollment = String.format("CENENR%d%07d", year, seq);

        input.setId(IdGen.cuid());
        input.setCode(code);
        input.setEnrollmentNumber(enrollment);
        input.setRegistrationDate(LocalDate.now().toString());
        if (input.getCreatedAt() == null) input.setCreatedAt(java.time.Instant.now());

        // Link the assigned center head (existing user) to this center.
        String headEmail = null;
        if (StringUtils.hasText(input.getCenterHeadUserId())) {
            User head = users.findById(input.getCenterHeadUserId()).orElse(null);
            if (head != null) {
                head.setRole(Role.CENTER);
                head.setCenterId(input.getId());
                if (input.getZoneId() != null) head.setZoneId(input.getZoneId());
                users.save(head);
                headEmail = head.getEmail();
                if (!StringUtils.hasText(input.getEmail())) input.setEmail(headEmail);
            }
        }

        Center saved = centers.save(input);

        byte[] pdf = buildPdf(saved, headEmail);
        boolean emailSent = false;
        String note;
        if (!StringUtils.hasText(headEmail)) {
            note = "No center head email — PDF available to download.";
        } else if (mailSender.isEmpty() || !StringUtils.hasText(mailFrom)) {
            note = "Mail not configured (set MAIL_USERNAME / MAIL_PASSWORD) — PDF available to download.";
        } else {
            try {
                sendEmail(headEmail, saved, pdf);
                emailSent = true;
                note = "Registration PDF emailed to " + headEmail;
            } catch (Exception ex) {
                note = "Email failed: " + ex.getMessage() + " — PDF available to download.";
                log.warn("Failed to email center registration to {}", headEmail, ex);
            }
        }

        return new CenterRegistrationResult(saved, code, enrollment, headEmail, emailSent, note);
    }

    public byte[] pdfFor(String id) {
        Center c = centers.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Center not found: " + id));
        return buildPdf(c, c.getEmail());
    }

    /** Render the Center Registration Account Details PDF. */
    public byte[] buildPdf(Center c, String loginId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            doc.add(new Paragraph("Center Registration Account Details")
                    .setBold().setFontSize(16).setMarginBottom(18));

            Table table = new Table(UnitValue.createPercentArray(new float[]{34, 66}))
                    .setWidth(UnitValue.createPercentValue(100));
            row(table, "Center Name:", nz(c.getName()));
            row(table, "Center Type:", nz(c.getCenterType()));
            row(table, "Center Code:", nz(c.getCode()));
            row(table, "Center Enrollment Number:", nz(c.getEnrollmentNumber()));
            row(table, "Center ID:", nz(c.getId()));
            row(table, "Registration Date:", nz(c.getRegistrationDate()));
            row(table, "Address:", nz(c.getAddress()));
            row(table, "Locality:", nz(c.getLocality()));
            row(table, "District:", nz(c.getDistrict()));
            row(table, "Taluk / Town:", nz(c.getTaluk()));
            row(table, "Village / Gram Panchayat:", nz(c.getGramPanchayat()));
            row(table, "Pincode:", nz(c.getPincode()));
            row(table, "Contact Number:", nz(c.getContactNumber()));
            row(table, "Email:", nz(c.getEmail()));
            row(table, "Login ID:", nz(loginId));
            row(table, "Date of MOU:", nz(c.getDateOfMou()));
            row(table, "Contract Duration:", nz(c.getContractDuration()));
            doc.add(table);

            doc.add(new Paragraph(
                    "\nThis is an auto-generated document. Please keep your login credentials secure.")
                    .setFontSize(10).setItalic().setMarginTop(20));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate registration PDF: " + e.getMessage());
        }
        return out.toByteArray();
    }

    private void sendEmail(String to, Center c, byte[] pdf) throws Exception {
        MimeMessage msg = mailSender.get().createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject("MSYEP — Center Registration: " + nz(c.getName()));
        helper.setText("Dear " + nz(c.getName()) + ",\n\n"
                + "Your center has been registered on MSYEP.\n\n"
                + "Center Code: " + nz(c.getCode()) + "\n"
                + "Center Enrollment Number: " + nz(c.getEnrollmentNumber()) + "\n\n"
                + "The full registration details are in the attached PDF.\n\nRegards,\nMSYEP");
        helper.addAttachment("Center-" + nz(c.getName()) + ".pdf", new ByteArrayResource(pdf));
        mailSender.get().send(msg);
    }

    private static void row(Table t, String label, String value) {
        t.addCell(new Cell().add(new Paragraph(label).setBold()).setBorder(Border.NO_BORDER));
        t.addCell(new Cell().add(new Paragraph(value)).setBorder(Border.NO_BORDER));
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}
