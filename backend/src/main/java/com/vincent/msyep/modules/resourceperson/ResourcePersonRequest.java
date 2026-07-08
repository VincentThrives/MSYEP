package com.vincent.msyep.modules.resourceperson;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A center's request for guest resource persons (up to 10). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "resource_person_requests")
public class ResourcePersonRequest {

    @Id
    private String id;

    @Indexed(unique = true)
    private String centerId;

    private int countRequired;

    @Builder.Default
    private List<Person> persons = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String organization;
        private String name;
        private String designation;
        private String phone;
    }
}
