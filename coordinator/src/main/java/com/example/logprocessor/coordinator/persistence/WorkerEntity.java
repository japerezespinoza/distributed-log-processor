package com.example.logprocessor.coordinator.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "workers")
public class WorkerEntity {

    @Id
    private String workerId;

    private Instant lastHeartbeat;

    @Enumerated(EnumType.STRING)
    private WorkerStatus status;

    protected WorkerEntity() {
    }

    public WorkerEntity(String workerId, Instant lastHeartbeat, WorkerStatus status) {
        this.workerId = workerId;
        this.lastHeartbeat = lastHeartbeat;
        this.status = status;
    }

    public String getWorkerId() { return workerId; }
    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public WorkerStatus getStatus() { return status; }

    public void markActive(Instant heartbeatAt) {
        this.lastHeartbeat = heartbeatAt;
        this.status = WorkerStatus.ACTIVE;
    }

    public void markDead() {
        this.status = WorkerStatus.DEAD;
    }
}
