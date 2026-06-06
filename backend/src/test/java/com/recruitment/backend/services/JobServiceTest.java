package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.JobDTO;
import com.recruitment.backend.domain.entities.Company;
import com.recruitment.backend.domain.entities.CompanyMember;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.CompanyStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.domain.enums.JoinStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.mappers.JobMapper;
import com.recruitment.backend.notifications.services.NotificationFacade;
import com.recruitment.backend.repositories.CompanyMemberRepository;
import com.recruitment.backend.repositories.CompanyRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.repositories.UserRepository;
import com.recruitment.backend.services.ai.pipeline.JobEmbeddingTextBuilder;
import com.recruitment.backend.services.ai.pipeline.TextNormalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {
    @Mock
    private JobRepository jobRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyMemberRepository companyMemberRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private TextNormalizationService textNormalizationService;
    @Mock
    private JobEmbeddingTextBuilder jobEmbeddingTextBuilder;
    @Mock
    private JobAsyncProcessingService jobAsyncProcessingService;
    @Mock
    private JobMapper jobMapper;
    @Mock
    private NotificationFacade notificationFacade;
    @Mock
    private AdminSettingsService adminSettingsService;

    private JobService jobService;
    private User recruiter;

    @BeforeEach
    void setUp() {
        jobService = new JobService(
                jobRepository,
                userRepository,
                companyMemberRepository,
                companyRepository,
                textNormalizationService,
                jobEmbeddingTextBuilder,
                jobAsyncProcessingService,
                jobMapper,
                notificationFacade,
                adminSettingsService
        );
        recruiter = User.builder()
                .id(UUID.randomUUID())
                .email("recruiter@example.com")
                .build();
    }

    @Test
    void verifiedCompanyPublishesJobWhenAutoApproveIsEnabled() {
        Company company = company(CompanyStatus.ACTIVE);
        mockCreateJobDependencies(company);
        when(adminSettingsService.autoApproveJobsFromVerifiedCompanies()).thenReturn(true);
        when(adminSettingsService.requireAdminApprovalForAllJobs()).thenReturn(false);

        JobDTO result = jobService.createJob(request(), recruiter.getEmail());

        assertThat(result.getStatus()).isEqualTo(JobStatus.PUBLISHED);
    }

    @Test
    void unverifiedCompanyCreatesPendingJob() {
        Company company = company(CompanyStatus.PENDING);
        mockCreateJobDependencies(company);

        JobDTO result = jobService.createJob(request(), recruiter.getEmail());

        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void candidateJobListOnlyReturnsPublishedJobs() {
        Job published = Job.builder().id(UUID.randomUUID()).status(JobStatus.PUBLISHED).build();
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(User.builder()
                .id(UUID.randomUUID())
                .email("candidate@example.com")
                .build()));
        when(jobRepository.findByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED)).thenReturn(List.of(published));
        when(jobMapper.toDto(published)).thenReturn(JobDTO.builder()
                .id(published.getId())
                .status(JobStatus.PUBLISHED)
                .build());

        List<JobDTO> result = jobService.getJobsForUser("candidate@example.com", List.of("ROLE_CANDIDATE"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(JobStatus.PUBLISHED);
    }

    @Test
    void candidateCannotViewRejectedOrFlaggedJobDetail() {
        for (JobStatus status : List.of(JobStatus.REJECTED, JobStatus.FLAGGED)) {
            UUID jobId = UUID.randomUUID();
            Job job = Job.builder().id(jobId).status(status).build();
            when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> jobService.getJobById(jobId, "candidate@example.com", List.of("ROLE_CANDIDATE")))
                    .isInstanceOf(AppException.class);
        }
    }

    @Test
    void approvedCompanyMemberCanViewPrivateCompanyJobDetail() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .email("owner@example.com")
                .build();
        Company company = company(CompanyStatus.ACTIVE);
        UUID jobId = UUID.randomUUID();
        Job pendingJob = Job.builder()
                .id(jobId)
                .status(JobStatus.PENDING)
                .company(company)
                .recruiter(recruiter)
                .build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(pendingJob));
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(companyMemberRepository.existsByCompany_IdAndUser_IdAndJoinStatus(company.getId(), owner.getId(), JoinStatus.APPROVED))
                .thenReturn(true);
        when(jobMapper.toDto(pendingJob)).thenReturn(JobDTO.builder()
                .id(jobId)
                .status(JobStatus.PENDING)
                .build());

        JobDTO result = jobService.getJobById(jobId, owner.getEmail(), List.of("ROLE_RECRUITER"));

        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void recruiterListShowsAllCompanyJobsForApprovedMember() {
        Company company = company(CompanyStatus.ACTIVE);
        Job teammateJob = Job.builder()
                .id(UUID.randomUUID())
                .status(JobStatus.PENDING)
                .company(company)
                .recruiter(User.builder().id(UUID.randomUUID()).email("teammate@example.com").build())
                .build();
        when(userRepository.findByEmail(recruiter.getEmail())).thenReturn(Optional.of(recruiter));
        when(companyMemberRepository.findFirstByUser_IdAndJoinStatus(recruiter.getId(), JoinStatus.APPROVED))
                .thenReturn(Optional.of(CompanyMember.builder().company(company).user(recruiter).build()));
        when(jobRepository.findByCompany_IdOrderByCreatedAtDesc(company.getId())).thenReturn(List.of(teammateJob));
        when(jobMapper.toDto(teammateJob)).thenReturn(JobDTO.builder()
                .id(teammateJob.getId())
                .status(JobStatus.PENDING)
                .build());

        List<JobDTO> result = jobService.getJobsForUser(recruiter.getEmail(), List.of("ROLE_RECRUITER"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(teammateJob.getId());
    }

    private void mockCreateJobDependencies(Company company) {
        when(userRepository.findByEmail(recruiter.getEmail())).thenReturn(Optional.of(recruiter));
        when(companyMemberRepository.findFirstByUser_IdAndJoinStatus(recruiter.getId(), JoinStatus.APPROVED))
                .thenReturn(Optional.of(CompanyMember.builder().company(company).user(recruiter).build()));
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
        when(jobEmbeddingTextBuilder.buildEmbeddingText(any(Job.class))).thenReturn("job text");
        when(textNormalizationService.normalize("job text")).thenReturn("job text");
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(UUID.randomUUID());
            }
            return job;
        });
        when(jobMapper.toDto(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            return JobDTO.builder()
                    .id(job.getId())
                    .status(job.getStatus())
                    .build();
        });
    }

    private Company company(CompanyStatus status) {
        return Company.builder()
                .id(UUID.randomUUID())
                .name("Acme")
                .status(status)
                .build();
    }

    private JobDTO request() {
        return JobDTO.builder()
                .title("Java Developer")
                .description("Build APIs")
                .build();
    }
}
