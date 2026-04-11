package com.recruitment.backend.services.ai.pipeline;

import com.recruitment.backend.services.ai.config.AiProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TextNormalizationService {

    private final AiProperties aiProperties;

    public TextNormalizationService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    public String normalize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String cleaned = rawText.replaceAll("\\u0000", " ")
                .replace("\r", "\n")
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll(" {2,}", " ")
                .trim();

        List<String> lines = Arrays.stream(cleaned.split("\\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());

        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String line : lines) {
            String key = line.toLowerCase();
            freq.put(key, freq.getOrDefault(key, 0) + 1);
        }

        return lines.stream()
                .filter(line -> freq.getOrDefault(line.toLowerCase(), 0) <= 2)
                .collect(Collectors.joining("\n"));
    }

    public List<String> chunkByApproxTokens(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int tokenSize = aiProperties.getEmbedding().getChunkTokenSize();
        int overlap = aiProperties.getEmbedding().getChunkTokenOverlap();
        int approxCharsPerToken = 4;
        int chunkCharSize = tokenSize * approxCharsPerToken;
        int overlapChars = overlap * approxCharsPerToken;

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkCharSize);
            chunks.add(text.substring(start, end));
            if (end == text.length()) {
                break;
            }
            start = Math.max(0, end - overlapChars);
        }
        return chunks;
    }

    public String detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return "unknown";
        }
        long vietnameseMarkers = text.chars()
                .filter(ch -> "ăâđêôơưĂÂĐÊÔƠƯáàảãạấầẩẫậắằẳẵặéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ".indexOf(ch) >= 0)
                .count();
        return vietnameseMarkers > 5 ? "vi" : "en";
    }
}
