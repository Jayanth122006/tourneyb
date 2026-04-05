package com.example.back.service;

import com.example.back.entity.Match;
import com.example.back.entity.Squad;
import com.example.back.repository.MatchRepository;
import com.example.back.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final SquadRepository squadRepository;
    private final JavaMailSender mailSender;

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

        // --- PRODUCTION GMAIL SMTP DELIVERY ---
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("tourneygames1@gmail.com");
            message.setTo(recipients.toArray(new String[0]));
            message.setSubject("🔥 Match Details - " + match.getSquad1());
            
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
            message.setText(textContent);

            System.out.println("👉 Attempting Gmail SMTP send to: " + String.join(", ", recipients));
            mailSender.send(message);

            match.setSent(true);
            matchRepository.saveAndFlush(match);
            System.out.println("✅ SUCCESS: Gmail SMTP mail delivered to squad 1 & squad 2.");
            return recipients;
        } catch (Exception e) {
            System.err.println("❌ GMAIL SMTP FAILED: " + e.getMessage());
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
