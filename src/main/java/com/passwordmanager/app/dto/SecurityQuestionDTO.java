package com.passwordmanager.app.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class SecurityQuestionDTO {

    private Long id;

    @NotBlank(message = "Question is required")
    private String questionText;

    @NotBlank(message = "Answer is required")
    private String answer;
}
