package com.metertocash.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.Channels;
import com.metertocash.common.NoticeCommand;
import com.metertocash.common.NoticeEvent;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NoticeWorker {

    private final NoticeRepository notices;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public NoticeWorker(NoticeRepository notices, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.notices = notices;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.NOTICE)
    @Transactional
    public void onNotice(NoticeCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":notice")) {
            return;
        }
        NoticeEntity row = new NoticeEntity();
        row.setId(command.billId());
        row.setAmountCents(command.amountCents());
        row.setStatus("ISSUED");
        notices.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.NOTICES, command.billId(), new NoticeEvent(command.billId(), "ISSUED"));
    }
}
