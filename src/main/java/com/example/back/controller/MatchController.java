package com.example.back.controller;

import com.example.back.entity.Match;
import com.example.back.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping("/generate")
    public ResponseEntity<List<Match>> generateMatches() {
        return ResponseEntity.ok(matchService.generateMatches());
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<?> updateMatch(@PathVariable Long id, @RequestBody Match match) {
        try {
            return ResponseEntity.ok(matchService.updateMatch(id, match));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/send/{id}")
    public ResponseEntity<?> sendMatchEmail(@PathVariable Long id) {
        try {
            matchService.sendMatchEmail(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAllMatches() {
        matchService.clearAllMatches();
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<?> getAllMatches() {
        try {
            return ResponseEntity.ok(matchService.getAllMatches());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }
}
