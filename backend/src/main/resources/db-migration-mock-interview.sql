CREATE TABLE IF NOT EXISTS mock_interview_sessions (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL REFERENCES candidates(user_id),
    job_id UUID NOT NULL REFERENCES jobs(id),
    interview_type VARCHAR(20) NOT NULL,
    language VARCHAR(10) NOT NULL,
    planned_duration_minutes INTEGER NOT NULL,
    soft_limit_seconds INTEGER NOT NULL,
    hard_limit_seconds INTEGER NOT NULL,
    actual_duration_seconds INTEGER,
    status VARCHAR(30) NOT NULL,
    prompt_version VARCHAR(100),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    failure_code VARCHAR(100),
    failure_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mock_interview_candidate_created
    ON mock_interview_sessions(candidate_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_mock_interview_job
    ON mock_interview_sessions(job_id);

CREATE TABLE IF NOT EXISTS mock_interview_questions (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mock_interview_sessions(id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    question_type VARCHAR(30) NOT NULL,
    question_text TEXT NOT NULL,
    competency VARCHAR(100),
    expected_topics JSONB,
    rubric JSONB,
    is_follow_up BOOLEAN NOT NULL DEFAULT FALSE,
    parent_question_id UUID REFERENCES mock_interview_questions(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mock_interview_question_sequence UNIQUE(session_id, sequence_number)
);

CREATE TABLE IF NOT EXISTS mock_interview_turns (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mock_interview_sessions(id) ON DELETE CASCADE,
    question_id UUID REFERENCES mock_interview_questions(id) ON DELETE SET NULL,
    client_event_id UUID NOT NULL,
    sequence_number INTEGER NOT NULL,
    speaker VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    started_offset_ms INTEGER,
    ended_offset_ms INTEGER,
    is_final BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mock_interview_turn_sequence UNIQUE(session_id, sequence_number),
    CONSTRAINT uq_mock_interview_turn_event UNIQUE(session_id, client_event_id)
);

CREATE TABLE IF NOT EXISTS mock_interview_feedback (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE REFERENCES mock_interview_sessions(id) ON DELETE CASCADE,
    overall_score INTEGER NOT NULL CHECK (overall_score BETWEEN 0 AND 100),
    score_label VARCHAR(30) NOT NULL,
    confidence VARCHAR(20) NOT NULL,
    overall_summary TEXT NOT NULL,
    criteria_scores JSONB NOT NULL,
    strengths JSONB NOT NULL,
    improvements JSONB NOT NULL,
    next_steps JSONB NOT NULL,
    question_feedback JSONB NOT NULL,
    schema_version VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
