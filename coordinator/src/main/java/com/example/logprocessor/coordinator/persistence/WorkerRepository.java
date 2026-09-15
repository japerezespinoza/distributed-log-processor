package com.example.logprocessor.coordinator.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface WorkerRepository extends JpaRepository<WorkerEntity, String> {

    List<WorkerEntity> findByStatusAndLastHeartbeatBefore(WorkerStatus status, Instant cutoff);
}
