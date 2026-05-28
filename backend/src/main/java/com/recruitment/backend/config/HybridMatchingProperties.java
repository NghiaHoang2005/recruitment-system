package com.recruitment.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "matching.hybrid")
public class HybridMatchingProperties {

    private Weights weights = new Weights();
    private int candidatePoolSize = 50;
    private int maxLimit = 50;
    private double requiredSkillWeight = 0.7;
    private double preferredSkillWeight = 0.3;

    @Getter
    @Setter
    public static class Weights {
        private double semantic = 0.6;
        private double fts = 0.25;
        private double skills = 0.15;
    }
}
