package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.*;
import com.recruitment.backend.repositories.JobCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-categories")
@RequiredArgsConstructor
public class JobCategoryController {
    private final JobCategoryRepository jobCategoryRepository;

    @GetMapping
    public ApiResponse<List<JobCategoryDTO>> getCategories() {
        return ApiResponse.success(jobCategoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(category -> JobCategoryDTO.builder()
                        .code(category.getCode())
                        .name(category.getName())
                        .build())
                .toList());
    }
}
