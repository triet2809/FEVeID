package feveid.event.entity;

import feveid.event.enums.EventPublication;
import feveid.event.enums.EventVisibility;
import feveid.event.schedule.PhaseSlot;
import feveid.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "events")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 200)
    @NotNull
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Size(max = 500)
    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Size(max = 300)
    @Column(name = "location", length = 300)
    private String location;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private EventVisibility visibility = EventVisibility.INTERNAL;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "publication", nullable = false, length = 20)
    @Builder.Default
    private EventPublication publication = EventPublication.DRAFT;

    // null = không giới hạn
    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "registration_opens_at")
    private Instant registrationOpensAt;

    @Column(name = "registration_closes_at")
    private Instant registrationClosesAt;

    // Người phụ trách (leader sự kiện hoặc người gửi Event Proposal); null = chưa gán
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_member_id")
    private Member owner;

    // Lịch giai đoạn: cascade = lưu/xóa Event thì lưu/xóa luôn các giai đoạn;
    // orphanRemoval = bỏ một giai đoạn khỏi list thì Hibernate DELETE dòng đó
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startsAt ASC")
    @Builder.Default
    private List<EventPhase> phases = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public List<PhaseSlot> phaseSlots() {
        return phases.stream().map(EventPhase::toSlot).toList();
    }

    // Thay cả lịch. Không gán phases = newList: Hibernate đang theo dõi chính đối tượng list cũ,
    // thay list khác thì orphanRemoval báo lỗi -> xóa rồi thêm vào list cũ
    public void replacePhases(List<EventPhase> newPhases) {
        phases.clear();
        for (EventPhase phase : newPhases) {
            phase.setEvent(this);
            phases.add(phase);
        }
    }
}
