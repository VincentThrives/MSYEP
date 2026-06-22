package com.vincent.msyep.modules.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One node of the Karnataka administrative hierarchy.
 * Seed rows carry district + taluk (gramPanchayat null); GP rows carry all three.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "locations")
@CompoundIndex(name = "dtg", def = "{'district':1,'taluk':1,'gramPanchayat':1}", unique = true)
public class Location {

    @Id
    private String id;

    @Indexed
    private String district;

    private String taluk;

    private String gramPanchayat;
}
