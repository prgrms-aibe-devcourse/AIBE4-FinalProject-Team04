package kr.java.minyeonglogapi.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogRequest {
    private String level;
    private String message;
    private String service;
    private String timestamp; // k6에서 보내줄 경우를 대비
}
