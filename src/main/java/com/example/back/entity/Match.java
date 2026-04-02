package com.example.back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String squad1;

    @Column(nullable = false)
    private String squad2;

    @Column(name = "room_id")
    private String roomId;

    @Column(name = "room_password")
    private String password;

    @Column(name = "match_date_str")
    private String matchDate;

    @Column(name = "match_time_str")
    private String matchTime;

    private boolean sent = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

}
