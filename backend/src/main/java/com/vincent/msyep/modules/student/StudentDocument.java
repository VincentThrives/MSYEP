package com.vincent.msyep.modules.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** An uploaded student document (photo, Aadhaar, PAN, marks cards, fee receipt, caste cert). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDocument {
    private String type;
    private String label;
    private String filename;
    private long size;
    /** Stored relative path on disk. */
    private String path;
}
