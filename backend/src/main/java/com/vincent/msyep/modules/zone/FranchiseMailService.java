package com.vincent.msyep.modules.zone;

import com.vincent.msyep.modules.finance.MailLog;
import com.vincent.msyep.modules.finance.MailLogRepository;
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
 * Emails the two franchise documents — the certificate and the MOU — to the zone/franchise.
 * Auto-fires on zone creation and from the Zone Mail page; every dispatch is recorded in the
 * shared {@link MailLog} history (channel = ZONE). When SMTP isn't configured it logs the intent
 * (documents are still generated and downloadable) so the flow works end-to-end before credentials.
 */
@Service
public class FranchiseMailService {

    private static final Logger log = LoggerFactory.getLogger(FranchiseMailService.class);

    private final FranchisePdfService pdf;
    private final MailLogRepository mailLogs;
    private final Optional<JavaMailSender> mailSender;
    private final String mailFrom;

    public FranchiseMailService(FranchisePdfService pdf,
                                MailLogRepository mailLogs,
                                Optional<JavaMailSender> mailSender,
                                @Value("${spring.mail.username:}") String mailFrom) {
        this.pdf = pdf;
        this.mailLogs = mailLogs;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    /** Send certificate + MOU to a single zone (auto on create, or the per-row send) and log it. */
    public String sendDocuments(Zone zone) {
        SendResult r = sendOne(zone, null, null);
        logBatch(List.of(zone), List.of(r), null, null);
        return r.note();
    }

    /** Bulk send from the Zone Mail page; records one history entry for the batch. */
    public Map<String, String> sendToZones(List<Zone> zones, String subject, String body) {
        Map<String, String> result = new LinkedHashMap<>();
        List<SendResult> results = new ArrayList<>();
        for (Zone z : zones) {
            SendResult r = sendOne(z, subject, body);
            result.put(z.getId(), r.token());
            results.add(r);
        }
        logBatch(zones, results, subject, body);
        return result;
    }

    /** Sent-mail history for the Zone wing. */
    public List<MailLog> history() {
        return mailLogs.findTop100ByChannelOrderBySentAtDesc("ZONE");
    }

    // ------------------------------------------------------------------ internals

    private record SendResult(String recipient, String token, String note, boolean stub) {}

    private SendResult sendOne(Zone zone, String subject, String body) {
        byte[] certificate = pdf.buildCertificate(zone);
        byte[] mou = pdf.buildMou(zone);
        String to = firstNonBlank(zone.getContactEmail(), zone.getEmail());
        String who = firstNonBlank(zone.getFranchiseeName(), zone.getOrganizationName(), zone.getName());

        if (!StringUtils.hasText(to)) {
            return new SendResult("", "NO_EMAIL", "No franchise email on file — generated for download only.", false);
        }
        String subj = StringUtils.hasText(subject) ? subject : "Your KP-MSYEP Franchise Certificate & MOU — " + who;
        String text = StringUtils.hasText(body) ? body
                : "Dear " + who + ",\n\nCongratulations on joining the KP-MSYEP franchise network.\n"
                + "Please find attached your Franchise Certificate and the signed MOU.\n\nRegards,\nYKTK · KP-MSYEP";

        if (mailSender.isEmpty() || !StringUtils.hasText(mailFrom)) {
            log.info("Mail not configured — franchise certificate ({} bytes) + MOU ({} bytes) ready to email to {}",
                    certificate.length, mou.length, to);
            return new SendResult(to, "SENT", "Mail not configured — would email to " + to + ".", true);
        }
        try {
            MimeMessage msg = mailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subj);
            helper.setText(text);
            helper.addAttachment("Franchise-Certificate.pdf", new ByteArrayResource(certificate));
            helper.addAttachment("Franchise-MOU.pdf", new ByteArrayResource(mou));
            mailSender.get().send(msg);
            log.info("Franchise documents emailed to {}", to);
            return new SendResult(to, "SENT", "Certificate & MOU emailed to " + to + ".", false);
        } catch (Exception e) {
            log.warn("Franchise email failed for {}: {}", to, e.getMessage());
            return new SendResult(to, "FAILED:" + e.getMessage(), "Email failed: " + e.getMessage(), false);
        }
    }

    private void logBatch(List<Zone> zones, List<SendResult> results, String subject, String body) {
        try {
            LinkedHashSet<String> recipients = new LinkedHashSet<>();
            List<String> names = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            Map<String, String> res = new LinkedHashMap<>();
            int sent = 0;
            boolean anyStub = false;
            for (int i = 0; i < zones.size(); i++) {
                Zone z = zones.get(i);
                SendResult r = results.get(i);
                names.add(firstNonBlank(z.getFranchiseeName(), z.getOrganizationName(), z.getName()));
                ids.add(z.getId());
                if (StringUtils.hasText(r.recipient())) recipients.add(r.recipient());
                res.put(z.getId(), r.token());
                if (r.token().startsWith("SENT")) sent++;
                if (r.stub()) anyStub = true;
            }
            int total = zones.size();
            mailLogs.save(MailLog.builder()
                    .channel("ZONE")
                    .sentAt(Instant.now())
                    .recipients(new ArrayList<>(recipients))
                    .subject(StringUtils.hasText(subject) ? subject : "KP-MSYEP Franchise Certificate & MOU")
                    .body(body)
                    .studentIds(ids)
                    .studentNames(names)
                    .attachment("Franchise-Certificate.pdf, Franchise-MOU.pdf")
                    .sent(sent).total(total)
                    .status((anyStub ? "stub — SMTP not configured · " : "") + sent + "/" + total + " sent")
                    .stub(anyStub)
                    .results(res)
                    .build());
        } catch (Exception e) {
            log.warn("zone mail-log save failed: {}", e.getMessage());
        }
    }

    private static String firstNonBlank(String... vs) {
        for (String v : vs) if (StringUtils.hasText(v)) return v;
        return "";
    }
}
