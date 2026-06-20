package com.skillnet.api.dto.student;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QuizAnswerSubmissionDTO {

    private String questionId;

    private Integer optionIndex;

    private String textAnswer;

    private List<String> matchingAnswers = new ArrayList<>();
}
