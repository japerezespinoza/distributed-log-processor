package com.example.logprocessor.coordinator.service;

import com.example.logprocessor.common.JobStatus;
import com.example.logprocessor.coordinator.persistence.JobEntity;
import com.example.logprocessor.coordinator.persistence.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ExpiredJobRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(ExpiredJobRecoveryService.class);

    private final JobRepository jobRepository;

    public ExpiredJobRecoveryService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void requeueExpiredJobs() {
        Instant now = Instant.now();
        for (JobEntity job : jobRepository.findByStatusAndLeaseUntilBefore(JobStatus.RUNNING, now)) {
            job.requeueExpiredJob();
            jobRepository.save(job);
            logger.info("Expired job {} requeued", job.getId());
        }
    }
}
