package com.vincent.msyep.modules.student;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.modules.student.dto.StudentRegistrationResult;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService service;
    private final StudentRegistrationService registration;
    private final StudentExportService export;

    public StudentController(StudentService service, StudentRegistrationService registration,
                             StudentExportService export) {
        this.service = service;
        this.registration = registration;
        this.export = export;
    }

    /** Filtered list for the View Students page. */
    @GetMapping("/filter")
    public ApiResponse<List<Student>> filter(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String taluk,
            @RequestParam(required = false) String gramPanchayat,
            @RequestParam(required = false) String centerId,
            @RequestParam(required = false) String caste) {
        return ApiResponse.ok(export.filter(district, taluk, gramPanchayat, centerId, caste));
    }

    /** Export filtered students to Excel. */
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportExcel(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String taluk,
            @RequestParam(required = false) String gramPanchayat,
            @RequestParam(required = false) String centerId,
            @RequestParam(required = false) String caste) {
        byte[] xlsx = export.toExcel(export.filter(district, taluk, gramPanchayat, centerId, caste));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(xlsx));
    }

    /** Combined PDF of the selected students' documents (not a zip). */
    @PostMapping("/documents-pdf")
    public ResponseEntity<ByteArrayResource> documentsPdf(@RequestBody DocsRequest req) {
        byte[] pdf = export.documentsPdf(req.studentIds(), req.docType());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student-documents.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    public record DocsRequest(List<String> studentIds, String docType) {}

    @GetMapping
    public ApiResponse<List<Student>> list(@RequestParam(required = false) String centerId) {
        return ApiResponse.ok(centerId == null ? service.findAll() : service.findByCenter(centerId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Student> get(@PathVariable String id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER')")
    public ApiResponse<StudentRegistrationResult> create(@RequestBody Student student) {
        StudentRegistrationResult result = registration.register(student);
        return ApiResponse.ok(result.note(), result);
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER','STUDENT')")
    public ApiResponse<Student> uploadDocument(
            @PathVariable String id,
            @RequestParam("type") String type,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Document uploaded", service.attachDocument(id, type, label, file));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER')")
    public ApiResponse<Student> update(@PathVariable String id, @RequestBody Student student) {
        return ApiResponse.ok("Student updated", service.update(id, student));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok("Student deleted", null);
    }
}
