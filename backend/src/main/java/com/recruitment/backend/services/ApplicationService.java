package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.ApplicationRequest;
import com.recruitment.backend.domain.dtos.ApplicationResponse;
import com.recruitment.backend.domain.entities.*;
import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.enums.*;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.notifications.services.NotificationFacade;
import com.recruitment.backend.repositories.*;
import com.recruitment.backend.utils.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final CvRepository cvRepository;
    private final JobRepository jobRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final JobMatchService jobMatchService;
    private final NotificationFacade notificationFacade;
    private final SecurityUtil securityUtil;

    @Transactional
    public ApplicationResponse apply(ApplicationRequest request) {
        User currentUser = securityUtil.getCurrentUser();
        Candidate candidate = candidateRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));

        if (request.getJobId() == null || request.getCvId() == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Cv cv = cvRepository.findByIdAndCandidateUserId(request.getCvId(), candidate.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));

        if (applicationRepository.existsByJob_IdAndCandidate_UserIdAndStatusNot(job.getId(), candidate.getUserId(), ApplicationStatus.WITHDRAWN)) {
            throw new AppException(ErrorCode.APPLICATION_ALREADY_EXISTS);
        }

        Integer aiScore = null;
        try {
            JobMatchService.MatchScore score = jobMatchService.matchJob(candidate.getUserId(), job.getId(), cv.getId());
            aiScore = score.getFitScore();
        } catch (Exception ex) {
            log.warn("Could not calculate application AI score. candidate={}, job={}, cv={}, error={}",
                    candidate.getUserId(), job.getId(), cv.getId(), ex.getMessage());
        }

        Application application = Application.builder()
                .job(job)
                .candidate(candidate)
                .cv(cv)
                .status(ApplicationStatus.APPLIED)
                .aiScore(aiScore)
                .coverLetter(request.getCoverLetter())
                .build();

        Application saved = applicationRepository.save(application);
        try {
            notificationFacade.notifyApplicationSubmitted(
                    currentUser.getEmail(),
                    candidate.getFullName() == null ? currentUser.getEmail() : candidate.getFullName(),
                    job.getTitle(),
                    "application-submitted:" + saved.getId()
            );
        } catch (Exception ex) {
            log.warn("Could not enqueue application submitted notification for application {}: {}", saved.getId(), ex.getMessage());
        }

        return toResponse(saved);
    }

    public List<ApplicationResponse> getMyApplications() {
        User currentUser = securityUtil.getCurrentUser();
        return applicationRepository.findByCandidate_UserIdOrderByAppliedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ApplicationResponse getMyApplication(UUID applicationId) {
        User currentUser = securityUtil.getCurrentUser();
        return applicationRepository.findByIdAndCandidate_UserId(applicationId, currentUser.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    @Transactional
    public ApplicationResponse withdraw(UUID applicationId) {
        User currentUser = securityUtil.getCurrentUser();
        Application application = applicationRepository.findByIdAndCandidate_UserId(applicationId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        if (application.getStatus() == ApplicationStatus.HIRED || application.getStatus() == ApplicationStatus.REJECTED) {
            throw new AppException(ErrorCode.APPLICATION_INVALID_STATUS);
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        return toResponse(applicationRepository.save(application));
    }

    public List<ApplicationResponse> getRecruiterApplications() {
        Company company = getRecruiterCompany();
        return applicationRepository.findByJob_Company_IdOrderByAppliedAtDesc(company.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ApplicationResponse> getRecruiterJobApplications(UUID jobId) {
        Company company = getRecruiterCompany();
        return applicationRepository.findByJob_IdAndJob_Company_IdOrderByAppliedAtDesc(jobId, company.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ApplicationResponse getRecruiterApplication(UUID applicationId) {
        Company company = getRecruiterCompany();
        return applicationRepository.findByIdAndJob_Company_Id(applicationId, company.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    @Transactional
    public ApplicationResponse updateStatus(UUID applicationId, ApplicationStatus status) {
        if (status == null || status == ApplicationStatus.WITHDRAWN) {
            throw new AppException(ErrorCode.APPLICATION_INVALID_STATUS);
        }

        User currentUser = securityUtil.getCurrentUser();
        Company company = getRecruiterCompany(currentUser);
        Application application = applicationRepository.findByIdAndJob_Company_Id(applicationId, company.getId())
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        application.setStatus(status);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(currentUser);
        Application saved = applicationRepository.save(application);

        if (status == ApplicationStatus.REJECTED || status == ApplicationStatus.HIRED) {
            try {
                notificationFacade.notifyApplicationResult(
                        saved.getCandidate().getUser().getEmail(),
                        saved.getCandidate().getFullName() == null ? saved.getCandidate().getUser().getEmail() : saved.getCandidate().getFullName(),
                        saved.getJob().getTitle(),
                        status == ApplicationStatus.HIRED,
                        "",
                        "application-result:" + saved.getId() + ":" + status
                );
            } catch (Exception ex) {
                log.warn("Could not enqueue application result notification for application {}: {}", saved.getId(), ex.getMessage());
            }
        }

        return toResponse(saved);
    }

    private Company getRecruiterCompany() {
        return getRecruiterCompany(securityUtil.getCurrentUser());
    }

    private Company getRecruiterCompany(User user) {
        CompanyMember membership = companyMemberRepository.findFirstByUser_IdAndJoinStatus(user.getId(), JoinStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_MEMBER_NOT_FOUND));
        return membership.getCompany();
    }

    private ApplicationResponse toResponse(Application application) {
        Job job = application.getJob();
        Candidate candidate = application.getCandidate();
        Cv cv = application.getCv();
        Company company = job.getCompany();
        User user = candidate.getUser();

        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .companyId(company != null ? company.getId() : null)
                .companyName(company != null ? company.getName() : null)
                .candidateId(candidate.getUserId())
                .candidateName(candidate.getFullName())
                .candidateEmail(user != null ? user.getEmail() : null)
                .candidatePhone(candidate.getPhoneNumber())
                .cvId(cv.getId())
                .cvName(cv.getCvName())
                .cvUrl(cv.getFileUrl())
                .status(application.getStatus())
                .aiScore(application.getAiScore())
                .coverLetter(application.getCoverLetter())
                .appliedAt(application.getAppliedAt())
                .updatedAt(application.getUpdatedAt())
                .reviewedAt(application.getReviewedAt())
                .reviewedBy(application.getReviewedBy() != null ? application.getReviewedBy().getId() : null)
                .build();
    }
}
