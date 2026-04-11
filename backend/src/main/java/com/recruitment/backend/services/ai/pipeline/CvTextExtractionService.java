package com.recruitment.backend.services.ai.pipeline;

import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class CvTextExtractionService {

    private final Tika tika = new Tika();

    public String extractTextFromSignedUrl(UUID cvId, String fileUrl) {
        String requestId = cvId.toString();
        long start = System.currentTimeMillis();
        File tempFile = null;
        try {
            tempFile = File.createTempFile("cv_temp_", ".tmp");
            URL url = new URL(fileUrl);
            try (InputStream in = url.openStream()) {
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            String text = tika.parseToString(tempFile);
            long latency = System.currentTimeMillis() - start;
            log.info("CV text extracted. requestId={}, latencyMs={}", requestId, latency);
            return text;
        } catch (Exception e) {
            throw new AppException(ErrorCode.READ_FILE_FAILED);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (Exception e) {
                    log.error("Cannot delete temp file: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }
}
