package kr.java.patchnotedemo.dto;

public record IssueDummyResponse(
        String title,
        String description,
        String resolutionNote,
        String priority,
        String assignee) {}
