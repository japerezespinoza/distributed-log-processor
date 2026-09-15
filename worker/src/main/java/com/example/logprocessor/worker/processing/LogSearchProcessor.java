package com.example.logprocessor.worker.processing;

import com.example.logprocessor.common.LogJob;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LogSearchProcessor {

    public long countMatches(LogJob job) throws IOException {
        try (var lines = Files.lines(Path.of(job.source()))) {
            return lines.filter(line -> line.contains(job.pattern())).count();
        }
    }
}
