package com.example.twitstock.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request model for the analyse endpoint.
 *
 * <p>Bound from form data or JSON body by Spring MVC / Thymeleaf.
 */
@Data
public class AnalyzeRequest {

    /**
     * Twitter handle to analyse.
     * May or may not include a leading {@code @} — the controller strips it.
     */
    @NotBlank(message = "Username must not be blank")
    private String username;
}
