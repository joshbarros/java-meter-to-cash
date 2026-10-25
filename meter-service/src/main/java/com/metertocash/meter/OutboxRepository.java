package com.metertocash.meter;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxMessage, String> {

    List<OutboxMessage> findTop25BySentAtIsNullOrderByCreatedAtAsc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OutboxMessage m set m.sentAt = :sentAt where m.id = :id and m.sentAt is null")
    int markSent(@Param("id") String id, @Param("sentAt") Instant sentAt);
}
