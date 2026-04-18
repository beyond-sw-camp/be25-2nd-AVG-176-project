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

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    // 카드 등록
    public CardResponse createCard(CardRequest request) {

        validateCardInfo(request);
        
        Card card = new Card();
        card.setUserId(request.getUserId());

        card.setCardType(request.getCardType());

        String number = request.getCardNumber().replaceAll("[^0-9]", "");
        number = number.replaceAll("(.{4})(?=.)", "$1-");

        card.setCardNumber(number);
        //card.setCardNumber(request.getCardNumber());

        // 카드 잔액/한도 자동 생성.
        List<Long> limits = List.of(
                1_000_000L,
                5_000_000L,
                10_000_000L,
                20_000_000L,
                50_000_000L,
                60_000_000L,
                10_000_0000L
        );

        // 랜덤 선택
        long cardLimit = limits.get(ThreadLocalRandom.current().nextInt(limits.size()));

        // 잔액 : 한도보다 작은 값
        long balance = ThreadLocalRandom.current().nextLong(0, cardLimit);

        card.setCardLimit(cardLimit);
        card.setBalance(balance);
        card.setActive(true);

        // AES 암호화
        card.setCvcEncrypted(CryptoUtil.encrypt(request.getCvc()));
        card.setExpiryEncrypted(CryptoUtil.encrypt(request.getExpiry()));

        Card saved = cardRepository.save(card);

        return toResponse(saved);
    }


    public CardResponse getCard(Long id) {

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("카드 없음"));

        return toResponse(card);
    }

    // 카드 전체 조회
    public List<CardResponse> getCards(Long userId) {

        List<Card> cards = userId == null
                ? cardRepository.findAll()
                : cardRepository.findByUserId(userId);

        return cards.stream()
                .map(this::toResponse)
                .toList();
    }

    // 카드 삭제
    public void deleteCard(Long id) {

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("카드 없음"));

        cardRepository.delete(card);
    }

    // 복호화
    public DecryptedCardInfo getDecryptedCard(Long cardId) {

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("카드 없음"));

        // 마스킹 해독
        String decryptedCvc = CryptoUtil.decrypt(card.getCvcEncrypted());
        String decryptedExpiry = CryptoUtil.decrypt(card.getExpiryEncrypted());

        System.out.println("CVC = " + decryptedCvc);
        System.out.println("Expiry = " + decryptedExpiry);


        DecryptedCardInfo info = new DecryptedCardInfo();

        info.setCardNumber(card.getCardNumber());
        info.setCvc(decryptedCvc);
        info.setExpiry(decryptedExpiry);

        info.setBalance(card.getBalance());
        info.setCardLimit(card.getCardLimit());

        return info;
    }

    // 카드 활성화 / 비활성화
    public CardResponse toggleCard(Long id) {

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("카드 없음"));

        card.setActive(!card.isActive());

        Card saved = cardRepository.save(card);

        return toResponse(saved);
    }

    private void validateCardInfo(CardRequest request) {

        // CVC 3자리 확인
        if (!request.getCvc().matches("\\d{3}")) {
            throw new RuntimeException("CVC는 3자리 숫자여야 합니다");
        }

        // expiry 형식 확인
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

    private CardResponse toResponse(Card card) {

        CardResponse res = new CardResponse();

        res.setId(card.getId());
        res.setUserId(card.getUserId());
        res.setCardType(card.getCardType());

        // 카드번호 마스킹
        res.setCardNumber(MaskingUtil.maskCardNumber(card.getCardNumber()));

        res.setBalance(card.getBalance());
        res.setCardLimit(card.getCardLimit());
        res.setActive(card.isActive());

        res.setCvc(MaskingUtil.maskCvc());
        res.setExpiry(MaskingUtil.maskExpiry());

        return res;
    }


}
