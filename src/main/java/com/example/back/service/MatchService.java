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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final SquadRepository squadRepository;
    private final JavaMailSender mailSender; // ✅ Standard Spring Mail

    private static final Object emailLock = new Object();

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

        // --- THE FINAL SMTP FORTRESS ---
        synchronized (emailLock) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("tourneygames1@gmail.com");
                
                // ✅ KEY FIX: Use BOTH emails in a single transaction
                message.setTo(recipients.toArray(new String[0]));
                
                message.setSubject("🔥 Match Details - " + match.getSquad1() + " [Ref: " + (System.currentTimeMillis() % 10000) + "]");
                
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
                message.setText(text);

                // ✅ LOGS FOR VERIFICATION
                System.out.println("----------------------------------------");
                System.out.println("PREPARING TO SEND EMAIL TO:");
                for (String email : recipients) {
                    System.out.println("👉 Recipient: " + email);
                }

                mailSender.send(message);

                match.setSent(true);
                matchRepository.saveAndFlush(match);
                
                // Brief cool-down to be extra safe with Mailtrap's SMTP connection limit
                Thread.sleep(1500); 

                System.out.println("✅ SMTP EMAIL SENT SUCCESSFULLY!");
                System.out.println("----------------------------------------");
                return recipients;
            } catch (Exception e) {
                System.err.println("SMTP EMAIL FAILED: " + e.getMessage());
                throw new RuntimeException("Mail server error: " + e.getMessage());
            }
        }
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
