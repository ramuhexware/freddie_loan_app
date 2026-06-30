package com.freddieapp.card.service;

import com.freddieapp.card.dto.CardRequest;
import com.freddieapp.card.dto.CardResponse;
import com.freddieapp.card.entity.Card;
import com.freddieapp.card.entity.Card.CardStatus;
import com.freddieapp.card.entity.Card.CardType;
import com.freddieapp.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final Random random = new Random();

    @Transactional
    public CardResponse createCard(CardRequest request) {
        log.info("Generating new {} card for customerId={}", request.getCardType(), request.getCustomerId());

        String cardNumber = generateCardNumber();
        String cvv = generateCvv();
        LocalDate expiryDate = LocalDate.now().plusYears(5); // Cards valid for 5 years

        BigDecimal creditLimit = request.getCardType() == CardType.CREDIT 
                ? (request.getCreditLimit() != null ? request.getCreditLimit() : new BigDecimal("5000.00")) 
                : BigDecimal.ZERO;

        BigDecimal initialBalance = request.getCardType() == CardType.CREDIT ? creditLimit : BigDecimal.ZERO;

        Card card = Card.builder()
                .customerId(request.getCustomerId())
                .cardNumber(cardNumber)
                .cardHolderName(request.getCardHolderName())
                .cardType(request.getCardType())
                .cvv(cvv)
                .expiryDate(expiryDate)
                .creditLimit(creditLimit)
                .availableBalance(initialBalance)
                .cardStatus(CardStatus.INACTIVE)
                .build();

        Card savedCard = cardRepository.save(card);
        log.info("Successfully created card ID={} status={}", savedCard.getId(), savedCard.getCardStatus());
        return mapToResponse(savedCard);
    }

    public List<CardResponse> getCardsByCustomerId(UUID customerId) {
        log.info("Fetching cards for customerId={}", customerId);
        return cardRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CardResponse updateStatus(UUID cardId, CardStatus status) {
        log.info("Updating card status for cardId={} to {}", cardId, status);
        int rows = cardRepository.updateCardStatus(cardId, status);
        if (rows == 0) {
            throw new IllegalArgumentException("Card not found: " + cardId);
        }
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
        return mapToResponse(card);
    }

    @Transactional
    public CardResponse processTransaction(UUID cardId, BigDecimal amount) {
        log.info("Processing transaction on cardId={} with amount={}", cardId, amount);
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));

        if (card.getCardStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Card is not ACTIVE: status=" + card.getCardStatus());
        }

        BigDecimal newBalance = card.getAvailableBalance().add(amount); // amount can be negative for purchase
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient funds / credit limit exceeded");
        }

        cardRepository.updateAvailableBalance(cardId, newBalance);
        card.setAvailableBalance(newBalance);
        return mapToResponse(card);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder("4"); // Visa standard prefix
        for (int i = 0; i < 15; i++) {
            sb.append(random.nextInt(10));
        }
        String number = sb.toString();
        // Check uniqueness via native query existsByCardNumber
        if (cardRepository.existsByCardNumber(number)) {
            return generateCardNumber();
        }
        return number;
    }

    private String generateCvv() {
        return String.format("%03d", random.nextInt(1000));
    }

    private CardResponse mapToResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .customerId(card.getCustomerId())
                .cardNumber(maskCardNumber(card.getCardNumber()))
                .cardHolderName(card.getCardHolderName())
                .cardType(card.getCardType())
                .expiryDate(card.getExpiryDate())
                .creditLimit(card.getCreditLimit())
                .availableBalance(card.getAvailableBalance())
                .cardStatus(card.getCardStatus())
                .build();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) {
            return cardNumber;
        }
        return "**** **** **** " + cardNumber.substring(12);
    }
}
