package com.example.back.service;

import com.example.back.entity.Match;
import com.example.back.entity.Squad;
import com.example.back.repository.MatchRepository;
import com.example.back.repository.SquadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final SquadRepository squadRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${mailtrap.token}")
    private String mailtrapToken;

    @Value("${mailtrap.inboxId}")
    private String mailtrapInboxId;

    @Transactional
    public Match updateMatch(Long id, com.example.back.dto.MatchDTO dto) {
        System.out.println("DEBUG ROOT: Service processing DTO for ID " + id);
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        match.setRoomId(dto.getRoomId());
        match.setPassword(dto.getPassword());
        match.setMatchDate(dto.getMatchDate());
        match.setMatchTime(dto.getMatchTime());
        
        Match saved = matchRepository.saveAndFlush(match);
        System.out.println("DEBUG ROOT: Persistent Save Complete. RoomId in DB is now: " + saved.getRoomId());
        return saved;
    }

    public List<String> sendMatchEmail(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getRoomId() == null || match.getPassword() == null || 
            match.getMatchDate() == null || match.getMatchTime() == null) {
            throw new RuntimeException("Match details are incomplete. Please fill and Save first.");
        }

        List<String> recipients = new ArrayList<>();
        
        String squad1Name = match.getSquad1() != null ? match.getSquad1().trim() : "";
        Squad s1 = squadRepository.findBySquadNameIgnoreCase(squad1Name);
        if (s1 != null) {
            recipients.add(s1.getEmail());
        }
        
        String squad2Name = match.getSquad2() != null ? match.getSquad2().trim() : "";
        if (!"BYE".equalsIgnoreCase(squad2Name)) {
            Squad s2 = squadRepository.findBySquadNameIgnoreCase(squad2Name);
            if (s2 != null) {
                recipients.add(s2.getEmail());
            }
        }

        if (recipients.isEmpty()) {
            throw new RuntimeException("No valid emails found for squads.");
        }

        // --- DOUBLE-TAP API DELIVERY (Guarantees both squads get separate emails) ---
        int successCount = 0;
        try {
            System.out.println(">>> STARTING DOUBLE-TAP DELIVERY FOR MATCH ID: " + id);
            
            String url = "https://sandbox.api.mailtrap.io/api/send/" + mailtrapInboxId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Api-Token", mailtrapToken);

            for (String email : recipients) {
                System.out.println("👉 Attempting to send separate API mail to: " + email);
                
                // Construct JSON body for THIS recipient
                Map<String, Object> body = new HashMap<>();
                body.put("from", Map.of("email", "hello@demomailtrap.co", "name", "Tourney Admin"));
                body.put("to", List.of(Map.of("email", email)));
                body.put("subject", "🔥 Match Details - " + match.getSquad1());
                
                String textContent = String.format(
                    "🔥 Match Details 🔥\n\n" +
                    "Match: %s vs %s\n\n" +
                    "Room ID: %s\n" +
                    "Password: %s\n" +
                    "Date: %s\n" +
                    "Time: %s\n\n" +
                    "All the best 🎮",
                    match.getSquad1(), match.getSquad2(),
                    match.getRoomId(), match.getPassword(), 
                    match.getMatchDate(), match.getMatchTime()
                );
                body.put("text", textContent);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    successCount++;
                    System.out.println("✅ PARTIAL SUCCESS: Email to [" + email + "] sent.");
                }
                
                // Small gap to prevent Sandbox rate limits
                Thread.sleep(300);
            }

            if (successCount > 0) {
                match.setSent(true);
                matchRepository.saveAndFlush(match);
                System.out.println("🏁 TOTAL SUCCESS: " + successCount + " emails delivered.");
                return recipients;
            } else {
                throw new RuntimeException("Zero emails were successfully delivered.");
            }
        } catch (Exception e) {
            System.err.println("❌ DOUBLE-TAP FAILED: " + e.getMessage());
            throw new RuntimeException("Mail server error: " + e.getMessage());
        }
    }

    @Transactional
    public List<Match> generateMatches() {
        matchRepository.deleteAll();
        List<Squad> squads = squadRepository.findByPaymentStatusIgnoreCase("SUCCESS");
        
        if (squads.isEmpty()) {
            throw new RuntimeException("No SUCCESSFUL payments found in database!");
        }
        
        List<String> squadNames = squads.stream()
                .map(Squad::getSquadName)
                .collect(Collectors.toList());
        Collections.shuffle(squadNames);

        List<Match> matches = new ArrayList<>();
        for (int i = 0; i < squadNames.size(); i += 2) {
            Match match = new Match();
            match.setSquad1(squadNames.get(i));
            if (i + 1 < squadNames.size()) {
                match.setSquad2(squadNames.get(i + 1));
            } else {
                match.setSquad2("BYE");
            }
            matches.add(matchRepository.save(match));
        }
        return matches;
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    public void deleteMatch(Long id) {
        matchRepository.deleteById(id);
    }

    public void clearAllMatches() {
        matchRepository.deleteAll();
    }
}
