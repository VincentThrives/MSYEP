package com.vincent.msyep.common;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Atomic named sequence (e.g. "center") used to mint unique business numbers. */
@Data
@Document(collection = "counters")
public class Counter {
    @Id
    private String id;
    private long seq;
}
