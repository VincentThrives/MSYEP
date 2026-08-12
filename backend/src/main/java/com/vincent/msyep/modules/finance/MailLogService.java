package com.vincent.msyep.modules.finance;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Persists mail-log entries together with their openable PDF attachments (shared by all wings). */
@Service
public class MailLogService {

    private final MailLogRepository logs;
    private final MailAttachmentRepository attachments;

    public MailLogService(MailLogRepository logs, MailAttachmentRepository attachments) {
        this.logs = logs;
        this.attachments = attachments;
    }

    /** One attached file: display name + PDF bytes. */
    public record Att(String name, byte[] data) {}

    /** Save the log and its attachments; fills {@code attachmentNames} from the files (order preserved). */
    public void save(MailLog log, List<Att> files) {
        if (files == null) files = List.of();
        List<String> names = new ArrayList<>();
        for (Att a : files) names.add(a.name());
        log.setAttachmentNames(names);
        MailLog saved = logs.save(log);
        for (int i = 0; i < files.size(); i++) {
            Att a = files.get(i);
            if (a.data() != null && a.data().length > 0) {
                attachments.save(MailAttachment.builder()
                        .logId(saved.getId()).index(i).name(a.name()).data(a.data()).build());
            }
        }
    }

    public List<MailLog> history(String channel) {
        return logs.findTop100ByChannelOrderBySentAtDesc(channel);
    }

    public MailAttachment attachment(String logId, int index) {
        return attachments.findByLogIdAndIndex(logId, index).orElse(null);
    }

    /** Delete a history entry and its stored attachments. */
    public void delete(String logId) {
        attachments.deleteByLogId(logId);
        logs.deleteById(logId);
    }
}
