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
            // 1. In ra URL để nếu lỗi, bạn copy URL này dán lên trình duyệt xem có tải được không
            log.info("Bắt đầu tải file từ URL: {}", fileUrl);

            tempFile = File.createTempFile("cv_temp_", ".tmp");
            URL url = new URL(fileUrl);

            // 2. Dùng HttpURLConnection thay vì url.openStream()
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            // Giả lập trình duyệt để không bị Cloudinary CDN chặn
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setRequestMethod("GET");

            // 3. Kiểm tra mã lỗi HTTP
            int responseCode = connection.getResponseCode();
            if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                log.error("Cloudinary từ chối tải file. Mã HTTP: {}", responseCode);
                throw new Exception("HTTP Error Code: " + responseCode);
            }

            // 4. Lưu file và đọc bằng Tika
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            String text = tika.parseToString(tempFile);
            long latency = System.currentTimeMillis() - start;
            log.info("CV text extracted. requestId={}, latencyMs={}", requestId, latency);
            return text;

        } catch (Exception e) {
            // QUAN TRỌNG: In ra lỗi gốc để biết chính xác bị gì
            log.error("Lỗi chi tiết khi tải hoặc đọc file CV ID {}: {}", cvId, e.getMessage(), e);
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
