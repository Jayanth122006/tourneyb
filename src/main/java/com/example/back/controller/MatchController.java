package com.example.back.controller;

import com.example.back.entity.Match;
import com.example.back.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<Match> updateMatch(@PathVariable Long id, @RequestBody Match match) {
        return ResponseEntity.ok(matchService.updateMatch(id, match));
    }

    @PostMapping("/send/{id}")
    public ResponseEntity<Void> sendMatchEmail(@PathVariable Long id) {
        matchService.sendMatchEmail(id);
        return ResponseEntity.ok().build();
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
    public ResponseEntity<List<Match>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }
}
