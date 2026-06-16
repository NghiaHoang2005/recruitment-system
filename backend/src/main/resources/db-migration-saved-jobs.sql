CREATE TABLE IF NOT EXISTS candidate_saved_jobs (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL,
    job_id UUID NOT NULL,
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_candidate_saved_jobs_candidate_job UNIQUE (candidate_id, job_id),
    CONSTRAINT fk_candidate_saved_jobs_candidate
        FOREIGN KEY (candidate_id) REFERENCES candidates(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_candidate_saved_jobs_job
        FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_candidate_saved_jobs_candidate_saved_at
    ON candidate_saved_jobs(candidate_id, saved_at DESC);
