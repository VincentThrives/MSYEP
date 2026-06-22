package com.vincent.msyep.modules.entrance.dto;

import java.util.List;
import java.util.Map;

public class EntranceDtos {

    /** A question as served to the candidate — no correct answer revealed. */
    public record QuestionView(String questionId, String question, List<String> options) {}

    public record StartResponse(String attemptId, int durationMinutes, String startedAt,
                                List<QuestionView> questions) {}

    public record SubmitRequest(Map<String, String> answers) {}

    /** One graded row returned after submit. */
    public record ResultItem(String question, List<String> options, String correctAnswer,
                             String selectedAnswer, boolean correct) {}

    public record ResultResponse(String attemptId, int score, int total, boolean passed,
                                 List<ResultItem> items) {}
}
