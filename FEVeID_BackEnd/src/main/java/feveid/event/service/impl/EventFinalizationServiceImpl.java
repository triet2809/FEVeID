package feveid.event.service.impl;

import feveid.common.config.TimeConfig;
import feveid.common.exception.BadRequestException;
import feveid.common.exception.ConflictException;
import feveid.common.exception.NotFoundException;
import feveid.event.dto.FinalizationResponse;
import feveid.event.dto.FinalizationResponse.Person;
import feveid.event.dto.FinalizeEventRequest;
import feveid.event.entity.Event;
import feveid.event.entity.EventStaff;
import feveid.event.enums.EventStatus;
import feveid.event.enums.PhaseKind;
import feveid.event.enums.StaffRole;
import feveid.event.repository.EventRepository;
import feveid.event.repository.EventStaffRepository;
import feveid.event.schedule.EventStatusCalculator;
import feveid.event.service.EventFinalizationService;
import feveid.member.entity.Member;
import feveid.member.enums.MemberStatus;
import feveid.member.repository.MemberRepository;
import feveid.point.config.PointRules;
import feveid.point.entity.PointTransaction;
import feveid.point.enums.PointTransactionType;
import feveid.point.repository.PointTransactionRepository;
import feveid.registration.entity.Registration;
import feveid.registration.enums.RegistrationStatus;
import feveid.registration.repository.RegistrationRepository;
import feveid.term.entity.Term;
import feveid.term.enums.TermSeason;
import feveid.term.repository.TermRepository;
import feveid.term.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static feveid.event.service.impl.EventStaffServiceImpl.DUPLICATE_MEMBER;
import static feveid.event.service.impl.EventStaffServiceImpl.MEMBER_DISQUALIFIED;
import static feveid.event.service.impl.EventStaffServiceImpl.MEMBER_NOT_FOUND;

// Chốt sự kiện (requirements.md 11.1, Bài 13.2)
@Service
@RequiredArgsConstructor
public class EventFinalizationServiceImpl implements EventFinalizationService {

    // Thông báo lỗi dùng chung với test (FinalizeEventTest) và các chỗ bị khóa sau khi chốt
    public static final String EVENT_NOT_FOUND = "Không tìm thấy sự kiện";
    public static final String ALREADY_FINALIZED = "Sự kiện đã được chốt";
    public static final String NOT_COMPLETED = "Chỉ chốt được sau khi sự kiện kết thúc";

    private static final Comparator<Person> BY_NAME = Comparator.comparing(Person::fullName);

