package com.vincent.msyep.modules.finance;

import com.vincent.msyep.common.exception.ResourceNotFoundException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.vincent.msyep.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves the actual PDF attached to a Sent-Mail history entry (any wing) so it opens on click. */
@RestController
@RequestMapping("/api/v1/mail")
public class MailController {

    private final MailLogService mailLog;

    public MailController(MailLogService mailLog) {
        this.mailLog = mailLog;
    }

    @GetMapping("/{logId}/attachment/{index}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','FINANCE')")
    public ResponseEntity<ByteArrayResource> attachment(@PathVariable String logId, @PathVariable int index) {
        MailAttachment a = mailLog.attachment(logId, index);
        if (a == null || a.getData() == null) {
            throw new ResourceNotFoundException("Attachment not found");
        }
        String name = a.getName() == null ? "attachment.pdf" : a.getName().replaceAll("[^A-Za-z0-9._ -]", "_") + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + name)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(a.getData()));
    }

    /** Delete a sent-mail history entry and its stored attachments (any wing). */
    @DeleteMapping("/{logId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','STAFF','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable String logId) {
        mailLog.delete(logId);
        return ApiResponse.ok("Deleted", null);
    }
}
