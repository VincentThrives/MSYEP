package com.vincent.msyep.modules.student;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.config.security.MsyepPrincipal;
import com.vincent.msyep.modules.finance.MailLog;
import com.vincent.msyep.modules.student.dto.StudentMailRequest;
import com.vincent.msyep.modules.student.dto.StudentRegistrationResult;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService service;
    private final StudentRegistrationService registration;
    private final StudentExportService export;
    private final StudentMailService studentMail;

    public StudentController(StudentService service, StudentRegistrationService registration,
                             StudentExportService export, StudentMailService studentMail) {
        this.service = service;
        this.registration = registration;
        this.export = export;
        this.studentMail = studentMail;
    }

    /** Bulk-send the document packet to selected students' emails (Students Mail page). */
    @PostMapping("/send-mail")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<java.util.Map<String, String>> sendMail(@Valid @RequestBody StudentMailRequest req) {
        return ApiResponse.ok("Mail dispatch complete", studentMail.sendToStudents(req.studentIds(), req.subject(), req.body()));
    }

    /** Sent-mail history for the Student wing. */
    @GetMapping("/mail-history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<List<MailLog>> mailHistory() {
        return ApiResponse.ok(studentMail.history());
    }

    /**
     * Data scope for the current login:
     * CENTER → only its own center's students, ZONE → only its zone's students,
     * STUDENT → only its own record, ADMIN/SUPER_ADMIN → everything (respect explicit filters).
     */
    private record Scope(String centerId, String zoneId, String studentId) {}

    private Scope scope(MsyepPrincipal p, String requestedCenterId) {
        if (p == null) return new Scope(requestedCenterId, null, null);
        return switch (p.role()) {
            case "CENTER" -> new Scope(p.centerId(), null, null);
            case "ZONE" -> new Scope(null, p.zoneId(), null);
            case "STUDENT" -> new Scope(null, null, p.studentId());
            default -> new Scope(requestedCenterId, null, null);
        };
    }

    /** Filtered list for the View Students page. */
    @GetMapping("/filter")
    public ApiResponse<List<Student>> filter(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String taluk,
            @RequestParam(required = false) String gramPanchayat,
            @RequestParam(required = false) String centerId,
            @RequestParam(required = false) String caste,
            @AuthenticationPrincipal MsyepPrincipal principal) {
        Scope s = scope(principal, centerId);
        return ApiResponse.ok(export.filter(district, taluk, gramPanchayat, s.centerId(), s.zoneId(), s.studentId(), caste));
    }

    /** Export filtered students to Excel. */
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportExcel(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String taluk,
            @RequestParam(required = false) String gramPanchayat,
            @RequestParam(required = false) String centerId,
            @RequestParam(required = false) String caste,
            @AuthenticationPrincipal MsyepPrincipal principal) {
        Scope s = scope(principal, centerId);
        byte[] xlsx = export.toExcel(export.filter(district, taluk, gramPanchayat, s.centerId(), s.zoneId(), s.studentId(), caste));
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

    /** ZIP of the selected students' documents (by type) + each student's Resume PDF. */
    @PostMapping("/documents-zip")
    public ResponseEntity<ByteArrayResource> documentsZip(@RequestBody DocsRequest req) {
        byte[] zip = export.documentsZip(req.studentIds(), req.docType());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student-documents.zip")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(new ByteArrayResource(zip));
    }

    @GetMapping
    public ApiResponse<List<Student>> list(@RequestParam(required = false) String centerId,
                                           @AuthenticationPrincipal MsyepPrincipal principal) {
        Scope s = scope(principal, centerId);
        return ApiResponse.ok(export.filter(null, null, null, s.centerId(), s.zoneId(), s.studentId(), null));
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
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal MsyepPrincipal p) {
        if (p != null && "STUDENT".equals(p.role()) && !id.equals(p.studentId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only upload to your own profile");
        }
        return ApiResponse.ok("Document uploaded", service.attachDocument(id, type, label, file));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER','STUDENT')")
    public ApiResponse<Student> update(@PathVariable String id, @RequestBody Student student,
                                       @AuthenticationPrincipal MsyepPrincipal p) {
        if (p != null && "STUDENT".equals(p.role())) {
            if (!id.equals(p.studentId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only update your own profile");
            }
            // Students can't move themselves between zones/centers or change their allotment codes.
            service.findById(id); // 404 if missing
            student.setZoneId(null);
            student.setCenterId(null);
            student.setRegisterNo(null);
            student.setBatchCode(null);
        }
        return ApiResponse.ok("Student updated", service.update(id, student));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok("Student deleted", null);
    }
}
