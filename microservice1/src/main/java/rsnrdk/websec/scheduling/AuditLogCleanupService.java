package rsnrdk.websec.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rsnrdk.websec.repository.AuditLogRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Business logic of the cleanup job. The transaction boundary is here, not in the Quartz job.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogCleanupService {

    static final String ANONYMOUS = "anonymous";

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    /**
     * Deletes audit records created before the start of the day {@code retentionDays} ago.
     * The cutoff is a day boundary, so running it again on the same day deletes nothing new (idempotent).
     *
     * @return number of deleted records (or records that would be deleted when {@code dryRun} is true)
     */
    @Transactional
    public int deleteOlderThan(int retentionDays, boolean anonymousOnly, boolean dryRun) {
        if (retentionDays < 1) {
            throw new IllegalArgumentException("retentionDays must be at least 1, got " + retentionDays);
        }
        Instant cutoff = LocalDate.now(clock).minusDays(retentionDays).atStartOfDay(clock.getZone()).toInstant();

        if (dryRun) {
            long matching = anonymousOnly
                    ? auditLogRepository.countByUsernameAndCreatedAtBefore(ANONYMOUS, cutoff)
                    : auditLogRepository.countByCreatedAtBefore(cutoff);
            log.info("Dry run: {} audit records created before {} would be deleted", matching, cutoff);
            return (int) matching;
        }

        int deleted = anonymousOnly
                ? auditLogRepository.deleteByUsernameCreatedBefore(ANONYMOUS, cutoff)
                : auditLogRepository.deleteCreatedBefore(cutoff);
        log.info("Deleted {} {}audit records created before {}", deleted, anonymousOnly ? "anonymous " : "", cutoff);
        return deleted;
    }
}
