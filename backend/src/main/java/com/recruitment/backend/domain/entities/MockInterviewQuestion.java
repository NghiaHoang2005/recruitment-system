package com.recruitment.backend.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mock_interview_questions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "sequence_number"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private MockInterviewSession session;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "question_type", nullable = false, length = 30)
    private String questionType;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(length = 100)
    private String competency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_topics", columnDefinition = "jsonb")
    private String expectedTopics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rubric", columnDefinition = "jsonb")
    private String rubric;

    @Column(name = "is_follow_up", nullable = false)
    private boolean followUp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id")
    private MockInterviewQuestion parentQuestion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
