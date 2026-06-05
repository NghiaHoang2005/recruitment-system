package com.recruitment.backend.services;

import com.recruitment.backend.config.HybridMatchingProperties;
import com.recruitment.backend.domain.dtos.Cv.CvItemResponse;
import com.recruitment.backend.domain.dtos.Cv.CvRecommendationResponse;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.CvEmbedding;
import com.recruitment.backend.domain.entities.Cv.EmbeddingType;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.JobEmbedding;
import com.recruitment.backend.domain.entities.JobSkill;
import com.recruitment.backend.domain.enums.JobEmbeddingType;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.domain.enums.RequirementSectionType;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CandidateSkillRepository;
import com.recruitment.backend.repositories.CvEmbeddingRepository;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.repositories.JobEmbeddingRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.repositories.JobSkillRepository;
import com.recruitment.backend.utils.VectorSearchUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobMatchService {

    private static final double SEMANTIC_SKILLS_WEIGHT = 0.33;
    private static final double SEMANTIC_DESCRIPTION_WEIGHT = 0.33;
    private static final double SEMANTIC_REQUIREMENTS_WEIGHT = 0.34;
    private static final int MAX_FTS_QUERY_LENGTH = 2000;

    private final CvRepository cvRepository;
    private final CvEmbeddingRepository cvEmbeddingRepository;
    private final JobRepository jobRepository;
    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final JobSkillRepository jobSkillRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final HybridMatchingProperties hybridMatchingProperties;
    private final MatchingWeightService matchingWeightService;
    private final MatchingMonitoringService matchingMonitoringService;

    public MatchScore matchJob(UUID candidateUserId, UUID jobId, UUID cvId) {
        Cv cv = resolveCv(candidateUserId, cvId);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        MatchingWeights weights = matchingWeightService.resolveWeightsForCompany(
                job.getCompany() != null ? job.getCompany().getId() : null
        );

        long startTime = System.currentTimeMillis();
        List<CvEmbedding> cvEmbeddings = cvEmbeddingRepository.findByCvId(cv.getId());
        List<JobEmbedding> jobEmbeddings = jobEmbeddingRepository.findByJob_Id(jobId);

        SemanticScore semanticScore = computeSemanticScore(jobEmbeddings, cvEmbeddings);
        Double skillScore = computeSkillScoreForCandidate(candidateUserId, jobId, weights);
        FtsScoreBundle ftsBundle = computeJobFtsScores(cv, JobStatus.PUBLISHED.name(), resolveCandidatePoolSize(1));
        Double ftsScore = ftsBundle.enabled()
                ? ftsBundle.scores().getOrDefault(jobId, 0.0)
                : null;
        Double hybridScore = combineHybridScores(
                semanticScore == null ? null : semanticScore.getScore(),
                ftsScore,
                skillScore,
                weights
        );
        long latencyMs = System.currentTimeMillis() - startTime;

        try {
            matchingMonitoringService.recordMatchingEvent(
                    com.recruitment.backend.domain.enums.MatchingRequestType.MATCH,
                    job.getCompany() != null ? job.getCompany().getId() : null,
                    jobId,
                    cv.getId(),
                    hybridScore,
                    semanticScore == null ? null : semanticScore.getScore(),
                    ftsScore,
                    skillScore,
                    latencyMs
            );
        } catch (Exception e) {
            // Log but don't fail on monitoring error
        }

        return MatchScore.builder()
                .jobId(job.getId())
                .cvId(cv.getId())
                .fitScore(toPercent(hybridScore))
                .semanticScore(toPercent(semanticScore == null ? null : semanticScore.getScore()))
                .ftsScore(toPercent(ftsScore))
                .skillScore(toPercent(skillScore))
                .skillsScore(toPercent(semanticScore == null ? null : semanticScore.getSkillsScore()))
                .descriptionScore(toPercent(semanticScore == null ? null : semanticScore.getDescriptionScore()))
                .requirementsScore(toPercent(semanticScore == null ? null : semanticScore.getRequirementsScore()))
                .model(semanticScore == null ? null : semanticScore.getModel())
                .dimensions(semanticScore == null ? null : semanticScore.getDimensions())
                .build();
    }

    public List<RecommendationScore> recommendJobs(UUID candidateUserId, UUID cvId, int topK) {
        Cv cv = resolveCv(candidateUserId, cvId);
        return recommendJobsInternal(cv, candidateUserId, topK, null, true);
    }

    public List<RecommendationScore> recommendJobsForEvaluation(UUID cvId, int topK, MatchingWeights weightsOverride) {
        Cv cv = resolveCvForEvaluation(cvId);
        UUID candidateUserId = cv.getCandidate() != null ? cv.getCandidate().getUserId() : null;
        return recommendJobsInternal(cv, candidateUserId, topK, weightsOverride, false);
    }

    public List<CvRecommendationResponse> recommendCvs(UUID jobId, int topK) {
        return recommendCvsInternal(jobId, topK, null, true);
    }

    public List<CvRecommendationResponse> recommendCvsForEvaluation(UUID jobId, int topK, MatchingWeights weightsOverride) {
        return recommendCvsInternal(jobId, topK, weightsOverride, false);
    }

    private List<RecommendationScore> recommendJobsInternal(
            Cv cv,
            UUID candidateUserId,
            int topK,
            MatchingWeights weightsOverride,
            boolean recordMonitoring
    ) {
        int safeTopK = clampTopK(topK);
        int poolSize = resolveCandidatePoolSize(safeTopK);
        String status = JobStatus.PUBLISHED.name();

        FtsScoreBundle ftsBundle = computeJobFtsScores(cv, status, poolSize);
        SemanticScoreBundle semanticScores = computeSemanticScoresForJobs(cv, status, poolSize);

        Set<UUID> candidateJobIds = new HashSet<>();
        candidateJobIds.addAll(ftsBundle.scores().keySet());
        candidateJobIds.addAll(semanticScores.scores().keySet());
        if (candidateJobIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Double> skillScores = weightsOverride != null
                ? computeSkillScoresForJobs(candidateUserId, candidateJobIds, weightsOverride)
                : computeSkillScoresForJobs(candidateUserId, candidateJobIds);
        List<Job> jobs = jobRepository.findAllById(candidateJobIds);
        Map<UUID, Job> jobsMap = jobs.stream()
                .collect(Collectors.toMap(Job::getId, j -> j));

        List<ScoredId> scored = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        Map<UUID, MatchingWeights> companyWeightsCache = new HashMap<>();
        for (UUID jobId : candidateJobIds) {
            Job job = jobsMap.get(jobId);
            if (job == null) continue;

            UUID companyId = job.getCompany() != null ? job.getCompany().getId() : null;
            MatchingWeights weights;
            if (weightsOverride != null) {
                weights = weightsOverride;
            } else if (companyWeightsCache.containsKey(companyId)) {
                weights = companyWeightsCache.get(companyId);
            } else {
                weights = matchingWeightService.resolveWeightsForCompany(companyId);
                companyWeightsCache.put(companyId, weights);
            }

            Double semanticScore = semanticScores.scores().get(jobId);
            Double ftsScore = ftsBundle.enabled()
                    ? ftsBundle.scores().getOrDefault(jobId, 0.0)
                    : null;
            Double skillScore = skillScores.get(jobId);
            Double finalScore = combineHybridScores(semanticScore, ftsScore, skillScore, weights);
            if (finalScore == null) {
                continue;
            }
            scored.add(new ScoredId(jobId, finalScore));
        }

        scored.sort(scoredComparator());
        List<RecommendationScore> results = scored.stream()
                .limit(safeTopK)
                .map(item -> new RecommendationScore(item.id(), toPercentValue(item.score())))
                .toList();

        if (recordMonitoring) {
            long latencyMs = System.currentTimeMillis() - startTime;
            try {
                for (RecommendationScore result : results) {
                    Job job = jobsMap.get(result.jobId);
                    if (job != null) {
                        matchingMonitoringService.recordMatchingEvent(
                                com.recruitment.backend.domain.enums.MatchingRequestType.RECOMMEND_JOBS,
                                job.getCompany() != null ? job.getCompany().getId() : null,
                                job.getId(),
                                cv.getId(),
                                result.fitScore / 100.0,
                                null,
                                null,
                                null,
                                latencyMs / results.size()
                        );
                    }
                }
            } catch (Exception e) {
                // Log but don't fail on monitoring error
            }
        }

        return results;
    }

    private List<CvRecommendationResponse> recommendCvsInternal(
            UUID jobId,
            int topK,
            MatchingWeights weightsOverride,
            boolean recordMonitoring
    ) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        int safeTopK = clampTopK(topK);
        int poolSize = resolveCandidatePoolSize(safeTopK);
        List<JobEmbedding> jobEmbeddings = jobEmbeddingRepository.findByJob_Id(jobId);

        MatchingWeights weights = weightsOverride != null
                ? weightsOverride
                : matchingWeightService.resolveWeightsForCompany(
                job.getCompany() != null ? job.getCompany().getId() : null
        );

        FtsScoreBundle ftsBundle = computeCvFtsScores(job, poolSize);
        Set<UUID> semanticCandidates = collectSemanticCandidateCvs(jobEmbeddings, poolSize);

        Set<UUID> candidateCvIds = new HashSet<>();
        candidateCvIds.addAll(ftsBundle.scores().keySet());
        candidateCvIds.addAll(semanticCandidates);
        if (candidateCvIds.isEmpty()) {
            return List.of();
        }

        List<Cv> cvs = cvRepository.findByIdInAndCandidate_OpenToWorkTrueAndIsDefaultTrue(new ArrayList<>(candidateCvIds));
        if (cvs.isEmpty()) {
            return List.of();
        }
        Set<UUID> openToWorkCvIds = cvs.stream()
                .map(Cv::getId)
                .collect(Collectors.toSet());
        Map<UUID, Cv> cvMap = cvs.stream()
                .collect(Collectors.toMap(Cv::getId, cv -> cv));
        Map<UUID, List<CvEmbedding>> cvEmbeddingMap = loadCvEmbeddings(openToWorkCvIds);

        Map<UUID, Set<String>> candidateSkills = loadCandidateSkills(cvs);
        List<JobSkill> jobSkills = jobSkillRepository.findByJob_Id(jobId);

        List<ScoredId> scored = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        for (UUID cvId : openToWorkCvIds) {
            Cv cv = cvMap.get(cvId);
            if (cv == null) {
                continue;
            }
            List<CvEmbedding> cvEmbeddings = cvEmbeddingMap.getOrDefault(cvId, List.of());
            SemanticScore semanticScore = computeSemanticScore(jobEmbeddings, cvEmbeddings);
            Set<String> skills = candidateSkills.get(cv.getCandidate().getUserId());
            Double skillScore = computeSkillScore(skills, jobSkills, weights);
            Double ftsScore = ftsBundle.enabled()
                    ? ftsBundle.scores().getOrDefault(cvId, 0.0)
                    : null;
            Double finalScore = combineHybridScores(
                    semanticScore == null ? null : semanticScore.getScore(),
                    ftsScore,
                    skillScore,
                    weights
            );
            if (finalScore == null) {
                continue;
            }
            scored.add(new ScoredId(cvId, finalScore));
        }

        scored.sort(scoredComparator());
        List<CvRecommendationResponse> results = scored.stream()
                .limit(safeTopK)
                .map(item -> {
                    Cv cv = cvMap.get(item.id());
                    if (cv == null) {
                        return null;
                    }
                    CvItemResponse cvItem = CvItemResponse.builder()
                            .id(cv.getId())
                            .cvName(cv.getCvName())
                            .uploadedAt(cv.getUploadedAt())
                            .isDefault(Boolean.TRUE.equals(cv.getIsDefault()))
                            .aiStatus(cv.getAiStatus())
                            .build();
                    return CvRecommendationResponse.builder()
                            .cv(cvItem)
                            .matchScore(toPercentValue(item.score()))
                            .candidateId(cv.getCandidate() != null ? cv.getCandidate().getUserId() : null)
                            .candidateName(cv.getCandidate() != null ? cv.getCandidate().getFullName() : null)
                            .candidateHeadline(cv.getCandidate() != null ? cv.getCandidate().getHeadline() : null)
                            .candidateAvatar(cv.getCandidate() != null ? cv.getCandidate().getProfilePictureUrl() : null)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        if (recordMonitoring) {
            long latencyMs = System.currentTimeMillis() - startTime;
            try {
                for (CvRecommendationResponse result : results) {
                    matchingMonitoringService.recordMatchingEvent(
                            com.recruitment.backend.domain.enums.MatchingRequestType.RECOMMEND_CVS,
                            job.getCompany() != null ? job.getCompany().getId() : null,
                            jobId,
                            result.getCv().getId(),
                            result.getMatchScore() / 100.0,
                            null,
                            null,
                            null,
                            latencyMs / results.size()
                    );
                }
            } catch (Exception e) {
                // Log but don't fail on monitoring error
            }
        }

        return results;
    }

    private Cv resolveCv(UUID candidateUserId, UUID cvId) {
        if (cvId != null) {
            return cvRepository.findByIdAndCandidateUserId(cvId, candidateUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));
        }
        return cvRepository.findFirstByCandidateUserIdOrderByIsDefaultDescUploadedAtDesc(candidateUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));
    }

    private Cv resolveCvForEvaluation(UUID cvId) {
        if (cvId == null) {
            throw new AppException(ErrorCode.CV_NOT_FOUND);
        }
        return cvRepository.findById(cvId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));
    }

    private int clampTopK(int topK) {
        int max = Math.max(hybridMatchingProperties.getMaxLimit(), 1);
        if (topK <= 0) {
            return 1;
        }
        return Math.min(topK, max);
    }

    private int resolveCandidatePoolSize(int topK) {
        int base = Math.max(hybridMatchingProperties.getCandidatePoolSize(), 1);
        return Math.max(base, topK);
    }

    private SemanticScoreBundle computeSemanticScoresForJobs(Cv cv, String status, int poolSize) {
        List<CvEmbedding> cvEmbeddings = cvEmbeddingRepository.findByCvId(cv.getId());
        CvEmbedding cvSkills = findCvEmbedding(cvEmbeddings, EmbeddingType.SKILLS, null, null);
        CvEmbedding cvExperience = findCvEmbedding(cvEmbeddings, EmbeddingType.EXPERIENCE, null, null);

        Map<UUID, Double> skillsScores = loadSimilarityScores(cvSkills, JobEmbeddingType.SKILLS, poolSize, status);
        Map<UUID, Double> descriptionScores = loadSimilarityScores(cvExperience, JobEmbeddingType.DESCRIPTION, poolSize, status);
        Map<UUID, Double> requiredBySkills = loadSimilarityScores(cvSkills, JobEmbeddingType.REQUIRED_REQUIREMENTS, poolSize, status);
        Map<UUID, Double> requiredByExperience = loadSimilarityScores(cvExperience, JobEmbeddingType.REQUIRED_REQUIREMENTS, poolSize, status);
        Map<UUID, Double> requiredScores = mergeMax(requiredBySkills, requiredByExperience);

        Map<UUID, Double> preferredBySkills = loadSimilarityScores(cvSkills, JobEmbeddingType.PREFERRED_REQUIREMENTS, poolSize, status);
        Map<UUID, Double> preferredByExperience = loadSimilarityScores(cvExperience, JobEmbeddingType.PREFERRED_REQUIREMENTS, poolSize, status);
        Map<UUID, Double> preferredScores = mergeMax(preferredBySkills, preferredByExperience);

        Set<UUID> candidateIds = new HashSet<>();
        candidateIds.addAll(skillsScores.keySet());
        candidateIds.addAll(descriptionScores.keySet());
        candidateIds.addAll(requiredScores.keySet());
        candidateIds.addAll(preferredScores.keySet());

        Map<UUID, Double> semanticScores = new HashMap<>();
        for (UUID jobId : candidateIds) {
            Double skills = skillsScores.get(jobId);
            Double description = descriptionScores.get(jobId);
            Double required = requiredScores.get(jobId);
            Double preferred = preferredScores.get(jobId);
            Double requirements = combineRequiredPreferred(required, preferred);
            Double semanticScore = combineSemanticScores(skills, description, requirements);
            if (semanticScore != null) {
                semanticScores.put(jobId, semanticScore);
            }
        }

        return new SemanticScoreBundle(semanticScores);
    }

    private FtsScoreBundle computeJobFtsScores(Cv cv, String status, int poolSize) {
        // FTS is disabled for AI Document-to-Document matching to prevent redundancy and false negatives.
        // We rely 100% on Semantic Search and Skill Matching instead.
        return new FtsScoreBundle(Map.of(), false);
    }

    private FtsScoreBundle computeCvFtsScores(Job job, int poolSize) {
        // FTS is disabled for AI Document-to-Document matching to prevent redundancy and false negatives.
        // We rely 100% on Semantic Search and Skill Matching instead.
        return new FtsScoreBundle(Map.of(), false);
    }

    private String buildFtsQueryFromCv(Cv cv) {
        if (cv == null) {
            return null;
        }
        String text = firstNonBlank(cv.getNormalizedText(), cv.getRawText());
        return normalizeFtsQuery(text);
    }

    private String buildFtsQueryFromJob(Job job) {
        if (job == null) {
            return null;
        }
        String text = firstNonBlank(job.getNormalizedText(), job.getDescription());
        if (job.getTitle() != null && !job.getTitle().isBlank()) {
            text = text == null ? job.getTitle() : job.getTitle() + " " + text;
        }
        return normalizeFtsQuery(text);
    }

    private String normalizeFtsQuery(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > MAX_FTS_QUERY_LENGTH) {
            return normalized.substring(0, MAX_FTS_QUERY_LENGTH);
        }
        return normalized;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private Map<UUID, Double> normalizeFtsJobRows(List<JobRepository.JobFtsView> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        double maxRank = rows.stream()
                .map(JobRepository.JobFtsView::getRank)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(0.0);
        if (maxRank <= 0.0) {
            return Map.of();
        }
        Map<UUID, Double> scores = new HashMap<>();
        for (JobRepository.JobFtsView row : rows) {
            if (row.getJobId() != null && row.getRank() != null) {
                scores.put(UUID.fromString(row.getJobId()), clampScore(row.getRank() / maxRank));
            }
        }
        return scores;
    }

    private Map<UUID, Double> normalizeFtsCvRows(List<CvRepository.CvFtsView> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        double maxRank = rows.stream()
                .map(CvRepository.CvFtsView::getRank)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(0.0);
        if (maxRank <= 0.0) {
            return Map.of();
        }
        Map<UUID, Double> scores = new HashMap<>();
        for (CvRepository.CvFtsView row : rows) {
            if (row.getCvId() != null && row.getRank() != null) {
                scores.put(UUID.fromString(row.getCvId()), clampScore(row.getRank() / maxRank));
            }
        }
        return scores;
    }

    private Map<UUID, Double> computeSkillScoresForJobs(UUID candidateUserId, Collection<UUID> jobIds) {
        return computeSkillScoresForJobs(candidateUserId, jobIds, null);
    }

    private Map<UUID, Double> computeSkillScoresForJobs(
            UUID candidateUserId,
            Collection<UUID> jobIds,
            MatchingWeights weightsOverride
    ) {
        Set<String> candidateSkills = loadCandidateSkillNames(candidateUserId);
        if (candidateSkills.isEmpty() || jobIds == null || jobIds.isEmpty()) {
            return Map.of();
        }
        List<JobSkill> jobSkills = jobSkillRepository.findByJob_IdIn(new ArrayList<>(jobIds));
        Map<UUID, List<JobSkill>> jobSkillMap = jobSkills.stream()
                .collect(Collectors.groupingBy(skill -> skill.getJob().getId()));

        MatchingWeights defaultWeights = weightsOverride != null
                ? weightsOverride
                : MatchingWeights.fromConfig(hybridMatchingProperties);
        Map<UUID, Double> scores = new HashMap<>();
        for (UUID jobId : jobIds) {
            Double score = computeSkillScore(candidateSkills, jobSkillMap.get(jobId), defaultWeights);
            if (score != null) {
                scores.put(jobId, score);
            }
        }
        return scores;
    }

    private Double computeSkillScoreForCandidate(UUID candidateUserId, UUID jobId, MatchingWeights weights) {
        Set<String> candidateSkills = loadCandidateSkillNames(candidateUserId);
        if (candidateSkills.isEmpty()) {
            return null;
        }
        List<JobSkill> jobSkills = jobSkillRepository.findByJob_Id(jobId);
        return computeSkillScore(candidateSkills, jobSkills, weights);
    }

    private Set<String> loadCandidateSkillNames(UUID candidateUserId) {
        if (candidateUserId == null) {
            return Set.of();
        }
        return candidateSkillRepository.findByCandidateUserId(candidateUserId).stream()
                .map(mapping -> mapping.getSkill().getName())
                .filter(Objects::nonNull)
                .map(name -> name.trim().toLowerCase())
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());
    }

    private Map<UUID, Set<String>> loadCandidateSkills(List<Cv> cvs) {
        if (cvs == null || cvs.isEmpty()) {
            return Map.of();
        }
        Set<UUID> candidateIds = cvs.stream()
                .map(cv -> cv.getCandidate().getUserId())
                .collect(Collectors.toSet());
        if (candidateIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Set<String>> skillMap = new HashMap<>();
        candidateSkillRepository.findByCandidateUserIdIn(new ArrayList<>(candidateIds)).forEach(mapping -> {
            UUID candidateId = mapping.getId().getCandidateId();
            String name = mapping.getSkill().getName();
            if (candidateId == null || name == null) {
                return;
            }
            String normalized = name.trim().toLowerCase();
            if (normalized.isBlank()) {
                return;
            }
            skillMap.computeIfAbsent(candidateId, key -> new HashSet<>()).add(normalized);
        });
        return skillMap;
    }

    private Double computeSkillScore(Set<String> candidateSkills, List<JobSkill> jobSkills, MatchingWeights weights) {
        if (candidateSkills == null || candidateSkills.isEmpty() || jobSkills == null || jobSkills.isEmpty()) {
            return null;
        }
        long requiredTotal = jobSkills.stream()
                .filter(skill -> skill.getRequirementType() == RequirementSectionType.REQUIRED)
                .count();
        long preferredTotal = jobSkills.stream()
                .filter(skill -> skill.getRequirementType() == RequirementSectionType.PREFERRED)
                .count();
        long otherTotal = jobSkills.stream()
                .filter(skill -> skill.getRequirementType() == RequirementSectionType.OTHER)
                .count();

        long requiredMatched = jobSkills.stream()
                .filter(skill -> skill.getRequirementType() == RequirementSectionType.REQUIRED)
                .map(this::normalizeSkillName)
                .filter(Objects::nonNull)
                .filter(candidateSkills::contains)
                .count();
        long preferredMatched = jobSkills.stream()
                .filter(skill -> skill.getRequirementType() == RequirementSectionType.PREFERRED)
                .map(this::normalizeSkillName)
                .filter(Objects::nonNull)
                .filter(candidateSkills::contains)
                .count();
        long otherMatched = jobSkills.stream()
                .filter(skill -> skill.getRequirementType() == RequirementSectionType.OTHER)
                .map(this::normalizeSkillName)
                .filter(Objects::nonNull)
                .filter(candidateSkills::contains)
                .count();

        Double requiredScore = requiredTotal > 0 ? (double) requiredMatched / requiredTotal : null;
        Double preferredScore = preferredTotal > 0 ? (double) preferredMatched / preferredTotal : null;

        if (requiredScore == null && preferredScore == null) {
            if (otherTotal > 0) {
                return clampScore((double) otherMatched / otherTotal);
            }
            return null;
        }

        if (requiredScore == null) {
            return clampScore(preferredScore);
        }
        if (preferredScore == null) {
            return clampScore(requiredScore);
        }

        double requiredWeight = Math.max(weights.requiredSkillWeight(), 0.0);
        double preferredWeight = Math.max(weights.preferredSkillWeight(), 0.0);
        double totalWeight = requiredWeight + preferredWeight;
        if (totalWeight <= 0.0) {
            totalWeight = 1.0;
            requiredWeight = 1.0;
            preferredWeight = 0.0;
        }
        double normalizedRequired = requiredWeight / totalWeight;
        double normalizedPreferred = preferredWeight / totalWeight;

        return clampScore((normalizedRequired * requiredScore) + (normalizedPreferred * preferredScore));
    }

    private Set<UUID> collectSemanticCandidateCvs(List<JobEmbedding> jobEmbeddings, int poolSize) {
        Set<UUID> candidates = new HashSet<>();
        JobEmbedding jobSkills = findJobEmbedding(jobEmbeddings, JobEmbeddingType.SKILLS);
        JobEmbedding jobDescription = findJobEmbedding(jobEmbeddings, JobEmbeddingType.DESCRIPTION);
        JobEmbedding jobRequired = findJobEmbedding(jobEmbeddings, JobEmbeddingType.REQUIRED_REQUIREMENTS);
        JobEmbedding jobPreferred = findJobEmbedding(jobEmbeddings, JobEmbeddingType.PREFERRED_REQUIREMENTS);

        candidates.addAll(findTopCvIds(jobSkills, EmbeddingType.SKILLS, poolSize));
        candidates.addAll(findTopCvIds(jobDescription, EmbeddingType.EXPERIENCE, poolSize));
        candidates.addAll(findTopCvIds(jobRequired, EmbeddingType.SKILLS, poolSize));
        candidates.addAll(findTopCvIds(jobRequired, EmbeddingType.EXPERIENCE, poolSize));
        candidates.addAll(findTopCvIds(jobPreferred, EmbeddingType.SKILLS, poolSize));
        candidates.addAll(findTopCvIds(jobPreferred, EmbeddingType.EXPERIENCE, poolSize));
        return candidates;
    }

    private List<UUID> findTopCvIds(JobEmbedding jobEmbedding, EmbeddingType cvType, int poolSize) {
        if (jobEmbedding == null || jobEmbedding.getVector() == null) {
            return List.of();
        }
        if (jobEmbedding.getModel() == null || jobEmbedding.getDimensions() == null) {
            return List.of();
        }
        String vectorLiteral = VectorSearchUtil.toVectorLiteral(jobEmbedding.getVector());
        List<String> ids = cvEmbeddingRepository.findTopMatchingCvIdsByTypeAndModelAndDimensions(
                vectorLiteral,
                cvType.name(),
                jobEmbedding.getModel(),
                jobEmbedding.getDimensions(),
                poolSize
        );
        return ids.stream().map(UUID::fromString).collect(Collectors.toList());
    }

    private Map<UUID, List<CvEmbedding>> loadCvEmbeddings(Collection<UUID> cvIds) {
        if (cvIds == null || cvIds.isEmpty()) {
            return Map.of();
        }
        List<CvEmbedding> embeddings = cvEmbeddingRepository.findByCvIdIn(new ArrayList<>(cvIds));
        return embeddings.stream()
                .collect(Collectors.groupingBy(embedding -> embedding.getCv().getId()));
    }

    private SemanticScore computeSemanticScore(List<JobEmbedding> jobEmbeddings, List<CvEmbedding> cvEmbeddings) {
        JobEmbedding jobSkills = findJobEmbedding(jobEmbeddings, JobEmbeddingType.SKILLS);
        JobEmbedding jobDescription = findJobEmbedding(jobEmbeddings, JobEmbeddingType.DESCRIPTION);
        JobEmbedding jobRequired = findJobEmbedding(jobEmbeddings, JobEmbeddingType.REQUIRED_REQUIREMENTS);
        JobEmbedding jobPreferred = findJobEmbedding(jobEmbeddings, JobEmbeddingType.PREFERRED_REQUIREMENTS);

        if (jobSkills == null && jobDescription == null && jobRequired == null && jobPreferred == null) {
            return null;
        }

        String skillModel = firstNonNull(
                jobSkills == null ? null : jobSkills.getModel(),
                jobRequired == null ? null : jobRequired.getModel(),
                jobPreferred == null ? null : jobPreferred.getModel()
        );
        Integer skillDimensions = firstNonNull(
                jobSkills == null ? null : jobSkills.getDimensions(),
                jobRequired == null ? null : jobRequired.getDimensions(),
                jobPreferred == null ? null : jobPreferred.getDimensions()
        );
        String expModel = firstNonNull(
                jobDescription == null ? null : jobDescription.getModel(),
                jobRequired == null ? null : jobRequired.getModel(),
                jobPreferred == null ? null : jobPreferred.getModel()
        );
        Integer expDimensions = firstNonNull(
                jobDescription == null ? null : jobDescription.getDimensions(),
                jobRequired == null ? null : jobRequired.getDimensions(),
                jobPreferred == null ? null : jobPreferred.getDimensions()
        );

        CvEmbedding cvSkills = findCvEmbedding(cvEmbeddings, EmbeddingType.SKILLS, skillModel, skillDimensions);
        CvEmbedding cvExperience = findCvEmbedding(cvEmbeddings, EmbeddingType.EXPERIENCE, expModel, expDimensions);

        Double skillsSim = normalizeSimilarity(cosineSimilarity(
                jobSkills == null ? null : jobSkills.getVector(),
                cvSkills == null ? null : cvSkills.getVector()));
        Double descriptionSim = normalizeSimilarity(cosineSimilarity(
                jobDescription == null ? null : jobDescription.getVector(),
                cvExperience == null ? null : cvExperience.getVector()));
        Double requiredSim = normalizeSimilarity(maxSimilarity(jobRequired, cvSkills, cvExperience));
        Double preferredSim = jobPreferred == null ? null
                : normalizeSimilarity(maxSimilarity(jobPreferred, cvSkills, cvExperience));

        Double requirementsScore = combineRequiredPreferred(requiredSim, preferredSim);
        Double semanticScore = combineSemanticScores(skillsSim, descriptionSim, requirementsScore);
        if (semanticScore == null) {
            return null;
        }

        return SemanticScore.builder()
                .score(semanticScore)
                .skillsScore(skillsSim)
                .descriptionScore(descriptionSim)
                .requirementsScore(requirementsScore)
                .model(jobSkills == null ? null : jobSkills.getModel())
                .dimensions(jobSkills == null ? null : jobSkills.getDimensions())
                .build();
    }

    private Double combineSemanticScores(Double skills, Double description, Double requirements) {
        double sum = 0.0;
        double total = 0.0;
        if (skills != null) {
            sum += SEMANTIC_SKILLS_WEIGHT * skills;
            total += SEMANTIC_SKILLS_WEIGHT;
        }
        if (description != null) {
            sum += SEMANTIC_DESCRIPTION_WEIGHT * description;
            total += SEMANTIC_DESCRIPTION_WEIGHT;
        }
        if (requirements != null) {
            sum += SEMANTIC_REQUIREMENTS_WEIGHT * requirements;
            total += SEMANTIC_REQUIREMENTS_WEIGHT;
        }
        if (total == 0.0) {
            return null;
        }
        return clampScore(sum / total);
    }

    private Double combineRequiredPreferred(Double required, Double preferred) {
        if (required == null && preferred == null) {
            return null;
        }
        if (required == null) {
            return clampScore(preferred);
        }
        if (preferred == null) {
            return clampScore(required);
        }
        double requiredWeight = Math.max(hybridMatchingProperties.getRequiredSkillWeight(), 0.0);
        double preferredWeight = Math.max(hybridMatchingProperties.getPreferredSkillWeight(), 0.0);
        double total = requiredWeight + preferredWeight;
        if (total <= 0.0) {
            return clampScore(required);
        }
        return clampScore((requiredWeight * required + preferredWeight * preferred) / total);
    }

    private Double combineHybridScores(Double semanticScore, Double ftsScore, Double skillScore, MatchingWeights weights) {
        double semanticWeight = Math.max(weights.semanticWeight(), 0.0);
        double skillWeight = Math.max(weights.skillsWeight(), 0.0);

        double sum = 0.0;
        double total = 0.0;
        if (semanticScore != null) {
            sum += semanticWeight * semanticScore;
            total += semanticWeight;
        }
        // FTS is disabled for AI match, weight is naturally redistributed to Semantic and Skill
        if (skillScore != null) {
            sum += skillWeight * skillScore;
            total += skillWeight;
        }
        if (total == 0.0) {
            return null;
        }
        return clampScore(sum / total);
    }

    private CvEmbedding findCvEmbedding(List<CvEmbedding> embeddings, EmbeddingType type, String model, Integer dimensions) {
        if (embeddings == null) {
            return null;
        }
        return embeddings.stream()
                .filter(embedding -> embedding.getType() == type)
                .filter(embedding -> model == null || Objects.equals(model, embedding.getModel()))
                .filter(embedding -> dimensions == null || Objects.equals(dimensions, embedding.getDimensions()))
                .sorted(Comparator.comparing(embedding -> embedding.getChunkIndex() == null ? 0 : embedding.getChunkIndex()))
                .findFirst()
                .orElse(null);
    }

    private JobEmbedding findJobEmbedding(List<JobEmbedding> embeddings, JobEmbeddingType type) {
        if (embeddings == null) {
            return null;
        }
        return embeddings.stream()
                .filter(embedding -> embedding.getEmbeddingType() == type)
                .findFirst()
                .orElse(null);
    }

    private Double maxSimilarity(JobEmbedding jobEmbedding, CvEmbedding cvSkills, CvEmbedding cvExperience) {
        if (jobEmbedding == null) {
            return null;
        }
        Double skillSim = normalizeSimilarity(cosineSimilarity(
                jobEmbedding.getVector(),
                cvSkills == null ? null : cvSkills.getVector()));
        Double expSim = normalizeSimilarity(cosineSimilarity(
                jobEmbedding.getVector(),
                cvExperience == null ? null : cvExperience.getVector()));
        if (skillSim == null && expSim == null) {
            return null;
        }
        if (skillSim == null) {
            return expSim;
        }
        if (expSim == null) {
            return skillSim;
        }
        return Math.max(skillSim, expSim);
    }

    private Double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return null;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            double a = vectorA[i];
            double b = vectorB[i];
            dot += a * b;
            normA += a * a;
            normB += b * b;
        }
        if (normA == 0.0 || normB == 0.0) {
            return null;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Double normalizeSimilarity(Double similarity) {
        if (similarity == null) {
            return null;
        }
        return clampScore(similarity);
    }

    private Double clampScore(Double score) {
        if (score == null) {
            return null;
        }
        return Math.max(0.0, Math.min(1.0, score));
    }

    private Integer toPercent(Double similarity) {
        if (similarity == null) {
            return null;
        }
        double clamped = Math.max(0.0, Math.min(1.0, similarity));
        return (int) Math.round(clamped * 100.0);
    }

    private int toPercentValue(Double similarity) {
        Integer percent = toPercent(similarity);
        return percent == null ? 0 : percent;
    }

    private Map<UUID, Double> loadSimilarityScores(
            CvEmbedding cvEmbedding,
            JobEmbeddingType jobType,
            int topK,
            String status
    ) {
        if (cvEmbedding == null || cvEmbedding.getVector() == null) {
            return Map.of();
        }
        if (cvEmbedding.getModel() == null || cvEmbedding.getDimensions() == null) {
            return Map.of();
        }
        String vectorLiteral = VectorSearchUtil.toVectorLiteral(cvEmbedding.getVector());
        List<JobEmbeddingRepository.JobEmbeddingScoreView> rows =
                jobEmbeddingRepository.findTopJobScoresByTypeModelDimensionsAndStatus(
                        vectorLiteral,
                        jobType.name(),
                        cvEmbedding.getModel(),
                        cvEmbedding.getDimensions(),
                        status,
                        topK
                );
        Map<UUID, Double> scores = new HashMap<>();
        for (JobEmbeddingRepository.JobEmbeddingScoreView row : rows) {
            Double similarity = clampScore(VectorSearchUtil.distanceToSimilarity(row.getDistance()));
            if (similarity != null) {
                scores.put(UUID.fromString(row.getJobId()), similarity);
            }
        }
        return scores;
    }

    private Map<UUID, Double> mergeMax(Map<UUID, Double> first, Map<UUID, Double> second) {
        Map<UUID, Double> merged = new HashMap<>();
        for (Map.Entry<UUID, Double> entry : first.entrySet()) {
            merged.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<UUID, Double> entry : second.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Math::max);
        }
        return merged;
    }

    private Comparator<ScoredId> scoredComparator() {
        return Comparator.comparing(ScoredId::score).reversed()
                .thenComparing(ScoredId::id);
    }

    private String normalizeSkillName(JobSkill jobSkill) {
        if (jobSkill == null || jobSkill.getSkill() == null || jobSkill.getSkill().getName() == null) {
            return null;
        }
        String normalized = jobSkill.getSkill().getName().trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    private String firstNonNull(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Integer firstNonNull(Integer... values) {
        if (values == null) {
            return null;
        }
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private record ScoredId(UUID id, double score) {
    }

    private record SemanticScoreBundle(Map<UUID, Double> scores) {
    }

    private record FtsScoreBundle(Map<UUID, Double> scores, boolean enabled) {
    }

    @Getter
    @Builder
    private static class SemanticScore {
        private Double score;
        private Double skillsScore;
        private Double descriptionScore;
        private Double requirementsScore;
        private String model;
        private Integer dimensions;
    }

    @Getter
    @Builder
    public static class MatchScore {
        private UUID jobId;
        private UUID cvId;
        private Integer fitScore;
        private Integer semanticScore;
        private Integer ftsScore;
        private Integer skillScore;
        private Integer skillsScore;
        private Integer descriptionScore;
        private Integer requirementsScore;
        private String model;
        private Integer dimensions;
    }

    @Getter
    public static class RecommendationScore {
        private final UUID jobId;
        private final int fitScore;

        public RecommendationScore(UUID jobId, int fitScore) {
            this.jobId = jobId;
            this.fitScore = fitScore;
        }
    }
}
