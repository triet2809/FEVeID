package feveid.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    // CLB hoạt động theo giờ Việt Nam, không phụ thuộc giờ của máy chủ
    public static final ZoneId CLUB_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock clock() {
        return Clock.system(CLUB_ZONE);
    }
}
