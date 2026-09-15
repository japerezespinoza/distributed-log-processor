package com.example.logprocessor.coordinator.api;

import com.example.logprocessor.coordinator.persistence.WorkerEntity;
import com.example.logprocessor.coordinator.persistence.WorkerStatus;
import com.example.logprocessor.coordinator.persistence.WorkerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/workers")
public class WorkerController {

    private final WorkerRepository workerRepository;

    public WorkerController(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<WorkerEntity> register(@RequestBody WorkerRegistrationRequest request) {
        Instant now = Instant.now();
        WorkerEntity worker = workerRepository.findById(request.workerId())
                .orElseGet(() -> new WorkerEntity(request.workerId(), now, WorkerStatus.ACTIVE));
        worker.markActive(now);
        return ResponseEntity.ok(workerRepository.save(worker));
    }

    @PostMapping("/{workerId}/heartbeat")
    public ResponseEntity<WorkerEntity> heartbeat(@PathVariable String workerId) {
        return workerRepository.findById(workerId)
                .map(worker -> {
                    worker.markActive(Instant.now());
                    return ResponseEntity.ok(workerRepository.save(worker));
                })
                .orElseGet(() -> ResponseEntity.<WorkerEntity>notFound().build());
    }
}
