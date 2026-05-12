package com.recruitment.backend.services.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class SupabaseStorageService {
    private static final String JSON_CONTENT_TYPE = "application/json";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucket;
    private final int signedUrlTtlSeconds;

    public SupabaseStorageService(
            ObjectMapper objectMapper,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.bucket}") String bucket,
            @Value("${supabase.signed-url-ttl-seconds:7200}") int signedUrlTtlSeconds
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
        this.supabaseUrl = normalizeBaseUrl(supabaseUrl);
        this.serviceRoleKey = serviceRoleKey;
        this.bucket = bucket;
        this.signedUrlTtlSeconds = signedUrlTtlSeconds;
    }

    public String uploadCv(MultipartFile file, String folder) throws IOException {
        String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
        String fileName = folder + "/" + UUID.randomUUID() + "_" + sanitizedFilename;
        String objectPath = normalizeObjectPath(fileName);
        String url = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, encodeObjectPath(objectPath));

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .header("Content-Type", file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                .build();

        HttpResponse<String> response = sendRequest(request);
        log.info("Supabase upload response: status={}, body={}", response.statusCode(), response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AppException(ErrorCode.CV_PROCESSING_FAILED);
        }

        return fileName;
    }

    public String getPresignedUrl(String fileName) {
        String objectPath = normalizeObjectPath(fileName);
        String url = String.format("%s/storage/v1/object/sign/%s/%s", supabaseUrl, bucket, encodeObjectPath(objectPath));

        String payload;
        try {
            payload = objectMapper.writeValueAsString(new SignedUrlRequest(signedUrlTtlSeconds));
        } catch (Exception e) {
            throw new AppException(ErrorCode.PRESIGNED_URL_FAILED);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .header("Content-Type", JSON_CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = sendRequest(request);
        if (response.statusCode() == 404) {
            log.warn("Supabase signed URL not found: status={}, body={}", response.statusCode(), response.body());
            throw new AppException(ErrorCode.URL_NOT_FOUND);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Supabase signed URL failed: status={}, body={}", response.statusCode(), response.body());
            throw new AppException(ErrorCode.PRESIGNED_URL_FAILED);
        }

        try {
            JsonNode node = objectMapper.readTree(response.body());
            String signedUrl = node.path("signedURL").asText(null);
            if (signedUrl == null || signedUrl.isBlank()) {
                throw new AppException(ErrorCode.PRESIGNED_URL_FAILED);
            }

            if (signedUrl.startsWith("http")) {
                return signedUrl;
            }
            if (signedUrl.startsWith("/storage/")) {
                return supabaseUrl + signedUrl;
            }
            if (signedUrl.startsWith("/object/")) {
                return supabaseUrl + "/storage/v1" + signedUrl;
            }

            return supabaseUrl + "/storage/v1/" + signedUrl;
        } catch (Exception e) {
            throw new AppException(ErrorCode.PRESIGNED_URL_FAILED);
        }
    }

    public void deleteFile(String fileName) {
        String objectPath = normalizeObjectPath(fileName);
        String url = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, encodeObjectPath(objectPath));

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .DELETE()
                .build();

        HttpResponse<String> response = sendRequest(request);
        if (response.statusCode() == 404) {
            return;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AppException(ErrorCode.CV_PROCESSING_FAILED);
        }
    }

    private HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new AppException(ErrorCode.CV_PROCESSING_FAILED);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }

        String sanitized = filename.trim().replace("\\\\", "/");
        int lastSlash = sanitized.lastIndexOf('/');
        if (lastSlash >= 0) {
            sanitized = sanitized.substring(lastSlash + 1);
        }
        sanitized = sanitized.replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.isBlank()) {
            return "file";
        }

        return sanitized;
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }

        String trimmed = url.trim();
        if (trimmed.endsWith("/rest/v1")) {
            trimmed = trimmed.substring(0, trimmed.length() - "/rest/v1".length());
        } else if (trimmed.endsWith("/rest/v1/")) {
            trimmed = trimmed.substring(0, trimmed.length() - "/rest/v1/".length());
        } else if (trimmed.endsWith("/storage/v1")) {
            trimmed = trimmed.substring(0, trimmed.length() - "/storage/v1".length());
        } else if (trimmed.endsWith("/storage/v1/")) {
            trimmed = trimmed.substring(0, trimmed.length() - "/storage/v1/".length());
        }
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }

    private String normalizeObjectPath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        String normalized = fileName.trim().replace("\\\\", "/");
        if (normalized.startsWith("http")) {
            String marker = "/storage/v1/object/";
            int markerIndex = normalized.indexOf(marker);
            if (markerIndex >= 0) {
                String afterMarker = normalized.substring(markerIndex + marker.length());
                while (afterMarker.startsWith("/")) {
                    afterMarker = afterMarker.substring(1);
                }
                String bucketPrefix = bucket + "/";
                if (afterMarker.startsWith(bucketPrefix)) {
                    return afterMarker.substring(bucketPrefix.length());
                }
                return afterMarker;
            }
        }

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    private String encodeObjectPath(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return "";
        }

        String[] segments = objectPath.split("/");
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(encodePathSegment(segments[i]));
        }

        return encoded.toString();
    }

    private String encodePathSegment(String segment) {
        try {
            return URLEncoder.encode(segment, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            return segment;
        }
    }

    private record SignedUrlRequest(int expiresIn) {}
}
