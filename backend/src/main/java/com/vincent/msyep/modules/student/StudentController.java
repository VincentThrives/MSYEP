package com.vincent.msyep.modules.student;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.modules.student.dto.StudentRegistrationResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService service;
    private final StudentRegistrationService registration;

    public StudentController(StudentService service, StudentRegistrationService registration) {
        this.service = service;
        this.registration = registration;
    }

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
