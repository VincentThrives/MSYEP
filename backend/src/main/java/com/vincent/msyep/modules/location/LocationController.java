package com.vincent.msyep.modules.location;

import com.vincent.msyep.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @GetMapping("/districts")
    public ApiResponse<List<String>> districts() {
        return ApiResponse.ok(service.districts());
    }

    @GetMapping("/taluks")
    public ApiResponse<List<String>> taluks(@RequestParam String district) {
        return ApiResponse.ok(service.taluks(district));
    }

    @GetMapping("/gram-panchayats")
    public ApiResponse<List<String>> gramPanchayats(@RequestParam String district,
                                                    @RequestParam String taluk) {
        return ApiResponse.ok(service.gramPanchayats(district, taluk));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ApiResponse<Map<String, Integer>> importExcel(@RequestParam("file") MultipartFile file) {
        int added = service.importExcel(file);
        return ApiResponse.ok("Imported " + added + " new location rows", Map.of("added", added));
    }
}
