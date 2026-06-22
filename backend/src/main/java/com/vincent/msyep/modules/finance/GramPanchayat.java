package com.vincent.msyep.modules.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Gram Panchayat → contact email mapping (provided by the client).
 * Selecting a GP in the finance table auto-loads {@code email}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "gram_panchayats")
public class GramPanchayat {

    @Id
    private String id;

    @Indexed
    private String name;

    private String taluk;
    private String district;

    /** Auto-loaded recipient for "Send mail". */
    private String email;

    private String contactPerson;
}
