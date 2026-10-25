package com.metertocash.meter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.AdvanceCommand;
import com.metertocash.common.AdvanceEvent;
import com.metertocash.common.Channels;
import com.metertocash.common.RewindCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MeterWorker {

    private final MeterRepository meters;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public MeterWorker(MeterRepository meters, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.meters = meters;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.ADVANCE)
    @Transactional
    public void onAdvance(AdvanceCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":advance")) {
            return;
        }
        if (meters.advance(command.meterId(), command.previous(), command.next()) == 1) {
            publish(command.billId(), command.next(), "ADVANCED");
            return;
        }
        publish(command.billId(), command.previous(), "CONFLICT");
    }

    @RabbitListener(queues = Channels.REWIND)
    @Transactional
    public void onRewind(RewindCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":rewind")) {
            return;
        }
        if (meters.rewind(command.meterId(), command.previous(), command.next()) == 1) {
            publish(command.billId(), command.previous(), "REWOUND");
        }
    }

    private void publish(String billId, int lastRead, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.ADVANCES, billId, new AdvanceEvent(billId, lastRead, status));
    }
}
