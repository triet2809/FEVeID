package feveid.event.schedule;

import feveid.event.enums.PhaseKind;

import java.time.Instant;

// Một giai đoạn trong lịch, dạng dữ liệu thuần (không phải entity) để logic tính toán không phụ thuộc JPA
public record PhaseSlot(String name, Instant startsAt, PhaseKind kind) {
}
