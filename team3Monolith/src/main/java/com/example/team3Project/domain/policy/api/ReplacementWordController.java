package com.example.team3Project.domain.policy.api;

import com.example.team3Project.domain.policy.application.ReplacementWordService;
import com.example.team3Project.domain.policy.dto.ReplacementWordCreateRequest;
import com.example.team3Project.domain.policy.dto.ReplacementWordResponse;
import com.example.team3Project.domain.policy.dto.ReplacementWordUpdateRequest;
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
@RequestMapping("/policies/replacement-words")
public class ReplacementWordController {

    // 치환어도 사용자별 정책 데이터이므로 로그인 사용자 기준으로만 다룬다.
    private final ReplacementWordService replacementWordService;

    @PostMapping
    public ResponseEntity<ReplacementWordResponse> createReplacementWord(
            @LoginUser User user,
            @Valid @RequestBody ReplacementWordCreateRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        ReplacementWordResponse response =
                replacementWordService.createReplacementWord(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ReplacementWordResponse>> getReplacementWords(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<ReplacementWordResponse> response = replacementWordService.getReplacementWords(user.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userReplacementWordId}")
    public ResponseEntity<Void> deleteReplacementWord(
            @LoginUser User user,
            @PathVariable Long userReplacementWordId
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        replacementWordService.deleteReplacementWord(user.getId(), userReplacementWordId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userReplacementWordId}")
    public ResponseEntity<ReplacementWordResponse> updateReplacementWord(
            @LoginUser User user,
            @PathVariable Long userReplacementWordId,
            @Valid @RequestBody ReplacementWordUpdateRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        ReplacementWordResponse response =
                replacementWordService.updateReplacementWord(user.getId(), userReplacementWordId, request);
        return ResponseEntity.ok(response);
    }
}
