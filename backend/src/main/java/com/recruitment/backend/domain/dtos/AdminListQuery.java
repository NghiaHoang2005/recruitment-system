package com.recruitment.backend.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminListQuery {
    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    private String keyword;
    private String status;
    private String sort;
    private LocalDate fromDate;
    private LocalDate toDate;
}
