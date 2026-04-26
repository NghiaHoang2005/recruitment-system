package com.recruitment.backend.domain.dtos;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRecruiterRequest {
    private String fullName;
    private String gender;
    private String phone;
    private String position;
}
