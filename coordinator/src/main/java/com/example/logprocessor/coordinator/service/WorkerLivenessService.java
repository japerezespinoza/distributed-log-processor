package com.example.logprocessor.coordinator.service;

import com.example.logprocessor.coordinator.persistence.WorkerEntity;
import com.example.logprocessor.coordinator.persistence.WorkerRepository;
import com.example.logprocessor.coordinator.persistence.WorkerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WorkerLivenessService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerLivenessService.class);
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 15;

    private final WorkerRepository workerRepository;

    public WorkerLivenessService(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void markStaleWorkersDead() {
        Instant cutoff = Instant.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        for (WorkerEntity worker : workerRepository.findByStatusAndLastHeartbeatBefore(
                WorkerStatus.ACTIVE, cutoff)) {
            worker.markDead();
            workerRepository.save(worker);
            logger.info("Worker {} marked DEAD", worker.getWorkerId());
        }
    }
}
