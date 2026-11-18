package feveid.term.service.impl;

import feveid.common.exception.ConflictException;
import feveid.common.exception.NotFoundException;
import feveid.term.dto.ShopStatusRequest;
import feveid.term.dto.TermRequest;
import feveid.term.dto.TermResponse;
import feveid.term.entity.Term;
import feveid.term.repository.TermRepository;
import feveid.term.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TermServiceImpl implements TermService {

    private final TermRepository termRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TermResponse> getTerms() {
        return termRepository.findAllByOrderByStartDateDesc().stream()
                .map(TermResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TermResponse getTermById(Long id) {
        return TermResponse.from(findById(id));
    }

    @Override
    @Transactional
    public TermResponse createTerm(TermRequest request) {
        if(termRepository.existsByNameIgnoreCase(request.name())){
            throw new ConflictException("Tên kỳ đã tồn tại");
        }
        if(termRepository.existsByStartDateLessThanEqualAndEndDateGreaterThanEqual(request.endDate(), request.startDate())){
            throw new ConflictException("Thời gian kỳ bị chồng lên một kỳ khác");
        }
        Term saved = termRepository.save(Term.builder()
                .name(request.name())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build());
        return TermResponse.from(saved);
    }

    @Override
    @Transactional
    public TermResponse updateTerm(Long id, TermRequest request) {
        Term term = findById(id);
        // So với các kỳ KHÁC (IdNot): giữ nguyên tên/ngày của chính mình thì không tính là trùng
        if(termRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)){
            throw new ConflictException("Tên kỳ đã tồn tại");

        }
        if(termRepository.existsByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(request.endDate(), request.startDate(), id)){
            throw new ConflictException("Thời gian kỳ bị chồng lên một kỳ khác");
        }
        term.setName(request.name());
        term.setStartDate(request.startDate());
        term.setEndDate(request.endDate());
        // shopOpen không đổi ở đây: mở/đóng shop sẽ có endpoint riêng
        return TermResponse.from(term);
    }

    @Override
    @Transactional
    public TermResponse changeShopStatus(Long id, ShopStatusRequest request) {
        Term term = findById(id);
        // Gửi lại đúng trạng thái hiện tại thì vẫn 200, không lỗi (idempotent)
        term.setShopOpen(request.open());
        return TermResponse.from(term);
    }
    private Term findById(Long id) {
        return termRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ"));
    }
}
