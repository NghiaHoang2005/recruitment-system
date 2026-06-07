package com.recruitment.backend.mappers;

import com.recruitment.backend.domain.dtos.RecruiterDashboardResponse;
import com.recruitment.backend.domain.entities.Application;
import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.User;
import org.springframework.stereotype.Component;

@Component
public class RecruiterDashboardMapper {
    public RecruiterDashboardResponse.RecentApplicationItem toRecentApplicationItem(Application application) {
        if (application == null) {
            return null;
        }

        Job job = application.getJob();
        Candidate candidate = application.getCandidate();
        Cv cv = application.getCv();
        User candidateUser = candidate != null ? candidate.getUser() : null;

        return RecruiterDashboardResponse.RecentApplicationItem.builder()
                .id(application.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .candidateId(candidate != null ? candidate.getUserId() : null)
                .candidateName(candidate != null ? candidate.getFullName() : null)
                .candidateEmail(candidateUser != null ? candidateUser.getEmail() : null)
                .cvId(cv != null ? cv.getId() : null)
                .cvName(cv != null ? cv.getCvName() : null)
                .status(application.getStatus())
                .aiScore(application.getAiScore())
                .appliedAt(application.getAppliedAt())
                .build();
    }
}
