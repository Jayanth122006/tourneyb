package com.example.back.controller;

import com.example.back.entity.Setting;
import com.example.back.entity.Squad;
import com.example.back.repository.MatchRepository;
import com.example.back.repository.SettingRepository;
import com.example.back.repository.SquadRepository;
import com.example.back.repository.SupportQueryRepository;
import com.example.back.service.SquadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final SquadRepository squadRepository;
    private final MatchRepository matchRepository;
    private final SupportQueryRepository supportRepository;
    private final SquadService squadService;
    private final SettingRepository settingRepository;

    @PostMapping("/verify-pin")
    public ResponseEntity<Map<String, Object>> verifyPin(@RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        if ("2826".equals(pin)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Identity Verified"));
        }
        return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid Identity PIN"));
    }

    @PostMapping("/registration-status")
    public ResponseEntity<Map<String, Object>> toggleRegistration(@RequestBody Map<String, String> body) {
        String status = body.get("status"); // "true" or "false"
        Setting setting = settingRepository.findById("registration_open")
                .orElse(new Setting("registration_open", "true"));
        setting.setValue(status);
        settingRepository.save(setting);
        return ResponseEntity.ok(Map.of("status", status));
    }

    @GetMapping("/registration-status")
    public ResponseEntity<Map<String, String>> getRegistrationStatus() {
        String status = settingRepository.findById("registration_open")
                .map(s -> s.getValue())
                .orElse("true");
        return ResponseEntity.ok(Map.of("status", status));
    }

    @PutMapping("/squads/{id}/payment")
    public ResponseEntity<Squad> updatePaymentStatus(
            @PathVariable Long id, 
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(squadService.updatePaymentStatus(id, status));
    }

    @DeleteMapping("/reset")
    @Transactional
    public ResponseEntity<Map<String, String>> resetTournament() {
        matchRepository.deleteAll();
        squadRepository.deleteAll();
        supportRepository.deleteAll();
        return ResponseEntity.ok(Map.of("message", "Tournament data cleared successfully"));
    }
}
