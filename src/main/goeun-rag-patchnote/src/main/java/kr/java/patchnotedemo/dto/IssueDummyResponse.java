package kr.java.patchnotedemo.dto;

public record IssueDummyResponse(
        String title,
        String description,
        String resolution_note,
        String priority,
        String assignee) {}
