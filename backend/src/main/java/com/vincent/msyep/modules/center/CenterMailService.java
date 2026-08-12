package com.vincent.msyep.modules.center;

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
 * Emails the Center Batch Approval PDF to a center. Auto-fires on center creation and from the
 * Center Mail page; every dispatch — with the actual PDF — is recorded in the shared history
 * (channel = CENTER). Stub-safe when SMTP isn't configured.
 */
@Service
public class CenterMailService {

    private static final Logger log = LoggerFactory.getLogger(CenterMailService.class);

    private final CenterBatchApprovalPdfService batchApproval;
    private final MailLogService mailLog;
    private final Optional<JavaMailSender> mailSender;
    private final String mailFrom;

    public CenterMailService(CenterBatchApprovalPdfService batchApproval,
                             MailLogService mailLog,
                             Optional<JavaMailSender> mailSender,
                             @Value("${spring.mail.username:}") String mailFrom) {
        this.batchApproval = batchApproval;
        this.mailLog = mailLog;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    /** Send the Batch Approval PDF to a single center (auto on create, or per-row send) and log it. */
    public String sendDocuments(Center center) {
        return sendToCenters(List.of(center), null, null)
                .getOrDefault(center.getId(), "NO_EMAIL");
    }

    /** Bulk send from the Center Mail page; records one history entry with the PDFs. */
    public Map<String, String> sendToCenters(List<Center> centers, String subject, String body) {
        Map<String, String> result = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<SendResult> results = new ArrayList<>();
        List<MailLogService.Att> files = new ArrayList<>();
        for (Center c : centers) {
            String who = firstNonBlank(c.getName(), c.getCode());
            names.add(who);
            ids.add(c.getId());
            String to = firstNonBlank(c.getContactEmail(), c.getEmail());
            if (!StringUtils.hasText(to)) { result.put(c.getId(), "NO_EMAIL"); results.add(new SendResult("", "NO_EMAIL", false)); continue; }
            byte[] pdf;
            try {
                pdf = batchApproval.build(c.getId());
            } catch (Exception e) {
                result.put(c.getId(), "FAILED:" + e.getMessage());
                results.add(new SendResult(to, "FAILED:" + e.getMessage(), false));
                continue;
            }
            String subj = StringUtils.hasText(subject) ? subject : "KP-MSYEP Center Batch Approval — " + who;
            String text = StringUtils.hasText(body) ? body
                    : "Dear " + who + ",\n\nPlease find attached your KP-MSYEP Center Batch Approval document.\n\nRegards,\nYKTK · KP-MSYEP";
            SendResult r = send(to, subj, text, pdf);
            result.put(c.getId(), r.token());
            results.add(r);
            files.add(new MailLogService.Att("Batch Approval — " + who, pdf));
        }
        saveLog(ids, names, results, subject, body, files);
        return result;
    }

    /** Sent-mail history for the Center wing. */
    public List<MailLog> history() {
        return mailLog.history("CENTER");
    }

    // ------------------------------------------------------------------ internals

    private record SendResult(String recipient, String token, boolean stub) {}

    private SendResult send(String to, String subject, String text, byte[] pdf) {
        if (mailSender.isEmpty() || !StringUtils.hasText(mailFrom)) {
            log.info("Mail not configured — center batch approval ({} bytes) ready to email to {}", pdf.length, to);
            return new SendResult(to, "SENT", true);
        }
        try {
            MimeMessage msg = mailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            helper.addAttachment("Batch-Approval.pdf", new ByteArrayResource(pdf));
            mailSender.get().send(msg);
            return new SendResult(to, "SENT", false);
        } catch (Exception e) {
            log.warn("Center email failed for {}: {}", to, e.getMessage());
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
                    .channel("CENTER")
                    .sentAt(Instant.now())
                    .recipients(new ArrayList<>(recipients))
                    .subject(StringUtils.hasText(subject) ? subject : "KP-MSYEP Center Batch Approval")
                    .body(body)
                    .studentIds(new ArrayList<>(ids))
                    .studentNames(new ArrayList<>(names))
                    .attachment("Batch-Approval.pdf")
                    .sent(sent).total(total)
                    .status((anyStub ? "stub — SMTP not configured · " : "") + sent + "/" + total + " sent")
                    .stub(anyStub)
                    .results(res)
                    .build();
            mailLog.save(logEntry, files);
        } catch (Exception e) {
            log.warn("center mail-log save failed: {}", e.getMessage());
        }
    }

    private static String firstNonBlank(String... vs) {
        for (String v : vs) if (StringUtils.hasText(v)) return v;
        return "";
    }
}
