package com.metertocash.audit;

import java.time.Instant;

import com.metertocash.common.AdjustmentEvent;
import com.metertocash.common.AdvanceEvent;
import com.metertocash.common.BalanceEvent;
import com.metertocash.common.BillFact;
import com.metertocash.common.Channels;
import com.metertocash.common.ChargeEvent;
import com.metertocash.common.FieldEvent;
import com.metertocash.common.NoticeEvent;
import com.metertocash.common.PaymentEvent;
import com.metertocash.common.PremiseEvent;
import com.metertocash.common.ReadingEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditWorker {

    private final AuditRepository entries;

    public AuditWorker(AuditRepository entries) {
        this.entries = entries;
    }

    @KafkaListener(topics = Channels.BILLS, groupId = "audit-bills")
    @Transactional
    public void onBill(BillFact event) {
        add("BillFact", event.billId(), event.status());
    }

    @KafkaListener(topics = Channels.READINGS, groupId = "audit-readings")
    @Transactional
    public void onReading(ReadingEvent event) {
        add("ReadingEvent", event.billId(), event.status());
    }

    @KafkaListener(topics = Channels.CHARGES, groupId = "audit-charges")
    @Transactional
    public void onCharge(ChargeEvent event) {
        add("ChargeEvent", event.billId(), event.status());
    }

    @KafkaListener(topics = Channels.ADJUSTMENTS, groupId = "audit-adjustments")
    @Transactional
    public void onAdjustment(AdjustmentEvent event) {
        add("AdjustmentEvent", event.billId(), event.status());
    }

    @KafkaListener(topics = Channels.NOTICES, groupId = "audit-notices")
    @Transactional
    public void onNotice(NoticeEvent event) {
        add("NoticeEvent", event.billId(), event.status());
    }

    @KafkaListener(topics = Channels.BALANCES, groupId = "audit-balances")
    @Transactional
    public void onBalance(BalanceEvent event) {
        add("BalanceEvent", event.billId(), event.status());
    }

    @KafkaListener(topics = Channels.PAYMENTS, groupId = "audit-payments")
    @Transactional
    public void onPayment(PaymentEvent event) {
        add("PaymentEvent", event.billId(), event.status());
    }

    @KafkaListener(topics = Channels.FIELD, groupId = "audit-field")
    @Transactional
    public void onField(FieldEvent event) {
        add("FieldEvent", event.billId(), event.status());
    }

    @KafkaListener(topics = Channels.PREMISE_EVENTS, groupId = "audit-premise-events")
    @Transactional
    public void onPremise(PremiseEvent event) {
        add("PremiseEvent", event.billId(), event.status());
    }

    @KafkaListener(topics = Channels.ADVANCES, groupId = "audit-advances")
    @Transactional
    public void onAdvance(AdvanceEvent event) {
        add("AdvanceEvent", event.billId(), event.status());
    }

    private void add(String kind, String billId, String status) {
        String eventId = kind + ":" + billId + ":" + status;
        if (entries.existsById(eventId)) {
            return;
        }
        AuditEntry row = new AuditEntry();
        row.setId(eventId);
        row.setBillId(billId);
        row.setKind(kind);
        row.setStatus(status);
        row.setRecordedAt(Instant.now());
        entries.save(row);
    }
}
