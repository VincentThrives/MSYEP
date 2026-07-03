package com.vincent.msyep.modules.student;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.vincent.msyep.modules.center.Center;
import com.vincent.msyep.modules.center.CenterRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Excel export + combined-PDF download for students. */
@Service
public class StudentExportService {

    private final MongoTemplate mongo;
    private final StudentRepository students;
    private final CenterRepository centers;
    private final String uploadsDir;

    public StudentExportService(MongoTemplate mongo, StudentRepository students,
                                CenterRepository centers,
                                @Value("${app.uploads-dir:uploads}") String uploadsDir) {
        this.mongo = mongo;
        this.students = students;
        this.centers = centers;
        this.uploadsDir = uploadsDir;
    }

    /** Filtered student query for the View Students / Download pages. */
    public List<Student> filter(String district, String taluk, String gramPanchayat,
                                String centerId, String caste) {
        Query q = new Query();
        if (StringUtils.hasText(district)) q.addCriteria(Criteria.where("district").is(district));
        if (StringUtils.hasText(taluk)) q.addCriteria(Criteria.where("taluk").is(taluk));
        if (StringUtils.hasText(gramPanchayat)) q.addCriteria(Criteria.where("gramPanchayat").is(gramPanchayat));
        if (StringUtils.hasText(centerId)) q.addCriteria(Criteria.where("centerId").is(centerId));
        if (StringUtils.hasText(caste)) q.addCriteria(Criteria.where("caste").is(caste));
        return mongo.find(q, Student.class);
    }

    /** Export the given students to an .xlsx. */
    public byte[] toExcel(List<Student> list) {
        Map<String, String> centerNames = centerNameMap();
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Students");
            String[] headers = {"Reg No", "Batch Code", "Name", "Email", "Phone", "Gender",
                    "Caste", "District", "Taluk", "Gram Panchayat", "Center", "Status"};
            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int r = 1;
            for (Student s : list) {
                Row row = sheet.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(nz(s.getRegisterNo()));
                row.createCell(c++).setCellValue(nz(s.getBatchCode()));
                row.createCell(c++).setCellValue(nz(s.getName()));
                row.createCell(c++).setCellValue(nz(s.getEmail()));
                row.createCell(c++).setCellValue(nz(s.getPhone()));
                row.createCell(c++).setCellValue(nz(s.getGender()));
                row.createCell(c++).setCellValue(nz(s.getCaste()));
                row.createCell(c++).setCellValue(nz(s.getDistrict()));
                row.createCell(c++).setCellValue(nz(s.getTaluk()));
                row.createCell(c++).setCellValue(nz(s.getGramPanchayat()));
                row.createCell(c++).setCellValue(centerNames.getOrDefault(s.getCenterId(), ""));
                row.createCell(c).setCellValue(s.isActive() ? "Active" : "Inactive");
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build Excel: " + e.getMessage());
        }
    }

    /**
     * Build a single combined PDF of the selected students' documents (not a zip).
     * Each student's documents become image pages, grouped under a header.
     * If docType is given (and not "All"), only that document type is included.
     */
    public byte[] documentsPdf(List<String> studentIds, String docType) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {
            boolean any = false;
            for (String id : studentIds) {
                Student s = students.findById(id).orElse(null);
                if (s == null) continue;
                doc.add(new Paragraph("Student: " + nz(s.getName())
                        + "  (" + nz(s.getRegisterNo()) + ")").setBold().setFontSize(14));
                for (var d : s.getDocuments()) {
                    if (StringUtils.hasText(docType) && !"All".equalsIgnoreCase(docType)
                            && !docType.equals(d.getType())) continue;
                    Path p = Paths.get(uploadsDir).resolve(d.getPath().replace("uploads/", ""));
                    if (!Files.exists(p)) continue;
                    try {
                        doc.add(new Paragraph(nz(d.getLabel())).setFontSize(10).setItalic());
                        Image img = new Image(ImageDataFactory.create(p.toAbsolutePath().toString()));
                        img.setAutoScale(true);
                        doc.add(img);
                        any = true;
                    } catch (Exception ignored) { /* skip non-image / unreadable */ }
                }
            }
            if (!any) doc.add(new Paragraph("No documents found for the selected students / type."));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build documents PDF: " + e.getMessage());
        }
        return out.toByteArray();
    }

    private Map<String, String> centerNameMap() {
        Map<String, String> m = new HashMap<>();
        for (Center c : centers.findAll()) m.put(c.getId(), c.getName());
        return m;
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}
