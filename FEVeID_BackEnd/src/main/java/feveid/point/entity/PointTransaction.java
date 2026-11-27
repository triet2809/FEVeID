package feveid.point.entity;

import feveid.event.entity.Event;
import feveid.member.entity.Member;
import feveid.point.enums.PointTransactionType;
import feveid.term.entity.Term;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

// Một dòng sổ cái. @Immutable + không có @Setter: Hibernate không bao giờ sinh UPDATE cho entity này.
// DB cũng chặn UPDATE/DELETE bằng trigger (V12), đây là lớp thứ hai
@Getter
@Entity
@Immutable
@Table(name = "point_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Hibernate cần, code thì dùng builder
@AllArgsConstructor
@Builder
public class PointTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    // dương = cộng, âm = trừ, không bao giờ 0
    @Column(name = "amount", nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PointTransactionType type;

    // Có giá trị <=> ORGANIZE / SUPPORT / ATTEND
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "reason", length = 500)
    private String reason;

    // HR/BCN ghi giao dịch; null = hệ thống tự ghi (chốt sự kiện, đổi quà)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_member_id")
    private Member createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
