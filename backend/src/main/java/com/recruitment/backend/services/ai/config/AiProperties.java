package com.recruitment.backend.services.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private Extraction extraction = new Extraction();
    private Embedding embedding = new Embedding();
    private Prompts prompts = new Prompts();

    @Getter
    @Setter
    public static class Extraction {
        private String activeProvider = "gemini";
        private String model = "gemini-flash-latest";
        private String fallbackModel = "gemini-flash-lite-latest";
        private String promptVersion = "cv_extract_v3";
        private Double temperature = 0.1;
        private Integer maxTokens = 2000;
        private Integer retries = 2;
        private Integer timeoutMs = 20000;
    }

    @Getter
    @Setter
    public static class Embedding {
        private String activeProvider = "gemini";
        private String model = "gemini-embedding-001";
        private Integer dimensions = 768;
        private Integer batchSize = 32;
        private Integer maxCharactersPerInput = 8000;
        private Integer chunkTokenSize = 500;
        private Integer chunkTokenOverlap = 50;
        private Integer retries = 2;
        private Integer timeoutMs = 20000;

        public Integer getRecommendedDimensions() {
            if (model == null) {
                return dimensions;
            }

            String provider = activeProvider != null ? activeProvider.toLowerCase() : "gemini";
            String modelLower = model.toLowerCase();

            if (provider.equals("gemini") || modelLower.contains("embedding-001")) {
                return 768;
            }

            return dimensions;
        }

        public boolean isValidDimensions() {
            return dimensions != null && dimensions == 768;
        }

        public String getDimensionDescription() {
            Integer dims = getRecommendedDimensions();
            if (dims == null) return "unknown";

            if (dims == 768) return "768 (Gemini FREE tier)";
            return dims + " (custom)";
        }
    }

    @Getter
    @Setter
    public static class Prompts {
        private String activeVersion = "cv_extract_v3";
        private Map<String, String> templates = new HashMap<>();
    }
}
