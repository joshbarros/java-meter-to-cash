package com.metertocash.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.BalanceCutCommand;
import com.metertocash.common.Channels;
import com.metertocash.common.FieldCutCommand;
import com.metertocash.common.FieldEvent;
import com.metertocash.common.FieldRestoreCommand;
import com.metertocash.common.MeterRules;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class FieldWorker {

    private final CutRepository cuts;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;
    private final TransactionTemplate tx;

    public FieldWorker(CutRepository cuts, OutboxRepository outbox, ObjectMapper mapper, EntityManager em,
            PlatformTransactionManager manager) {
        this.cuts = cuts;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
        this.tx = new TransactionTemplate(manager);
    }

    @RabbitListener(queues = Channels.FIELD_CUT)
    public void onCut(FieldCutCommand command) {
        Boolean dead = tx.execute(status -> {
            if (!InboxClaims.claim(em, command.billId() + ":field-cut")) {
                return false;
            }
            if (MeterRules.sealedMeter(command.sealToken())) {
                OutboxWriter.kafka(outbox, mapper, Channels.FIELD, command.billId(),
                        new FieldEvent(command.billId(), "FAILED"));
                return true;
            }
            CutEntity row = new CutEntity();
            row.setId(command.billId());
            row.setSealToken(command.sealToken());
            row.setStatus("AUTHORIZED");
            cuts.save(row);
            OutboxWriter.kafka(outbox, mapper, Channels.FIELD, command.billId(),
                    new FieldEvent(command.billId(), "AUTHORIZED"));
            OutboxWriter.rabbit(outbox, mapper, Channels.BALANCE_CUT, command.billId(),
                    new BalanceCutCommand(command.billId()));
            return false;
        });
        if (Boolean.TRUE.equals(dead)) {
            throw new AmqpRejectAndDontRequeueException("sealed");
        }
    }

    @RabbitListener(queues = Channels.FIELD_RESTORE)
    @Transactional
    public void onRestore(FieldRestoreCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":field-restore")) {
            return;
        }
        CutEntity row = cuts.findById(command.billId()).orElse(null);
        if (row == null || !"AUTHORIZED".equals(row.getStatus())) {
            return;
        }
        row.setStatus("RESTORED");
        OutboxWriter.kafka(outbox, mapper, Channels.FIELD, command.billId(), new FieldEvent(command.billId(), "RESTORED"));
    }
}
