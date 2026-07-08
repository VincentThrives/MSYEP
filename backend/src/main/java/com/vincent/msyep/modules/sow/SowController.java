package com.vincent.msyep.modules.sow;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.config.security.MsyepPrincipal;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sow")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','ZONE','CENTER')")
public class SowController {

    private final SowService service;

    public SowController(SowService service) {
        this.service = service;
    }

    /** The center whose SOW we operate on: the logged-in center, or ?centerId for admins. */
    private String centerId(MsyepPrincipal p, String requested) {
        if (p != null && "CENTER".equals(p.role()) && StringUtils.hasText(p.centerId())) return p.centerId();
        if (StringUtils.hasText(requested)) return requested;
        if (p != null && StringUtils.hasText(p.centerId())) return p.centerId();
        throw new IllegalArgumentException("No center context — provide centerId");
    }

    @GetMapping("/list")
    public ApiResponse<List<SowSubmission>> list(@RequestParam(required = false) String centerId,
                                                 @AuthenticationPrincipal MsyepPrincipal p) {
        return ApiResponse.ok(service.listForCenter(centerId(p, centerId)));
    }

    @GetMapping("/{programIndex}")
    public ApiResponse<SowSubmission> get(@PathVariable int programIndex,
                                          @RequestParam(required = false) String centerId,
                                          @AuthenticationPrincipal MsyepPrincipal p) {
        return ApiResponse.ok(service.get(centerId(p, centerId), programIndex));
    }

    public record SaveRequest(Map<String, String> fields, Map<String, String> photos) {}

    @PostMapping("/{programIndex}")
    public ApiResponse<SowSubmission> save(@PathVariable int programIndex,
                                           @RequestParam(required = false) String centerId,
                                           @RequestBody SaveRequest req,
                                           @AuthenticationPrincipal MsyepPrincipal p) {
        SowSubmission saved = service.save(centerId(p, centerId), programIndex, req.fields(), req.photos());
        return ApiResponse.ok("Program " + programIndex + " saved", saved);
    }

    /** Download the one-page PDF; also emails it to the center's college mail-id. */
    @PostMapping("/{programIndex}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable int programIndex,
                                                      @RequestParam(required = false) String centerId,
                                                      @AuthenticationPrincipal MsyepPrincipal p) {
        SowService.DownloadResult r = service.downloadAndEmail(centerId(p, centerId), programIndex);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=SOW-Program-" + programIndex + ".pdf")
                .header("X-Sow-Note", r.note())
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(r.pdf()));
    }
}
