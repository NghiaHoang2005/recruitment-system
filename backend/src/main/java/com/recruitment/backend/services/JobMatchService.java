package com.recruitment.backend.services;

import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.CvEmbedding;
import com.recruitment.backend.domain.entities.Cv.EmbeddingType;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.JobEmbedding;
import com.recruitment.backend.domain.enums.JobEmbeddingType;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CvEmbeddingRepository;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.repositories.JobEmbeddingRepository;
import com.recruitment.backend.repositories.JobRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class JobMatchService {

    private static final double SKILLS_WEIGHT = 0.33;
    private static final double DESCRIPTION_WEIGHT = 0.33;
    private static final double REQUIREMENTS_WEIGHT = 0.34;
    private static final double REQUIRED_RATIO = 0.7;
    private static final double PREFERRED_RATIO = 0.3;

    private final CvRepository cvRepository;
    private final CvEmbeddingRepository cvEmbeddingRepository;
    private final JobRepository jobRepository;
    private final JobEmbeddingRepository jobEmbeddingRepository;

    public MatchScore matchJob(UUID candidateUserId, UUID jobId, UUID cvId) {
        Cv cv = resolveCv(candidateUserId, cvId);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        List<CvEmbedding> cvEmbeddings = cvEmbeddingRepository.findByCvId(cv.getId());
        List<JobEmbedding> jobEmbeddings = jobEmbeddingRepository.findByJob_Id(jobId);
        return computeMatchScore(job, cv, cvEmbeddings, jobEmbeddings);
    }

    public List<RecommendationScore> recommendJobs(UUID candidateUserId, UUID cvId, int topK) {
        Cv cv = resolveCv(candidateUserId, cvId);
        List<CvEmbedding> cvEmbeddings = cvEmbeddingRepository.findByCvId(cv.getId());

        CvEmbedding cvSkills = findCvEmbedding(cvEmbeddings, EmbeddingType.SKILLS, null, null);
        CvEmbedding cvExperience = findCvEmbedding(cvEmbeddings, EmbeddingType.EXPERIENCE, null, null);
        if (cvSkills == null || cvExperience == null) {
            return List.of();
        }
        if (cvSkills.getModel() == null || cvSkills.getDimensions() == null
                || cvExperience.getModel() == null || cvExperience.getDimensions() == null) {
            return List.of();
        }
        if (!Objects.equals(cvSkills.getModel(), cvExperience.getModel())
                || !Objects.equals(cvSkills.getDimensions(), cvExperience.getDimensions())) {
            return List.of();
        }

        int searchK = Math.max(topK * 3, 50);
        String status = JobStatus.PUBLISHED.name();

        Map<UUID, Double> skillsScores = loadSimilarityScores(
                cvSkills, JobEmbeddingType.SKILLS, searchK, status);
        Map<UUID, Double> descriptionScores = loadSimilarityScores(
                cvExperience, JobEmbeddingType.DESCRIPTION, searchK, status);

        Map<UUID, Double> requiredBySkills = loadSimilarityScores(
                cvSkills, JobEmbeddingType.REQUIRED_REQUIREMENTS, searchK, status);
        Map<UUID, Double> requiredByExperience = loadSimilarityScores(
                cvExperience, JobEmbeddingType.REQUIRED_REQUIREMENTS, searchK, status);
        Map<UUID, Double> requiredScores = mergeMax(requiredBySkills, requiredByExperience);

        Map<UUID, Double> preferredBySkills = loadSimilarityScores(
                cvSkills, JobEmbeddingType.PREFERRED_REQUIREMENTS, searchK, status);
        Map<UUID, Double> preferredByExperience = loadSimilarityScores(
                cvExperience, JobEmbeddingType.PREFERRED_REQUIREMENTS, searchK, status);
        Map<UUID, Double> preferredScores = mergeMax(preferredBySkills, preferredByExperience);

        List<RecommendationScore> results = new ArrayList<>();
        for (UUID jobId : skillsScores.keySet()) {
            Double skills = skillsScores.get(jobId);
            Double description = descriptionScores.get(jobId);
            Double required = requiredScores.get(jobId);
            if (skills == null || description == null || required == null) {
                continue;
            }
            Double preferred = preferredScores.get(jobId);
            double requirementsScore = preferred == null
                    ? required
                    : (REQUIRED_RATIO * required) + (PREFERRED_RATIO * preferred);
            double finalScore = (SKILLS_WEIGHT * skills)
                    + (DESCRIPTION_WEIGHT * description)
                    + (REQUIREMENTS_WEIGHT * requirementsScore);
            results.add(new RecommendationScore(jobId, toPercent(finalScore)));
        }

        results.sort(Comparator.comparingInt(RecommendationScore::getFitScore).reversed());
        if (results.size() > topK) {
            return results.subList(0, topK);
        }
        return results;
    }

    private MatchScore computeMatchScore(
            Job job,
            Cv cv,
            List<CvEmbedding> cvEmbeddings,
            List<JobEmbedding> jobEmbeddings
    ) {
        JobEmbedding jobSkills = findJobEmbedding(jobEmbeddings, JobEmbeddingType.SKILLS);
        JobEmbedding jobDescription = findJobEmbedding(jobEmbeddings, JobEmbeddingType.DESCRIPTION);
        JobEmbedding jobRequired = findJobEmbedding(jobEmbeddings, JobEmbeddingType.REQUIRED_REQUIREMENTS);
        JobEmbedding jobPreferred = findJobEmbedding(jobEmbeddings, JobEmbeddingType.PREFERRED_REQUIREMENTS);

        if (jobSkills == null || jobDescription == null || jobRequired == null
                || jobSkills.getModel() == null || jobSkills.getDimensions() == null) {
            return MatchScore.builder()
                    .jobId(job.getId())
                    .cvId(cv.getId())
                    .fitScore(null)
                    .build();
        }

        CvEmbedding cvSkills = findCvEmbedding(cvEmbeddings, EmbeddingType.SKILLS, jobSkills.getModel(), jobSkills.getDimensions());
        CvEmbedding cvExperience = findCvEmbedding(cvEmbeddings, EmbeddingType.EXPERIENCE, jobDescription.getModel(), jobDescription.getDimensions());
        if (cvSkills == null || cvExperience == null) {
            return MatchScore.builder()
                    .jobId(job.getId())
                    .cvId(cv.getId())
                    .fitScore(null)
                    .build();
        }

        Double skillsSim = normalizeSimilarity(cosineSimilarity(jobSkills.getVector(), cvSkills.getVector()));
        Double descriptionSim = normalizeSimilarity(cosineSimilarity(jobDescription.getVector(), cvExperience.getVector()));
        Double requiredSim = normalizeSimilarity(maxSimilarity(jobRequired, cvSkills, cvExperience));
        if (skillsSim == null || descriptionSim == null || requiredSim == null) {
            return MatchScore.builder()
                    .jobId(job.getId())
                    .cvId(cv.getId())
                    .fitScore(null)
                    .build();
        }

        Double preferredSim = jobPreferred == null
                ? null
                : normalizeSimilarity(maxSimilarity(jobPreferred, cvSkills, cvExperience));
        double requirementsScore = preferredSim == null
                ? requiredSim
                : (REQUIRED_RATIO * requiredSim) + (PREFERRED_RATIO * preferredSim);
        double finalScore = (SKILLS_WEIGHT * skillsSim)
                + (DESCRIPTION_WEIGHT * descriptionSim)
                + (REQUIREMENTS_WEIGHT * requirementsScore);

        return MatchScore.builder()
                .jobId(job.getId())
                .cvId(cv.getId())
                .fitScore(toPercent(finalScore))
                .skillsScore(toPercent(skillsSim))
                .descriptionScore(toPercent(descriptionSim))
                .requirementsScore(toPercent(requirementsScore))
                .model(jobSkills.getModel())
                .dimensions(jobSkills.getDimensions())
                .build();
    }

    private Cv resolveCv(UUID candidateUserId, UUID cvId) {
        if (cvId != null) {
            return cvRepository.findByIdAndCandidateUserId(cvId, candidateUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));
        }
        return cvRepository.findFirstByCandidateUserIdOrderByIsDefaultDescUploadedAtDesc(candidateUserId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));
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

    private Double maxSimilarity(JobEmbedding jobEmbedding, CvEmbedding cvSkills, CvEmbedding cvExperience) {
        Double skillSim = normalizeSimilarity(cosineSimilarity(jobEmbedding.getVector(), cvSkills.getVector()));
        Double expSim = normalizeSimilarity(cosineSimilarity(jobEmbedding.getVector(), cvExperience.getVector()));
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
        return Math.max(0.0, Math.min(1.0, similarity));
    }

    private int toPercent(double similarity) {
        double clamped = Math.max(0.0, Math.min(1.0, similarity));
        return (int) Math.round(clamped * 100.0);
    }

    private Map<UUID, Double> loadSimilarityScores(
            CvEmbedding cvEmbedding,
            JobEmbeddingType jobType,
            int topK,
            String status
    ) {
        if (cvEmbedding.getVector() == null) {
            return Map.of();
        }
        if (cvEmbedding.getModel() == null || cvEmbedding.getDimensions() == null) {
            return Map.of();
        }
        String vectorLiteral = toVectorLiteral(cvEmbedding.getVector());
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
            double similarity = distanceToSimilarity(row.getDistance());
            scores.put(row.getJobId(), similarity);
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

    private double distanceToSimilarity(Double distance) {
        if (distance == null) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, 1.0 - distance));
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(vector[i]);
        }
        builder.append("]");
        return builder.toString();
    }

    @Getter
    @Builder
    public static class MatchScore {
        private UUID jobId;
        private UUID cvId;
        private Integer fitScore;
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
