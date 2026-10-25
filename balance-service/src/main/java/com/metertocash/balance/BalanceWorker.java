package com.metertocash.balance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.ApplyCommand;
import com.metertocash.common.BalanceCutCommand;
import com.metertocash.common.BalanceEvent;
import com.metertocash.common.Channels;
import com.metertocash.common.OpenBalanceCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BalanceWorker {

    private final BalanceRepository balances;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public BalanceWorker(BalanceRepository balances, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.balances = balances;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.OPEN_BALANCE)
    @Transactional
    public void onOpen(OpenBalanceCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":open")) {
            return;
        }
        if (balances.existsById(command.billId())) {
            return;
        }
        BalanceEntity row = new BalanceEntity();
        row.setId(command.billId());
        row.setOpenCents(command.amountCents());
        row.setStatus("OPEN");
        balances.save(row);
        publish(command.billId(), "", command.amountCents(), "OPENED");
    }

    @RabbitListener(queues = Channels.APPLY, concurrency = "4")
    @Transactional
    public void onApply(ApplyCommand command) {
        if (!InboxClaims.claim(em, command.requestId() + ":apply")) {
            return;
        }
        if (balances.apply(command.billId(), command.amountCents()) != 1) {
            publish(command.billId(), command.requestId(), 0, "SHORT");
            return;
        }
        BalanceEntity row = balances.findById(command.billId()).orElseThrow();
        publish(command.billId(), command.requestId(), row.getOpenCents(), "APPLIED");
    }

    @RabbitListener(queues = Channels.BALANCE_CUT, concurrency = "4")
    @Transactional
    public void onCut(BalanceCutCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":balance-cut")) {
            return;
        }
        if (balances.cut(command.billId()) != 1) {
            publish(command.billId(), "", 0, "SKIPPED");
            return;
        }
        BalanceEntity row = balances.findById(command.billId()).orElseThrow();
        publish(command.billId(), "", row.getOpenCents(), "CUT");
    }

    private void publish(String billId, String requestId, int openCents, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.BALANCES, billId,
                new BalanceEvent(billId, requestId, openCents, status));
    }
}
