package com.vincent.msyep.modules.center;

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
 * Emails the Center Batch Approval PDF to a center. Auto-fires on center creation and from the
 * Center Mail page; every dispatch is recorded in the shared {@link MailLog} history (channel = CENTER).
 * When SMTP isn't configured it logs the intent so the flow works end-to-end before credentials.
 */
@Service
public class CenterMailService {

    private static final Logger log = LoggerFactory.getLogger(CenterMailService.class);

    private final CenterBatchApprovalPdfService batchApproval;
    private final MailLogRepository mailLogs;
    private final Optional<JavaMailSender> mailSender;
    private final String mailFrom;

    public CenterMailService(CenterBatchApprovalPdfService batchApproval,
                             MailLogRepository mailLogs,
                             Optional<JavaMailSender> mailSender,
                             @Value("${spring.mail.username:}") String mailFrom) {
        this.batchApproval = batchApproval;
        this.mailLogs = mailLogs;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    /** Send the Batch Approval PDF to a single center (auto on create, or per-row send) and log it. */
    public String sendDocuments(Center center) {
        SendResult r = sendOne(center, null, null);
        logBatch(List.of(center), List.of(r), null, null);
        return r.note();
    }

    /** Bulk send from the Center Mail page; records one history entry for the batch. */
    public Map<String, String> sendToCenters(List<Center> centers, String subject, String body) {
        Map<String, String> result = new LinkedHashMap<>();
        List<SendResult> results = new ArrayList<>();
        for (Center c : centers) {
            SendResult r = sendOne(c, subject, body);
            result.put(c.getId(), r.token());
            results.add(r);
        }
        logBatch(centers, results, subject, body);
        return result;
    }

    /** Sent-mail history for the Center wing. */
    public List<MailLog> history() {
        return mailLogs.findTop100ByChannelOrderBySentAtDesc("CENTER");
    }

    // ------------------------------------------------------------------ internals

    private record SendResult(String recipient, String token, String note, boolean stub) {}

    private SendResult sendOne(Center center, String subject, String body) {
        String to = firstNonBlank(center.getContactEmail(), center.getEmail());
        String who = firstNonBlank(center.getName(), center.getCode());
        if (!StringUtils.hasText(to)) {
            return new SendResult("", "NO_EMAIL", "No center email on file — generated for download only.", false);
        }
        byte[] pdf;
        try {
            pdf = batchApproval.build(center.getId());
        } catch (Exception e) {
            log.warn("batch-approval build failed for {}: {}", center.getId(), e.getMessage());
            return new SendResult(to, "FAILED:" + e.getMessage(), "PDF build failed: " + e.getMessage(), false);
        }
        String subj = StringUtils.hasText(subject) ? subject : "KP-MSYEP Center Batch Approval — " + who;
        String text = StringUtils.hasText(body) ? body
                : "Dear " + who + ",\n\nPlease find attached your KP-MSYEP Center Batch Approval document.\n\nRegards,\nYKTK · KP-MSYEP";

        if (mailSender.isEmpty() || !StringUtils.hasText(mailFrom)) {
            log.info("Mail not configured — center batch approval ({} bytes) ready to email to {}", pdf.length, to);
            return new SendResult(to, "SENT", "Mail not configured — would email to " + to + ".", true);
        }
        try {
            MimeMessage msg = mailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subj);
            helper.setText(text);
            helper.addAttachment("Batch-Approval.pdf", new ByteArrayResource(pdf));
            mailSender.get().send(msg);
            log.info("Center batch approval emailed to {}", to);
            return new SendResult(to, "SENT", "Batch Approval emailed to " + to + ".", false);
        } catch (Exception e) {
            log.warn("Center email failed for {}: {}", to, e.getMessage());
            return new SendResult(to, "FAILED:" + e.getMessage(), "Email failed: " + e.getMessage(), false);
        }
    }

    private void logBatch(List<Center> centers, List<SendResult> results, String subject, String body) {
        try {
            LinkedHashSet<String> recipients = new LinkedHashSet<>();
            List<String> names = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            Map<String, String> res = new LinkedHashMap<>();
            int sent = 0;
            boolean anyStub = false;
            for (int i = 0; i < centers.size(); i++) {
                Center c = centers.get(i);
                SendResult r = results.get(i);
                names.add(firstNonBlank(c.getName(), c.getCode()));
                ids.add(c.getId());
                if (StringUtils.hasText(r.recipient())) recipients.add(r.recipient());
                res.put(c.getId(), r.token());
                if (r.token().startsWith("SENT")) sent++;
                if (r.stub()) anyStub = true;
            }
            int total = centers.size();
            mailLogs.save(MailLog.builder()
                    .channel("CENTER")
                    .sentAt(Instant.now())
                    .recipients(new ArrayList<>(recipients))
                    .subject(StringUtils.hasText(subject) ? subject : "KP-MSYEP Center Batch Approval")
                    .body(body)
                    .studentIds(ids)
                    .studentNames(names)
                    .attachment("Batch-Approval.pdf")
                    .sent(sent).total(total)
                    .status((anyStub ? "stub — SMTP not configured · " : "") + sent + "/" + total + " sent")
                    .stub(anyStub)
                    .results(res)
                    .build());
        } catch (Exception e) {
            log.warn("center mail-log save failed: {}", e.getMessage());
        }
    }

    private static String firstNonBlank(String... vs) {
        for (String v : vs) if (StringUtils.hasText(v)) return v;
        return "";
    }
}
