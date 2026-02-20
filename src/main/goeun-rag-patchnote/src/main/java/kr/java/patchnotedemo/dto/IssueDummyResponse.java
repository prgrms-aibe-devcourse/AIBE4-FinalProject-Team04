package kr.java.patchnotedemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssueDummyResponse(
        String title,
        String description,
        @JsonProperty("resolution_note") String resolutionNote,
        String priority,
        String assignee) {}
