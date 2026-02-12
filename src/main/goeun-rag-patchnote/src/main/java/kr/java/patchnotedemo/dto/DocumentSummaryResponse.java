package kr.java.patchnotedemo.dto;

import kr.java.patchnotedemo.enums.PatchType;

public record DocumentSummaryResponse(String title, String summary, PatchType category) {}
