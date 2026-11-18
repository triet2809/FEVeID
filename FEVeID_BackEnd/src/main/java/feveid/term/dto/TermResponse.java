package feveid.term.dto;

import feveid.term.entity.Term;

import java.time.LocalDate;

public record TermResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean shopOpen
) {
    public static TermResponse from(Term term) {
        return new TermResponse(term.getId(), term.getName(), term.getStartDate(),term.getEndDate(), term.isShopOpen());
    }
}
