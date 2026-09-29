package com.retrobolsa.api.game.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SubmitQuizRequestDto {

    @NotEmpty
    @Valid
    private List<Answer> answers;

    @Getter
    @Setter
    public static class Answer {
        @NotNull
        private UUID questionId;
        @NotNull
        private UUID optionId;
    }
}
