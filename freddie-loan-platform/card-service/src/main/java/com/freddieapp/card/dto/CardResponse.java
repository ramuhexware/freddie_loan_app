package com.freddieapp.card.dto;

import com.freddieapp.card.entity.Card.CardStatus;
import com.freddieapp.card.entity.Card.CardType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardResponse {

    private UUID id;
    private UUID customerId;
    private String cardNumber;
    private String cardHolderName;
    private CardType cardType;
    private LocalDate expiryDate;
    private BigDecimal creditLimit;
    private BigDecimal availableBalance;
    private CardStatus cardStatus;
}
