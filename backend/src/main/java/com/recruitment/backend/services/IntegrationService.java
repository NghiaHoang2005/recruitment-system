package com.recruitment.backend.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class IntegrationService {
    private final Tika tika = new Tika();

    public String extractTextFromUrl(String fileUrl) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("cv_temp_", ".tmp");

            URL url = new URL(fileUrl);
            try (InputStream in = url.openStream()) {
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return tika.parseToString(tempFile);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải hoặc đọc file (PDF/DOCX)", e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (Exception e) {
                    log.error("Không thể xóa file tạm: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }

    public String callAiToParseJson(String rawText) {
        // Gửi rawText tới OpenAI/Gemini, prompt yêu cầu trả về chuẩn JSON
        return "{\"full_name\": \"Nguyễn Văn A\", \"extracted_skills\": [\"Java\", \"PostgreSQL\"]}";
    }
}