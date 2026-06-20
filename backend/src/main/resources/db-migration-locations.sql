CREATE TABLE IF NOT EXISTS locations (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(40) NOT NULL,
    aliases TEXT[] NOT NULL DEFAULT '{}',
    display_order INTEGER NOT NULL DEFAULT 0
);

ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES locations(id);

CREATE INDEX IF NOT EXISTS idx_jobs_location_id ON jobs(location_id);

INSERT INTO locations (id, code, name, type, aliases, display_order) VALUES
    ('00000000-0000-0000-0000-000000001001', 'HA_NOI', 'Hà Nội', 'PROVINCE', ARRAY['Hà Nội', 'Ha Noi', 'Hanoi'], 1),
    ('00000000-0000-0000-0000-000000001002', 'HUE', 'Huế', 'PROVINCE', ARRAY['Huế', 'Hue', 'Thừa Thiên Huế', 'Thua Thien Hue'], 2),
    ('00000000-0000-0000-0000-000000001003', 'LAI_CHAU', 'Lai Châu', 'PROVINCE', ARRAY['Lai Châu', 'Lai Chau'], 3),
    ('00000000-0000-0000-0000-000000001004', 'DIEN_BIEN', 'Điện Biên', 'PROVINCE', ARRAY['Điện Biên', 'Dien Bien'], 4),
    ('00000000-0000-0000-0000-000000001005', 'SON_LA', 'Sơn La', 'PROVINCE', ARRAY['Sơn La', 'Son La'], 5),
    ('00000000-0000-0000-0000-000000001006', 'LANG_SON', 'Lạng Sơn', 'PROVINCE', ARRAY['Lạng Sơn', 'Lang Son'], 6),
    ('00000000-0000-0000-0000-000000001007', 'QUANG_NINH', 'Quảng Ninh', 'PROVINCE', ARRAY['Quảng Ninh', 'Quang Ninh', 'Hạ Long', 'Ha Long'], 7),
    ('00000000-0000-0000-0000-000000001008', 'THANH_HOA', 'Thanh Hóa', 'PROVINCE', ARRAY['Thanh Hóa', 'Thanh Hoa'], 8),
    ('00000000-0000-0000-0000-000000001009', 'NGHE_AN', 'Nghệ An', 'PROVINCE', ARRAY['Nghệ An', 'Nghe An', 'Vinh'], 9),
    ('00000000-0000-0000-0000-000000001010', 'HA_TINH', 'Hà Tĩnh', 'PROVINCE', ARRAY['Hà Tĩnh', 'Ha Tinh'], 10),
    ('00000000-0000-0000-0000-000000001011', 'CAO_BANG', 'Cao Bằng', 'PROVINCE', ARRAY['Cao Bằng', 'Cao Bang'], 11),
    ('00000000-0000-0000-0000-000000001012', 'TUYEN_QUANG', 'Tuyên Quang', 'PROVINCE', ARRAY['Tuyên Quang', 'Tuyen Quang', 'Hà Giang', 'Ha Giang'], 12),
    ('00000000-0000-0000-0000-000000001013', 'LAO_CAI', 'Lào Cai', 'PROVINCE', ARRAY['Lào Cai', 'Lao Cai', 'Yên Bái', 'Yen Bai', 'Sa Pa', 'Sapa'], 13),
    ('00000000-0000-0000-0000-000000001014', 'THAI_NGUYEN', 'Thái Nguyên', 'PROVINCE', ARRAY['Thái Nguyên', 'Thai Nguyen', 'Bắc Kạn', 'Bac Kan'], 14),
    ('00000000-0000-0000-0000-000000001015', 'PHU_THO', 'Phú Thọ', 'PROVINCE', ARRAY['Phú Thọ', 'Phu Tho', 'Vĩnh Phúc', 'Vinh Phuc', 'Hòa Bình', 'Hoa Binh'], 15),
    ('00000000-0000-0000-0000-000000001016', 'BAC_NINH', 'Bắc Ninh', 'PROVINCE', ARRAY['Bắc Ninh', 'Bac Ninh', 'Bắc Giang', 'Bac Giang'], 16),
    ('00000000-0000-0000-0000-000000001017', 'HUNG_YEN', 'Hưng Yên', 'PROVINCE', ARRAY['Hưng Yên', 'Hung Yen', 'Thái Bình', 'Thai Binh'], 17),
    ('00000000-0000-0000-0000-000000001018', 'HAI_PHONG', 'Hải Phòng', 'PROVINCE', ARRAY['Hải Phòng', 'Hai Phong', 'Hải Dương', 'Hai Duong'], 18),
    ('00000000-0000-0000-0000-000000001019', 'NINH_BINH', 'Ninh Bình', 'PROVINCE', ARRAY['Ninh Bình', 'Ninh Binh', 'Hà Nam', 'Ha Nam', 'Nam Định', 'Nam Dinh'], 19),
    ('00000000-0000-0000-0000-000000001020', 'QUANG_TRI', 'Quảng Trị', 'PROVINCE', ARRAY['Quảng Trị', 'Quang Tri', 'Quảng Bình', 'Quang Binh', 'Đồng Hới', 'Dong Hoi'], 20),
    ('00000000-0000-0000-0000-000000001021', 'DA_NANG', 'Đà Nẵng', 'PROVINCE', ARRAY['Đà Nẵng', 'Da Nang', 'Danang', 'Quảng Nam', 'Quang Nam'], 21),
    ('00000000-0000-0000-0000-000000001022', 'QUANG_NGAI', 'Quảng Ngãi', 'PROVINCE', ARRAY['Quảng Ngãi', 'Quang Ngai', 'Kon Tum'], 22),
    ('00000000-0000-0000-0000-000000001023', 'GIA_LAI', 'Gia Lai', 'PROVINCE', ARRAY['Gia Lai', 'Bình Định', 'Binh Dinh', 'Pleiku', 'Quy Nhơn', 'Quy Nhon'], 23),
    ('00000000-0000-0000-0000-000000001024', 'KHANH_HOA', 'Khánh Hòa', 'PROVINCE', ARRAY['Khánh Hòa', 'Khanh Hoa', 'Ninh Thuận', 'Ninh Thuan', 'Nha Trang'], 24),
    ('00000000-0000-0000-0000-000000001025', 'LAM_DONG', 'Lâm Đồng', 'PROVINCE', ARRAY['Lâm Đồng', 'Lam Dong', 'Đắk Nông', 'Dak Nong', 'Đắc Nông', 'Bình Thuận', 'Binh Thuan', 'Đà Lạt', 'Da Lat', 'Phan Thiết', 'Phan Thiet'], 25),
    ('00000000-0000-0000-0000-000000001026', 'DAK_LAK', 'Đắk Lắk', 'PROVINCE', ARRAY['Đắk Lắk', 'Dak Lak', 'Đắc Lắc', 'Dac Lac', 'Phú Yên', 'Phu Yen', 'Buôn Ma Thuột', 'Buon Ma Thuot', 'Tuy Hòa', 'Tuy Hoa'], 26),
    ('00000000-0000-0000-0000-000000001027', 'HO_CHI_MINH', 'TP.HCM', 'PROVINCE', ARRAY['TP.HCM', 'TP HCM', 'HCM', 'Hồ Chí Minh', 'Ho Chi Minh', 'Ho Chi Minh City', 'Sài Gòn', 'Sai Gon', 'Bình Dương', 'Binh Duong', 'Bà Rịa - Vũng Tàu', 'Ba Ria Vung Tau', 'Vũng Tàu', 'Vung Tau', 'Thủ Dầu Một', 'Thu Dau Mot'], 27),
    ('00000000-0000-0000-0000-000000001028', 'DONG_NAI', 'Đồng Nai', 'PROVINCE', ARRAY['Đồng Nai', 'Dong Nai', 'Bình Phước', 'Binh Phuoc', 'Biên Hòa', 'Bien Hoa', 'Đồng Xoài', 'Dong Xoai'], 28),
    ('00000000-0000-0000-0000-000000001029', 'TAY_NINH', 'Tây Ninh', 'PROVINCE', ARRAY['Tây Ninh', 'Tay Ninh', 'Long An', 'Tân An', 'Tan An'], 29),
    ('00000000-0000-0000-0000-000000001030', 'CAN_THO', 'Cần Thơ', 'PROVINCE', ARRAY['Cần Thơ', 'Can Tho', 'Hậu Giang', 'Hau Giang', 'Sóc Trăng', 'Soc Trang'], 30),
    ('00000000-0000-0000-0000-000000001031', 'VINH_LONG', 'Vĩnh Long', 'PROVINCE', ARRAY['Vĩnh Long', 'Vinh Long', 'Bến Tre', 'Ben Tre', 'Trà Vinh', 'Tra Vinh'], 31),
    ('00000000-0000-0000-0000-000000001032', 'DONG_THAP', 'Đồng Tháp', 'PROVINCE', ARRAY['Đồng Tháp', 'Dong Thap', 'Tiền Giang', 'Tien Giang', 'Mỹ Tho', 'My Tho'], 32),
    ('00000000-0000-0000-0000-000000001033', 'CA_MAU', 'Cà Mau', 'PROVINCE', ARRAY['Cà Mau', 'Ca Mau', 'Bạc Liêu', 'Bac Lieu'], 33),
    ('00000000-0000-0000-0000-000000001034', 'AN_GIANG', 'An Giang', 'PROVINCE', ARRAY['An Giang', 'Kiên Giang', 'Kien Giang', 'Rạch Giá', 'Rach Gia', 'Phú Quốc', 'Phu Quoc'], 34),
    ('00000000-0000-0000-0000-000000001099', 'OTHER', 'Khác', 'PROVINCE', ARRAY['Khác', 'Other'], 999)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    aliases = EXCLUDED.aliases,
    display_order = EXCLUDED.display_order;

UPDATE jobs
SET location_id = (SELECT id FROM locations WHERE code = 'HO_CHI_MINH')
WHERE location_id IN (
    SELECT id FROM locations WHERE code IN ('BINH_DUONG', 'BA_RIA_VUNG_TAU')
);

DELETE FROM locations
WHERE code IN ('BINH_DUONG', 'BA_RIA_VUNG_TAU');

UPDATE jobs j
SET location_id = l.id
FROM locations l
WHERE j.location_id IS NULL
  AND j.location IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM unnest(l.aliases) AS alias
      WHERE j.location ILIKE concat('%', alias, '%')
  );
