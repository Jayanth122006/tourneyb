package com.example.back.controller;

import com.example.back.entity.Squad;
import com.example.back.service.SquadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/squads")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SquadController {

    private final SquadService squadService;

    @PostMapping("/register")
    public ResponseEntity<Squad> registerSquad(@RequestBody Squad squad) {
        Squad registeredSquad = squadService.adminRegisterSquad(squad);
        return ResponseEntity.ok(registeredSquad);
    }

    @GetMapping
    public ResponseEntity<List<Squad>> getAllSquads() {
        return ResponseEntity.ok(squadService.getAllSquads());
    }
}
