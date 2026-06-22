package com.vincent.msyep.modules.entrance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.vincent.msyep.common.exception.ResourceNotFoundException;
import com.vincent.msyep.modules.entrance.EntranceAttempt.AttemptItem;
import com.vincent.msyep.modules.entrance.dto.EntranceDtos.*;
import com.vincent.msyep.modules.student.Student;
import com.vincent.msyep.modules.student.StudentRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EntranceTestService {

    private static final Logger log = LoggerFactory.getLogger(EntranceTestService.class);
    private static final int QUESTIONS = 10;
    private static final int PASS_MARK = 5;
    private static final int DURATION_MIN = 10;
    private static final long GRACE_SEC = 30;

    private final QuestionBankRepository bank;
    private final EntranceAttemptRepository attempts;
    private final StudentRepository students;
    private final String uploadsDir;
    private final Random rnd = new Random();

    public EntranceTestService(QuestionBankRepository bank, EntranceAttemptRepository attempts,
                               StudentRepository students,
                               @Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.bank = bank;
        this.attempts = attempts;
        this.students = students;
        this.uploadsDir = uploadsDir;
    }

    /** Seed the question bank from the bundled JSON on first boot. */
    @PostConstruct
    void seed() {
        if (bank.count() > 0) return;
        try (InputStream in = new ClassPathResource("entrance-questions.json").getInputStream()) {
            record Row(String q, String a, String c) {}
            List<Row> rows = new ObjectMapper().readValue(in, new TypeReference<List<Row>>() {});
            Map<String, QuestionBank> uniq = new LinkedHashMap<>();
            for (Row r : rows) {
                if (!StringUtils.hasText(r.q()) || !StringUtils.hasText(r.a())) continue;
                uniq.putIfAbsent(r.q().trim().toLowerCase(), QuestionBank.builder()
                        .question(r.q().trim()).answer(r.a().trim()).category(r.c()).build());
            }
            bank.saveAll(uniq.values());
            log.info("Seeded {} entrance-test questions", uniq.size());
        } catch (Exception e) {
            log.warn("Could not seed entrance questions: {}", e.getMessage());
        }
    }

    /** Start a test: store the selfie, pick 10 random MCQs, begin the 10-minute window. */
    public StartResponse start(String studentId, MultipartFile selfie) {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        List<QuestionBank> all = bank.findAll();
        if (all.size() < QUESTIONS) {
            throw new IllegalStateException("Question bank not ready");
        }
        Map<String, List<QuestionBank>> byCat = all.stream()
                .collect(Collectors.groupingBy(q -> q.getCategory() == null ? "" : q.getCategory()));

        List<QuestionBank> pool = new ArrayList<>(all);
        Collections.shuffle(pool, rnd);
        List<QuestionBank> picked = pool.subList(0, QUESTIONS);

        List<AttemptItem> items = new ArrayList<>();
        for (QuestionBank q : picked) {
            items.add(AttemptItem.builder()
                    .questionId(q.getId())
                    .question(q.getQuestion())
                    .options(buildOptions(q, byCat, all))
                    .correctAnswer(q.getAnswer())
                    .build());
        }

        EntranceAttempt attempt = attempts.save(EntranceAttempt.builder()
                .studentId(studentId)
                .studentName(student.getName())
                .items(items)
                .startedAt(Instant.now())
                .status("IN_PROGRESS")
                .build());

        if (selfie != null && !selfie.isEmpty()) {
            attempt.setSelfiePath(storeSelfie(attempt.getId(), selfie));
            attempts.save(attempt);
        }

        List<QuestionView> views = items.stream()
                .map(i -> new QuestionView(i.getQuestionId(), i.getQuestion(), i.getOptions()))
                .toList();
        return new StartResponse(attempt.getId(), DURATION_MIN, attempt.getStartedAt().toString(), views);
    }

    /** Build 4 shuffled options: correct + 3 same-category distractors (fallback any). */
    private List<String> buildOptions(QuestionBank q, Map<String, List<QuestionBank>> byCat,
                                      List<QuestionBank> all) {
        LinkedHashSet<String> opts = new LinkedHashSet<>();
        opts.add(q.getAnswer());

        List<String> sameCat = byCat.getOrDefault(q.getCategory(), List.of()).stream()
                .map(QuestionBank::getAnswer)
                .filter(a -> !a.equalsIgnoreCase(q.getAnswer()))
                .distinct().collect(Collectors.toList());
        Collections.shuffle(sameCat, rnd);
        for (String d : sameCat) { if (opts.size() >= 4) break; opts.add(d); }

        if (opts.size() < 4) {
            List<String> any = all.stream().map(QuestionBank::getAnswer)
                    .filter(a -> opts.stream().noneMatch(o -> o.equalsIgnoreCase(a)))
                    .distinct().collect(Collectors.toList());
            Collections.shuffle(any, rnd);
            for (String d : any) { if (opts.size() >= 4) break; opts.add(d); }
        }

        List<String> options = new ArrayList<>(opts);
        Collections.shuffle(options, rnd);
        return options;
    }

    /** Grade the attempt: enforce the 10-minute window, score, and mark pass/fail. */
    public ResultResponse submit(String attemptId, Map<String, String> answers) {
        EntranceAttempt attempt = attempts.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));
        if ("SUBMITTED".equals(attempt.getStatus())) {
            return toResult(attempt);
        }
        long elapsed = ChronoUnit.SECONDS.between(attempt.getStartedAt(), Instant.now());
        boolean expired = elapsed > (DURATION_MIN * 60L + GRACE_SEC);

        int score = 0;
        Map<String, String> ans = answers == null ? Map.of() : answers;
        for (AttemptItem item : attempt.getItems()) {
            // Ignore late answers if the window expired (auto-submit grades what was saved).
            String sel = expired ? item.getSelectedAnswer() : ans.get(item.getQuestionId());
            item.setSelectedAnswer(sel);
            boolean ok = sel != null && norm(sel).equals(norm(item.getCorrectAnswer()));
            item.setCorrect(ok);
            if (ok) score++;
        }
        attempt.setScore(score);
        attempt.setTotal(attempt.getItems().size());
        attempt.setPassed(score >= PASS_MARK);
        attempt.setStatus("SUBMITTED");
        attempt.setSubmittedAt(Instant.now());
        attempts.save(attempt);
        return toResult(attempt);
    }

    public List<EntranceAttempt> attemptsFor(String studentId) {
        return attempts.findByStudentIdOrderByStartedAtDesc(studentId);
    }

    /** Result PDF with the student's selfie, score, and pass/fail. */
    public byte[] resultPdf(String attemptId) {
        EntranceAttempt a = attempts.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {
            doc.add(new Paragraph("MSYEP — Entrance Test Result").setBold().setFontSize(16));
            doc.add(new Paragraph("Candidate: " + nz(a.getStudentName())));
            doc.add(new Paragraph("Score: " + a.getScore() + " / " + a.getTotal()));
            doc.add(new Paragraph("Result: " + (a.isPassed() ? "PASS" : "FAIL")).setBold());
            if (StringUtils.hasText(a.getSelfiePath())) {
                try {
                    Path p = Paths.get(uploadsDir).resolve(a.getSelfiePath().replace("uploads/", ""));
                    if (Files.exists(p)) {
                        Image img = new Image(ImageDataFactory.create(p.toAbsolutePath().toString()));
                        img.setWidth(120);
                        doc.add(new Paragraph("Selfie:"));
                        doc.add(img);
                    }
                } catch (Exception ignored) { }
            }
            doc.add(new Paragraph("\nThis is an auto-generated result.").setItalic().setFontSize(10));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate result PDF: " + e.getMessage());
        }
        return out.toByteArray();
    }

    private String storeSelfie(String attemptId, MultipartFile selfie) {
        try {
            Path dir = Paths.get(uploadsDir, "entrance", attemptId);
            Files.createDirectories(dir);
            String name = "selfie.jpg";
            Path target = dir.resolve(name);
            selfie.transferTo(target.toAbsolutePath());
            return Paths.get("entrance", attemptId, name).toString();
        } catch (Exception e) {
            log.warn("Could not store selfie for {}: {}", attemptId, e.getMessage());
            return null;
        }
    }

    private ResultResponse toResult(EntranceAttempt a) {
        List<ResultItem> items = a.getItems().stream()
                .map(i -> new ResultItem(i.getQuestion(), i.getOptions(), i.getCorrectAnswer(),
                        i.getSelectedAnswer(), i.isCorrect()))
                .toList();
        return new ResultResponse(a.getId(), a.getScore(), a.getTotal(), a.isPassed(), items);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("[\\s.]+$", "").replaceAll("\\s+", " ");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
