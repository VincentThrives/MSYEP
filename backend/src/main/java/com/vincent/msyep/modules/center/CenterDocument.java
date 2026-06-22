package com.vincent.msyep.modules.center;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** An uploaded center document (board photo, kit photo, MOU copy, etc.). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CenterDocument {
    /** Slot key: buildingBoard, kitReceived, requisitionSigned, semInfoCopy, mouSigned, entranceTest. */
    private String type;
    /** Human label of the slot. */
    private String label;
    private String filename;
    private long size;
    /** Stored relative path on disk. */
    private String path;
}
