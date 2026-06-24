-- Migration: Add profile fields to recruiters table
-- Adds headline and profile_picture_url columns to support recruiter personal profile page

ALTER TABLE recruiters
    ADD COLUMN IF NOT EXISTS headline VARCHAR(255),
    ADD COLUMN IF NOT EXISTS profile_picture_url VARCHAR(1024);
