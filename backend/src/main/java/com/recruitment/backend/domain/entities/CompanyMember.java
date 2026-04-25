package com.recruitment.backend.domain.entities;

import com.recruitment.backend.domain.enums.CompanyRole;
import com.recruitment.backend.domain.enums.JoinStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "company_members")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private JoinStatus joinStatus = JoinStatus.PENDING;

    @Enumerated(value = EnumType.STRING)
    private CompanyRole role;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDate requestedAt;

    private LocalDate reviewedAt;
}
