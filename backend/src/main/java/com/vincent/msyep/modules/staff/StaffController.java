package com.vincent.msyep.modules.staff;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.common.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final StaffRepository repo;

    public StaffController(StaffRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ApiResponse<List<Staff>> list(@RequestParam(required = false) String zoneId,
                                         @RequestParam(required = false) String centerId) {
        if (zoneId != null) return ApiResponse.ok(repo.findByZoneId(zoneId));
        if (centerId != null) return ApiResponse.ok(repo.findByCenterId(centerId));
        return ApiResponse.ok(repo.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER')")
    public ApiResponse<Staff> create(@RequestBody Staff staff) {
        staff.setId(null);
        return ApiResponse.ok("Staff created", repo.save(staff));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER')")
    public ApiResponse<Staff> update(@PathVariable String id, @RequestBody Staff staff) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Staff not found: " + id);
        staff.setId(id);
        return ApiResponse.ok("Staff updated", repo.save(staff));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Staff not found: " + id);
        repo.deleteById(id);
        return ApiResponse.ok("Staff deleted", null);
    }
}
