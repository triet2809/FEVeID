package feveid.term.service;

import feveid.term.dto.ShopStatusRequest;
import feveid.term.dto.TermRequest;
import feveid.term.dto.TermResponse;
import feveid.term.entity.Term;

import java.time.LocalDate;
import java.util.List;

public interface TermService {
    TermResponse getTermById(Long id);
    TermResponse createTerm(TermRequest request);
    TermResponse changeShopStatus(Long id, ShopStatusRequest request);
    List<TermResponse> getTerms(Boolean shopOpen);
    TermResponse getCurrentTerm();

    // Dùng giữa các service khi GHI điểm (điều chỉnh tay, chốt sự kiện): kỳ chứa ngày date, chưa có thì tự tạo
    // theo lịch cố định. Chỗ chỉ đọc (xem điểm, /api/terms/current) không gọi hàm này
    Term getOrCreateContaining(LocalDate date);
}
