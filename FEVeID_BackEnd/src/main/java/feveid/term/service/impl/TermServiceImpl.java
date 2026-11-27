package feveid.term.service.impl;

import feveid.common.exception.ConflictException;
import feveid.common.exception.NotFoundException;
import feveid.term.dto.ShopStatusRequest;
import feveid.term.dto.TermRequest;
import feveid.term.dto.TermResponse;
import feveid.term.entity.Term;
import feveid.term.enums.TermSeason;
import feveid.term.repository.TermRepository;
import feveid.term.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TermServiceImpl implements TermService {

    // Thông báo lỗi dùng chung với test (TermTest)
    public static final String TERM_EXISTS = "Kỳ này đã tồn tại";
    public static final String TERM_OVERLAP = "Thời gian kỳ bị chồng lên một kỳ khác";

    private final TermRepository termRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public TermResponse getTermById(Long id) {
        return TermResponse.from(findById(id));
    }

    // Không có API sửa kỳ: tên và ngày đã cố định theo mùa + năm
    @Override
    @Transactional
    public TermResponse createTerm(TermRequest request) {
        TermSeason season = request.season();
        int year = request.year();
        String name = season.nameOf(year);
        LocalDate start = season.startOf(year);
        LocalDate end = season.endOf(year);
        if (termRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(TERM_EXISTS);
        }
        // Chỉ xảy ra với dữ liệu cũ tạo trước khi chốt lịch cố định (ngày lệch chuẩn)
        if (termRepository.existsByStartDateLessThanEqualAndEndDateGreaterThanEqual(end, start)) {
            throw new ConflictException(TERM_OVERLAP);
        }
        Term saved = termRepository.save(Term.builder()
                .name(name)
                .startDate(start)
                .endDate(end)
                .build());
        return TermResponse.from(saved);
    }

    // Không lock, không "kiểm tra rồi mới thêm" (2 request cùng thấy "chưa có" rồi cùng thêm -> 500):
    // để DB tự xử lý trùng bằng ON CONFLICT DO NOTHING, rồi đọc lại
    @Override
    @Transactional
    public Term getOrCreateContaining(LocalDate date) {
        Optional<Term> existing = termRepository.findContaining(date);
        if (existing.isPresent()) {
            return existing.get(); // đường thường gặp: kỳ đã có, không cần INSERT
        }
        TermSeason season = TermSeason.of(date);
        int year = date.getYear();
        termRepository.insertIfAbsent(season.nameOf(year), season.startOf(year), season.endOf(year));
        // Đã thêm (hoặc request khác vừa thêm trước) -> tìm thấy. Không thấy = bị chặn bởi kỳ cũ ngày lệch chuẩn
        return termRepository.findContaining(date)
                .orElseThrow(() -> new ConflictException(TERM_OVERLAP));
    }

    @Override
    @Transactional
    public TermResponse changeShopStatus(Long id, ShopStatusRequest request) {
        Term term = findById(id);
        // Gửi lại đúng trạng thái hiện tại thì vẫn 200, không lỗi (idempotent)
        term.setShopOpen(request.open());
        return TermResponse.from(term);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TermResponse> getTerms(Boolean shopOpen) {
        //null = không lọc (trả tất cả); true/false chỉ các kỳ đang mở/đóng gian hàng
        List<Term> terms = (shopOpen == null)
                ? termRepository.findAllByOrderByStartDateDesc()
                : termRepository.findByShopOpenOrderByStartDateDesc(shopOpen);
        return terms.stream().map(TermResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TermResponse getCurrentTerm() {
        // Lấy ngày qua Clock (không gọi LocalDate.now() trơn) để test đặt được "hôm nay"
        LocalDate today = LocalDate.now(clock);
        return termRepository.findContaining(today)
                .map(TermResponse::from)
                .orElseThrow(() -> new NotFoundException("Hiện không có kỳ nào đang diễn ra"));
    }

    private Term findById(Long id) {
        return termRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ"));
    }
}
