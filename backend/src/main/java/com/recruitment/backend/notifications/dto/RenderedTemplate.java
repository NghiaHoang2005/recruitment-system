package com.recruitment.backend.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RenderedTemplate {
    private final String subject;
    private final String body;
}
