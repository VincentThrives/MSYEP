package com.vincent.msyep.modules.finance;

import com.vincent.msyep.common.exception.ResourceNotFoundException;
import com.vincent.msyep.modules.center.Center;
import com.vincent.msyep.modules.center.CenterRepository;
import com.vincent.msyep.modules.finance.dto.FinanceRow;
import com.vincent.msyep.modules.finance.dto.SendMailRequest;
import com.vincent.msyep.modules.student.Student;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FinanceService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FinanceService.class);

    private final MongoTemplate mongo;
    private final CenterRepository centerRepo;
    private final GramPanchayatRepository gpRepo;
    private final GpBlueprintPdfService gpBlueprint;
    private final MailLogService mailLog;
    private final Optional<JavaMailSender> mailSender;
    private final String mailUsername;

    public FinanceService(MongoTemplate mongo, CenterRepository centerRepo,
                          GramPanchayatRepository gpRepo, GpBlueprintPdfService gpBlueprint,
                          MailLogService mailLog,
                          Optional<JavaMailSender> mailSender,
                          @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}") String mailUsername) {
        this.mongo = mongo;
        this.centerRepo = centerRepo;
        this.gpRepo = gpRepo;
        this.gpBlueprint = gpBlueprint;
        this.mailLog = mailLog;
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
    }

    /** Sent-mail history (most recent first) for the Finance wing. */
    public List<MailLog> mailHistory() {
        return mailLog.history("FINANCE");
    }

    /** SMTP is "configured" only when a JavaMailSender bean exists AND a username is set. */
    private boolean mailConfigured() {
        return mailSender.isPresent() && StringUtils.hasText(mailUsername);
    }

    /** Filter students by any combination of district / taluk / gram panchayat / center. */
    public List<FinanceRow> fetch(String district, String taluk, String gramPanchayat, String centerId) {
        Query q = new Query();
        if (StringUtils.hasText(district)) q.addCriteria(Criteria.where("district").is(district));
        if (StringUtils.hasText(taluk)) q.addCriteria(Criteria.where("taluk").is(taluk));
        if (StringUtils.hasText(gramPanchayat)) q.addCriteria(Criteria.where("gramPanchayat").is(gramPanchayat));
        if (StringUtils.hasText(centerId)) q.addCriteria(Criteria.where("centerId").is(centerId));

        List<Student> students = mongo.find(q, Student.class);

        // Pre-load center names and GP emails referenced by the result set.
        Map<String, String> centerNames = new HashMap<>();
        Map<String, String> gpEmails = new HashMap<>();

        List<FinanceRow> rows = new ArrayList<>();
        int i = 1;
        for (Student s : students) {
            String centerName = s.getCenterId() == null ? null :
                    centerNames.computeIfAbsent(s.getCenterId(),
                            id -> centerRepo.findById(id).map(Center::getName).orElse(null));
            String gpEmail = s.getGramPanchayat() == null ? null :
                    gpEmails.computeIfAbsent(s.getGramPanchayat(), this::resolveGpEmail);
            rows.add(new FinanceRow(
                    i++, s.getId(), s.getName(), s.getDistrict(), s.getTaluk(),
                    s.getGramPanchayat(), s.getCenterId(), centerName, gpEmail,
                    s.getDocuments() == null ? 0 : s.getDocuments().size()));
        }
        return rows;
    }

    /** Auto-resolve the Gram Panchayat email from the mapping table. */
    public String resolveGpEmail(String gramPanchayatName) {
        if (!StringUtils.hasText(gramPanchayatName)) return null;
        return gpRepo.findByNameIgnoreCase(gramPanchayatName)
                .map(GramPanchayat::getEmail)
                .orElse(null);
    }

    public List<String> distinctDistricts() { return distinct("district"); }
    public List<String> distinctTaluks()    { return distinct("taluk"); }
    public List<String> distinctGramPanchayats() { return distinct("gramPanchayat"); }

    private List<String> distinct(String field) {
        return mongo.findDistinct(new Query(), field, Student.class, String.class)
                .stream().filter(StringUtils::hasText).sorted().toList();
    }

    /**
     * Send the selected students' FW documents to their Gram Panchayat email.
     * Returns a per-student status map. The GP email is auto-resolved unless overridden.
     */
    public Map<String, String> sendDocuments(SendMailRequest req) {
        Map<String, String> result = new HashMap<>();
        // Recipient mail IDs explicitly picked in the finance wing (multi-select) win over
        // the per-student auto-resolved GP email.
        List<String> picked = req.recipientEmails() == null ? List.of()
                : req.recipientEmails().stream().filter(StringUtils::hasText).distinct().toList();
        // Build the GP Blue Print packet once and attach it to every GP mail.
        byte[] blueprint = null;
        if (StringUtils.hasText(req.gramPanchayat())) {
            try {
                blueprint = gpBlueprint.build(req.gramPanchayat(), req.taluk(), req.district());
            } catch (Exception ex) {
                log.warn("GP blueprint build failed for {}: {}", req.gramPanchayat(), ex.getMessage());
            }
        }
        java.util.LinkedHashSet<String> allRecipients = new java.util.LinkedHashSet<>();
        List<String> names = new java.util.ArrayList<>();
        int sent = 0;
        for (String studentId : req.studentIds()) {
            Student s = mongo.findById(studentId, Student.class);
            if (s == null) {
                result.put(studentId, "NOT_FOUND");
                names.add("(unknown)");
                continue;
            }
            names.add(s.getName() == null ? "" : s.getName());
            List<String> recipients = !picked.isEmpty() ? picked
                    : StringUtils.hasText(req.overrideEmail()) ? List.of(req.overrideEmail())
                    : (StringUtils.hasText(resolveGpEmail(s.getGramPanchayat()))
                        ? List.of(resolveGpEmail(s.getGramPanchayat())) : List.of());
            if (recipients.isEmpty()) {
                result.put(studentId, "NO_GP_EMAIL");
                continue;
            }
            try {
                send(recipients, s, req, blueprint);
                result.put(studentId, "SENT:" + String.join(", ", recipients));
                allRecipients.addAll(recipients);
                sent++;
            } catch (Exception ex) {
                result.put(studentId, "FAILED:" + ex.getMessage());
            }
        }
        // Record this dispatch in the sent-mail history (best-effort — never breaks the send).
        try {
            boolean stub = !mailConfigured();
            int total = req.studentIds().size();
            String gpLabel = req.gramPanchayat() == null ? "" : req.gramPanchayat();
            MailLog logEntry = MailLog.builder()
                    .channel("FINANCE")
                    .sentAt(java.time.Instant.now())
                    .recipients(new java.util.ArrayList<>(allRecipients))
                    .subject(StringUtils.hasText(req.subject()) ? req.subject() : "MSYEP — Student FW Documents")
                    .body(req.body())
                    .gramPanchayat(req.gramPanchayat()).taluk(req.taluk()).district(req.district())
                    .studentIds(new java.util.ArrayList<>(req.studentIds()))
                    .studentNames(names)
                    .attachment(blueprint != null ? "GP-Blueprint-" + gpLabel + ".pdf" : null)
                    .sent(sent).total(total)
                    .status((stub ? "stub — SMTP not configured · " : "") + sent + "/" + total + " sent")
                    .stub(stub)
                    .results(new HashMap<>(result))
                    .build();
            List<MailLogService.Att> files = new java.util.ArrayList<>();
            if (blueprint != null) files.add(new MailLogService.Att("GP Blue Print — " + gpLabel, blueprint));
            mailLog.save(logEntry, files);
        } catch (Exception ex) {
            log.warn("mail-log save failed: {}", ex.getMessage());
        }
        return result;
    }

    private void send(List<String> to, Student s, SendMailRequest req, byte[] blueprint) {
        String subject = StringUtils.hasText(req.subject())
                ? req.subject()
                : "MSYEP — Documents for student " + s.getName();
        String body = StringUtils.hasText(req.body()) ? req.body() :
                "Dear Gram Panchayat,\n\nPlease find the MSYEP documents for student "
                        + s.getName() + " (GP: " + s.getGramPanchayat() + ").\n\nRegards,\nMSYEP Finance";
        if (!mailConfigured()) {
            // No SMTP credentials (dev / stub mode): simulate a successful dispatch so the flow is verifiable.
            log.info("[MAIL SIMULATED] to={} subject={} student={} attachment={}",
                    to, subject, s.getName(), blueprint != null ? "GP-Blueprint.pdf" : "none");
            return;
        }
        try {
            jakarta.mail.internet.MimeMessage mime = mailSender.get().createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper h =
                    new org.springframework.mail.javamail.MimeMessageHelper(mime, blueprint != null, "UTF-8");
            h.setTo(to.toArray(new String[0]));
            h.setSubject(subject);
            h.setText(body);
            if (blueprint != null) {
                h.addAttachment("GP-Blueprint.pdf",
                        new org.springframework.core.io.ByteArrayResource(blueprint));
            }
            mailSender.get().send(mime);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    // ---- Gram Panchayat mapping CRUD ----
    public List<GramPanchayat> listGramPanchayats() { return gpRepo.findAll(); }

    public GramPanchayat saveGramPanchayat(GramPanchayat gp) { return gpRepo.save(gp); }

    public void deleteGramPanchayat(String id) {
        if (!gpRepo.existsById(id)) throw new ResourceNotFoundException("Gram Panchayat not found: " + id);
        gpRepo.deleteById(id);
    }
}
