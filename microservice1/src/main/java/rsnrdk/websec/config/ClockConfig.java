package rsnrdk.websec.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * One clock for the scheduled business logic, in the same zone as the cron triggers,
 * so "yesterday" and "30 days ago" never depend on the server time zone.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${scheduling.time-zone:Europe/Kyiv}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
