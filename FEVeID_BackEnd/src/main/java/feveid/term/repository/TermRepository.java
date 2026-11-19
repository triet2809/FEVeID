package feveid.term.repository;

import feveid.term.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // Dùng khi tạo mới
    boolean existsByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate end, LocalDate start);


    //Dùng khi cập nhật: bỏ qua chính kỳ đang sửa
    boolean existsByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(LocalDate end, LocalDate start, Long id);

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    //Kỳ chứa ngày date(tính cả ngày đầu và ngày cuối). Các kỳ không chồng lên nhau nên có tối đa 1 kỳ
    @Query("SELECT t FROM Term t WHERE t.startDate <= :date AND t.endDate >= :date")
    Optional<Term> findContaining(@Param("date") LocalDate date);
    List<Term> findByShopOpenOrderByStartDateDesc(boolean shopOpen);


}
