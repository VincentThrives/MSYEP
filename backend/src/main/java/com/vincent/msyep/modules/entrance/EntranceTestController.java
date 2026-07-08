package com.vincent.msyep.modules.entrance;

import com.vincent.msyep.common.ApiResponse;
import com.vincent.msyep.modules.entrance.dto.EntranceDtos.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/entrance-test")
public class EntranceTestController {

    private final EntranceTestService service;

    public EntranceTestController(EntranceTestService service) {
        this.service = service;
    }

    /** Start a test — requires a selfie. Returns 10 random MCQs and the 10-minute window. */
    @PostMapping(value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StartResponse> start(@RequestParam String studentId,
                                            @RequestParam("selfie") MultipartFile selfie) {
        return ApiResponse.ok(service.start(studentId, selfie));
    }

    @PostMapping("/{attemptId}/submit")
    public ApiResponse<ResultResponse> submit(@PathVariable String attemptId,
                                              @RequestBody SubmitRequest req) {
        return ApiResponse.ok("Test submitted", service.submit(attemptId, req.answers()));
    }

    @GetMapping("/attempts")
    public ApiResponse<List<EntranceAttempt>> attempts(@RequestParam String studentId) {
        return ApiResponse.ok(service.attemptsFor(studentId));
    }

    /** The student's completed result (data is null if they haven't taken it yet). */
    @GetMapping("/result")
    public ApiResponse<ResultResponse> result(@RequestParam String studentId) {
        return ApiResponse.ok(service.latestResult(studentId));
    }

    @GetMapping("/{attemptId}/result-pdf")
    public ResponseEntity<ByteArrayResource> resultPdf(@PathVariable String attemptId) {
        byte[] pdf = service.resultPdf(attemptId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=entrance-result-" + attemptId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }
}
