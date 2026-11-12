package rsnrdk.websec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rsnrdk.websec.entity.ActivityReport;

import java.time.LocalDate;
import java.util.Optional;

public interface ActivityReportRepository extends JpaRepository<ActivityReport, Long> {
    Optional<ActivityReport> findByReportDate(LocalDate reportDate);
}
