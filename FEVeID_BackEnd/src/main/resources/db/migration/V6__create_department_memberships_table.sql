CREATE TABLE department_memberships(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    department_id  BIGINT NOT NULL REFERENCES departments(id),
    position VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dm_dates CHECK ( end_date IS NULL OR end_date >= start_date)
);
-- Mỗi thành viên chỉ ở 1 ban trong 1 thời điểm
CREATE UNIQUE INDEX uq_dm_member_current
    ON department_memberships (member_id)
    WHERE end_date IS NULL;

-- Mỗi bạn chỉ có 1 trưởng ban duy nhất
CREATE UNIQUE INDEX uq_dm_leader_current
    ON department_memberships (department_id)
    WHERE position = 'LEADER' AND end_date IS NULL ;

CREATE INDEX idx_dm_department_current
    ON department_memberships (department_id)
    WHERE end_date IS NULL;

