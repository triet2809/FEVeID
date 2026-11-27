package feveid.point.service.impl;

import feveid.common.exception.ConflictException;
import feveid.common.exception.NotFoundException;
import feveid.member.entity.Member;
import feveid.member.repository.MemberRepository;
import feveid.point.dto.PointAdjustmentRequest;
import feveid.point.dto.PointSummaryResponse;
import feveid.point.dto.PointTransactionResponse;
import feveid.point.entity.PointTransaction;
import feveid.point.enums.PointTransactionType;
import feveid.point.repository.PointTransactionRepository;
import feveid.point.service.PointService;
import feveid.term.entity.Term;
import feveid.term.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    // Thông báo lỗi dùng chung với test (PointTest)
    public static final String MEMBER_NOT_FOUND = "Không tìm thấy thành viên";
    public static final String TERM_NOT_FOUND = "Không tìm thấy kỳ";
    public static final String NO_CURRENT_TERM = "Hôm nay không thuộc kỳ hoạt động nào, hãy chọn kỳ cần điều chỉnh";
    public static final String NEGATIVE_BALANCE = "Không đủ điểm: số dư của kỳ không được âm";

    private final PointTransactionRepository pointTransactionRepository;
    private final MemberRepository memberRepository;
    private final TermRepository termRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PointSummaryResponse getMySummary(String email) {
        // Đã qua bộ lọc JWT nên tài khoản chắc chắn tồn tại; không có hồ sơ member là lỗi dữ liệu
        Member member = memberRepository.findByAccountEmail(email)
                .orElseThrow(() -> new NotFoundException(MEMBER_NOT_FOUND));
        return summaryOf(member);
    }

    @Override
    @Transactional(readOnly = true)
    public PointSummaryResponse getMemberSummary(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(MEMBER_NOT_FOUND));
        return summaryOf(member);
    }

    // Không có cột số dư nào: mọi con số đều cộng từ sổ cái lúc đọc
    private PointSummaryResponse summaryOf(Member member) {
        Optional<Term> current = currentTerm();
        long balance = current
                .map(term -> pointTransactionRepository.balanceOf(member.getId(), term.getId()))
                .orElse(0L);
        return new PointSummaryResponse(
                member.getId(),
                member.getFullName(),
                current.map(Term::getId).orElse(null),
                current.map(Term::getName).orElse(null),
                balance,
                pointTransactionRepository.lifetimeOf(member.getId(), PointTransactionType.SHOP),
                pointTransactionRepository.findByMemberIdOrderByCreatedAtDescIdDesc(member.getId()).stream()
                        .map(PointTransactionResponse::from)
                        .toList());
    }

    // LÕI – chủ dự án tự viết. Thứ tự giống đăng ký ở Bài 11: khóa -> kiểm tra -> ghi
    @Override
    @Transactional
    public PointTransactionResponse adjust(String staffEmail, Long memberId, PointAdjustmentRequest request) {
        // TODO 1. staff = người ghi giao dịch (memberRepository.findByAccountEmail)
        // TODO 2. KHÓA dòng thành viên được điều chỉnh (memberRepository.findByIdForUpdate),
        //         không có -> NotFoundException(MEMBER_NOT_FOUND)
        // TODO 3. Kỳ: request.termId() có giá trị -> termRepository.findById, không có -> NotFoundException(TERM_NOT_FOUND);
        //         bỏ trống -> currentTerm(), không có -> ConflictException(NO_CURRENT_TERM)
        // TODO 4. Số dư sau điều chỉnh = balanceOf(...) + request.amount(); < 0 -> ConflictException(NEGATIVE_BALANCE)
        // TODO 5. Lưu PointTransaction mới: type ADJUSTMENT, amount, reason, term, member, createdBy = staff
        // TODO 6. return PointTransactionResponse.from(...)
        throw new UnsupportedOperationException("TODO: Bài 12.4");
    }

    // Kỳ chứa ngày hôm nay theo giờ VN (Clock của TimeConfig). Khoảng trống giữa 2 kỳ -> rỗng
    private Optional<Term> currentTerm() {
        return termRepository.findContaining(LocalDate.now(clock));
    }
}
