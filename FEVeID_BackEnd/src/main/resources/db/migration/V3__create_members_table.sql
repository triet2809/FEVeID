CREATE TABLE members (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id BIGINT NOT NULL UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL ,
    student_code VARCHAR(20) UNIQUE,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    member_status VARCHAR(30) NOT NULL DEFAULT 'CTV',
    joined_at DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_members_status ON members (member_status);

--Tài khoản tạo trước khi tạo bảng member chưa có hồ sơ:
--tạo hồ sơ tạm, lấy email làm họ tên (HR sửa lại sau)
INSERT INTO members (account_id, full_name, member_status)
SELECT id, email, 'ACTIVE' FROM accounts;