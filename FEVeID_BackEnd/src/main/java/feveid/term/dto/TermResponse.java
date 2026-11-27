package feveid.term.dto;

import feveid.term.entity.Term;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record TermResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate endDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean shopOpen
) {
    public static TermResponse from(Term term) {
        return new TermResponse(term.getId(), term.getName(), term.getStartDate(),term.getEndDate(), term.isShopOpen());
    }
}
