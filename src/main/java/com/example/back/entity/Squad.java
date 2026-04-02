package com.example.back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "squads")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Squad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String squadId;

    @Column(unique = true, nullable = false)
    private String squadName;

    @Column(nullable = false)
    private String leaderName;

    @Column(unique = true, nullable = false)
    private String phone;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String p1Name;
    @Column(nullable = false)
    private String p1Uid;

    @Column(nullable = false)
    private String p2Name;
    @Column(nullable = false)
    private String p2Uid;

    @Column(nullable = false)
    private String p3Name;
    @Column(nullable = false)
    private String p3Uid;

    @Column(nullable = false)
    private String p4Name;
    @Column(nullable = false)
    private String p4Uid;

    private String p5Name;
    private String p5Uid;

    @Column(nullable = false)
    private String paymentStatus = "PENDING";

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
