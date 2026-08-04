package com.vincent.msyep.modules.cv;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.vincent.msyep.common.IdGen;
import com.vincent.msyep.common.exception.ResourceNotFoundException;
import com.vincent.msyep.modules.student.Student;
import com.vincent.msyep.modules.student.StudentDocument;
import com.vincent.msyep.modules.student.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** CV generation: profile-completeness check, ₹90 payment (Razorpay-ready, stubbed until keyed), and the PDF. */
@Service
public class CvService {

    private static final Logger log = LoggerFactory.getLogger(CvService.class);
    private static final int AMOUNT_PAISE = 9000; // ₹90
    private static final DeviceRgb GREEN = new DeviceRgb(14, 81, 50);
    private static final DeviceRgb SIDEBAR = new DeviceRgb(237, 245, 240);

    private final CvPaymentRepository payments;
    private final StudentRepository students;
    private final String uploadsDir;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;

    public CvService(CvPaymentRepository payments, StudentRepository students,
                     @Value("${app.uploads-dir:uploads}") String uploadsDir,
                     @Value("${razorpay.key-id:}") String razorpayKeyId,
                     @Value("${razorpay.key-secret:}") String razorpayKeySecret) {
        this.payments = payments;
        this.students = students;
        this.uploadsDir = uploadsDir;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
    }

    /** No real keys configured → run in stub mode (auto-confirm payment so the flow is testable). */
    public boolean stub() {
        return !StringUtils.hasText(razorpayKeyId) || !StringUtils.hasText(razorpayKeySecret);
    }

    // ---- Completeness ----

    public record FieldReq(String key, String label, int tab) {}

    private static final List<FieldReq> REQUIRED = List.of(
            new FieldReq("name", "Full Name", 0),
            new FieldReq("phone", "Mobile Number", 0),
            new FieldReq("email", "Email", 0),
            new FieldReq("dateOfBirth", "Date of Birth", 1),
            new FieldReq("gender", "Gender", 1),
            new FieldReq("educationalQualification", "Educational Qualification", 2),
            new FieldReq("careerGoal", "Career Objective", 2),
            new FieldReq("technicalSkills", "Technical Skills", 2),
            new FieldReq("district", "District", 3),
            new FieldReq("passportPhoto", "Passport Size Photo", 5));

    public record MissingField(String label, int tab) {}
    public record StatusView(boolean complete, List<MissingField> missing, boolean paid) {}

    public StatusView status(String studentId) {
        Student s = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        List<MissingField> missing = missing(s);
        return new StatusView(missing.isEmpty(), missing, isPaid(studentId));
    }

    private List<MissingField> missing(Student s) {
        List<MissingField> out = new ArrayList<>();
        for (FieldReq r : REQUIRED) {
            boolean present = switch (r.key()) {
                case "name" -> has(s.getName());
                case "phone" -> has(s.getPhone());
                case "email" -> has(s.getEmail());
                case "dateOfBirth" -> has(s.getDateOfBirth());
                case "gender" -> has(s.getGender());
                case "educationalQualification" -> has(s.getEducationalQualification());
                case "careerGoal" -> has(s.getCareerGoal());
                case "technicalSkills" -> has(s.getTechnicalSkills());
                case "district" -> has(s.getDistrict());
                case "passportPhoto" -> photoDoc(s) != null;
                default -> true;
            };
            if (!present) out.add(new MissingField(r.label(), r.tab()));
        }
        return out;
    }

    public boolean isPaid(String studentId) {
        return payments.existsByStudentIdAndStatus(studentId, "PAID");
    }

    // ---- Payment ----

    public record OrderView(String orderId, int amountPaise, String currency,
                            String keyId, boolean stub, boolean alreadyPaid) {}

    public OrderView createOrder(String studentId) {
        Student s = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (!missing(s).isEmpty()) {
            throw new IllegalArgumentException("Please complete your profile before generating the CV.");
        }
        if (isPaid(studentId)) {
            return new OrderView(null, AMOUNT_PAISE, "INR", razorpayKeyId, stub(), true);
        }
        String orderId = stub() ? "stub_" + IdGen.cuid() : createRazorpayOrder();
        payments.save(CvPayment.builder()
                .studentId(studentId).status("CREATED").orderId(orderId)
                .amountPaise(AMOUNT_PAISE).stub(stub()).createdAt(Instant.now()).build());
        return new OrderView(orderId, AMOUNT_PAISE, "INR", razorpayKeyId, stub(), false);
    }

