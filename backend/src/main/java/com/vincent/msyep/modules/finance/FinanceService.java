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

    private final MongoTemplate mongo;
    private final CenterRepository centerRepo;
    private final GramPanchayatRepository gpRepo;
    private final Optional<JavaMailSender> mailSender;

    public FinanceService(MongoTemplate mongo, CenterRepository centerRepo,
                          GramPanchayatRepository gpRepo, Optional<JavaMailSender> mailSender) {
        this.mongo = mongo;
        this.centerRepo = centerRepo;
        this.gpRepo = gpRepo;
        this.mailSender = mailSender;
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
        for (String studentId : req.studentIds()) {
            Student s = mongo.findById(studentId, Student.class);
            if (s == null) {
                result.put(studentId, "NOT_FOUND");
                continue;
            }
            String to = StringUtils.hasText(req.overrideEmail())
                    ? req.overrideEmail()
                    : resolveGpEmail(s.getGramPanchayat());
            if (!StringUtils.hasText(to)) {
                result.put(studentId, "NO_GP_EMAIL");
                continue;
            }
            try {
                send(to, s, req);
                result.put(studentId, "SENT:" + to);
            } catch (Exception ex) {
                result.put(studentId, "FAILED:" + ex.getMessage());
            }
        }
        return result;
    }

    private void send(String to, Student s, SendMailRequest req) {
        if (mailSender.isEmpty()) {
            throw new IllegalStateException("Mail server not configured");
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(StringUtils.hasText(req.subject())
                ? req.subject()
                : "MSYEP — Documents for student " + s.getName());
        String body = StringUtils.hasText(req.body()) ? req.body() :
                "Dear Gram Panchayat,\n\nPlease find the MSYEP documents for student "
                        + s.getName() + " (GP: " + s.getGramPanchayat() + ").\n\nRegards,\nMSYEP Finance";
        msg.setText(body);
        mailSender.get().send(msg);
    }

    // ---- Gram Panchayat mapping CRUD ----
    public List<GramPanchayat> listGramPanchayats() { return gpRepo.findAll(); }

    public GramPanchayat saveGramPanchayat(GramPanchayat gp) { return gpRepo.save(gp); }

    public void deleteGramPanchayat(String id) {
        if (!gpRepo.existsById(id)) throw new ResourceNotFoundException("Gram Panchayat not found: " + id);
        gpRepo.deleteById(id);
    }
}
