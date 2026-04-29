package com.example.team3Project.domain.settlement.api;

import com.example.team3Project.domain.settlement.application.CardService;
import com.example.team3Project.domain.settlement.dto.CardRequest;
import com.example.team3Project.domain.settlement.dto.CardResponse;
import com.example.team3Project.domain.settlement.dto.DecryptedCardInfo;
import com.example.team3Project.domain.user.User;
import com.example.team3Project.global.annotation.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/cards", "/api/cards"})
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @LoginUser User user,
            @RequestBody CardRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(cardService.createCard(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> getCards(@LoginUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(cardService.getCards(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCard(
            @LoginUser User user,
            @PathVariable Long id
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(cardService.getCard(user.getId(), id));
    }

    @GetMapping("/{id}/decrypt")
    public ResponseEntity<DecryptedCardInfo> decryptCard(
            @LoginUser User user,
            @PathVariable Long id
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(cardService.getDecryptedCard(user.getId(), id));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<CardResponse> toggleCard(
            @LoginUser User user,
            @PathVariable Long id
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(cardService.toggleCard(user.getId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(
            @LoginUser User user,
            @PathVariable Long id
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        cardService.deleteCard(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
