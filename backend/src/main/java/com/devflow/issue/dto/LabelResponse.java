package com.devflow.issue.dto;

import java.util.UUID;

public record LabelResponse(
        UUID id,
        String name,
        String color) {
}
