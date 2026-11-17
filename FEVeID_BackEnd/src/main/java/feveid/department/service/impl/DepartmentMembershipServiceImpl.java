package feveid.department.service.impl;

import feveid.common.exception.ConflictException;
import feveid.common.exception.NotFoundException;
import feveid.department.dto.AssignDepartmentRequest;
import feveid.department.dto.DepartmentMemberResponse;
import feveid.department.dto.MembershipResponse;
import feveid.department.entity.Department;
import feveid.department.entity.DepartmentMembership;
import feveid.department.enums.DepartmentPosition;
import feveid.department.repository.DepartmentMembershipRepository;
import feveid.department.repository.DepartmentRepository;
import feveid.department.service.DepartmentMembershipService;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import feveid.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartmentMembershipServiceImpl implements DepartmentMembershipService {

    private final DepartmentMembershipRepository membershipRepository;
    private final DepartmentRepository departmentRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public MembershipResponse assign(Long memberId, AssignDepartmentRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên"));
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ban"));

        if (member.getMemberStatus() == MemberStatus.DISQUALIFIED) {
            throw new ConflictException("Thành viên đã bị xóa khỏi câu lạc bộ, không thể gán vào ban! Vui lòng bắt đầu hành trình lại từ CTV");
        }

        Optional<DepartmentMembership> current = membershipRepository.findByMemberIdAndEndDateIsNull(memberId);

        //PUT idemotent: gửi lại đúng ban + chức vụ hiện tại thì không tạo lịch sử mới
        if (current.isPresent() && current.get().getDepartment().getId().equals(department.getId()) //vì kiểu dữ liệu là Long nên dùng equals không dùng ==
                && current.get().getPosition() == request.position() // lần này kiểu dữ liệu là enum nên == được
        ) {
            return MembershipResponse.from(current.get());
        }
        if (request.position() == DepartmentPosition.LEADER
                && membershipRepository.existsByDepartmentIdAndPositionAndEndDateIsNull(department.getId(), DepartmentPosition.LEADER)) {
            throw new ConflictException("Ban này đã có trưởng ban");
        }

        current.ifPresent(old -> {
            old.setEndDate(LocalDate.now());
            // Đẩy UPDATE xuống DB NGAY, trước khi INSERT dòng mới
            membershipRepository.flush();
        });
        DepartmentMembership saved = membershipRepository.save(DepartmentMembership.builder()
                .member(member)
                .department(department)
                .position(request.position())
                .build());
        return MembershipResponse.from(saved);
    }

    @Override
    @Transactional
    public void remove(Long memberId) {
        DepartmentMembership current = membershipRepository.findByMemberIdAndEndDateIsNull(memberId)
                .orElseThrow(() -> new NotFoundException("Thành viên chưa thuộc ban nào"));
        current.setEndDate(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentMemberResponse> getCurrentMembers(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new NotFoundException("Không tìm thấy ban");
        }
        return membershipRepository.findByDepartmentIdAndEndDateIsNull(departmentId).stream()
                .sorted(Comparator.comparing(DepartmentMembership::getPosition)          // theo thứ tự khai báo enum
                        .thenComparing(m -> m.getMember().getFullName()))
                .map(DepartmentMemberResponse::from)
                .toList();
    }
}
