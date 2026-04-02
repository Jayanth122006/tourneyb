package com.example.back.dto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchDTO {
    private String roomId;
    private String password;
    private String matchDate;
    private String matchTime;
}
