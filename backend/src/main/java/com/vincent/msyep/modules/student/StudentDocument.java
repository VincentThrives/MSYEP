package com.vincent.msyep.modules.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A document attached to a student (FW/finance docs, certificates, etc.). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDocument {
    private String label;
    /** Storage path or URL where the file lives. */
    private String url;
    private String type;
}
