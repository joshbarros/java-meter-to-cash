package com.metertocash.charge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.Channels;
import com.metertocash.common.ChargeEvent;
import com.metertocash.common.MeterRules;
import com.metertocash.common.RateCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChargeWorker {

    private final ChargeRepository charges;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public ChargeWorker(ChargeRepository charges, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.charges = charges;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.RATE)
    @Transactional
    public void onRate(RateCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":rate")) {
            return;
        }
        try {
            int amount = MeterRules.charge(command.consumption(), command.rateCents());
            ChargeEntity row = new ChargeEntity();
            row.setId(command.billId());
            row.setAmountCents(amount);
            row.setStatus("RATED");
            charges.save(row);
            publish(command.billId(), amount, "RATED");
        } catch (ArithmeticException ex) {
            publish(command.billId(), 0, "REJECTED");
        }
    }

    private void publish(String billId, int amountCents, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.CHARGES, billId, new ChargeEvent(billId, amountCents, status));
    }
}
