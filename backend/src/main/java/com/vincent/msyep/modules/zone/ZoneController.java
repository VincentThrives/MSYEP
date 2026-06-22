package com.vincent.msyep.modules.zone;

import com.vincent.msyep.common.ApiResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/zones")
public class ZoneController {

    private final ZoneService service;

    public ZoneController(ZoneService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Zone>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Zone> get(@PathVariable String id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<Zone> create(@RequestBody Zone zone) {
        return ApiResponse.ok("Zone created", service.create(zone));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<Zone> update(@PathVariable String id, @RequestBody Zone zone) {
        return ApiResponse.ok("Zone updated", service.update(id, zone));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok("Zone deleted", null);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<Map<String, Integer>> importExcel(@RequestParam("file") MultipartFile file) {
        int count = service.importExcel(file);
        return ApiResponse.ok("Imported " + count + " zones", Map.of("imported", count));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<ByteArrayResource> pdf(@PathVariable String id) {
        byte[] pdf = service.toPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=zone-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }
}
