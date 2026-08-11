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
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;
    private final Optional<JavaMailSender> mailSender;
    private final String mailFrom;
    private final CenterMailService centerMail;

    public CenterRegistrationService(CenterRepository centers, UserRepository users,
                                     CounterService counters,
                                     org.springframework.security.crypto.password.PasswordEncoder encoder,
                                     Optional<JavaMailSender> mailSender,
                                     @Value("${spring.mail.username:}") String mailFrom,
                                     CenterMailService centerMail) {
        this.centers = centers;
        this.users = users;
        this.counters = counters;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        this.centerMail = centerMail;
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
        String batchCode = String.format("BATCH-%d-%04d", year, seq);

        input.setId(IdGen.cuid());
        input.setCode(code);
        input.setEnrollmentNumber(enrollment);
        input.setBatchCode(batchCode);
        input.setRegistrationDate(LocalDate.now().toString());
        if (!StringUtils.hasText(input.getBatchYear())) input.setBatchYear(input.getAcademicYear());
        if (input.getCreatedAt() == null) input.setCreatedAt(java.time.Instant.now());

        // Create the CENTER login from the typed User ID + Password.
        String loginId = null;
        if (StringUtils.hasText(input.getUserId()) && StringUtils.hasText(input.getPassword())) {
            String email = input.getUserId().toLowerCase().trim();
            if (users.existsByEmail(email)) {
                throw new IllegalArgumentException("User ID already in use: " + email);
            }
            users.save(User.builder()
                    .name(input.getName())
                    .email(email)
                    .passwordHash(encoder.encode(input.getPassword()))
                    .role(Role.CENTER)
                    .centerId(input.getId())
                    .zoneId(input.getZoneId())
                    .active(true)
                    .build());
            loginId = email;
        } else if (StringUtils.hasText(input.getCenterHeadUserId())) {
            // Legacy: assign an existing user as head.
            User head = users.findById(input.getCenterHeadUserId()).orElse(null);
            if (head != null) {
                head.setRole(Role.CENTER);
                head.setCenterId(input.getId());
                if (input.getZoneId() != null) head.setZoneId(input.getZoneId());
                users.save(head);
                loginId = head.getEmail();
            }
        }

        String rawPassword = input.getPassword(); // captured for the credentials email
        input.setPassword(null); // never persist the plaintext password
        Center saved = centers.save(input);

        // Deliver to the Center Mail-ID if given, else the login id.
        String deliveryEmail = StringUtils.hasText(saved.getEmail()) ? saved.getEmail() : loginId;

        byte[] pdf = buildPdf(saved, loginId);
        boolean emailSent = false;
        String note;
        if (!StringUtils.hasText(deliveryEmail)) {
            note = "No email on the center — PDF available to download.";
        } else if (mailSender.isEmpty() || !StringUtils.hasText(mailFrom)) {
            note = "Mail not configured (set MAIL_USERNAME / MAIL_PASSWORD) — PDF available to download.";
        } else {
            try {
                sendEmail(deliveryEmail, saved, pdf, loginId, rawPassword);
                emailSent = true;
                note = "Registration PDF + login credentials emailed to " + deliveryEmail
                        + (StringUtils.hasText(saved.getPrincipalNumber())
                            ? ". WhatsApp to principal " + saved.getPrincipalNumber()
                                + " is pending a WhatsApp gateway."
                            : "");
            } catch (Exception ex) {
                note = "Email failed: " + ex.getMessage() + " — PDF available to download.";
                log.warn("Failed to email center registration to {}", deliveryEmail, ex);
            }
        }

        // Auto-email the Center Batch Approval PDF to the center's own address (recorded in Center mail history).
        try {
            centerMail.sendDocuments(saved);
        } catch (Exception e) {
            log.warn("auto center-mail on create failed for {}: {}", saved.getId(), e.getMessage());
        }

        return new CenterRegistrationResult(saved, code, enrollment, batchCode, loginId, emailSent, note);
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
            row(table, "Academic Year:", nz(c.getAcademicYear()));
            row(table, "Center Code:", nz(c.getCode()));
            row(table, "Batch Code:", nz(c.getBatchCode()));
            row(table, "Center Enrollment Number:", nz(c.getEnrollmentNumber()));
            row(table, "Center ID:", nz(c.getId()));
            row(table, "Principal:", nz(c.getPrincipalName()) + "  " + nz(c.getPrincipalNumber()));
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

    private void sendEmail(String to, Center c, byte[] pdf, String loginId, String rawPassword) throws Exception {
        MimeMessage msg = mailSender.get().createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject("MSYEP — Center Registration & Login: " + nz(c.getName()));
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(nz(c.getName())).append(",\n\n")
                .append("Your center has been registered on MSYEP.\n\n")
                .append("Center Code: ").append(nz(c.getCode())).append("\n")
                .append("Center Enrollment Number: ").append(nz(c.getEnrollmentNumber())).append("\n");
        if (StringUtils.hasText(loginId)) {
            body.append("\n--- Login credentials (keep confidential) ---\n")
                    .append("User ID: ").append(loginId).append("\n")
                    .append("Password: ").append(nz(rawPassword)).append("\n")
                    .append("Sign in at the MSYEP portal (Staff tab).\n");
        }
        body.append("\nThe full registration details are in the attached PDF.\n\nRegards,\nMSYEP");
        helper.setText(body.toString());
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
