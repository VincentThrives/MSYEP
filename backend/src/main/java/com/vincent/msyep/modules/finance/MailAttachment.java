package com.vincent.msyep.modules.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** The actual PDF bytes attached to one {@link MailLog}, openable from the Sent Mail history. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "mail_attachments")
public class MailAttachment {

    @Id
    private String id;

    private String logId;   // owning MailLog
    private int index;       // position within the mail (matches MailLog.attachmentNames)
    private String name;     // display filename

    @JsonIgnore
    private byte[] data;      // the PDF bytes (never serialized in listings)
}
