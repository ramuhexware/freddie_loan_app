package com.freddieapp.card.dto;

import com.freddieapp.card.entity.Card.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotBlank(message = "Card holder name is required")
    @Size(max = 100, message = "Card holder name cannot exceed 100 characters")
    private String cardHolderName;

    @NotNull(message = "Card type is required")
    private CardType cardType;

    private BigDecimal creditLimit;
}