    private final EventRepository eventRepository;
    private final EventStaffRepository eventStaffRepository;
    private final MemberRepository memberRepository;
    private final RegistrationRepository registrationRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final TermRepository termRepository;
    private final TermService termService;
    private final PointRules pointRules;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public FinalizationResponse getFinalization(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(EVENT_NOT_FOUND));
        return event.isFinalized() ? resultOf(event) : previewOf(event, clock.instant());
    }

    // Thứ tự giống đăng ký (Bài 11) và check-in (Bài 12): KHÓA -> kiểm tra -> ghi
    @Override
    @Transactional
    public FinalizationResponse finalizeEvent(String staffEmail, Long eventId, FinalizeEventRequest request) {
        Instant now = clock.instant();
        Member staff = memberRepository.findByAccountEmail(staffEmail)
                .orElseThrow(() -> new NotFoundException(MEMBER_NOT_FOUND));

        // 1. Khóa dòng sự kiện. 2 HR bấm chốt cùng lúc: người sau ĐỢI ở đây, tới lượt thì thấy "đã chốt" ở bước 2.
        // Không khóa thì cả hai cùng thấy "chưa chốt" và cùng phát điểm (unique index V12 sẽ chặn -> 500 thay vì 409)
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException(EVENT_NOT_FOUND));
        if (event.isFinalized()) {
            throw new ConflictException(ALREADY_FINALIZED);
        }
        // 2. Chỉ chốt khi đã COMPLETED (lịch đã khóa, không ai check-in thêm bằng QR được nữa)
        if (statusOf(event, now) != EventStatus.COMPLETED) {
            throw new ConflictException(NOT_COMPLETED);
        }

        // 3. Team HR đã tick: mỗi người một vai trò, phải tồn tại, chưa bị loại
        Map<Long, Member> team = loadTeam(request);
        Set<Long> organizerIds = new LinkedHashSet<>(request.organizerIds());
        Set<Long> supporterIds = new LinkedHashSet<>(request.supporterIds());

        // 4. Kỳ nhận điểm = kỳ chứa ngày kết thúc sự kiện theo giờ VN (chốt muộn sang kỳ sau vẫn tính kỳ cũ)
        Term term = termService.getOrCreateContaining(endDateOf(event));

        // 5. Người tham gia = member đã check-in, trừ người trong team (tổ chức có check-in vẫn chỉ nhận +4)
        List<Member> attendees = checkedInMembers(eventId).stream()
                .filter(member -> !team.containsKey(member.getId()))
                .toList();

        // 6. Ghi sổ cái: mỗi người đúng 1 dòng. Khách không có tài khoản nên không có dòng nào
        List<PointTransaction> rows = new ArrayList<>();
        organizerIds.forEach(id -> rows.add(row(team.get(id), PointTransactionType.ORGANIZE,
                pointRules.getOrganize(), event, term, staff)));
        supporterIds.forEach(id -> rows.add(row(team.get(id), PointTransactionType.SUPPORT,
                pointRules.getSupport(), event, term, staff)));
        attendees.forEach(member -> rows.add(row(member, PointTransactionType.ATTEND,
                pointRules.getAttend(), event, term, staff)));
        pointTransactionRepository.saveAll(rows);

        // 7. Khóa sự kiện (Hibernate tự UPDATE khi commit) rồi chuyển người đăng ký mà không đến sang NO_SHOW.
        // Tất cả trong 1 transaction: lỗi ở bất kỳ bước nào thì không ai được điểm, sự kiện vẫn chưa chốt
        event.setFinalizedAt(now);
        event.setFinalizedBy(staff);
        registrationRepository.markNoShow(eventId);

        return resultOf(event);
    }

    // ---------- Xem trước (chưa chốt) ----------

    private FinalizationResponse previewOf(Event event, Instant now) {
        Long eventId = event.getId();
        List<Person> organizers = new ArrayList<>();
        List<Person> supporters = new ArrayList<>();
        Set<Long> teamIds = new HashSet<>();
        for (EventStaff staff : eventStaffRepository.findByEventIdOrderByRoleAscMemberFullNameAsc(eventId)) {
            Member member = staff.getMember();
            if (member.getMemberStatus() == MemberStatus.DISQUALIFIED) {
                continue; // bị loại sau khi được phân công -> không tick sẵn
            }
            teamIds.add(member.getId());
            (staff.getRole() == StaffRole.ORGANIZER ? organizers : supporters).add(Person.from(member));
        }
        List<Person> attendees = checkedInMembers(eventId).stream()
                .filter(member -> !teamIds.contains(member.getId()))
                .map(Person::from)
                .toList();

        boolean completed = statusOf(event, now) == EventStatus.COMPLETED;
        LocalDate endDate = endDateOf(event);
        long totalPoints = (long) organizers.size() * pointRules.getOrganize()
                + (long) supporters.size() * pointRules.getSupport()
                + (long) attendees.size() * pointRules.getAttend();
        return new FinalizationResponse(
                eventId, false, null, null,
                completed, completed ? null : NOT_COMPLETED,
                endDate == null ? null : termNameOf(endDate),
                pointRules.getOrganize(), pointRules.getSupport(), pointRules.getAttend(),
                organizers, supporters, attendees,
                registrationRepository.countByEventIdAndStatusAndMemberIsNull(eventId, RegistrationStatus.CHECKED_IN),
                registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED),
                totalPoints);
    }

    // Chỉ đọc: kỳ chưa có thì hiện tên kỳ SẼ được tạo, không tạo ở đây (GET không được ghi dữ liệu)
    private String termNameOf(LocalDate date) {
        return termRepository.findContaining(date)
                .map(Term::getName)
                .orElseGet(() -> TermSeason.of(date).nameOf(date.getYear()));
    }

    // ---------- Kết quả (đã chốt): đọc lại từ sổ cái, không tính lại ----------

    private FinalizationResponse resultOf(Event event) {
        Long eventId = event.getId();
        List<PointTransaction> rows = pointTransactionRepository.findByEventIdOrderByIdAsc(eventId);
        Map<PointTransactionType, List<Person>> byType = rows.stream()
                .collect(Collectors.groupingBy(PointTransaction::getType,
                        Collectors.mapping(row -> Person.from(row.getMember()), Collectors.toList())));
        String termName = rows.isEmpty() ? termNameOf(endDateOf(event)) : rows.getFirst().getTerm().getName();
        return new FinalizationResponse(
                eventId, true, event.getFinalizedAt(), event.getFinalizedBy().getFullName(),
                false, ALREADY_FINALIZED,
                termName,
                pointRules.getOrganize(), pointRules.getSupport(), pointRules.getAttend(),
                sorted(byType.get(PointTransactionType.ORGANIZE)),
                sorted(byType.get(PointTransactionType.SUPPORT)),
                sorted(byType.get(PointTransactionType.ATTEND)),
                registrationRepository.countByEventIdAndStatusAndMemberIsNull(eventId, RegistrationStatus.CHECKED_IN),
                registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.NO_SHOW),
                rows.stream().mapToLong(PointTransaction::getAmount).sum());
    }

    // ---------- Helpers ----------

    // Trùng trong 1 list hoặc giữa 2 list -> 400. Lấy mọi member trong 1 câu SELECT ... IN (...)
    private Map<Long, Member> loadTeam(FinalizeEventRequest request) {
        Set<Long> ids = new HashSet<>();
        for (Long id : concat(request.organizerIds(), request.supporterIds())) {
            if (!ids.add(id)) {
                throw new BadRequestException(DUPLICATE_MEMBER);
            }
        }
        Map<Long, Member> members = memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        // id nằm trong body -> 400 (giống EventStaffServiceImpl)
        if (members.size() != ids.size()) {
            throw new BadRequestException(MEMBER_NOT_FOUND);
        }
        if (members.values().stream().anyMatch(m -> m.getMemberStatus() == MemberStatus.DISQUALIFIED)) {
            throw new BadRequestException(MEMBER_DISQUALIFIED);
        }
        return members;
    }

    private List<Member> checkedInMembers(Long eventId) {
        return registrationRepository
                .findByEventIdAndStatusAndMemberIsNotNullOrderByIdAsc(eventId, RegistrationStatus.CHECKED_IN).stream()
                .map(Registration::getMember)
                .toList();
    }

    private static PointTransaction row(Member member, PointTransactionType type, int amount,
                                        Event event, Term term, Member staff) {
        return PointTransaction.builder()
                .member(member)
                .term(term)
                .amount(amount)
                .type(type)
                .event(event)
                .createdBy(staff)
                .build();
    }

    // Ngày kết thúc = ngày bắt đầu giai đoạn COMPLETED đầu tiên, theo giờ VN. null khi chưa có lịch
    private static LocalDate endDateOf(Event event) {
        Instant endsAt = event.firstStartOf(PhaseKind.COMPLETED);
        return endsAt == null ? null : LocalDate.ofInstant(endsAt, TimeConfig.CLUB_ZONE);
    }

    private static EventStatus statusOf(Event event, Instant now) {
        return EventStatusCalculator.compute(event.getPublication(), event.phaseSlots(), now).status();
    }

    private static List<Person> sorted(List<Person> people) {
        return people == null ? List.of() : people.stream().sorted(BY_NAME).toList();
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }
}
