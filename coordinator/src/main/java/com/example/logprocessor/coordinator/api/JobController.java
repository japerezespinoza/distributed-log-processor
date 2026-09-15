package com.example.logprocessor.coordinator.api;

import com.example.logprocessor.common.CompletionRequest;
import com.example.logprocessor.common.FailureRequest;
import com.example.logprocessor.common.JobStatus;
import com.example.logprocessor.common.LogJob;
import com.example.logprocessor.coordinator.persistence.JobEntity;
import com.example.logprocessor.coordinator.persistence.JobRepository;
import com.example.logprocessor.coordinator.service.JobClaimService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobRepository jobRepository;
    private final JobClaimService jobClaimService;

    public JobController(JobRepository jobRepository, JobClaimService jobClaimService) {
        this.jobRepository = jobRepository;
        this.jobClaimService = jobClaimService;
    }

    @PostMapping
    public ResponseEntity<LogJob> createJob(@RequestBody CreateJobRequest request) {
        JobEntity savedJob = jobRepository.save(new JobEntity(
                UUID.randomUUID().toString(),
                request.source(),
                request.pattern(),
                Instant.now(),
                JobStatus.QUEUED));

        return ResponseEntity.status(201).body(new LogJob(
                savedJob.getId(),
                savedJob.getSource(),
                savedJob.getPattern(),
                savedJob.getSubmittedAt(),
                savedJob.getStatus(),
                savedJob.getWorkerId(),
                savedJob.getLeaseUntil(),
                savedJob.getMatchCount(),
                savedJob.getAttemptCount(),
                savedJob.getMaxAttempts(),
                savedJob.getLastError()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogJob> getJob(@PathVariable String id) {
        return jobRepository.findById(id)
                .map(job -> ResponseEntity.ok(toLogJob(job)))
                .orElseGet(() -> ResponseEntity.<LogJob>notFound().build());
    }

    @PostMapping("/claim")
    public ResponseEntity<LogJob> claimJob(@RequestParam("workerId") String workerId) {
        return jobClaimService.claimJob(workerId)
                .map(job -> ResponseEntity.ok(toLogJob(job)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{jobId}/complete")
    @Transactional
    public ResponseEntity<LogJob> completeJob(
            @PathVariable String jobId, @RequestBody CompletionRequest request) {
        return jobRepository.findById(jobId)
                .map(job -> {
                    if (job.getStatus() != JobStatus.RUNNING
                            || !java.util.Objects.equals(request.workerId(), job.getWorkerId())) {
                        return ResponseEntity.status(409).<LogJob>build();
                    }
                    job.complete(request.matchCount());
                    return ResponseEntity.ok(toLogJob(jobRepository.save(job)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{jobId}/fail")
    @Transactional
    public ResponseEntity<LogJob> failJob(
            @PathVariable String jobId, @RequestBody FailureRequest request) {
        return jobRepository.findById(jobId)
                .map(job -> {
                    if (job.getStatus() != JobStatus.RUNNING
                            || !java.util.Objects.equals(request.workerId(), job.getWorkerId())) {
                        return ResponseEntity.status(409).<LogJob>build();
                    }
                    job.fail(request.errorMessage());
                    return ResponseEntity.ok(toLogJob(jobRepository.save(job)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private LogJob toLogJob(JobEntity job) {
        return new LogJob(
                job.getId(),
                job.getSource(),
                job.getPattern(),
                job.getSubmittedAt(),
                job.getStatus(),
                job.getWorkerId(),
                job.getLeaseUntil(),
                job.getMatchCount(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                job.getLastError());
    }
}
