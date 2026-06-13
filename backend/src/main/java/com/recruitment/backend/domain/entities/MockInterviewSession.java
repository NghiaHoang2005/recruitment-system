package com.recruitment.backend.domain.entities;

import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.enums.MockInterviewStatus;
import com.recruitment.backend.domain.enums.MockInterviewType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mock_interview_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false, length = 20)
    private MockInterviewType interviewType;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "planned_duration_minutes", nullable = false)
    private Integer plannedDurationMinutes;

    @Column(name = "soft_limit_seconds", nullable = false)
    private Integer softLimitSeconds;

    @Column(name = "hard_limit_seconds", nullable = false)
    private Integer hardLimitSeconds;

    @Column(name = "actual_duration_seconds")
    private Integer actualDurationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MockInterviewStatus status;

    @Column(name = "prompt_version", length = 100)
    private String promptVersion;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    @Builder.Default
    private List<MockInterviewQuestion> questions = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    @Builder.Default
    private List<MockInterviewTurn> turns = new ArrayList<>();

    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private MockInterviewFeedback feedback;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
