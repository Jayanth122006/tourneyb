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

    @org.springframework.beans.factory.annotation.Value("${sendgrid.api.key}")
    private String sendgridApiKey;

    @org.springframework.beans.factory.annotation.Value("${sendgrid.from.email}")
    private String fromEmail;

    public Squad finalizeRegistration(String orderId) {
        Squad squad = squadRepository.findByOrderId(orderId);
        if (squad != null && !"SUCCESS".equals(squad.getPaymentStatus())) {
            squad.setPaymentStatus("SUCCESS");
            Squad savedSquad = squadRepository.save(squad);
            
            // Send automatic confirmation email
            try {
                sendRegistrationEmail(savedSquad);
            } catch (Exception e) {
                System.err.println("❌ Failed to send registration email: " + e.getMessage());
                // We don't throw error here to ensure registration still succeeds in DB
            }
            return savedSquad;
        }
        if (squad != null && "SUCCESS".equals(squad.getPaymentStatus())) {
            return squad; // Already finalized
        }
        throw new RuntimeException("Pending registration not found for order.");
    }

    private void sendRegistrationEmail(Squad squad) {
        if (squad.getEmail() == null || squad.getEmail().isEmpty()) return;

        String subject = "✅ Registration Successful - " + squad.getSquadName();
        String textContent = String.format(
            "Hello %s,\n\n" +
            "Your registration for the upcoming tournament is CONFIRMED!\n\n" +
            "Squad Name: %s\n" +
            "Leader: %s\n\n" +
            "What happens next?\n" +
            "You will receive the Room ID and Password by Friday via email.\n\n" +
            "⚠️ IMPORTANT: Please check your Spam or Junk folder just in case.\n" +
            "All official tournament emails will be sent from tourneygames1@gmail.com.\n\n" +
            "Get ready and all the best! 🎮",
            squad.getLeaderName(), squad.getSquadName(), squad.getLeaderName()
        );

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        
        java.util.List<java.util.Map<String, Object>> personalizations = new java.util.ArrayList<>();
        java.util.Map<String, Object> personalization = new java.util.HashMap<>();
        java.util.List<java.util.Map<String, String>> toList = new java.util.ArrayList<>();
        
        java.util.Map<String, String> to = new java.util.HashMap<>();
        to.put("email", squad.getEmail());
        toList.add(to);
        
        personalization.put("to", toList);
        personalization.put("subject", subject);
        personalizations.add(personalization);
        body.put("personalizations", personalizations);

        java.util.Map<String, String> from = new java.util.HashMap<>();
        from.put("email", fromEmail);
        from.put("name", "Tournament Team");
        body.put("from", from);

        java.util.List<java.util.Map<String, String>> content = new java.util.ArrayList<>();
        java.util.Map<String, String> contentItem = new java.util.HashMap<>();
        contentItem.put("type", "text/plain");
        contentItem.put("value", textContent);
        content.add(contentItem);
        body.put("content", content);

        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sendgridApiKey);

        org.springframework.http.HttpEntity<java.util.Map<String, Object>> request = new org.springframework.http.HttpEntity<>(body, headers);
        restTemplate.postForEntity("https://api.sendgrid.com/v3/mail/send", request, String.class);
        System.out.println("✅ Registration success email sent to: " + squad.getEmail());
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
