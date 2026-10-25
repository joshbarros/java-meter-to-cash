package com.metertocash.common;

public final class Channels {
    public static final String ACCOUNTS = "meter.accounts";
    public static final String PREMISES = "meter.premises";
    public static final String PREMISE_EVENTS = "meter.premise.events";
    public static final String METERS = "meter.meters";
    public static final String ADVANCES = "meter.advances";
    public static final String READINGS = "meter.readings";
    public static final String TARIFFS = "meter.tariffs";
    public static final String CHARGES = "meter.charges";
    public static final String BILLS = "meter.bills";
    public static final String ADJUSTMENTS = "meter.adjustments";
    public static final String NOTICES = "meter.notices";
    public static final String BALANCES = "meter.balances";
    public static final String PAYMENTS = "meter.payments";
    public static final String FIELD = "meter.field";

    public static final String READ = "meter.reading.read";
    public static final String READ_DLQ = "meter.reading.read.dlq";
    public static final String ACTUAL = "meter.reading.actual";
    public static final String ACTUAL_DLQ = "meter.reading.actual.dlq";
    public static final String RATE = "meter.charge.rate";
    public static final String RATE_DLQ = "meter.charge.rate.dlq";
    public static final String ADJUST = "meter.adjustment.adjust";
    public static final String ADJUST_DLQ = "meter.adjustment.adjust.dlq";
    public static final String NOTICE = "meter.notice.issue";
    public static final String NOTICE_DLQ = "meter.notice.issue.dlq";
    public static final String OPEN_BALANCE = "meter.balance.open";
    public static final String OPEN_BALANCE_DLQ = "meter.balance.open.dlq";
    public static final String APPLY = "meter.balance.apply";
    public static final String APPLY_DLQ = "meter.balance.apply.dlq";
    public static final String BALANCE_CUT = "meter.balance.cut";
    public static final String BALANCE_CUT_DLQ = "meter.balance.cut.dlq";
    public static final String TENDER = "meter.payment.tender";
    public static final String TENDER_DLQ = "meter.payment.tender.dlq";
    public static final String FIELD_CUT = "meter.field.cut";
    public static final String FIELD_CUT_DLQ = "meter.field.cut.dlq";
    public static final String FIELD_RESTORE = "meter.field.restore";
    public static final String FIELD_RESTORE_DLQ = "meter.field.restore.dlq";
    public static final String PREMISE_OFF = "meter.premise.off";
    public static final String PREMISE_OFF_DLQ = "meter.premise.off.dlq";
    public static final String PREMISE_ON = "meter.premise.on";
    public static final String PREMISE_ON_DLQ = "meter.premise.on.dlq";
    public static final String ADVANCE = "meter.meter.advance";
    public static final String ADVANCE_DLQ = "meter.meter.advance.dlq";
    public static final String REWIND = "meter.meter.rewind";
    public static final String REWIND_DLQ = "meter.meter.rewind.dlq";

    private Channels() {
    }
}
