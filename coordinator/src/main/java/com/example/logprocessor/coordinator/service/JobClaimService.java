package com.example.logprocessor.coordinator.service;

import com.example.logprocessor.common.JobStatus;
import com.example.logprocessor.coordinator.persistence.JobEntity;
import com.example.logprocessor.coordinator.persistence.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class JobClaimService {

    private final JobRepository jobRepository;

    public JobClaimService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Optional<JobEntity> claimJob(String workerId) {
        Optional<JobEntity> job = jobRepository.findFirstByStatusOrderBySubmittedAtAsc(JobStatus.QUEUED);
        job.ifPresent(entity -> entity.markRunning(workerId));
        return job;
    }
}
