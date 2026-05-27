package com.recruitment.backend.domain.entities;

import com.recruitment.backend.domain.enums.EmploymentType;
import com.recruitment.backend.domain.enums.JobLevel;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.domain.enums.WorkMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    @Lob
    private String normalizedText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String parsedData;

    @JdbcTypeCode(SqlTypes.OTHER)
    @Column(columnDefinition = "tsvector")
    private String searchTsv;

    private String workingTime;

    private String location;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    private JobLevel level;

    private Integer minSalary;

    private Integer maxSalary;

    private String currency;

    @Builder.Default
    private Boolean salaryNegotiable = false;

    private Integer headcount;

    private LocalDateTime deadline;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private JobStatus status = JobStatus.DRAFT;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<JobRequirementSection> requirementSections = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
