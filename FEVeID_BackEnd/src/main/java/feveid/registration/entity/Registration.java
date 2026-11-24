package feveid.registration.entity;

import feveid.event.entity.Event;
import feveid.member.entity.Member;
import feveid.registration.enums.RegistrationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "registrations")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // null = khách (dùng các field guest*)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Size(max = 100)
    @Column(name = "guest_name", length = 100)
    private String guestName;

    @Size(max = 255)
    @Column(name = "guest_email")
    private String guestEmail;

    @Size(max = 20)
    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @Size(max = 200)
    @Column(name = "guest_school", length = 200)
    private String guestSchool;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
