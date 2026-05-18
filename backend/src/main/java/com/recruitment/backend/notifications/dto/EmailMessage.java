package com.recruitment.backend.notifications.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailMessage {
    private String to;
    private String subject;
    private String body;
}
