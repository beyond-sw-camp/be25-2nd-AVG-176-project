package com.example.team3Project.domain.policy.api;

import com.example.team3Project.domain.policy.application.BlockedWordService;
import com.example.team3Project.domain.policy.dto.BlockedWordCreateRequest;
import com.example.team3Project.domain.policy.dto.BlockedWordResponse;
import com.example.team3Project.domain.policy.dto.BlockedWordUpdateRequest;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/policies/blocked-words")
public class BlockedWordController {

    // 금지어는 사용자별 정책 데이터이므로 모든 요청을 로그인 사용자 기준으로 처리한다.
    private final BlockedWordService blockedWordService;

    @PostMapping("/post-test/{id}")
    public int postTest(@PathVariable int id) {
        return id;
    }

    @PostMapping
    public ResponseEntity<BlockedWordResponse> createBlockedWord(
            @LoginUser User user,
            @Valid @RequestBody BlockedWordCreateRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        BlockedWordResponse response = blockedWordService.createBlockedWord(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BlockedWordResponse>> getBlockedWords(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<BlockedWordResponse> response = blockedWordService.getBlockedWords(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sourcing")
    public ResponseEntity<List<String>> getBlockedWordsForSourcing(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        // 소싱 서비스에서는 문자열 목록만 바로 사용할 수 있도록 평탄화해서 내려준다.
        List<String> response = blockedWordService.getBlockedWords(user.getId())
                .stream()
                .map(BlockedWordResponse::getBlockedWord)
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userBlockedWordId}")
    public ResponseEntity<Void> deleteBlockedWord(
            @LoginUser User user,
            @PathVariable Long userBlockedWordId
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        blockedWordService.deleteBlockedWord(user.getId(), userBlockedWordId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userBlockedWordId}")
    public ResponseEntity<BlockedWordResponse> updateBlockedWord(
            @LoginUser User user,
            @PathVariable Long userBlockedWordId,
            @Valid @RequestBody BlockedWordUpdateRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        BlockedWordResponse response =
                blockedWordService.updateBlockedWord(user.getId(), userBlockedWordId, request);
        return ResponseEntity.ok(response);
    }
}
