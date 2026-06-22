package com.vincent.msyep.modules.entrance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** One entrance-test question and its single correct answer. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "question_bank")
public class QuestionBank {

    @Id
    private String id;

    @Indexed(unique = true)
    private String question;

    private String answer;

    /** English / GK / Geography / Science / Math / Computer. */
    private String category;
}
