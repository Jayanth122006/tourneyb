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
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final SquadRepository squadRepository;
    private final RestTemplate restTemplate;

    @Value("${mailtrap.token}")
    private String mailtrapToken;

    @Value("${mailtrap.inboxId}")
    private String mailtrapInboxId;

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

    public void sendMatchEmail(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getRoomId() == null || match.getPassword() == null || 
            match.getMatchDate() == null || match.getMatchTime() == null) {
            throw new RuntimeException("Match details are incomplete. Please fill and Save first.");
        }

        List<String> recipients = new ArrayList<>();
        
        Squad s1 = squadRepository.findBySquadName(match.getSquad1());
        if (s1 != null) recipients.add(s1.getEmail());
        
        if (!"BYE".equals(match.getSquad2())) {
            Squad s2 = squadRepository.findBySquadName(match.getSquad2());
            if (s2 != null) recipients.add(s2.getEmail());
        }

        if (recipients.isEmpty()) {
            throw new RuntimeException("No valid emails found for squads.");
        }

        try {
            // THE ONE-SHOT SOLUTION: Send one bulk message to all recipients in ONE request.
            // This bypasses the "Too many emails per second" 429 error forever.
            sendBulkEmailAPI(recipients, match);
            
            match.setSent(true);
            matchRepository.saveAndFlush(match);
            System.out.println("API BULK EMAIL SENT SUCCESS");
        } catch (Exception e) {
            System.err.println("EMAIL FAILED: " + e.getMessage());
            throw new RuntimeException("Mail server error: " + e.getMessage());
        }
    }

    private void sendBulkEmailAPI(List<String> toEmails, Match match) {
        String url = "https://sandbox.api.mailtrap.io/api/send/" + mailtrapInboxId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Token", mailtrapToken);

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

        // Convert list of emails into Mailtrap's JSON format for "to"
        List<Map<String, String>> toList = toEmails.stream()
            .map(email -> Map.of("email", email))
            .collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("from", Map.of("email", "tourneygames1@gmail.com", "name", "Tourney Livid"));
        body.put("to", toList);
        body.put("subject", "🔥 Match Details - Tourney Livid");
        body.put("text", text);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
    }

    public List<Match> generateMatches() {
        // Clear existing matches before generating new ones (Efficient re-shuffle)
        matchRepository.deleteAll();

        // Fetch all squads
        List<Squad> squads = squadRepository.findAll();
        
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
