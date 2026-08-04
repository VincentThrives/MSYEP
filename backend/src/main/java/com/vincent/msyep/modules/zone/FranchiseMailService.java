package com.vincent.msyep.modules.zone;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Emails the two franchise documents — the certificate and the MOU — to the franchise on sign-up.
 * When SMTP isn't configured it logs the intent (documents are still generated and downloadable),
 * mirroring the SOW/WhatsApp behaviour so the flow works end-to-end before credentials are set.
 */
@Service
public class FranchiseMailService {

    private static final Logger log = LoggerFactory.getLogger(FranchiseMailService.class);

    private final FranchisePdfService pdf;
    private final Optional<JavaMailSender> mailSender;
    private final String mailFrom;

    public FranchiseMailService(FranchisePdfService pdf,
                                Optional<JavaMailSender> mailSender,
                                @Value("${spring.mail.username:}") String mailFrom) {
        this.pdf = pdf;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    /** Build both PDFs and email them to the franchise. Returns a human-readable outcome note. */
    public String sendDocuments(Zone zone) {
        byte[] certificate = pdf.buildCertificate(zone);
        byte[] mou = pdf.buildMou(zone);
        String to = firstNonBlank(zone.getContactEmail(), zone.getEmail());
        String who = firstNonBlank(zone.getFranchiseeName(), zone.getOrganizationName(), zone.getName());

        if (!StringUtils.hasText(to)) {
            return "No franchise email on file — certificate & MOU generated for download only.";
        }
        if (mailSender.isEmpty() || !StringUtils.hasText(mailFrom)) {
            log.info("Mail not configured — franchise certificate ({} bytes) + MOU ({} bytes) ready to email to {}",
                    certificate.length, mou.length, to);
            return "Mail not configured — documents generated (would email to " + to + ").";
        }
        try {
            MimeMessage msg = mailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject("Your KP-MSYEP Franchise Certificate & MOU — " + who);
            helper.setText("Dear " + who + ",\n\nCongratulations on joining the KP-MSYEP franchise network.\n"
                    + "Please find attached your Franchise Certificate and the signed MOU.\n\nRegards,\nYKTK · KP-MSYEP");
            helper.addAttachment("Franchise-Certificate.pdf", new ByteArrayResource(certificate));
            helper.addAttachment("Franchise-MOU.pdf", new ByteArrayResource(mou));
            mailSender.get().send(msg);
            log.info("Franchise documents emailed to {}", to);
            return "Certificate & MOU emailed to " + to + ".";
        } catch (Exception e) {
            log.warn("Franchise email failed for {}: {}", to, e.getMessage());
            return "Documents generated, but email failed: " + e.getMessage();
        }
    }

    private static String firstNonBlank(String... vs) {
        for (String v : vs) if (StringUtils.hasText(v)) return v;
        return "";
    }
}
