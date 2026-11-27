package feveid.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

// Dọn sạch DB trước mỗi test, dùng chung cho mọi class test chạy trên Postgres thật (Testcontainers).
// Các class test dùng CHUNG một DB, và trên CI (Linux) surefire chạy class theo thứ tự file system chứ không
// theo bảng chữ cái -> dữ liệu class trước để lại có thể chặn class sau (vd event_staff còn trỏ tới members
// thì members.deleteAll() báo lỗi khóa ngoại). Vì vậy không dọn bằng deleteAll() từng bảng theo thứ tự tự nhớ.
public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    // TRUNCATE mọi bảng trong 1 câu: CASCADE nên không cần biết thứ tự khóa ngoại, bảng mới thêm sau này
    // cũng tự được dọn. TRUNCATE không kích hoạt trigger theo dòng nên sổ cái điểm (V12) vẫn dọn được.
    // Bỏ qua flyway_schema_history (lịch sử migration)
    public static void clean(JdbcTemplate jdbcTemplate) {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT quote_ident(tablename) FROM pg_tables
                WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                """, String.class);
        if (!tables.isEmpty()) {
            jdbcTemplate.execute("TRUNCATE " + String.join(", ", tables) + " CASCADE");
        }
    }
}
