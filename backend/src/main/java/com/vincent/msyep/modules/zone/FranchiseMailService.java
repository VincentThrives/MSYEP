package com.vincent.msyep.modules.zone;

import com.vincent.msyep.modules.finance.MailLog;
import com.vincent.msyep.modules.finance.MailLogService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Emails the franchise Certificate + MOU to a zone. Auto-fires on zone creation and from the Zone
 * Mail page; every dispatch — with the actual PDFs — is recorded in the shared history (channel = ZONE).
 * Stub-safe when SMTP isn't configured.
 */
@Service
public class FranchiseMailService {

    private static final Logger log = LoggerFactory.getLogger(FranchiseMailService.class);

    private final FranchisePdfService pdf;
    private final MailLogService mailLog;
    private final Optional<JavaMailSender> mailSender;
    private final String mailFrom;

    public FranchiseMailService(FranchisePdfService pdf,
                                MailLogService mailLog,
                                Optional<JavaMailSender> mailSender,
                                @Value("${spring.mail.username:}") String mailFrom) {
        this.pdf = pdf;
        this.mailLog = mailLog;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    /** Send certificate + MOU to a single zone (auto on create, or per-row send) and log it. */
    public String sendDocuments(Zone zone) {
        Map<String, String> r = sendToZones(List.of(zone), null, null);
        return r.getOrDefault(zone.getId(), "NO_EMAIL");
    }

    /** Bulk send from the Zone Mail page; records one history entry with the PDFs. */
    public Map<String, String> sendToZones(List<Zone> zones, String subject, String body) {
        Map<String, String> result = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<SendResult> results = new ArrayList<>();
        List<MailLogService.Att> files = new ArrayList<>();
        for (Zone z : zones) {
            String who = firstNonBlank(z.getFranchiseeName(), z.getOrganizationName(), z.getName());
            names.add(who);
            ids.add(z.getId());
            String to = firstNonBlank(z.getContactEmail(), z.getEmail());
            byte[] certificate = pdf.buildCertificate(z);
            byte[] mou = pdf.buildMou(z);
            if (!StringUtils.hasText(to)) { result.put(z.getId(), "NO_EMAIL"); results.add(new SendResult("", "NO_EMAIL", false)); continue; }
            String subj = StringUtils.hasText(subject) ? subject : "Your KP-MSYEP Franchise Certificate & MOU — " + who;
            String text = StringUtils.hasText(body) ? body
                    : "Dear " + who + ",\n\nCongratulations on joining the KP-MSYEP franchise network.\n"
                    + "Please find attached your Franchise Certificate and the signed MOU.\n\nRegards,\nYKTK · KP-MSYEP";
            SendResult r = send(to, subj, text, certificate, mou);
            result.put(z.getId(), r.token());
            results.add(r);
            files.add(new MailLogService.Att("Certificate — " + who, certificate));
            files.add(new MailLogService.Att("MOU — " + who, mou));
        }
        saveLog(ids, names, results, subject, body, files);
        return result;
    }

    /** Sent-mail history for the Zone wing. */
    public List<MailLog> history() {
        return mailLog.history("ZONE");
    }

    // ------------------------------------------------------------------ internals

    private record SendResult(String recipient, String token, boolean stub) {}

    private SendResult send(String to, String subject, String text, byte[] certificate, byte[] mou) {
        if (mailSender.isEmpty() || !StringUtils.hasText(mailFrom)) {
            log.info("Mail not configured — franchise certificate ({} bytes) + MOU ({} bytes) ready to email to {}",
                    certificate.length, mou.length, to);
            return new SendResult(to, "SENT", true);
        }
        try {
            MimeMessage msg = mailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            helper.addAttachment("Franchise-Certificate.pdf", new ByteArrayResource(certificate));
            helper.addAttachment("Franchise-MOU.pdf", new ByteArrayResource(mou));
            mailSender.get().send(msg);
            return new SendResult(to, "SENT", false);
        } catch (Exception e) {
            log.warn("Franchise email failed for {}: {}", to, e.getMessage());
            return new SendResult(to, "FAILED:" + e.getMessage(), false);
        }
    }

    private void saveLog(List<String> ids, List<String> names, List<SendResult> results,
                         String subject, String body, List<MailLogService.Att> files) {
        try {
            LinkedHashSet<String> recipients = new LinkedHashSet<>();
            Map<String, String> res = new LinkedHashMap<>();
            int sent = 0;
            boolean anyStub = false;
            for (int i = 0; i < ids.size(); i++) {
                SendResult r = results.get(i);
                if (StringUtils.hasText(r.recipient())) recipients.add(r.recipient());
                res.put(ids.get(i), r.token());
                if (r.token().startsWith("SENT")) sent++;
                if (r.stub()) anyStub = true;
            }
            int total = ids.size();
            MailLog logEntry = MailLog.builder()
                    .channel("ZONE")
                    .sentAt(Instant.now())
                    .recipients(new ArrayList<>(recipients))
                    .subject(StringUtils.hasText(subject) ? subject : "KP-MSYEP Franchise Certificate & MOU")
                    .body(body)
                    .studentIds(new ArrayList<>(ids))
                    .studentNames(new ArrayList<>(names))
                    .attachment("Franchise-Certificate.pdf, Franchise-MOU.pdf")
                    .sent(sent).total(total)
                    .status((anyStub ? "stub — SMTP not configured · " : "") + sent + "/" + total + " sent")
                    .stub(anyStub)
                    .results(res)
                    .build();
            mailLog.save(logEntry, files);
        } catch (Exception e) {
            log.warn("zone mail-log save failed: {}", e.getMessage());
        }
    }

    private static String firstNonBlank(String... vs) {
        for (String v : vs) if (StringUtils.hasText(v)) return v;
        return "";
    }
}
