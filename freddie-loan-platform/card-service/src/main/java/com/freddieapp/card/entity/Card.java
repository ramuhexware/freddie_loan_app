package com.freddieapp.card.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "CARDS", schema = "freddie_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private UUID customerId;

    @Column(name = "CARD_NUMBER", nullable = false, unique = true, length = 16)
    private String cardNumber;

    @Column(name = "CARD_HOLDER_NAME", nullable = false, length = 100)
    private String cardHolderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "CARD_TYPE", nullable = false, length = 20)
    private CardType cardType;

    @Column(name = "EXPIRY_DATE", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "CVV", nullable = false, length = 3)
    private String cvv;

    @Column(name = "CREDIT_LIMIT", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "AVAILABLE_BALANCE", precision = 15, scale = 2)
    private BigDecimal availableBalance;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "CARD_STATUS", nullable = false, length = 20)
    private CardStatus cardStatus = CardStatus.INACTIVE;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    public enum CardType {
        CREDIT, DEBIT
    }

    public enum CardStatus {
        ACTIVE, INACTIVE, BLOCKED, EXPIRED
    }
}
