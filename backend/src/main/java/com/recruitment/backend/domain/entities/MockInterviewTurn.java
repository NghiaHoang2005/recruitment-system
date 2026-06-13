package com.recruitment.backend.domain.entities;

import com.recruitment.backend.domain.enums.InterviewSpeaker;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mock_interview_turns",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"session_id", "sequence_number"}),
                @UniqueConstraint(columnNames = {"session_id", "client_event_id"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewTurn {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private MockInterviewSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private MockInterviewQuestion question;

    @Column(name = "client_event_id", nullable = false)
    private UUID clientEventId;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewSpeaker speaker;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "started_offset_ms")
    private Integer startedOffsetMs;

    @Column(name = "ended_offset_ms")
    private Integer endedOffsetMs;

    @Column(name = "is_final", nullable = false)
    private boolean finalTurn;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
