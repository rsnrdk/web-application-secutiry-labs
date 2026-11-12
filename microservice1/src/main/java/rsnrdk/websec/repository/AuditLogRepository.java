package rsnrdk.websec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rsnrdk.websec.entity.AuditLog;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    long countByCreatedAtBefore(Instant cutoff);

    long countByUsernameAndCreatedAtBefore(String username, Instant cutoff);

    @Modifying
    @Query("delete from AuditLog a where a.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from AuditLog a where a.username = :username and a.createdAt < :cutoff")
    int deleteByUsernameCreatedBefore(@Param("username") String username, @Param("cutoff") Instant cutoff);

    @Query("select count(a) from AuditLog a where a.createdAt >= :from and a.createdAt < :to")
    long countInPeriod(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(a) from AuditLog a where a.status >= 400 and a.createdAt >= :from and a.createdAt < :to")
    long countErrorsInPeriod(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(distinct a.username) from AuditLog a "
            + "where a.username <> 'anonymous' and a.createdAt >= :from and a.createdAt < :to")
    long countDistinctUsersInPeriod(@Param("from") Instant from, @Param("to") Instant to);
}
