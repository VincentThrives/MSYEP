package com.vincent.msyep.modules.student;

import com.vincent.msyep.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
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
    public ApiResponse<Student> create(@RequestBody Student student) {
        return ApiResponse.ok("Student created", service.create(student));
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
