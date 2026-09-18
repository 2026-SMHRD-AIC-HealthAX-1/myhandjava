package com.smhrd.hometraining.crew.dto;

import jakarta.validation.constraints.NotBlank;

public record CrewCreateRequest(
        @NotBlank String name,
        String description,
        @NotBlank String concept
) {}
