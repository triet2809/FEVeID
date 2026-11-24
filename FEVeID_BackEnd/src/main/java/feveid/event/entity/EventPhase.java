package feveid.event.entity;

import feveid.event.enums.PhaseKind;
import feveid.event.schedule.PhaseSlot;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "event_phases")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventPhase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // Instant = một mốc tuyệt đối trên trục thời gian <-> cột timestamptz
    @NotNull
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private PhaseKind kind;

    // Đổi sang dạng dữ liệu thuần để đưa vào EventStatusCalculator / PhaseScheduleValidator
    public PhaseSlot toSlot() {
        return new PhaseSlot(name, startsAt, kind);
    }
}
