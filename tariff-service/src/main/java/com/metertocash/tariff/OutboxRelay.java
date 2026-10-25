package com.metertocash.tariff;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository messages;
    private final ObjectMapper mapper;
    private final KafkaTemplate<String, Object> kafka;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate tx;

    public OutboxRelay(OutboxRepository messages, ObjectMapper mapper, KafkaTemplate<String, Object> kafka,
            RabbitTemplate rabbit, PlatformTransactionManager manager) {
        this.messages = messages;
        this.mapper = mapper;
        this.kafka = kafka;
        this.rabbit = rabbit;
        this.tx = new TransactionTemplate(manager);
    }

    @Scheduled(fixedDelay = 200)
    public void publishPending() {
        for (OutboxMessage message : messages.findTop25BySentAtIsNullOrderByCreatedAtAsc()) {
            try {
                dispatch(message);
                tx.executeWithoutResult(status -> messages.markSent(message.getId(), Instant.now()));
            } catch (Exception ex) {
                log.warn("outbox send failed for {}: {}", message.getId(), ex.toString());
            }
        }
    }

    private void dispatch(OutboxMessage message) throws Exception {
        Class<?> type = Class.forName(message.getTypeName(), true, OutboxRelay.class.getClassLoader());
        Object body = mapper.readValue(message.getPayload(), type);
        if ("KAFKA".equals(message.getTransport())) {
            kafka.send(message.getDestination(), message.getMessageKey(), body).get(10, TimeUnit.SECONDS);
            return;
        }
        rabbit.convertAndSend(message.getDestination(), body);
    }
}
