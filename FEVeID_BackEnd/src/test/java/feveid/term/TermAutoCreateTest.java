package feveid.term;

import feveid.common.exception.ConflictException;
import feveid.support.ApiTestSupport;
import feveid.term.entity.Term;
import feveid.term.service.TermService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static feveid.term.service.impl.TermServiceImpl.TERM_OVERLAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Tự tạo kỳ khi cần ghi điểm (TermService.getOrCreateContaining)
class TermAutoCreateTest extends ApiTestSupport {

    @Autowired
    private TermService termService;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @Test
    void noTerm_createsTermOfThatSeason() {
        Term term = termService.getOrCreateContaining(LocalDate.of(2027, 3, 15));

        assertThat(term.getName()).isEqualTo("Spring 2027");
        assertThat(term.getStartDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(term.getEndDate()).isEqualTo(LocalDate.of(2027, 4, 30));
        assertThat(term.isShopOpen()).isFalse();
        assertThat(termRepository.count()).isEqualTo(1);
    }

    @Test
    void existingTerm_returnsIt_noNewRow() {
        Term first = termService.getOrCreateContaining(LocalDate.of(2027, 1, 1));
        Term again = termService.getOrCreateContaining(LocalDate.of(2027, 4, 30));

        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(termRepository.count()).isEqualTo(1);
    }

    @Test
    void legacyTermWithOddDates_containingDate_isUsed() {
        // Kỳ cũ tạo tay trước khi chốt lịch cố định: vẫn dùng nếu nó chứa ngày cần tìm
        Term legacy = termRepository.save(Term.builder()
                .name("Fall 2026 (cũ)")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 1, 3))
                .build());

        assertThat(termService.getOrCreateContaining(LocalDate.of(2027, 1, 2)).getId()).isEqualTo(legacy.getId());
    }

    @Test
    void legacyTermBlocksNewSeason_returns409() {
        // 10/01/2027 không thuộc kỳ cũ, nhưng Spring 2027 (01/01-30/04) chồng lên kỳ cũ -> không tạo được
        termRepository.save(Term.builder()
                .name("Fall 2026 (cũ)")
                .startDate(LocalDate.of(2026, 9, 5))
                .endDate(LocalDate.of(2027, 1, 3))
                .build());

        assertThatThrownBy(() -> termService.getOrCreateContaining(LocalDate.of(2027, 1, 10)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(TERM_OVERLAP);
    }

    @Test
    void concurrentCalls_sameSeason_oneRow_noException() throws Exception {
        // Nếu "kiểm tra chưa có rồi save()" thì 5 luồng cùng thấy chưa có, cùng INSERT -> 4 lỗi duplicate key.
        // ON CONFLICT DO NOTHING: luồng sau đợi luồng trước commit rồi bỏ qua, đọc lại thấy kỳ vừa tạo
        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        try {
            List<Future<Term>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    startGun.await();
                    return termService.getOrCreateContaining(LocalDate.of(2027, 6, 1));
                }));
            }
            startGun.countDown();
            List<Long> ids = new ArrayList<>();
            for (Future<Term> future : futures) {
                ids.add(future.get(30, TimeUnit.SECONDS).getId()); // exception trong luồng -> test đỏ
            }
            assertThat(ids).containsOnly(ids.getFirst());
        } finally {
            pool.shutdownNow();
        }

        assertThat(termRepository.count()).isEqualTo(1);
    }
}
