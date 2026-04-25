package com.recruitment.backend.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRecruiterResponse {
    private String fullName;
    private String gender;
    private String phone;
    private String position;
}