    public void verify(String studentId, String orderId, String paymentId, String signature) {
        CvPayment p = payments.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!p.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Order does not belong to this student");
        }
        if (!p.isStub()) {
            String expected = hmacSha256(orderId + "|" + paymentId, razorpayKeySecret);
            if (!expected.equals(signature)) {
                throw new IllegalArgumentException("Payment signature verification failed");
            }
        }
        p.setStatus("PAID");
        p.setPaymentId(paymentId);
        p.setPaidAt(Instant.now());
        payments.save(p);
    }

    /** Create a Razorpay order via REST (used only when real keys are set). */
    private String createRazorpayOrder() {
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            String body = "{\"amount\":" + AMOUNT_PAISE + ",\"currency\":\"INR\",\"payment_capture\":1}";
            String auth = java.util.Base64.getEncoder()
                    .encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8));
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                    .build();
            var resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            String json = resp.body();
            int i = json.indexOf("\"id\"");
            if (i < 0) throw new IllegalStateException("Razorpay order failed: " + json);
            int start = json.indexOf('"', json.indexOf(':', i) + 1) + 1;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create Razorpay order: " + e.getMessage());
        }
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC error: " + e.getMessage());
        }
    }

    // ---- PDF ----

    public byte[] buildCv(String studentId) {
        return buildCv(studentId, true);
    }

    /** Build the resume PDF. Admin exports pass requirePayment=false to skip the ₹90 gate. */
    public byte[] buildCv(String studentId, boolean requirePayment) {
        Student s = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (requirePayment && !isPaid(studentId)) {
            throw new IllegalStateException("Payment required before downloading the CV.");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {
            doc.setMargins(0, 0, 0, 0);

            Table layout = new Table(UnitValue.createPercentArray(new float[]{34, 66}))
                    .setWidth(UnitValue.createPercentValue(100));

            // ---- Left sidebar ----
            Cell left = new Cell().setBorder(Border.NO_BORDER)
                    .setBackgroundColor(SIDEBAR).setPadding(16);
            byte[] photo = readPhoto(s);
            if (photo != null) {
                try {
                    Image img = new Image(ImageDataFactory.create(photo));
                    img.setWidth(120).setHeight(140).setMarginBottom(12);
                    left.add(img);
                } catch (Exception ignored) { }
            }
            left.add(sideHeading("CONTACT"));
            left.add(sideLine(nz(s.getPhone())));
            left.add(sideLine(nz(s.getEmail())));
            left.add(sideLine(joinAddress(s)));

            left.add(sideHeading("PERSONAL"));
            left.add(sideLine("DOB: " + nz(s.getDateOfBirth())));
            left.add(sideLine("Gender: " + nz(s.getGender())));
            left.add(sideLine("Nationality: Indian"));

            left.add(sideHeading("SKILLS"));
            for (String skill : nz(s.getTechnicalSkills()).split(",")) {
                if (StringUtils.hasText(skill)) left.add(sideLine("• " + skill.trim()));
            }

            // ---- Right main ----
            Cell right = new Cell().setBorder(Border.NO_BORDER).setPadding(20);
            right.add(new Paragraph(nz(s.getName())).setBold().setFontSize(22).setFontColor(GREEN).setMarginBottom(0));
            right.add(new Paragraph(nz(s.getEducationalQualification())).setFontSize(11).setFontColor(new DeviceRgb(90, 100, 95)).setMarginTop(0));

            right.add(mainHeading("CAREER OBJECTIVE"));
            right.add(new Paragraph(nz(s.getCareerGoal())).setFontSize(10).setMarginBottom(4));

            right.add(mainHeading("EDUCATION"));
            boolean anyEdu = has(s.getSslcSchool()) || has(s.getSslcPercent()) || has(s.getSslcYear())
                    || has(s.getPuSchool()) || has(s.getPuPercent()) || has(s.getPuYear()) || has(s.getPuStream())
                    || has(s.getDegreeCollege()) || has(s.getDegreePercent()) || has(s.getDegreeYear()) || has(s.getDegreeStream());
            if (anyEdu) {
                Table edu = new Table(UnitValue.createPercentArray(new float[]{26, 30, 12, 32}))
                        .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(6);
                edu.addHeaderCell(eduHead("Qualification"));
                edu.addHeaderCell(eduHead("Institution / Course"));
                edu.addHeaderCell(eduHead("Year"));
                edu.addHeaderCell(eduHead("Percentage / CGPA"));
                if (has(s.getSslcSchool()) || has(s.getSslcPercent()) || has(s.getSslcYear())) {
                    eduRow(edu, "SSLC / 10th", null, s.getSslcSchool(), s.getSslcYear(), s.getSslcMarkType(), s.getSslcPercent());
                }
                if (has(s.getPuSchool()) || has(s.getPuPercent()) || has(s.getPuYear()) || has(s.getPuStream())) {
                    eduRow(edu, "PU / Diploma", s.getPuStream(), s.getPuSchool(), s.getPuYear(), s.getPuMarkType(), s.getPuPercent());
                }
                if (has(s.getDegreeCollege()) || has(s.getDegreePercent()) || has(s.getDegreeYear()) || has(s.getDegreeStream())) {
                    eduRow(edu, "Degree", s.getDegreeStream(), s.getDegreeCollege(), s.getDegreeYear(), s.getDegreeMarkType(), s.getDegreePercent());
                }
                right.add(edu);
            } else {
                right.add(bullet(nz(s.getEducationalQualification())
                        + (has(s.getCollegeName()) ? " — " + s.getCollegeName() : "")));
            }
            if (has(s.getRegisterNo())) right.add(bullet("MSYEP Register No: " + s.getRegisterNo()));

            String interests = interests(s);
            if (StringUtils.hasText(interests)) {
                right.add(mainHeading("INTERESTS"));
                right.add(new Paragraph(interests).setFontSize(10));
            }

            right.add(new Paragraph("\n").setFontSize(6));
            right.add(new Paragraph()
                    .add(new Text("Reference: ").setBold())
                    .add(new Text("Trained under Yukta Kaushalya Tarabethi — MSYEP (Kaushalya Patha)."))
                    .setFontSize(9).setFontColor(GREEN));

            layout.addCell(left);
            layout.addCell(right);
            doc.add(layout);

            // ---- Footer: YKTK logo, centred ----
            byte[] logo = readLogo();
            if (logo != null) {
                try {
                    Image lg = new Image(ImageDataFactory.create(logo));
                    lg.setWidth(210);
                    lg.setHorizontalAlignment(HorizontalAlignment.CENTER);
                    lg.setMarginTop(18).setMarginBottom(8);
                    doc.add(lg);
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build CV: " + e.getMessage());
        }
        return out.toByteArray();
    }

    /** The YKTK footer logo bundled in resources (null if missing). */
    private byte[] readLogo() {
        try (InputStream in = new ClassPathResource("yktk-logo.png").getInputStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] readPhoto(Student s) {
        StudentDocument d = photoDoc(s);
        if (d == null || !StringUtils.hasText(d.getPath())) return null;
        try {
            Path p = Paths.get(uploadsDir).resolve(d.getPath().replace("uploads/", ""));
            return Files.exists(p) ? Files.readAllBytes(p) : null;
        } catch (Exception e) {
            log.warn("Could not read CV photo for {}: {}", s.getId(), e.getMessage());
            return null;
        }
    }

    private static StudentDocument photoDoc(Student s) {
        if (s.getDocuments() == null) return null;
        return s.getDocuments().stream()
                .filter(d -> "passportPhoto".equals(d.getType())).findFirst().orElse(null);
    }

    private static String interests(Student s) {
        List<String> parts = new ArrayList<>();
        if (s.getHobbies() != null) parts.addAll(s.getHobbies());
        if (s.getInterestedCourses() != null) parts.addAll(s.getInterestedCourses());
        return String.join(", ", parts);
    }

    private static String joinAddress(Student s) {
        List<String> parts = new ArrayList<>();
        for (String v : new String[]{s.getPostalAddress(), s.getGramPanchayat(), s.getTaluk(), s.getDistrict(), s.getPincode()}) {
            if (StringUtils.hasText(v)) parts.add(v);
        }
        return String.join(", ", parts);
    }

    private static Paragraph sideHeading(String t) {
        return new Paragraph(t).setBold().setFontSize(11).setFontColor(GREEN).setMarginTop(12).setMarginBottom(2);
    }

    private static Paragraph sideLine(String t) {
        return new Paragraph(nz(t)).setFontSize(9).setMarginBottom(1);
    }

    private static Paragraph mainHeading(String t) {
        return new Paragraph(t).setBold().setFontSize(12).setFontColor(GREEN).setMarginTop(14).setMarginBottom(3);
    }

    private static Paragraph bullet(String t) {
        return new Paragraph("• " + nz(t)).setFontSize(10).setMarginBottom(1);
    }

    private static final DeviceRgb EDU_LINE = new DeviceRgb(206, 218, 210);

    /** Header cell for the EDUCATION table. */
    private static Cell eduHead(String t) {
        return new Cell().add(new Paragraph(t).setBold().setFontSize(7.5f).setFontColor(new DeviceRgb(255, 255, 255)))
                .setBackgroundColor(GREEN).setBorder(new SolidBorder(EDU_LINE, 0.5f)).setPadding(4);
    }

    /** One row in the EDUCATION table; the marks cell is highlighted with the chosen type. */
    private static void eduRow(Table t, String level, String stream, String institution,
                               String year, String markType, String value) {
        Paragraph q = new Paragraph().add(new Text(level).setBold().setFontSize(8.5f));
        if (has(stream)) q.add(new Text("\n" + stream).setFontSize(7).setFontColor(new DeviceRgb(90, 100, 95)));
        t.addCell(eduCell(q));
        t.addCell(eduCell(new Paragraph(nz(institution)).setFontSize(8.5f)));
        t.addCell(eduCell(new Paragraph(nz(year)).setFontSize(8.5f)));
        String mk = has(value) ? ((has(markType) ? markType : "Marks") + " - " + value) : "—";
        Cell marks = eduCell(new Paragraph(mk).setBold().setFontSize(8.5f).setFontColor(new DeviceRgb(14, 81, 50)));
        if (has(value)) marks.setBackgroundColor(new DeviceRgb(255, 243, 176));
        t.addCell(marks);
    }

    private static Cell eduCell(Paragraph p) {
        return new Cell().add(p.setMultipliedLeading(1.05f)).setBorder(new SolidBorder(EDU_LINE, 0.5f)).setPadding(4);
    }

    private static boolean has(String v) {
        return StringUtils.hasText(v);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
