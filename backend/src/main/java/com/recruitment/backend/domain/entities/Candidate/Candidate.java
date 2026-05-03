package com.recruitment.backend.domain.entities.Candidate;

import com.recruitment.backend.domain.entities.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "candidates")
public class Candidate {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String fullName;
    private String headline;
    private String phoneNumber;
    private String profilePictureUrl;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean openToWork = false;
}
