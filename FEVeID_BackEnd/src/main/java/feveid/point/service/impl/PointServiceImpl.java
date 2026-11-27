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
        // 1. người ghi giao dịch (HR/BCN)
        Member staff = memberRepository.findByAccountEmail(staffEmail)
                .orElseThrow(() -> new NotFoundException(MEMBER_NOT_FOUND));
        // 2. Khóa dòng thành viên trước khi tính số dư: 2 lần trừ điểm cùng lúc phải xếp hàng ở đây
        // nếu không cả hai cùng thấy số dư cũ, cùng qua kiểm tra -> âm điểm
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new NotFoundException(MEMBER_NOT_FOUND));
        // 3. Kỳ được điều chỉnh: chọn rõ (sửa điểm kỹ cũ) hoặc mặc định kỳ hiện tại
        Term term = request.termId() != null
                ? termRepository.findById(request.termId())
                .orElseThrow(() -> new  NotFoundException(TERM_NOT_FOUND))
                : currentTerm()
                .orElseThrow(() -> new ConflictException(NO_CURRENT_TERM));
        // 4. Số dư của kì đó sau điều chỉnh không được âm
        // Chỉ đúng vì được khóa ở bước 2
        long balance = pointTransactionRepository.balanceOf(member.getId(), term.getId());
        if(balance + request.amount() <0) {
            throw new  ConflictException(NEGATIVE_BALANCE);
        }
        // 5. Thêm 1 dòng mới vào sổ cái, không sửa dòng nào cũ
        PointTransaction transaction = PointTransaction.builder()
                .member(member)
                .term(term)
                .amount(request.amount())
                .type(PointTransactionType.ADJUSTMENT)
                .reason(request.reason())
                .createdBy(staff)
                .build();
        return PointTransactionResponse.from(pointTransactionRepository.save(transaction));
    }

    // Kỳ chứa ngày hôm nay theo giờ VN (Clock của TimeConfig). Khoảng trống giữa 2 kỳ -> rỗng
    private Optional<Term> currentTerm() {
        return termRepository.findContaining(LocalDate.now(clock));
    }
}
