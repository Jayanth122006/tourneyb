package com.example.back.service;

import com.example.back.entity.Match;
import com.example.back.entity.Squad;
import com.example.back.repository.MatchRepository;
import com.example.back.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final SquadRepository squadRepository;
    private final RestTemplate restTemplate;

    private static final Object emailLock = new Object();

    @Value("${mailtrap.token}")
    private String mailtrapToken;

    @Value("${mailtrap.inboxId}")
    private String mailtrapInboxId;

    @Transactional
    public Match updateMatch(Long id, com.example.back.dto.MatchDTO dto) {
        System.out.println("DEBUG ROOT: Service processing DTO for ID " + id);
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with ID: " + id));
        
        if (dto.getRoomId() != null) match.setRoomId(dto.getRoomId());
        if (dto.getPassword() != null) match.setPassword(dto.getPassword());
        if (dto.getMatchDate() != null) match.setMatchDate(dto.getMatchDate());
        if (dto.getMatchTime() != null) match.setMatchTime(dto.getMatchTime());
        
        Match saved = matchRepository.saveAndFlush(match);
        System.out.println("DEBUG ROOT: Persistent Save Complete. RoomId in DB is now: " + saved.getRoomId());
        return saved;
    }

    // No @Transactional here (External side-effects should be isolated)
    public List<String> sendMatchEmail(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getRoomId() == null || match.getPassword() == null || 
            match.getMatchDate() == null || match.getMatchTime() == null) {
            throw new RuntimeException("Match details are incomplete. Please fill and Save first.");
        }

        List<String> recipients = new ArrayList<>();
        
        String squad1Name = match.getSquad1() != null ? match.getSquad1().trim() : "";
        System.out.println("DEBUG ROOT: Looking up SQUAD1: [" + squad1Name + "]");
        Squad s1 = squadRepository.findBySquadNameIgnoreCase(squad1Name);
        if (s1 != null) {
            System.out.println("DEBUG ROOT: Found Squad1 Email: " + s1.getEmail());
            recipients.add(s1.getEmail());
        } else {
            System.err.println("CRITICAL: SQUAD1 NOT FOUND IN DB: [" + squad1Name + "]");
        }
        
        String squad2Name = match.getSquad2() != null ? match.getSquad2().trim() : "";
        if (!"BYE".equalsIgnoreCase(squad2Name)) {
            System.out.println("DEBUG ROOT: Looking up SQUAD2: [" + squad2Name + "]");
            Squad s2 = squadRepository.findBySquadNameIgnoreCase(squad2Name);
            if (s2 != null) {
                System.out.println("DEBUG ROOT: Found Squad2 Email: " + s2.getEmail());
                recipients.add(s2.getEmail());
            } else {
                System.err.println("CRITICAL: SQUAD2 NOT FOUND IN DB: [" + squad2Name + "]");
            }
        }

        if (recipients.isEmpty()) {
            throw new RuntimeException("No valid emails found for squads.");
        }

        List<String> sentSuccessfully = new ArrayList<>();
        
        // --- THE FINAL FORTRESS: Server-Wide Static Synchronization ---
        // STATIC lock ensures only one email goes out across the ENTIRE system.
        synchronized (emailLock) {
            try {
                for (int i = 0; i < recipients.size(); i++) {
                    String email = recipients.get(i);
                    
                    // Increased delay to 5.5 seconds for maximum "Sandbox" safety.
                    if (i > 0) {
                        System.out.println("DEBUG ROOT: [🔒 Lock Active] Waiting 5.5s for rate-limit safety...");
                        Thread.sleep(5500); 
                    }

                    System.out.println("DEBUG ROOT: Sending Individual API Mail to: " + email);
                    sendEmailAPI(email, match);
                    sentSuccessfully.add(email);
                }
                
                // Final non-transactional database update
                match.setSent(true);
                matchRepository.saveAndFlush(match);
                
                // Add a final cooldown after the match sequence to protect the NEXT click
                System.out.println("DEBUG ROOT: [🔒 Lock Release] Final 2s cooldown...");
                Thread.sleep(2000); 

                System.out.println("ALL INDIVIDUAL EMAILS SENT SUCCESSFULLY: " + sentSuccessfully);
                return sentSuccessfully;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Wait interrupted.");
            } catch (Exception e) {
                System.err.println("EMAIL SEQUENCE FAILED: " + e.getMessage());
                throw new RuntimeException("Mail server error during sequence: " + e.getMessage());
            }
        }
    }

    private void sendEmailAPI(String toEmail, Match match) {
        String url = "https://sandbox.api.mailtrap.io/api/send/" + mailtrapInboxId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Token", mailtrapToken);

        // Add a unique ID to each subject to prevent duplication issues.
        String subject = String.format("🔥 Match Details - %s [Ref: %s]", match.getSquad1(), System.currentTimeMillis() % 10000);

        String text = String.format(
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

        Map<String, Object> body = new HashMap<>();
        body.put("from", Map.of("email", "tourneygames1@gmail.com", "name", "Tourney Livid"));
        body.put("to", List.of(Map.of("email", toEmail)));
        body.put("subject", subject);
        body.put("text", text);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
    }

    @Transactional
    public List<Match> generateMatches() {
        // Clear existing matches before generating new ones (Efficient re-shuffle)
        matchRepository.deleteAll();

        // Fetch ONLY squads that have SUCCESS (Case-Insensitive)
        List<Squad> squads = squadRepository.findByPaymentStatusIgnoreCase("SUCCESS");
        
        if (squads.isEmpty()) {
            throw new RuntimeException("No SUCCESSFUL payments found in database! You can only generate matches for squads that have completed payment.");
        }
        
        System.out.println("DEBUG ROOT: Generating matches for " + squads.size() + " paid squads.");
        
        // Extract names
        List<String> squadNames = squads.stream()
                .map(Squad::getSquadName)
                .collect(Collectors.toList());

        // Shuffle randomly
        Collections.shuffle(squadNames);

        List<Match> generatedMatches = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Pair them up
        for (int i = 0; i < squadNames.size(); i += 2) {
            Match match = new Match();
            match.setSquad1(squadNames.get(i));
            match.setCreatedAt(now);
            
            if (i + 1 < squadNames.size()) {
                match.setSquad2(squadNames.get(i + 1));
            } else {
                match.setSquad2("BYE");
            }
            generatedMatches.add(match);
        }

        // Save and return
        return matchRepository.saveAll(generatedMatches);
    }



    public void deleteMatch(Long id) {
        matchRepository.deleteById(id);
    }

    public void clearAllMatches() {
        matchRepository.deleteAll();
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }
}
