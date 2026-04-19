package com.example.team3Project.domain.settlement.application;

import com.example.team3Project.domain.settlement.dao.Card;
import com.example.team3Project.domain.settlement.dao.CardRepository;
import com.example.team3Project.domain.settlement.dto.CardRequest;
import com.example.team3Project.domain.settlement.dto.CardResponse;
import com.example.team3Project.domain.settlement.dto.DecryptedCardInfo;
import com.example.team3Project.global.util.CryptoUtil;
import com.example.team3Project.global.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    public CardResponse createCard(Long userId, CardRequest request) {
        validateCardInfo(request);

        Card card = new Card();
        card.setUserId(userId);
        card.setCardType(request.getCardType());

        String number = request.getCardNumber().replaceAll("[^0-9]", "");
        number = number.replaceAll("(.{4})(?=.)", "$1-");
        card.setCardNumber(number);

        List<Long> limits = List.of(
                1_000_000L,
                5_000_000L,
                10_000_000L,
                20_000_000L,
                50_000_000L,
                60_000_000L,
                100_000_000L
        );

        long cardLimit = limits.get(ThreadLocalRandom.current().nextInt(limits.size()));
        long balance = ThreadLocalRandom.current().nextLong(0, cardLimit);

        card.setCardLimit(cardLimit);
        card.setBalance(balance);
        card.setActive(true);
        card.setCvcEncrypted(CryptoUtil.encrypt(request.getCvc()));
        card.setExpiryEncrypted(CryptoUtil.encrypt(request.getExpiry()));

        return toResponse(cardRepository.save(card));
    }

    public CardResponse getCard(Long userId, Long id) {
        return toResponse(findOwnedCard(userId, id));
    }

    public List<CardResponse> getCards(Long userId) {
        List<Card> cards = cardRepository.findByUserIdOrderByIdAsc(userId);
        return IntStream.range(0, cards.size())
                .mapToObj(index -> toResponse(cards.get(index), index + 1))
                .toList();
    }

    public void deleteCard(Long userId, Long id) {
        cardRepository.delete(findOwnedCard(userId, id));
    }

    public DecryptedCardInfo getDecryptedCard(Long userId, Long cardId) {
        return toDecryptedCardInfo(findOwnedCard(userId, cardId));
    }

    public CardResponse toggleCard(Long userId, Long id) {
        Card card = findOwnedCard(userId, id);
        card.setActive(!card.isActive());
        return toResponse(cardRepository.save(card));
    }

    private Card findOwnedCard(Long userId, Long cardId) {
        return cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new RuntimeException("카드 없음"));
    }

    private void validateCardInfo(CardRequest request) {
        if (!request.getCvc().matches("\\d{3}")) {
            throw new RuntimeException("CVC는 3자리 숫자여야 합니다");
        }

        if (!request.getExpiry().matches("\\d{2}/\\d{2}")) {
            throw new RuntimeException("유효기간 형식은 MM/YY 입니다");
        }

        if (isExpired(request.getExpiry())) {
            throw new RuntimeException("이미 만료된 카드입니다");
        }
    }

    private boolean isExpired(String expiry) {
        String[] parts = expiry.split("/");

        int month = Integer.parseInt(parts[0]);
        int year = 2000 + Integer.parseInt(parts[1]);

        YearMonth cardDate = YearMonth.of(year, month);
        YearMonth now = YearMonth.now();

        return cardDate.isBefore(now);
    }

    private DecryptedCardInfo toDecryptedCardInfo(Card card) {
        DecryptedCardInfo info = new DecryptedCardInfo();
        info.setCardNumber(card.getCardNumber());
        info.setCvc(CryptoUtil.decrypt(card.getCvcEncrypted()));
        info.setExpiry(CryptoUtil.decrypt(card.getExpiryEncrypted()));
        info.setBalance(card.getBalance());
        info.setCardLimit(card.getCardLimit());
        return info;
    }

    private CardResponse toResponse(Card card) {
        return toResponse(card, null);
    }

    private CardResponse toResponse(Card card, Integer paymentPriority) {
        CardResponse res = new CardResponse();
        res.setId(card.getId());
        res.setUserId(card.getUserId());
        res.setPaymentPriority(paymentPriority);
        res.setCardType(card.getCardType());
        res.setCardNumber(MaskingUtil.maskCardNumber(card.getCardNumber()));
        res.setBalance(card.getBalance());
        res.setCardLimit(card.getCardLimit());
        res.setActive(card.isActive());
        res.setCvc(MaskingUtil.maskCvc());
        res.setExpiry(MaskingUtil.maskExpiry());
        return res;
    }
}
