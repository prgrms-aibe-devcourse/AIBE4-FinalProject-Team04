package kr.java.patchnotedemo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import kr.java.patchnotedemo.dto.GenerateDraftRequest;
import kr.java.patchnotedemo.dto.PendingItemResponse;
import kr.java.patchnotedemo.enums.SourceType;
import kr.java.patchnotedemo.service.DummyDataService;
import kr.java.patchnotedemo.service.PatchNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patch-note")
@RequiredArgsConstructor
public class PatchNoteDemoApiController {

    private final DummyDataService dummyDataService;
    private final PatchNoteService patchNoteService;

    @PostMapping("/dummy-data")
    public ResponseEntity<String> generateDummy(
            @RequestParam SourceType type, @RequestParam String projectId)
            throws JsonProcessingException {

        dummyDataService.generateDummy(type, projectId);
        return ResponseEntity.ok("데이터 생성 요청됨");
    }

    @GetMapping("/pending-items")
    public ResponseEntity<List<PendingItemResponse>> getPendingItems(
            @RequestParam String projectId) {
        return ResponseEntity.ok(patchNoteService.getPendingItems(projectId));
    }

    @GetMapping("/excluded-items")
    public ResponseEntity<List<PendingItemResponse>> getExcludedItems(
            @RequestParam String projectId) {
        return ResponseEntity.ok(patchNoteService.getExcludedItems(projectId));
    }

    @DeleteMapping("/pending-items/{id}")
    public ResponseEntity<Void> excludeItem(@PathVariable Long id, @RequestParam String projectId) {
        patchNoteService.excludeItem(id, projectId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/excluded-items/{id}")
    public ResponseEntity<Void> restoreItem(@PathVariable Long id, @RequestParam String projectId) {
        patchNoteService.restoreItem(id, projectId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/draft")
    public ResponseEntity<String> generatePatchNoteDraft(
            @RequestBody GenerateDraftRequest request) {
        String draft = patchNoteService.generatePatchNoteDraft(request);
        return ResponseEntity.ok(draft);
    }
}
