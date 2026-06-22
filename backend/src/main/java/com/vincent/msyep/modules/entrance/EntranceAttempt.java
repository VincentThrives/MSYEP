package com.vincent.msyep.modules.entrance;

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

/** A single student's entrance-test attempt (10 random MCQs, 10-minute window). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "entrance_attempts")
public class EntranceAttempt {

    @Id
    private String id;

    @Indexed
    private String studentId;
    private String studentName;

    /** Stored selfie path (proof). */
    private String selfiePath;

    @Builder.Default
    private List<AttemptItem> items = new ArrayList<>();

    private int score;
    @Builder.Default
    private int total = 10;
    private boolean passed;

    /** IN_PROGRESS | SUBMITTED. */
    @Builder.Default
    private String status = "IN_PROGRESS";

    private Instant startedAt;
    private Instant submittedAt;

    /** One served question with its shuffled options and (server-side) correct answer. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttemptItem {
        private String questionId;
        private String question;
        private List<String> options;
        private String correctAnswer;
        private String selectedAnswer;
        private boolean correct;
    }
}
