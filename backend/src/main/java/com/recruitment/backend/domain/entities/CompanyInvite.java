package com.recruitment.backend.domain.entities;

import com.recruitment.backend.domain.enums.CompanyInviteStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "company_invites")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyInvite {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String email;

    @ManyToOne
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CompanyInviteStatus status = CompanyInviteStatus.PENDING;

    private LocalDate sentAt;
}
