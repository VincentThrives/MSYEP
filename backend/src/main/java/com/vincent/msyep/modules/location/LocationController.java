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

    public record AddDistrict(String name) {}
    public record AddTaluk(String district, String taluk) {}
    public record AddGp(String district, String taluk, String gramPanchayat) {}

    @PostMapping("/districts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<String> addDistrict(@RequestBody AddDistrict req) {
        String d = service.addDistrict(req.name());
        return ApiResponse.ok("District added", d);
    }

    @PostMapping("/taluks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<Void> addTaluk(@RequestBody AddTaluk req) {
        service.addTaluk(req.district(), req.taluk());
        return ApiResponse.ok("Taluk added", null);
    }

    @PostMapping("/gram-panchayats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<Void> addGp(@RequestBody AddGp req) {
        service.addGramPanchayat(req.district(), req.taluk(), req.gramPanchayat());
        return ApiResponse.ok("Village/GP added", null);
    }

    public record RenameDistrict(String oldName, String newName) {}
    public record RenameTaluk(String district, String oldTaluk, String newTaluk) {}
    public record RenameGp(String district, String taluk, String oldGp, String newGp) {}

    @PutMapping("/districts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<Void> renameDistrict(@RequestBody RenameDistrict req) {
        service.renameDistrict(req.oldName(), req.newName());
        return ApiResponse.ok("District renamed", null);
    }

    @PutMapping("/taluks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<Void> renameTaluk(@RequestBody RenameTaluk req) {
        service.renameTaluk(req.district(), req.oldTaluk(), req.newTaluk());
        return ApiResponse.ok("Taluk renamed", null);
    }

    @PutMapping("/gram-panchayats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<Void> renameGp(@RequestBody RenameGp req) {
        service.renameGramPanchayat(req.district(), req.taluk(), req.oldGp(), req.newGp());
        return ApiResponse.ok("Village/GP renamed", null);
    }

    @DeleteMapping("/districts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<Void> deleteDistrict(@RequestParam String name) {
        service.deleteDistrict(name);
        return ApiResponse.ok("District deleted", null);
    }

    @DeleteMapping("/taluks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<Void> deleteTaluk(@RequestParam String district, @RequestParam String taluk) {
        service.deleteTaluk(district, taluk);
        return ApiResponse.ok("Taluk deleted", null);
    }

    @DeleteMapping("/gram-panchayats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<Void> deleteGp(@RequestParam String district, @RequestParam String taluk,
                                      @RequestParam String gramPanchayat) {
        service.deleteGramPanchayat(district, taluk, gramPanchayat);
        return ApiResponse.ok("Village/GP deleted", null);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF')")
    public ApiResponse<Map<String, Integer>> importExcel(@RequestParam("file") MultipartFile file) {
        int added = service.importExcel(file);
        return ApiResponse.ok("Imported " + added + " new location rows", Map.of("added", added));
    }
}
