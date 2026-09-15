package com.example.logprocessor.coordinator.persistence;

import com.example.logprocessor.common.JobStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JobEntityRetryTest {

    @Test
    void failureBeforeMaximumAttemptsRequeuesJob() {
        JobEntity job = new JobEntity("job-1", "missing.log", "ERROR", Instant.now(), JobStatus.QUEUED);

        job.markRunning("worker-1");
        job.fail("file unavailable");

        assertEquals(1, job.getAttemptCount());
        assertEquals(3, job.getMaxAttempts());
        assertEquals(JobStatus.QUEUED, job.getStatus());
        assertEquals("file unavailable", job.getLastError());
        assertNull(job.getWorkerId());
        assertNull(job.getLeaseUntil());
    }

    @Test
    void failureAtMaximumAttemptsMarksJobFailed() {
        JobEntity job = new JobEntity("job-1", "missing.log", "ERROR", Instant.now(), JobStatus.QUEUED);

        job.markRunning("worker-1");
        job.fail("first failure");
        job.markRunning("worker-2");
        job.fail("second failure");
        job.markRunning("worker-3");
        job.fail("terminal failure");

        assertEquals(3, job.getAttemptCount());
        assertEquals(JobStatus.FAILED, job.getStatus());
        assertEquals("terminal failure", job.getLastError());
        assertNull(job.getWorkerId());
        assertNull(job.getLeaseUntil());
    }
}
