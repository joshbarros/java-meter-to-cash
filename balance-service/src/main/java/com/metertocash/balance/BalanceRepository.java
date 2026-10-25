package com.metertocash.balance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BalanceRepository extends JpaRepository<BalanceEntity, String> {

    // PostgreSQL evaluates every SET expression against the old row, so open_cents = :amt is the balance before this payment.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE balances SET open_cents = open_cents - :amt, status = CASE WHEN open_cents = :amt THEN 'PAID' ELSE status END WHERE bill_id = :id AND status IN ('OPEN','CUT') AND open_cents >= :amt", nativeQuery = true)
    int apply(@Param("id") String id, @Param("amt") int amt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE balances SET status = 'CUT' WHERE bill_id = :id AND status = 'OPEN' AND open_cents > 0", nativeQuery = true)
    int cut(@Param("id") String id);
}
