package com.example.back.controller;

import com.example.back.entity.Match;
import com.example.back.service.MatchService;
import com.example.back.dto.MatchDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateMatches() {
        try {
            return ResponseEntity.ok(matchService.generateMatches());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<?> updateMatch(@PathVariable Long id, @RequestBody MatchDTO dto) {
        try {
            System.out.println("DEBUG ROOT: Controller received DTO -> Room=" + dto.getRoomId() + ", Pass=" + dto.getPassword());
            return ResponseEntity.ok(matchService.updateMatch(id, dto));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/send/{id}")
    public ResponseEntity<?> sendMatchEmail(@PathVariable Long id) {
        try {
            List<String> recipients = matchService.sendMatchEmail(id);
            return ResponseEntity.ok(Map.of("message", "Emails sent successfully!", "recipients", recipients));
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
            List<Match> list = matchService.getAllMatches();
            System.out.println("DEBUG ROOT: Returning " + list.size() + " matches to UI.");
            if (!list.isEmpty()) {
                Match m = list.get(0);
                System.out.println("DEBUG ROOT: First Match Persistence Check -> Room: " + m.getRoomId() + ", Pass: " + m.getPassword());
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }
}
