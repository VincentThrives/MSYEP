package com.vincent.msyep.modules.entrance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
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

import java.io.ByteArrayInputStream;
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
    private final com.vincent.msyep.modules.notify.WhatsAppService whatsApp;
    private final String uploadsDir;
    private final Random rnd = new Random();

    public EntranceTestService(QuestionBankRepository bank, EntranceAttemptRepository attempts,
                               StudentRepository students,
                               com.vincent.msyep.modules.notify.WhatsAppService whatsApp,
                               @Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.bank = bank;
        this.attempts = attempts;
        this.students = students;
        this.whatsApp = whatsApp;
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

        // One attempt only — block if this student has already submitted the test.
        if (hasTaken(studentId)) {
            throw new IllegalArgumentException("You have already taken the entrance test. Only one attempt is allowed.");
        }

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

    /** Common question words that carry no topical signal. */
    private static final Set<String> STOP = Set.of(
            "which", "what", "who", "whom", "whose", "where", "when", "why", "how",
            "is", "are", "was", "were", "do", "does", "did", "has", "have", "had",
            "the", "a", "an", "of", "to", "in", "on", "at", "for", "and", "or", "by",
            "with", "from", "this", "that", "these", "those", "it", "its", "as", "be",
            "been", "being", "will", "would", "can", "could", "should", "name", "choose",
            "fill", "blank", "correct", "following", "called", "known", "word", "term",
            "you", "your", "one", "many", "give", "state", "here", "into");

    /** Meaningful (topical) words from a question. */
    private Set<String> keywords(String question) {
        if (question == null) return Set.of();
        Set<String> ks = new LinkedHashSet<>();
        for (String t : question.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {
            if (t.length() >= 4 && !STOP.contains(t)) ks.add(t);
        }
        return ks;
    }

    private static int overlap(Set<String> a, Set<String> b) {
        int n = 0;
        for (String s : a) if (b.contains(s)) n++;
        return n;
    }

    /**
     * Build 4 shuffled options: the correct answer + 3 distractors.
     * Distractors are drawn first from questions that share keywords with this one
     * (so a "festival" question yields other festivals), then any same-category
     * answer, then any answer as a last resort.
     */
    private List<String> buildOptions(QuestionBank q, Map<String, List<QuestionBank>> byCat,
                                      List<QuestionBank> all) {
        LinkedHashSet<String> opts = new LinkedHashSet<>();
        opts.add(q.getAnswer());

        Set<String> myKeys = keywords(q.getQuestion());
        List<QuestionBank> sameCat = byCat.getOrDefault(q.getCategory() == null ? "" : q.getCategory(), List.of());

        // 1) Relevance-ranked distractors: same category + shares >=1 topical keyword.
        List<Map.Entry<String, Integer>> scored = sameCat.stream()
                .filter(o -> o != q && o.getAnswer() != null && !o.getAnswer().equalsIgnoreCase(q.getAnswer()))
                .map(o -> Map.entry(o.getAnswer(), overlap(myKeys, keywords(o.getQuestion()))))
                .filter(e -> e.getValue() > 0)
                .collect(Collectors.toList());
        Collections.shuffle(scored, rnd);                                   // variety among equal scores
        scored.sort((x, y) -> Integer.compare(y.getValue(), x.getValue())); // stable: keeps shuffle order per tier
        for (Map.Entry<String, Integer> e : scored) {
            if (opts.size() >= 4) break;
            opts.add(e.getKey());
        }

        // 2) Fallback: any same-category answer.
        if (opts.size() < 4) {
            List<String> catAny = sameCat.stream().map(QuestionBank::getAnswer)
                    .filter(a -> a != null && opts.stream().noneMatch(o -> o.equalsIgnoreCase(a)))
                    .distinct().collect(Collectors.toList());
            Collections.shuffle(catAny, rnd);
            for (String d : catAny) { if (opts.size() >= 4) break; opts.add(d); }
        }

        // 3) Last resort: any answer at all.
        if (opts.size() < 4) {
            List<String> any = all.stream().map(QuestionBank::getAnswer)
                    .filter(a -> a != null && opts.stream().noneMatch(o -> o.equalsIgnoreCase(a)))
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
        sendResultToWhatsApp(attempt);
        return toResult(attempt);
    }

    /**
     * Deliver the result sheet to the student's WhatsApp number.
     * A WhatsApp Business/Cloud API (or Twilio/Gupshup) gateway isn't wired yet — this records
     * the intent so it's ready to send the moment credentials are configured.
     */
    private void sendResultToWhatsApp(EntranceAttempt attempt) {
        try {
            String phone = students.findById(attempt.getStudentId())
                    .map(Student::getPhone).orElse(null);
            String outcome = attempt.isPassed() ? "PASS" : "FAIL";
            if (org.springframework.util.StringUtils.hasText(phone)) {
                // Generate the result PDF (with letterhead) so it is ready to attach.
                byte[] pdf = resultPdf(attempt.getId());
                whatsApp.sendResultPdf(phone, attempt.getStudentName(), outcome,
                        attempt.getScore(), attempt.getTotal(), pdf,
                        "EntranceResult-" + attempt.getStudentName() + ".pdf");
            } else {
                log.info("Entrance result for {} not sent — no WhatsApp/mobile number on file.",
                        attempt.getStudentName());
            }
        } catch (Exception e) {
            log.warn("WhatsApp result hook failed: {}", e.getMessage());
        }
    }

    public List<EntranceAttempt> attemptsFor(String studentId) {
        return attempts.findByStudentIdOrderByStartedAtDesc(studentId);
    }

    /** True once the student has a SUBMITTED attempt. */
    public boolean hasTaken(String studentId) {
        return attempts.findByStudentIdOrderByStartedAtDesc(studentId).stream()
                .anyMatch(a -> "SUBMITTED".equals(a.getStatus()));
    }

    /** The student's completed result (latest submitted attempt), or null if not yet taken. */
    public ResultResponse latestResult(String studentId) {
        return attempts.findByStudentIdOrderByStartedAtDesc(studentId).stream()
                .filter(a -> "SUBMITTED".equals(a.getStatus()))
                .findFirst()
                .map(this::toResult)
                .orElse(null);
    }

    /** Result PDF with the student's selfie, score, and pass/fail. */
    public byte[] resultPdf(String attemptId) {
        EntranceAttempt a = attempts.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] template = readFormatTemplate();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer)) {

            // Stamp the letterhead / format page as a background behind every page.
            if (template != null) {
                try (PdfDocument tpl = new PdfDocument(new PdfReader(new ByteArrayInputStream(template)))) {
                    PdfFormXObject bg = tpl.getFirstPage().copyAsFormXObject(pdf);
                    pdf.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
                        PdfPage page = ((PdfDocumentEvent) event).getPage();
                        new PdfCanvas(page.newContentStreamBefore(), page.getResources(), page.getDocument())
                                .addXObjectFittedIntoRectangle(bg, page.getPageSize());
                    });
                } catch (Exception ex) {
                    log.warn("Could not apply entrance format background: {}", ex.getMessage());
                }
            }

            try (Document doc = new Document(pdf)) {
                // Leave room for the letterhead header (top) and footer (bottom).
                float top = 160, bottom = 80, side = 40;
                doc.setMargins(top, side, bottom, side);

                // A single full-height cell lets us center the result block both
                // horizontally and vertically in the space below the letterhead.
                // Trim a few points off the band so cell/table padding can't spill to page 2.
                float bandHeight = PageSize.A4.getHeight() - top - bottom - 12;
                Cell block = new Cell()
                        .setBorder(Border.NO_BORDER)
                        .setPadding(0)
                        .setHeight(bandHeight)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setTextAlignment(TextAlignment.CENTER);

                block.add(new Paragraph("Entrance Test Result").setBold().setFontSize(16)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(12));
                block.add(new Paragraph("Candidate: " + nz(a.getStudentName())).setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER));
                block.add(new Paragraph("Score: " + a.getScore() + " / " + a.getTotal()).setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER));
                block.add(new Paragraph("Result: " + (a.isPassed() ? "PASS" : "FAIL"))
                        .setBold().setFontSize(13).setTextAlignment(TextAlignment.CENTER));
                if (StringUtils.hasText(a.getSelfiePath())) {
                    try {
                        Path p = Paths.get(uploadsDir).resolve(a.getSelfiePath().replace("uploads/", ""));
                        if (Files.exists(p)) {
                            block.add(new Paragraph("Photo:").setFontSize(11).setMarginTop(8)
                                    .setTextAlignment(TextAlignment.CENTER));
                            Image img = new Image(ImageDataFactory.create(p.toAbsolutePath().toString()));
                            img.setWidth(120);
                            img.setHorizontalAlignment(HorizontalAlignment.CENTER);
                            block.add(img);
                        }
                    } catch (Exception ignored) { }
                }
                block.add(new Paragraph("\nThis is an auto-generated result.").setItalic().setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER));

                Table wrap = new Table(1).setWidth(UnitValue.createPercentValue(100));
                wrap.addCell(block);
                doc.add(wrap);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate result PDF: " + e.getMessage());
        }
        return out.toByteArray();
    }

    /** The bundled entrance-test letterhead / format page (null if missing). */
    private byte[] readFormatTemplate() {
        try (InputStream in = new ClassPathResource("entrance-format.pdf").getInputStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            return null;
        }
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
