package com.freddieapp.card.repository;

import com.freddieapp.card.entity.Card;
import com.freddieapp.card.entity.Card.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    // ─── Native SELECT Queries (PostgreSQL) ──────────────────────────────────

    @Query(value = """
            SELECT * FROM freddie_cards.cards
            WHERE card_number = :cardNumber
            LIMIT 1
            """,
            nativeQuery = true)
    Optional<Card> findByCardNumber(@Param("cardNumber") String cardNumber);

    @Query(value = """
            SELECT * FROM freddie_cards.cards
            WHERE customer_id = :customerId
            ORDER BY created_at DESC
            """,
            nativeQuery = true)
    List<Card> findByCustomerId(@Param("customerId") UUID customerId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM freddie_cards.cards
                WHERE card_number = :cardNumber
            )
            """,
            nativeQuery = true)
    boolean existsByCardNumber(@Param("cardNumber") String cardNumber);

    // ─── Native UPDATE Queries (PostgreSQL) ──────────────────────────────────

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE freddie_cards.cards
            SET card_status = :#{#status.name()},
                updated_at = NOW()
            WHERE id = :cardId
            """,
            nativeQuery = true)
    int updateCardStatus(@Param("cardId") UUID cardId, @Param("status") CardStatus status);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE freddie_cards.cards
            SET available_balance = :balance,
                updated_at = NOW()
            WHERE id = :cardId
            """,
            nativeQuery = true)
    int updateAvailableBalance(@Param("cardId") UUID cardId, @Param("balance") BigDecimal balance);
}
