package com.metertocash.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.ApplyCommand;
import com.metertocash.common.Channels;
import com.metertocash.common.MeterRules;
import com.metertocash.common.PaymentEvent;
import com.metertocash.common.TenderCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PaymentWorker {

    private final PaymentRepository payments;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;
    private final TransactionTemplate tx;

    public PaymentWorker(PaymentRepository payments, OutboxRepository outbox, ObjectMapper mapper, EntityManager em,
            PlatformTransactionManager manager) {
        this.payments = payments;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
        this.tx = new TransactionTemplate(manager);
    }

    @RabbitListener(queues = Channels.TENDER)
    public void onTender(TenderCommand command) {
        Boolean dead = tx.execute(status -> {
            if (!InboxClaims.claim(em, command.requestId() + ":tender")) {
                return false;
            }
            if (MeterRules.declinedToken(command.payToken())) {
                OutboxWriter.kafka(outbox, mapper, Channels.PAYMENTS, command.billId(),
                        new PaymentEvent(command.billId(), command.requestId(), "FAILED"));
                return true;
            }
            PaymentEntity row = new PaymentEntity();
            row.setId(command.requestId());
            row.setBillId(command.billId());
            row.setAmountCents(command.amountCents());
            row.setPayToken(command.payToken());
            row.setStatus("TENDERED");
            payments.save(row);
            OutboxWriter.kafka(outbox, mapper, Channels.PAYMENTS, command.billId(),
                    new PaymentEvent(command.billId(), command.requestId(), "TENDERED"));
            OutboxWriter.rabbit(outbox, mapper, Channels.APPLY, command.billId(),
                    new ApplyCommand(command.billId(), command.requestId(), command.amountCents()));
            return false;
        });
        if (Boolean.TRUE.equals(dead)) {
            throw new AmqpRejectAndDontRequeueException("declined");
        }
    }
}
