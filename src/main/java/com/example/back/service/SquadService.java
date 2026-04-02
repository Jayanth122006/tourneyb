package com.example.back.service;

import com.example.back.entity.Squad;
import com.example.back.repository.SettingRepository;
import com.example.back.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SquadService {

    private final SquadRepository squadRepository;
    private final SettingRepository settingRepository;

    public Squad preRegisterSquad(Map<String, Object> formData, String orderId) {
        String squadName = (String) formData.get("squadName");
        String phone = (String) formData.get("phone");
        String email = (String) formData.get("email");

        boolean isOpen = settingRepository.findById("registration_open")
                .map(s -> "true".equals(s.getValue()))
                .orElse(true);

        if (!isOpen) {
            throw new RuntimeException("Registration is currently closed.");
        }

        // Only enforce limits aggressively for PAIDs, but we'll stick to a blanket 100 to avoid over-allocation
        if (squadRepository.count() >= 100) {
            throw new RuntimeException("Tournament is full! (Max 100 squads)");
        }

        Squad existingSquad = squadRepository.findBySquadName(squadName);
        if (existingSquad != null && "SUCCESS".equals(existingSquad.getPaymentStatus())) {
            throw new RuntimeException("Squad Name already registered and paid!");
        }

        Squad existingPhone = squadRepository.findByPhone(phone);
        if (existingPhone != null && "SUCCESS".equals(existingPhone.getPaymentStatus())) {
            throw new RuntimeException("Phone number already registered and paid!");
        }

        Squad existingEmail = squadRepository.findByEmail(email);
        if (existingEmail != null && "SUCCESS".equals(existingEmail.getPaymentStatus())) {
            throw new RuntimeException("Email address already registered and paid!");
        }

        // If duplicate was PENDING, overwrite it to recycle it
        Squad squad = (existingSquad != null) ? existingSquad : 
                      (existingPhone != null ? existingPhone : 
                      (existingEmail != null ? existingEmail : new Squad()));

        squad.setSquadName(squadName);
        squad.setLeaderName((String) formData.get("leaderName"));
        squad.setPhone(phone);
        squad.setEmail(email);
        squad.setP1Name((String) formData.get("p1Name")); squad.setP1Uid((String) formData.get("p1Uid"));
        squad.setP2Name((String) formData.get("p2Name")); squad.setP2Uid((String) formData.get("p2Uid"));
        squad.setP3Name((String) formData.get("p3Name")); squad.setP3Uid((String) formData.get("p3Uid"));
        squad.setP4Name((String) formData.get("p4Name")); squad.setP4Uid((String) formData.get("p4Uid"));
        squad.setP5Name((String) formData.get("p5Name")); squad.setP5Uid((String) formData.get("p5Uid"));

        squad.setPaymentStatus("PENDING");
        squad.setOrderId(orderId);

        if (squad.getSquadId() == null) {
            long count = squadRepository.count();
            squad.setSquadId(String.format("SQD%04d", count + 1001));
            squad.setCreatedAt(LocalDateTime.now());
        }

        return squadRepository.save(squad);
    }

    public Squad finalizeRegistration(String orderId) {
        Squad squad = squadRepository.findByOrderId(orderId);
        if (squad != null) {
            squad.setPaymentStatus("SUCCESS");
            return squadRepository.save(squad);
        }
        throw new RuntimeException("Pending registration not found for order.");
    }

    public Squad adminRegisterSquad(Squad squad) {
        long count = squadRepository.count();
        squad.setSquadId(String.format("SQD%04d", count + 1001));
        squad.setPaymentStatus("SUCCESS");
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
        return squadRepository.findAll().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();
    }
}
