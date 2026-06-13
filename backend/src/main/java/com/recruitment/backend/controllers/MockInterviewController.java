package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.mockinterview.MockInterviewDtos.*;
import com.recruitment.backend.services.mockinterview.MockInterviewService;
import com.recruitment.backend.services.mockinterview.GeminiLiveTokenService;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidate/mock-interviews")
@RequiredArgsConstructor
public class MockInterviewController {
    private final MockInterviewService mockInterviewService;
    private final GeminiLiveTokenService geminiLiveTokenService;

    @PostMapping("/live-token")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<GeminiLiveTokenService.LiveToken>> liveToken() {
        return ResponseEntity.ok(ApiResponse.success(geminiLiveTokenService.createToken()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SessionResponse>> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(mockInterviewService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(mockInterviewService.list()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mockInterviewService.get(id)));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<SessionResponse>> start(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mockInterviewService.start(id)));
    }

    @PostMapping("/{id}/turns")
    public ResponseEntity<ApiResponse<List<TurnResponse>>> appendTurns(
            @PathVariable UUID id,
            @Valid @RequestBody AppendTurnsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(mockInterviewService.appendTurns(id, request)));
    }

    @PostMapping("/{id}/follow-ups")
    public ResponseEntity<ApiResponse<QuestionResponse>> createFollowUp(
            @PathVariable UUID id,
            @Valid @RequestBody FollowUpRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(mockInterviewService.createFollowUp(id, request)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<ResultResponse>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mockInterviewService.complete(id)));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<ApiResponse<ResultResponse>> result(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mockInterviewService.getResult(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        mockInterviewService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
