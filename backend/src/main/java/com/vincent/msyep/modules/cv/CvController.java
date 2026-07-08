package com.vincent.msyep.modules.cv;

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

@RestController
@RequestMapping("/api/v1/cv")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STUDENT')")
public class CvController {

    private final CvService service;

    public CvController(CvService service) {
        this.service = service;
    }

    /** The student this request acts on: the logged-in student, or ?studentId for admins. */
    private String studentId(MsyepPrincipal p, String requested) {
        if (p != null && "STUDENT".equals(p.role()) && StringUtils.hasText(p.studentId())) return p.studentId();
        if (StringUtils.hasText(requested)) return requested;
        if (p != null && StringUtils.hasText(p.studentId())) return p.studentId();
        throw new IllegalArgumentException("No student context — provide studentId");
    }

    @GetMapping("/status")
    public ApiResponse<CvService.StatusView> status(@RequestParam(required = false) String studentId,
                                                    @AuthenticationPrincipal MsyepPrincipal p) {
        return ApiResponse.ok(service.status(studentId(p, studentId)));
    }

    @PostMapping("/order")
    public ApiResponse<CvService.OrderView> order(@RequestParam(required = false) String studentId,
                                                  @AuthenticationPrincipal MsyepPrincipal p) {
        return ApiResponse.ok(service.createOrder(studentId(p, studentId)));
    }

    public record VerifyRequest(String orderId, String paymentId, String signature) {}

    @PostMapping("/verify")
    public ApiResponse<Boolean> verify(@RequestParam(required = false) String studentId,
                                       @RequestBody VerifyRequest req,
                                       @AuthenticationPrincipal MsyepPrincipal p) {
        service.verify(studentId(p, studentId), req.orderId(), req.paymentId(), req.signature());
        return ApiResponse.ok("Payment verified", true);
    }

    @GetMapping("/download")
    public ResponseEntity<ByteArrayResource> download(@RequestParam(required = false) String studentId,
                                                      @AuthenticationPrincipal MsyepPrincipal p) {
        String sid = studentId(p, studentId);
        if (!service.isPaid(sid)) {
            return ResponseEntity.status(402).build(); // Payment Required
        }
        byte[] pdf = service.buildCv(sid);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=MSYEP-Resume.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }
}
