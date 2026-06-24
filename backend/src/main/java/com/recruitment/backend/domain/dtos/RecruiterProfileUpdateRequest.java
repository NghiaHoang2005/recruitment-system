package com.recruitment.backend.domain.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecruiterProfileUpdateRequest {
    private String fullName;
    private String phoneNumber;
    private String headline;
}
