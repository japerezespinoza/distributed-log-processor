package com.example.logprocessor.coordinator.persistence;

import com.example.logprocessor.common.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.List;
import java.time.Instant;

public interface JobRepository extends JpaRepository<JobEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JobEntity> findFirstByStatusOrderBySubmittedAtAsc(JobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<JobEntity> findByStatusAndLeaseUntilBefore(JobStatus status, Instant leaseUntil);
}
