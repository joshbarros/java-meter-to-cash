package com.metertocash.adjustment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.AdjustCommand;
import com.metertocash.common.AdjustmentEvent;
import com.metertocash.common.Channels;
import com.metertocash.common.MeterRules;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdjustmentWorker {

    private final AdjustmentRepository adjustments;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public AdjustmentWorker(AdjustmentRepository adjustments, OutboxRepository outbox, ObjectMapper mapper,
            EntityManager em) {
        this.adjustments = adjustments;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.ADJUST)
    @Transactional
    public void onAdjust(AdjustCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":adjust")) {
            return;
        }
        try {
            int delta = MeterRules.adjustment(command.previous(), command.estimatedRead(), command.readValue(),
                    command.rateCents());
            AdjustmentEntity row = new AdjustmentEntity();
            row.setId(command.billId());
            row.setDeltaCents(delta);
            row.setStatus("POSTED");
            adjustments.save(row);
            publish(command.billId(), delta, "POSTED");
        } catch (ArithmeticException | IllegalArgumentException ex) {
            publish(command.billId(), 0, "REJECTED");
        }
    }

    private void publish(String billId, int deltaCents, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.ADJUSTMENTS, billId, new AdjustmentEvent(billId, deltaCents, status));
    }
}
