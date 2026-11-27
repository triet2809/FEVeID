package feveid.term.repository;

import feveid.term.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term,Long> {
    List<Term> findAllByOrderByStartDateDesc();

    //Hai đoạn [a, b] và [c, d] giao nhau <==> a <= d và c <= b
    // -> kỳ có sẵn giao với [start, end] <==> term.startDate <= end VÀ term.endDate >= start
    // Tham số gán THEO VỊ TRÍ: end trước, start sau
    boolean existsByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate end, LocalDate start);

    boolean existsByNameIgnoreCase(String name);

    //Kỳ chứa ngày date(tính cả ngày đầu và ngày cuối). Các kỳ không chồng lên nhau nên có tối đa 1 kỳ
    @Query("SELECT t FROM Term t WHERE t.startDate <= :date AND t.endDate >= :date")
    Optional<Term> findContaining(@Param("date") LocalDate date);
    List<Term> findByShopOpenOrderByStartDateDesc(boolean shopOpen);

    // Thêm kỳ nếu chưa có. ON CONFLICT DO NOTHING (không ghi tên constraint): đụng unique tên (V7) hoặc
    // EXCLUDE chồng ngày (V7) thì bỏ qua, không báo lỗi. 2 request cùng tạo một kỳ: request sau ĐỢI request
    // trước commit rồi bỏ qua -> không có 500 duplicate key. Trả về số dòng đã thêm (0 hoặc 1)
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO terms (name, start_date, end_date) VALUES (:name, :startDate, :endDate)
            ON CONFLICT DO NOTHING
            """)
    int insertIfAbsent(@Param("name") String name,
                       @Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate);
}
