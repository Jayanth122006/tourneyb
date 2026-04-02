package com.example.back.controller;

import com.example.back.entity.SupportQuery;
import com.example.back.repository.SupportQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/support")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SupportQueryController {

    private final SupportQueryRepository repository;

    @PostMapping
    public ResponseEntity<SupportQuery> saveQuery(@RequestBody SupportQuery query) {
        query.setCreatedAt(LocalDateTime.now());
        query.setStatus("PENDING");
        return ResponseEntity.ok(repository.save(query));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<SupportQuery> resolveQuery(@PathVariable Long id) {
        SupportQuery query = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Query not found"));
        query.setStatus("RESOLVED");
        return ResponseEntity.ok(repository.save(query));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuery(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<SupportQuery>> getAllQueries() {
        return ResponseEntity.ok(repository.findAll());
    }
}
