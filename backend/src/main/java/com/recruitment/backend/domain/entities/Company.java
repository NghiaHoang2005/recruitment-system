package com.recruitment.backend.domain.entities;

import com.recruitment.backend.domain.enums.CompanyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Table(name = "companies")
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String website;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    private String description;

    @Column(nullable = false)
    private String industry;

    @Column(nullable = false)
    private int companySize;

    @Column(nullable = false)
    private String taxCode;

    @Column(nullable = false)
    private String businessLicense;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private CompanyStatus status = CompanyStatus.PENDING;

}
