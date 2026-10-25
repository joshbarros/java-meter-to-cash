package com.metertocash.premise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.Channels;
import com.metertocash.common.PremiseEvent;
import com.metertocash.common.PremiseOffCommand;
import com.metertocash.common.PremiseOnCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PremiseWorker {

    private final PremiseRepository premises;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public PremiseWorker(PremiseRepository premises, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.premises = premises;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.PREMISE_OFF)
    @Transactional
    public void onOff(PremiseOffCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":premise-off")) {
            return;
        }
        if (premises.markOff(command.premiseId()) == 1) {
            publish(command.billId(), command.premiseId(), "OFF");
        }
    }

    @RabbitListener(queues = Channels.PREMISE_ON)
    @Transactional
    public void onOn(PremiseOnCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":premise-on")) {
            return;
        }
        if (premises.markOn(command.premiseId()) == 1) {
            publish(command.billId(), command.premiseId(), "ON");
        }
    }

    private void publish(String billId, String premiseId, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.PREMISE_EVENTS, billId, new PremiseEvent(billId, premiseId, status));
    }
}
