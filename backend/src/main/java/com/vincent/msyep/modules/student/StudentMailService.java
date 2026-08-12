package com.vincent.msyep.modules.student;

import com.vincent.msyep.modules.entrance.EntranceAttempt;
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
 * Emails documents to students. Auto-fires the entrance result the moment a student submits, and
 * powers the Students Mail page (bulk send of their document packet). Every dispatch — with the
 * actual PDF — is recorded in the shared history (channel = STUDENT). Stub-safe before SMTP is set.
 */
@Service
public class StudentMailService {

    private static final Logger log = LoggerFactory.getLogger(StudentMailService.class);

    private final StudentRepository students;
    private final StudentExportService export;
    private final MailLogService mailLog;
    private final Optional<JavaMailSender> mailSender;
    private final String mailFrom;

    public StudentMailService(StudentRepository students, StudentExportService export,
                              MailLogService mailLog,
                              Optional<JavaMailSender> mailSender,
                              @Value("${spring.mail.username:}") String mailFrom) {
        this.students = students;
        this.export = export;
        this.mailLog = mailLog;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    /** Auto-email the entrance result PDF (already built) to the student, and record it. */
    public void sendEntranceResult(EntranceAttempt attempt, byte[] resultPdf) {
        Student s = students.findById(attempt.getStudentId()).orElse(null);
        String to = s == null ? "" : nz(s.getEmail());
        String who = StringUtils.hasText(attempt.getStudentName()) ? attempt.getStudentName()
                : (s == null ? "Student" : nz(s.getName()));
        String outcome = attempt.isPassed() ? "PASS" : "FAIL";
        String subject = "Your MSYEP Entrance Test Result — " + outcome + " (" + attempt.getScore() + "/" + attempt.getTotal() + ")";
        String body = "Dear " + who + ",\n\nYou have completed the MSYEP entrance test.\n"
                + "Result: " + outcome + " — " + attempt.getScore() + "/" + attempt.getTotal() + ".\n"
                + "Your detailed result sheet is attached.\n\nRegards,\nYKTK · KP-MSYEP";
        SendResult r = send(to, subject, body, resultPdf, "EntranceResult.pdf");
        List<MailLogService.Att> files = new ArrayList<>();
        if (resultPdf != null) files.add(new MailLogService.Att("Entrance Result — " + who, resultPdf));
        saveLog(List.of(attempt.getStudentId()), List.of(who), List.of(r), subject, body, "EntranceResult.pdf", files);
    }

    /** Bulk send each student's document packet from the Students Mail page; one history entry. */
    public Map<String, String> sendToStudents(List<String> studentIds, String subject, String body) {
        Map<String, String> result = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        List<SendResult> results = new ArrayList<>();
        List<MailLogService.Att> files = new ArrayList<>();
        for (String id : studentIds) {
            Student s = students.findById(id).orElse(null);
            String who = s == null ? "(unknown)" : nz(s.getName());
            names.add(who);
            if (s == null) { result.put(id, "NOT_FOUND"); results.add(new SendResult("", "NOT_FOUND", false)); continue; }
            byte[] pdf;
            try {
                pdf = export.documentsPdf(List.of(id), "All");
            } catch (Exception e) {
                result.put(id, "FAILED:" + e.getMessage());
                results.add(new SendResult(nz(s.getEmail()), "FAILED:" + e.getMessage(), false));
                continue;
            }
            String subj = StringUtils.hasText(subject) ? subject : "MSYEP — Your Documents";
            String text = StringUtils.hasText(body) ? body
                    : "Dear " + who + ",\n\nPlease find attached your MSYEP document packet.\n\nRegards,\nYKTK · KP-MSYEP";
            SendResult r = send(nz(s.getEmail()), subj, text, pdf, "Student-Documents.pdf");
            result.put(id, r.token());
            results.add(r);
            files.add(new MailLogService.Att("Documents — " + who, pdf));
        }
        saveLog(studentIds, names, results, subject, body, "Student-Documents.pdf", files);
        return result;
    }

    /** Sent-mail history for the Student wing. */
    public List<MailLog> history() {
        return mailLog.history("STUDENT");
    }

    // ------------------------------------------------------------------ internals

    private record SendResult(String recipient, String token, boolean stub) {}

    private SendResult send(String to, String subject, String text, byte[] attachment, String filename) {
        if (!StringUtils.hasText(to)) return new SendResult("", "NO_EMAIL", false);
        if (mailSender.isEmpty() || !StringUtils.hasText(mailFrom)) {
            log.info("Mail not configured — student document ({} bytes) ready to email to {}",
                    attachment == null ? 0 : attachment.length, to);
            return new SendResult(to, "SENT", true);
        }
        try {
            MimeMessage msg = mailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            if (attachment != null) helper.addAttachment(filename, new ByteArrayResource(attachment));
            mailSender.get().send(msg);
            return new SendResult(to, "SENT", false);
        } catch (Exception e) {
            log.warn("Student email failed for {}: {}", to, e.getMessage());
            return new SendResult(to, "FAILED:" + e.getMessage(), false);
        }
    }

    private void saveLog(List<String> ids, List<String> names, List<SendResult> results,
                         String subject, String body, String attachmentLabel, List<MailLogService.Att> files) {
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
                    .channel("STUDENT")
                    .sentAt(Instant.now())
                    .recipients(new ArrayList<>(recipients))
                    .subject(StringUtils.hasText(subject) ? subject : "MSYEP — Student mail")
                    .body(body)
                    .studentIds(new ArrayList<>(ids))
                    .studentNames(new ArrayList<>(names))
                    .attachment(attachmentLabel)
                    .sent(sent).total(total)
                    .status((anyStub ? "stub — SMTP not configured · " : "") + sent + "/" + total + " sent")
                    .stub(anyStub)
                    .results(res)
                    .build();
            mailLog.save(logEntry, files);
        } catch (Exception e) {
            log.warn("student mail-log save failed: {}", e.getMessage());
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
