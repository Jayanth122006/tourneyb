package com.example.back.service;

import com.example.back.entity.Squad;
import com.example.back.repository.SettingRepository;
import com.example.back.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SquadService {

    private final SquadRepository squadRepository;
    private final SettingRepository settingRepository;

    public Squad registerSquad(Squad squad) {
        // Check if registration is open
        boolean isOpen = settingRepository.findById("registration_open")
                .map(s -> "true".equals(s.getValue()))
                .orElse(true);

        if (!isOpen) {
            throw new RuntimeException("Registration is currently closed.");
        }

        // Duplicate checks
        if (squadRepository.existsBySquadName(squad.getSquadName())) {
            throw new RuntimeException("Squad Name already registered!");
        }
        if (squadRepository.existsByPhone(squad.getPhone())) {
            throw new RuntimeException("Phone number already registered!");
        }
        if (squadRepository.existsByEmail(squad.getEmail())) {
            throw new RuntimeException("Email address already registered!");
        }

        // Registration Limit check (e.g. 100 squads)
        if (squadRepository.count() >= 100) {
            throw new RuntimeException("Tournament is full! (Max 100 squads)");
        }

        // Generate squad id logic
        long count = squadRepository.count();
        squad.setSquadId(String.format("SQD%04d", count + 1001));
        
        if (squad.getPaymentStatus() == null || !"SUCCESS".equals(squad.getPaymentStatus())) {
            squad.setPaymentStatus("PENDING");
        }
        squad.setCreatedAt(LocalDateTime.now());
        
        return squadRepository.save(squad);
    }

    public Squad updatePaymentStatus(Long id, String status) {
        Squad squad = squadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Squad not found"));
        squad.setPaymentStatus(status);
        return squadRepository.save(squad);
    }

    public List<Squad> getAllSquads() {
        // Return squads sorted by creation time (First come first serve)
        return squadRepository.findAll().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();
    }
}
