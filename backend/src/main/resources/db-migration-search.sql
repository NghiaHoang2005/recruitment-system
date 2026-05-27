-- Migration: Add normalized text + FTS vectors for CV/Job matching
-- Date: 2026-05-26
-- Purpose: Phase 0 baseline alignment for hybrid matching

ALTER TABLE cvs ADD COLUMN IF NOT EXISTS normalized_text TEXT;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS normalized_text TEXT;

ALTER TABLE cvs ADD COLUMN IF NOT EXISTS search_tsv tsvector;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS search_tsv tsvector;

CREATE OR REPLACE FUNCTION compute_cv_search_tsv(
    p_normalized TEXT,
    p_raw TEXT,
    p_parsed JSONB
) RETURNS tsvector AS $$
BEGIN
    RETURN setweight(to_tsvector('simple', COALESCE(p_normalized, '')), 'A')
        || setweight(to_tsvector('simple', COALESCE(p_raw, '')), 'B')
        || setweight(to_tsvector('simple', COALESCE(p_parsed::text, '')), 'C');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION update_cv_search_tsv()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_tsv = compute_cv_search_tsv(NEW.normalized_text, NEW.raw_text, NEW.parsed_data);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS cv_search_tsv_trigger ON cvs;
CREATE TRIGGER cv_search_tsv_trigger
BEFORE INSERT OR UPDATE OF normalized_text, raw_text, parsed_data ON cvs
FOR EACH ROW EXECUTE FUNCTION update_cv_search_tsv();

CREATE OR REPLACE FUNCTION compute_job_search_tsv(p_job_id UUID)
RETURNS tsvector AS $$
DECLARE
    job_title TEXT;
    job_description TEXT;
    job_normalized TEXT;
    req_text TEXT;
BEGIN
    SELECT title, description, normalized_text
    INTO job_title, job_description, job_normalized
    FROM jobs
    WHERE id = p_job_id;

    SELECT string_agg(jri.content, ' ')
    INTO req_text
    FROM job_requirement_items jri
    JOIN job_requirement_sections jrs ON jrs.id = jri.section_id
    WHERE jrs.job_id = p_job_id;

    RETURN setweight(to_tsvector('simple', COALESCE(job_normalized, '')), 'A')
        || setweight(to_tsvector('simple', COALESCE(job_title, '')), 'A')
        || setweight(to_tsvector('simple', COALESCE(job_description, '')), 'B')
        || setweight(to_tsvector('simple', COALESCE(req_text, '')), 'C');
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_job_search_tsv()
RETURNS TRIGGER AS $$
DECLARE
    req_text TEXT;
BEGIN
    SELECT string_agg(jri.content, ' ')
    INTO req_text
    FROM job_requirement_items jri
    JOIN job_requirement_sections jrs ON jrs.id = jri.section_id
    WHERE jrs.job_id = NEW.id;

    NEW.search_tsv = setweight(to_tsvector('simple', COALESCE(NEW.normalized_text, '')), 'A')
        || setweight(to_tsvector('simple', COALESCE(NEW.title, '')), 'A')
        || setweight(to_tsvector('simple', COALESCE(NEW.description, '')), 'B')
        || setweight(to_tsvector('simple', COALESCE(req_text, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS job_search_tsv_trigger ON jobs;
CREATE TRIGGER job_search_tsv_trigger
BEFORE INSERT OR UPDATE OF normalized_text, title, description ON jobs
FOR EACH ROW EXECUTE FUNCTION update_job_search_tsv();

CREATE OR REPLACE FUNCTION refresh_job_search_tsv_on_item()
RETURNS TRIGGER AS $$
DECLARE
    job_id UUID;
BEGIN
    SELECT jrs.job_id
    INTO job_id
    FROM job_requirement_sections jrs
    WHERE jrs.id = COALESCE(NEW.section_id, OLD.section_id);

    IF job_id IS NULL THEN
        RETURN NULL;
    END IF;

    UPDATE jobs
    SET search_tsv = compute_job_search_tsv(job_id)
    WHERE id = job_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS job_req_item_search_tsv_trigger ON job_requirement_items;
CREATE TRIGGER job_req_item_search_tsv_trigger
AFTER INSERT OR UPDATE OR DELETE ON job_requirement_items
FOR EACH ROW EXECUTE FUNCTION refresh_job_search_tsv_on_item();

CREATE OR REPLACE FUNCTION refresh_job_search_tsv_on_section()
RETURNS TRIGGER AS $$
DECLARE
    job_id UUID;
BEGIN
    job_id := COALESCE(NEW.job_id, OLD.job_id);
    IF job_id IS NULL THEN
        RETURN NULL;
    END IF;

    UPDATE jobs
    SET search_tsv = compute_job_search_tsv(job_id)
    WHERE id = job_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS job_req_section_search_tsv_trigger ON job_requirement_sections;
CREATE TRIGGER job_req_section_search_tsv_trigger
AFTER INSERT OR UPDATE OR DELETE ON job_requirement_sections
FOR EACH ROW EXECUTE FUNCTION refresh_job_search_tsv_on_section();

CREATE INDEX IF NOT EXISTS idx_cvs_search_tsv ON cvs USING gin (search_tsv);
CREATE INDEX IF NOT EXISTS idx_jobs_search_tsv ON jobs USING gin (search_tsv);

UPDATE cvs
SET normalized_text = COALESCE(normalized_text, raw_text)
WHERE normalized_text IS NULL;

UPDATE jobs
SET normalized_text = COALESCE(normalized_text, description)
WHERE normalized_text IS NULL;

UPDATE cvs
SET search_tsv = compute_cv_search_tsv(normalized_text, raw_text, parsed_data);

UPDATE jobs
SET search_tsv = compute_job_search_tsv(id);
