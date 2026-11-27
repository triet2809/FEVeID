package feveid.event.service.impl;

import feveid.common.exception.BadRequestException;
import feveid.common.exception.ConflictException;
import feveid.common.exception.NotFoundException;
import feveid.event.dto.EventStaffRequest;
import feveid.event.dto.EventStaffResponse;
import feveid.event.dto.StaffAssignmentRequest;
import feveid.event.entity.Event;
import feveid.event.entity.EventStaff;
import feveid.event.enums.EventPublication;
import feveid.event.repository.EventRepository;
import feveid.event.repository.EventStaffRepository;
import feveid.event.service.EventStaffService;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import feveid.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventStaffServiceImpl implements EventStaffService {

    // Thông báo lỗi dùng chung với test (EventStaffTest)
    public static final String EVENT_NOT_FOUND = "Không tìm thấy sự kiện";
    public static final String EVENT_CANCELLED = "Sự kiện đã hủy, không thể sửa team";
    public static final String EVENT_FINALIZED = "Sự kiện đã chốt, không thể sửa team";
    public static final String DUPLICATE_MEMBER = "Mỗi thành viên chỉ có một vai trò trong sự kiện";
    public static final String MEMBER_NOT_FOUND = "Thành viên không tồn tại";
    public static final String MEMBER_DISQUALIFIED = "Thành viên đã bị loại khỏi CLB, không thể thêm vào team";

    private final EventRepository eventRepository;
    private final EventStaffRepository eventStaffRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EventStaffResponse> getStaff(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException(EVENT_NOT_FOUND);
        }
        return staffOf(eventId);
    }

    @Override
    @Transactional
    public List<EventStaffResponse> replaceStaff(Long eventId, EventStaffRequest request) {
        // Khóa dòng sự kiện: 2 HR lưu team cùng lúc thì lần lượt (nếu không, cả hai cùng xóa rồi cùng thêm -> trùng
        // unique -> 500). Bài 13.2 chốt sự kiện cũng khóa đúng dòng này, nên không lưu team chen giữa lúc đang chốt
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException(EVENT_NOT_FOUND));
        if (event.getPublication() == EventPublication.CANCELLED) {
            throw new ConflictException(EVENT_CANCELLED);
        }
        // Đã chốt thì điểm đã phát theo team lúc chốt -> sửa team lúc này không còn ý nghĩa
        if (event.isFinalized()) {
            throw new ConflictException(EVENT_FINALIZED);
        }

        List<StaffAssignmentRequest> assignments = request.staff();
        Set<Long> memberIds = new HashSet<>();
        for (StaffAssignmentRequest assignment : assignments) {
            if (!memberIds.add(assignment.memberId())) {
                throw new BadRequestException(DUPLICATE_MEMBER);
            }
        }

        // 1 câu SELECT ... WHERE id IN (...) thay vì findById từng người
        Map<Long, Member> members = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        // id nằm trong body -> sai id là dữ liệu gửi lên sai -> 400 (giống ownerMemberId ở EventServiceImpl)
        if (members.size() != memberIds.size()) {
            throw new BadRequestException(MEMBER_NOT_FOUND);
        }
        if (members.values().stream().anyMatch(m -> m.getMemberStatus() == MemberStatus.DISQUALIFIED)) {
            throw new BadRequestException(MEMBER_DISQUALIFIED);
        }

        eventStaffRepository.deleteByEventId(eventId);
        eventStaffRepository.saveAll(assignments.stream()
                .map(assignment -> EventStaff.builder()
                        .event(event)
                        .member(members.get(assignment.memberId()))
                        .role(assignment.role())
                        .build())
                .toList());
        return staffOf(eventId);
    }

    private List<EventStaffResponse> staffOf(Long eventId) {
        return eventStaffRepository.findByEventIdOrderByRoleAscMemberFullNameAsc(eventId).stream()
                .map(EventStaffResponse::from)
                .toList();
    }
}
