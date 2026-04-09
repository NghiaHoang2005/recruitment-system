package com.recruitment.backend.controllers;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.recruitment.backend.domain.dtos.Cv.CvUploadCompleteRequest;
import com.recruitment.backend.domain.entities.Cv;
import com.recruitment.backend.domain.entities.CvStatus;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.services.AsyncCvProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileSignatureController {
    private final Cloudinary cloudinary;

    @GetMapping("/generate-signature")
    public ResponseEntity<Map<String, Object>> generateSignature() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Long currentUserId = currentUser.getId();

        long timestamp = Instant.now().getEpochSecond();
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM"));
        String dynamicFolder = "cv_uploads/" + currentMonth;

        Map<String, Object> params = new HashMap<>();
        params.put("timestamp", timestamp);
        params.put("folder", dynamicFolder);
        params.put("type", "authenticated");
        params.put("context", "uploader_id=" + currentUserId);

        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);

        Map<String, Object> response = new HashMap<>();
        response.put("signature", signature);
        response.put("timestamp", timestamp);
        response.put("folder", dynamicFolder);
        response.put("type", "authenticated");
        response.put("apiKey", cloudinary.config.apiKey);
        response.put("cloudName", cloudinary.config.cloudName);

        return ResponseEntity.ok(response);
    }
}
