package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.notifications.dto.requests.ApplicationResultNotificationRequest;
import com.recruitment.backend.notifications.dto.requests.ApplicationSubmittedNotificationRequest;
import com.recruitment.backend.notifications.dto.requests.InviteToApplyNotificationRequest;
import com.recruitment.backend.notifications.dto.requests.JobMatchNotificationRequest;
import com.recruitment.backend.notifications.services.NotificationFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationFacade notificationFacade;

    @PostMapping("/application-submitted")
    public ResponseEntity<ApiResponse<String>> notifyApplicationSubmitted(@RequestBody @Valid ApplicationSubmittedNotificationRequest request) {
        notificationFacade.notifyApplicationSubmitted(
                request.getEmail(),
                request.getCandidateName(),
                request.getJobTitle(),
                request.getIdempotencyKey()
        );
        return ResponseEntity.ok(ApiResponse.success("Application submitted notification queued"));
    }

    @PostMapping("/application-result")
    public ResponseEntity<ApiResponse<String>> notifyApplicationResult(@RequestBody @Valid ApplicationResultNotificationRequest request) {
        notificationFacade.notifyApplicationResult(
                request.getEmail(),
                request.getCandidateName(),
                request.getJobTitle(),
                request.getPassed(),
                request.getFeedback(),
                request.getIdempotencyKey()
        );
        return ResponseEntity.ok(ApiResponse.success("Application result notification queued"));
    }

    @PostMapping("/job-match")
    public ResponseEntity<ApiResponse<String>> notifyJobMatch(@RequestBody @Valid JobMatchNotificationRequest request) {
        notificationFacade.notifyJobMatch(
                request.getEmail(),
                request.getCandidateName(),
                request.getMatchedJobs(),
                request.getIdempotencyKey()
        );
        return ResponseEntity.ok(ApiResponse.success("Job match notification queued"));
    }

    @PostMapping("/invite-to-apply")
    public ResponseEntity<ApiResponse<String>> notifyInviteToApply(@RequestBody @Valid InviteToApplyNotificationRequest request) {
        notificationFacade.notifyInviteToApply(
                request.getEmail(),
                request.getCandidateName(),
                request.getCompanyName(),
                request.getJobTitle(),
                request.getJobUrl(),
                request.getIdempotencyKey()
        );
        return ResponseEntity.ok(ApiResponse.success("Invite to apply notification queued"));
    }

    @GetMapping("/invite-to-apply/status")
    public ResponseEntity<ApiResponse<Boolean>> checkInviteStatus(@RequestParam String jobId, @RequestParam String candidateId) {
        boolean hasInvited = notificationFacade.hasInvitedToApply(jobId, candidateId);
        return ResponseEntity.ok(ApiResponse.success(hasInvited));
    }
}
