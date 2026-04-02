package com.example.back.controller;

import com.example.back.entity.Setting;
import com.example.back.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingRepository repository;

    @GetMapping
    public ResponseEntity<List<Setting>> getAllSettings() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PutMapping("/{key}")
    public ResponseEntity<Setting> updateSetting(@PathVariable String key, @RequestBody Setting setting) {
        setting.setKey(key);
        return ResponseEntity.ok(repository.save(setting));
    }

    @GetMapping("/{key}")
    public ResponseEntity<Setting> getSetting(@PathVariable String key) {
        return ResponseEntity.ok(repository.findById(key)
                .orElse(new Setting(key, "true"))); // Default to true if not found
    }
}
