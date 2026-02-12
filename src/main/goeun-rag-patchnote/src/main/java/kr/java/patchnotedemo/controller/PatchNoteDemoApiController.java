package kr.java.patchnotedemo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import kr.java.patchnotedemo.dto.PendingItemResponse;
import kr.java.patchnotedemo.entity.PendingItem;
import kr.java.patchnotedemo.enums.PendingItemStatus;
import kr.java.patchnotedemo.enums.SourceType;
import kr.java.patchnotedemo.repository.PendingItemRepository;
import kr.java.patchnotedemo.service.DummyDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patch-note")
@RequiredArgsConstructor
public class PatchNoteDemoApiController {

    private final DummyDataService dummyDataService;
    private final PendingItemRepository pendingItemRepository;

    @PostMapping("/dummy-data")
    public ResponseEntity<String> generateDummy(
            @RequestParam SourceType type, @RequestParam String projectId)
            throws JsonProcessingException {

        dummyDataService.generateDummy(type, projectId);
        return ResponseEntity.ok("데이터 생성 요청됨");
    }

    @GetMapping("/pending-items")
    public ResponseEntity<List<PendingItemResponse>> getPendingItems(@RequestParam String projectId) {
        // EXCLUDED가 아닌 것만 조회
        return ResponseEntity.ok(
                pendingItemRepository.findByProjectIdAndStatus(
                        projectId, PendingItemStatus.PENDING)
                        .stream()
                        .map(PendingItemResponse::from)
                        .toList());
    }
}
