package com.metertocash.payment;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

final class OutboxWriter {
    private OutboxWriter() {
    }

    static void kafka(OutboxRepository outbox, ObjectMapper mapper, String topic, String key, Object body) {
        add(outbox, mapper, "KAFKA", topic, key, body);
    }

    static void rabbit(OutboxRepository outbox, ObjectMapper mapper, String queue, String key, Object body) {
        add(outbox, mapper, "RABBIT", queue, key, body);
    }

    private static void add(OutboxRepository outbox, ObjectMapper mapper, String transport,
            String destination, String key, Object body) {
        OutboxMessage message = new OutboxMessage();
        message.setId(UUID.randomUUID().toString());
        message.setTransport(transport);
        message.setDestination(destination);
        message.setMessageKey(key);
        message.setTypeName(body.getClass().getName());
        try {
            message.setPayload(mapper.writeValueAsString(body));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("outbox payload", ex);
        }
        message.setCreatedAt(Instant.now());
        outbox.save(message);
    }
}
