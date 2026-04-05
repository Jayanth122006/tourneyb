package com.example.back.service;

import com.example.back.entity.Match;
import com.example.back.entity.Squad;
import com.example.back.repository.MatchRepository;
import com.example.back.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final SquadRepository squadRepository;

    @org.springframework.beans.factory.annotation.Value("${sendgrid.from.email}")
    private String fromEmail;

    @org.springframework.beans.factory.annotation.Value("${sendgrid.api.key}")
    private String sendgridApiKey;

    @Transactional
    public Match updateMatch(Long id, com.example.back.dto.MatchDTO dto) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        match.setRoomId(dto.getRoomId());
        match.setPassword(dto.getPassword());
        match.setMatchDate(dto.getMatchDate());
        match.setMatchTime(dto.getMatchTime());
        
        return matchRepository.saveAndFlush(match);
    }

    public List<String> sendMatchEmail(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getRoomId() == null || match.getPassword() == null || 
            match.getMatchDate() == null || match.getMatchTime() == null) {
            throw new RuntimeException("Match details are incomplete. Please fill and Save first.");
        }

        List<String> recipients = new ArrayList<>();
        
        // --- RECIPIENT SEARCH ---
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

        // --- PRODUCTION SENDGRID HTTP API DELIVERY ---
        try {
            String textContent = String.format(
                "🔥 Match Details 🔥\n\n" +
                "Match: %s vs %s\n\n" +
                "Room ID: %s\n" +
                "Password: %s\n" +
                "Date: %s\n" +
                "Time: %s\n\n" +
                "⚠️ MATCH RULES & REGULATIONS ⚠️\n" +
                "- Do not enter house tops or room tops.\n" +
                "- Do not break gloo walls.\n" +
                "- No gloo climbing.\n" +
                "- No over camping.\n" +
                "- Hacks, modded APKs, or panels are strictly prohibited.\n" +
                "- Revives are allowed.\n" +
                "- No gun attributes.\n" +
                "- No character skills.\n" +
                "- Unlimited ammo and gloo walls.\n" +
                "- NOTE: Spectators will be room owner only. No other spectators are allowed.\n\n" +
                "All the best 🎮",
                match.getSquad1(), match.getSquad2(),
                match.getRoomId(), match.getPassword(), 
                match.getMatchDate(), match.getMatchTime()
            );
            
            String subject = "🔥 Match Details - " + match.getSquad1();

            // Construct SendGrid JSON Payload
            Map<String, Object> body = new HashMap<>();
            
            List<Map<String, Object>> personalizations = new ArrayList<>();
            Map<String, Object> personalization = new HashMap<>();
            List<Map<String, String>> toList = new ArrayList<>();
            for (String email : recipients) {
                Map<String, String> to = new HashMap<>();
                to.put("email", email);
                toList.add(to);
            }
            personalization.put("to", toList);
            personalization.put("subject", subject);
            personalizations.add(personalization);
            body.put("personalizations", personalizations);

            Map<String, String> from = new HashMap<>();
            from.put("email", fromEmail);
            body.put("from", from);

            List<Map<String, String>> content = new ArrayList<>();
            Map<String, String> contentItem = new HashMap<>();
            contentItem.put("type", "text/plain");
            contentItem.put("value", textContent);
            content.add(contentItem);
            body.put("content", content);

            // Send via HTTP POST
            System.out.println("👉 Attempting SendGrid HTTP API send to: " + String.join(", ", recipients));
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(sendgridApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity("https://api.sendgrid.com/v3/mail/send", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                match.setSent(true);
                matchRepository.saveAndFlush(match);
                System.out.println("✅ SUCCESS: SendGrid HTTP API mail delivered to squad 1 & squad 2. Status: " + response.getStatusCode().value());
                return recipients;
            } else {
                System.err.println("❌ SENDGRID HTTP API FAILED. Status: " + response.getStatusCode().value() + ", Body: " + response.getBody());
                throw new RuntimeException("SendGrid API error: " + response.getStatusCode().value());
            }

        } catch (Exception e) {
            System.err.println("❌ SENDGRID API FAILED: " + e.getMessage());
            throw new RuntimeException("Mail server error: " + e.getMessage());
        }
    }

    @Transactional
    public List<Match> generateMatches() {
        matchRepository.deleteAll();
        List<Squad> squads = squadRepository.findByPaymentStatusIgnoreCase("SUCCESS");
        
        if (squads.isEmpty()) {
            throw new RuntimeException("No successful payments found!");
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
