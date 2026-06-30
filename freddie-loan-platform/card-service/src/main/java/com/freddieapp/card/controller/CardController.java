package com.freddieapp.card.controller;

import com.freddieapp.card.dto.CardRequest;
import com.freddieapp.card.dto.CardResponse;
import com.freddieapp.card.entity.Card.CardStatus;
import com.freddieapp.card.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardRequest request) {
        log.info("REST request to create card for customer: {}", request.getCustomerId());
        CardResponse response = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<CardResponse>> getCardsByCustomerId(@PathVariable UUID customerId) {
        log.info("REST request to get cards for customer: {}", customerId);
        List<CardResponse> responses = cardService.getCardsByCustomerId(customerId);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{cardId}/status")
    public ResponseEntity<CardResponse> updateCardStatus(
            @PathVariable UUID cardId,
            @RequestParam CardStatus status) {
        log.info("REST request to update status of card {} to {}", cardId, status);
        CardResponse response = cardService.updateStatus(cardId, status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{cardId}/transaction")
    public ResponseEntity<CardResponse> processTransaction(
            @PathVariable UUID cardId,
            @RequestParam BigDecimal amount) {
        log.info("REST request to process transaction on card {} with amount: {}", cardId, amount);
        CardResponse response = cardService.processTransaction(cardId, amount);
        return ResponseEntity.ok(response);
    }
}
