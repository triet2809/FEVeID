package feveid.term.service;

import feveid.term.dto.ShopStatusRequest;
import feveid.term.dto.TermRequest;
import feveid.term.dto.TermResponse;

import java.util.List;

public interface TermService {
    TermResponse getTermById(Long id);
    TermResponse createTerm(TermRequest request);
    TermResponse updateTerm(Long id, TermRequest request);
    TermResponse changeShopStatus(Long id, ShopStatusRequest request);
    List<TermResponse> getTerms(Boolean shopOpen);
    TermResponse getCurrentTerm();
}
