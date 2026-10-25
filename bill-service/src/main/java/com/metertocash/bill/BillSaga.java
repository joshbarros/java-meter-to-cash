package com.metertocash.bill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.AdjustCommand;
import com.metertocash.common.AdjustmentEvent;
import com.metertocash.common.AdvanceCommand;
import com.metertocash.common.AdvanceEvent;
import com.metertocash.common.BalanceEvent;
import com.metertocash.common.BillFact;
import com.metertocash.common.Channels;
import com.metertocash.common.ChargeEvent;
import com.metertocash.common.FieldEvent;
import com.metertocash.common.FieldRestoreCommand;
import com.metertocash.common.MeterRules;
import com.metertocash.common.NoticeCommand;
import com.metertocash.common.NoticeEvent;
import com.metertocash.common.OpenBalanceCommand;
import com.metertocash.common.PaymentEvent;
import com.metertocash.common.PremiseEvent;
import com.metertocash.common.PremiseOffCommand;
import com.metertocash.common.PremiseOnCommand;
import com.metertocash.common.RateCommand;
import com.metertocash.common.ReadingEvent;
import com.metertocash.common.RewindCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BillSaga {

    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public BillSaga(OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @KafkaListener(topics = Channels.READINGS, groupId = "bill-readings")
    @Transactional
    public void onReading(ReadingEvent event) {
        BillEntity row = lock(event.billId());
        if (row == null) {
            return;
        }
        if ("REJECTED".equals(event.status())) {
            if ("READ".equals(row.getStatus()) || "RATED".equals(row.getStatus())) {
                cancel(row, "READ");
            }
            return;
        }
        if ("EMPTY".equals(event.status())) {
            if ("READ".equals(row.getStatus()) || "RATED".equals(row.getStatus())) {
                cancel(row, "EMPTY");
            }
            return;
        }
        if ("ESTIMATED".equals(event.status()) && "READ".equals(row.getStatus())) {
            row.setEstimated(true);
            row.setEstimatedRead(event.readValue());
            row.setReadValue(event.readValue());
            row.setConsumption(event.consumption());
            rate(row);
            return;
        }
        if ("ACTUAL".equals(event.status()) && "READ".equals(row.getStatus()) && !row.isEstimated()) {
            row.setReadValue(event.readValue());
            row.setConsumption(event.consumption());
            advance(row);
            return;
        }
        if ("ACTUAL".equals(event.status()) && "RATED".equals(row.getStatus()) && row.isEstimated()) {
            row.setReadValue(event.readValue());
            row.setConsumption(event.consumption());
            OutboxWriter.rabbit(outbox, mapper, Channels.ADJUST, row.getId(),
                    new AdjustCommand(row.getId(), row.getPreviousRead(), row.getEstimatedRead(), row.getReadValue(),
                            row.getRateCents()));
        }
    }

    @KafkaListener(topics = Channels.ADVANCES, groupId = "bill-advances")
    @Transactional
    public void onAdvance(AdvanceEvent event) {
        BillEntity row = lock(event.billId());
        if (row == null) {
            return;
        }
        if ("REWOUND".equals(event.status())) {
            return;
        }
        if ("CONFLICT".equals(event.status())) {
            if ("READ".equals(row.getStatus())) {
                cancel(row, "READ");
            } else if ("ADJUSTED".equals(row.getStatus())) {
                cancel(row, "ADJUST");
            }
            return;
        }
        if (!"ADVANCED".equals(event.status())) {
            return;
        }
        if ("READ".equals(row.getStatus()) && !row.isEstimated()) {
            row.setDialMoved(true);
            rate(row);
            return;
        }
        if ("ADJUSTED".equals(row.getStatus())) {
            row.setDialMoved(true);
            row.setStatus("BILLED");
            row.setOpenCents(row.getAmountCents());
            row.setReason("");
            fact(row);
            open(row);
        }
    }

    @KafkaListener(topics = Channels.CHARGES, groupId = "bill-charges")
    @Transactional
    public void onCharge(ChargeEvent event) {
        BillEntity row = lock(event.billId());
        if (row == null || !"READ".equals(row.getStatus())) {
            return;
        }
        if ("REJECTED".equals(event.status())) {
            cancel(row, "PRICE");
            return;
        }
        if (!"RATED".equals(event.status())) {
            return;
        }
        if (row.isEstimated() && !row.isDialMoved()) {
            row.setAmountCents(event.amountCents());
            row.setStatus("RATED");
            row.setReason("");
            fact(row);
            return;
        }
        if (row.isDialMoved()) {
            row.setAmountCents(event.amountCents());
            row.setOpenCents(event.amountCents());
            row.setStatus("BILLED");
            row.setReason("");
            fact(row);
            open(row);
        }
    }

    @KafkaListener(topics = Channels.ADJUSTMENTS, groupId = "bill-adjustments")
    @Transactional
    public void onAdjustment(AdjustmentEvent event) {
        BillEntity row = lock(event.billId());
        if (row == null || !"RATED".equals(row.getStatus())) {
            return;
        }
        if ("REJECTED".equals(event.status())) {
            cancel(row, "ADJUST");
            return;
        }
        if (!"POSTED".equals(event.status())) {
            return;
        }
        try {
            int next = MeterRules.openAfter(row.getAmountCents(), event.deltaCents());
            row.setAmountCents(next);
            row.setOpenCents(next);
            row.setStatus("ADJUSTED");
            row.setReason("");
            fact(row);
            advance(row);
        } catch (IllegalArgumentException | ArithmeticException ex) {
            cancel(row, "ADJUST");
        }
    }

    @KafkaListener(topics = Channels.NOTICES, groupId = "bill-notices")
    @Transactional
    public void onNotice(NoticeEvent event) {
        BillEntity row = lock(event.billId());
        if (row == null || !"BILLED".equals(row.getStatus()) || !"ISSUED".equals(event.status())) {
            return;
        }
        row.setStatus("NOTICED");
        row.setReason("");
        fact(row);
    }

    @KafkaListener(topics = Channels.BALANCES, groupId = "bill-balances")
    @Transactional
    public void onBalance(BalanceEvent event) {
        BillEntity row = lock(event.billId());
        if (row == null) {
            return;
        }
        if ("OPENED".equals(event.status()) && "BILLED".equals(row.getStatus())) {
            OutboxWriter.rabbit(outbox, mapper, Channels.NOTICE, row.getId(),
                    new NoticeCommand(row.getId(), row.getOpenCents()));
            return;
        }
        if ("CUT".equals(event.status()) && "NOTICED".equals(row.getStatus())) {
            row.setStatus("CUT");
            row.setReason("");
            fact(row);
            OutboxWriter.rabbit(outbox, mapper, Channels.PREMISE_OFF, row.getId(),
                    new PremiseOffCommand(row.getId(), row.getPremiseId()));
            return;
        }
        if (!"APPLIED".equals(event.status())) {
            return;
        }
        if ("NOTICED".equals(row.getStatus())) {
            row.setOpenCents(0);
            row.setStatus("PAID");
            row.setReason("");
            fact(row);
            return;
        }
        if ("CUT".equals(row.getStatus())) {
            row.setOpenCents(0);
            if (row.isServiceOff()) {
                row.setStatus("RESTORING");
                row.setReason("");
                fact(row);
                restore(row);
            } else {
                row.setStatus("CUT_PAID");
                row.setReason("");
                fact(row);
            }
            return;
        }
        if ("CUT_OFF".equals(row.getStatus())) {
            row.setOpenCents(0);
            row.setStatus("RESTORING");
            row.setReason("");
            fact(row);
            restore(row);
        }
    }

    @KafkaListener(topics = Channels.PREMISE_EVENTS, groupId = "bill-premise-events")
    @Transactional
    public void onPremise(PremiseEvent event) {
        BillEntity row = lock(event.billId());
        if (row == null) {
            return;
        }
        if ("OFF".equals(event.status())) {
            row.setServiceOff(true);
            if ("CUT".equals(row.getStatus())) {
                row.setStatus("CUT_OFF");
                row.setReason("");
                fact(row);
            } else if ("CUT_PAID".equals(row.getStatus())) {
                row.setStatus("RESTORING");
                row.setReason("");
                fact(row);
                restore(row);
            }
            return;
        }
        if ("ON".equals(event.status()) && "RESTORING".equals(row.getStatus())) {
            row.setServiceOff(false);
            row.setStatus("SETTLED");
            row.setReason("");
            fact(row);
        }
    }

    @KafkaListener(topics = Channels.FIELD, groupId = "bill-field")
    @Transactional
    public void onField(FieldEvent event) {
        BillEntity row = lock(event.billId());
        if (row == null || !"RESTORED".equals(event.status()) || !"RESTORING".equals(row.getStatus())) {
            return;
        }
        OutboxWriter.rabbit(outbox, mapper, Channels.PREMISE_ON, row.getId(),
                new PremiseOnCommand(row.getId(), row.getPremiseId()));
    }

    @KafkaListener(topics = Channels.PAYMENTS, groupId = "bill-payments")
    @Transactional
    public void onPayment(PaymentEvent event) {
        lock(event.billId());
    }

    private void rate(BillEntity row) {
        OutboxWriter.rabbit(outbox, mapper, Channels.RATE, row.getId(),
                new RateCommand(row.getId(), row.getConsumption(), row.getRateCents()));
    }

    private void advance(BillEntity row) {
        OutboxWriter.rabbit(outbox, mapper, Channels.ADVANCE, row.getId(),
                new AdvanceCommand(row.getId(), row.getMeterId(), row.getPreviousRead(), row.getReadValue()));
    }

    private void open(BillEntity row) {
        OutboxWriter.rabbit(outbox, mapper, Channels.OPEN_BALANCE, row.getId(),
                new OpenBalanceCommand(row.getId(), row.getOpenCents()));
    }

    private void restore(BillEntity row) {
        OutboxWriter.rabbit(outbox, mapper, Channels.FIELD_RESTORE, row.getId(), new FieldRestoreCommand(row.getId()));
    }

    private BillEntity lock(String id) {
        return em.find(BillEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    private void fact(BillEntity row) {
        OutboxWriter.kafka(outbox, mapper, Channels.BILLS, row.getId(),
                new BillFact(row.getId(), row.getStatus(), row.getReason() == null ? "" : row.getReason()));
    }

    private void cancel(BillEntity row, String reason) {
        if (!"READ".equals(row.getStatus()) && !"RATED".equals(row.getStatus()) && !"ADJUSTED".equals(row.getStatus())) {
            return;
        }
        boolean rewind = "PRICE".equals(reason) && row.isDialMoved();
        row.setStatus("CANCELLED");
        row.setReason(reason);
        fact(row);
        if (rewind) {
            OutboxWriter.rabbit(outbox, mapper, Channels.REWIND, row.getId(),
                    new RewindCommand(row.getId(), row.getMeterId(), row.getPreviousRead(), row.getReadValue()));
        }
    }
}
