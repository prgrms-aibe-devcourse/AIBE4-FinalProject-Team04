package kr.java.patchnotedemo.service;

import java.util.List;
import kr.java.patchnotedemo.dto.PendingItemResponse;
import kr.java.patchnotedemo.entity.PendingItem;
import kr.java.patchnotedemo.enums.PendingItemStatus;
import kr.java.patchnotedemo.repository.PendingItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatchNoteService {

    private final PendingItemRepository pendingItemRepository;

    public List<PendingItemResponse> getExcludedItems(String projectId) {
        return pendingItemRepository
                .findByProjectIdAndStatus(projectId, PendingItemStatus.EXCLUDED)
                .stream()
                .map(PendingItemResponse::from)
                .toList();
    }

    public List<PendingItemResponse> getPendingItems(String projectId) {
        return pendingItemRepository
                .findByProjectIdAndStatusNot(projectId, PendingItemStatus.EXCLUDED)
                .stream()
                .map(PendingItemResponse::from)
                .toList();
    }

    @Transactional
    public void excludeItem(Long id) {
        PendingItem item =
                pendingItemRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
        item.exclude();
    }

    @Transactional
    public void restoreItem(Long id) {
        PendingItem item =
                pendingItemRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
        item.restore();
    }
}
