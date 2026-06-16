CREATE TABLE IF NOT EXISTS job_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS job_category_mappings (
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES job_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (job_id, category_id)
);

CREATE INDEX IF NOT EXISTS idx_job_category_mappings_category
    ON job_category_mappings(category_id);

INSERT INTO job_categories (id, code, name, display_order) VALUES
    ('00000000-0000-0000-0000-000000000101', 'INFORMATION_TECHNOLOGY', 'Công nghệ thông tin', 1),
    ('00000000-0000-0000-0000-000000000102', 'SALES_BUSINESS', 'Kinh doanh / Bán hàng', 2),
    ('00000000-0000-0000-0000-000000000103', 'MARKETING_COMMUNICATIONS', 'Marketing / Truyền thông', 3),
    ('00000000-0000-0000-0000-000000000104', 'FINANCE_ACCOUNTING', 'Tài chính / Kế toán', 4),
    ('00000000-0000-0000-0000-000000000105', 'HUMAN_RESOURCES', 'Nhân sự', 5),
    ('00000000-0000-0000-0000-000000000106', 'OPERATIONS_LOGISTICS', 'Vận hành / Logistics', 6),
    ('00000000-0000-0000-0000-000000000107', 'ENGINEERING_MANUFACTURING', 'Kỹ thuật / Sản xuất', 7),
    ('00000000-0000-0000-0000-000000000108', 'DESIGN_CREATIVE', 'Thiết kế / Sáng tạo', 8),
    ('00000000-0000-0000-0000-000000000109', 'EDUCATION_TRAINING', 'Giáo dục / Đào tạo', 9),
    ('00000000-0000-0000-0000-000000000110', 'HEALTHCARE', 'Y tế / Chăm sóc sức khỏe', 10),
    ('00000000-0000-0000-0000-000000000111', 'LEGAL', 'Luật / Pháp lý', 11),
    ('00000000-0000-0000-0000-000000000112', 'OTHER', 'Khác', 12)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    display_order = EXCLUDED.display_order;
