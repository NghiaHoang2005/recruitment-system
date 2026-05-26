package com.recruitment.backend.services.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobExtractionResult {
    @JsonProperty("required_skills")
    private List<String> requiredSkills = new ArrayList<>();

    @JsonProperty("preferred_skills")
    private List<String> preferredSkills = new ArrayList<>();

    @JsonProperty("other_skills")
    private List<String> otherSkills = new ArrayList<>();

    @JsonProperty("summary")
    private String summary;
}
