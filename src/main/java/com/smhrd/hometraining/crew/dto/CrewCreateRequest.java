package com.smhrd.hometraining.crew.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CrewCreateRequest(
        @NotBlank String name,
        String description,
        @NotEmpty @Size(max = 3) List<String> concepts
) {}
