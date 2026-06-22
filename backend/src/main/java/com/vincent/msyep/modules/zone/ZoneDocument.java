package com.vincent.msyep.modules.zone;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** An uploaded franchise/zone document (Aadhaar, PAN, bank, logo, org copies, building copy). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneDocument {
    private String type;
    private String label;
    private String filename;
    private long size;
    private String path;
}
