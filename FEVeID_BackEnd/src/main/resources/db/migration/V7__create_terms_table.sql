CREATE TABLE terms(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    -- Gian hàng đổi quà của kì này: HR mở đóng thủ công
    shop_open BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_terms_dates CHECK ( end_date > start_date ),
    -- không có 2 kỳ chồng nhau ('[]' = tính cả ngày đầu và ngày cuối)
    CONSTRAINT ex_terms_no_overlap EXCLUDE USING gist (daterange(start_date, end_date,'[]') WITH &&)
);
CREATE UNIQUE INDEX up_terms_name_lower ON terms (LOWER(name));