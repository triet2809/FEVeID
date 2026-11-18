package feveid.term.repository;

import feveid.term.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

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


}
