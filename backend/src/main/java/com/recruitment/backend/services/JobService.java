package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.JobDTO;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.mappers.JobMapper;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobMapper jobMapper;

    public JobDTO createJob(JobDTO dto, String userEmail) {
        User recruiter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Job job = Job.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .requirements(dto.getRequirements())
                .location(dto.getLocation())
                .salaryRange(dto.getSalaryRange())
//                .companyName(dto.getCompanyName())
                .recruiter(recruiter)
                .build();

        Job savedJob = jobRepository.save(job);
        return jobMapper.toDto(savedJob);
    }

    public List<JobDTO> getAllJobs() {
        return jobRepository.findAll().stream().map(jobMapper::toDto).collect(Collectors.toList());
    }

    public JobDTO getJobById(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        return jobMapper.toDto(job);
    }
}
