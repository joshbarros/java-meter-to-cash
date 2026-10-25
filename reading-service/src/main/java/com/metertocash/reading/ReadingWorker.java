package com.metertocash.reading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.ActualCommand;
import com.metertocash.common.Channels;
import com.metertocash.common.MeterRules;
import com.metertocash.common.ReadCommand;
import com.metertocash.common.ReadingEvent;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReadingWorker {

    private final ReadingRepository readings;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public ReadingWorker(ReadingRepository readings, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.readings = readings;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.READ)
    @Transactional
    public void onRead(ReadCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":read")) {
            return;
        }
        try {
            int consumption = MeterRules.consumption(command.previous(), command.readValue());
            if (consumption == 0) {
                publish(command.billId(), 0, command.readValue(), "EMPTY");
                return;
            }
            String status = MeterRules.estimatedRoute(command.route()) ? "ESTIMATED" : "ACTUAL";
            ReadingEntity row = new ReadingEntity();
            row.setId(command.billId());
            row.setPreviousRead(command.previous());
            row.setReadValue(command.readValue());
            row.setConsumption(consumption);
            row.setRoute(command.route());
            row.setStatus(status);
            readings.save(row);
            publish(command.billId(), consumption, command.readValue(), status);
        } catch (IllegalArgumentException ex) {
            publish(command.billId(), 0, command.readValue(), "REJECTED");
        }
    }

    @RabbitListener(queues = Channels.ACTUAL)
    @Transactional
    public void onActual(ActualCommand command) {
        if (!InboxClaims.claim(em, command.billId() + ":actual")) {
            return;
        }
        try {
            int consumption = MeterRules.consumption(command.previous(), command.readValue());
            if (consumption == 0) {
                publish(command.billId(), 0, command.readValue(), "EMPTY");
                return;
            }
            ReadingEntity row = readings.findById(command.billId()).orElse(null);
            if (row == null) {
                publish(command.billId(), 0, command.readValue(), "REJECTED");
                return;
            }
            row.setReadValue(command.readValue());
            row.setConsumption(consumption);
            row.setStatus("ACTUAL");
            publish(command.billId(), consumption, command.readValue(), "ACTUAL");
        } catch (IllegalArgumentException ex) {
            publish(command.billId(), 0, command.readValue(), "REJECTED");
        }
    }

    private void publish(String billId, int consumption, int readValue, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.READINGS, billId,
                new ReadingEvent(billId, consumption, readValue, status));
    }
}
