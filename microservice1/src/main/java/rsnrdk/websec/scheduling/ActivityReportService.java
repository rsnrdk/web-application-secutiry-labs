package rsnrdk.websec.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rsnrdk.websec.entity.ActivityReport;
import rsnrdk.websec.repository.ActivityReportRepository;
import rsnrdk.websec.repository.AuditLogRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Builds a daily activity report from the audit log. One report per date (unique report_date).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityReportService {

    private final AuditLogRepository auditLogRepository;
    private final ActivityReportRepository activityReportRepository;
    private final Clock clock;

    /**
     * Creates or refreshes the report for the day {@code daysBack} days ago.
     * Idempotent: if the stored report already has the same numbers, nothing is written.
     */
    @Transactional
    public ActivityReport generateDailyReport(int daysBack) {
        if (daysBack < 0) {
            throw new IllegalArgumentException("daysBack must not be negative, got " + daysBack);
        }
        LocalDate date = LocalDate.now(clock).minusDays(daysBack);
        Instant from = date.atStartOfDay(clock.getZone()).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(clock.getZone()).toInstant();

        long total = auditLogRepository.countInPeriod(from, to);
        long users = auditLogRepository.countDistinctUsersInPeriod(from, to);
        long errors = auditLogRepository.countErrorsInPeriod(from, to);

        Optional<ActivityReport> existing = activityReportRepository.findByReportDate(date);
        if (existing.isPresent() && hasSameNumbers(existing.get(), total, users, errors)) {
            log.info("Activity report for {} is already up to date, nothing to change", date);
            return existing.get();
        }

        ActivityReport report = existing.orElseGet(() -> ActivityReport.builder().reportDate(date).build());
        report.setTotalRequests(total);
        report.setUniqueUsers(users);
        report.setErrorRequests(errors);
        report.setGeneratedAt(clock.instant());
        log.info("Activity report for {} saved: {} requests, {} users, {} errors", date, total, users, errors);
        return activityReportRepository.save(report);
    }

    private static boolean hasSameNumbers(ActivityReport report, long total, long users, long errors) {
        return report.getTotalRequests() == total && report.getUniqueUsers() == users && report.getErrorRequests() == errors;
    }
}
