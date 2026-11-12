package rsnrdk.websec.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "activity_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate reportDate;

    @Column(nullable = false)
    private long totalRequests;

    @Column(nullable = false)
    private long uniqueUsers;

    @Column(nullable = false)
    private long errorRequests;

    @Column(nullable = false)
    private Instant generatedAt;
}
