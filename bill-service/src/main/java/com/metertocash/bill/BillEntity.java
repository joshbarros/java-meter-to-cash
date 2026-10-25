package com.metertocash.bill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bills")
public class BillEntity {
    @Id
    private String id;

    @Column(name = "request_id", unique = true)
    private String requestId;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "premise_id")
    private String premiseId;

    @Column(name = "meter_id")
    private String meterId;

    @Column(name = "tariff_id")
    private String tariffId;

    @Column(name = "previous_read")
    private int previousRead;

    @Column(name = "read_value")
    private int readValue;

    @Column(name = "estimated_read")
    private int estimatedRead;

    private int consumption;

    @Column(name = "rate_cents")
    private int rateCents;

    @Column(name = "amount_cents")
    private int amountCents;

    @Column(name = "open_cents")
    private int openCents;

    private String route;

    private boolean estimated;

    @Column(name = "dial_moved")
    private boolean dialMoved;

    @Column(name = "service_off")
    private boolean serviceOff;

    @Column(name = "actual_requested")
    private boolean actualRequested;

    private String status;

    private String reason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getPremiseId() { return premiseId; }
    public void setPremiseId(String premiseId) { this.premiseId = premiseId; }
    public String getMeterId() { return meterId; }
    public void setMeterId(String meterId) { this.meterId = meterId; }
    public String getTariffId() { return tariffId; }
    public void setTariffId(String tariffId) { this.tariffId = tariffId; }
    public int getPreviousRead() { return previousRead; }
    public void setPreviousRead(int previousRead) { this.previousRead = previousRead; }
    public int getReadValue() { return readValue; }
    public void setReadValue(int readValue) { this.readValue = readValue; }
    public int getEstimatedRead() { return estimatedRead; }
    public void setEstimatedRead(int estimatedRead) { this.estimatedRead = estimatedRead; }
    public int getConsumption() { return consumption; }
    public void setConsumption(int consumption) { this.consumption = consumption; }
    public int getRateCents() { return rateCents; }
    public void setRateCents(int rateCents) { this.rateCents = rateCents; }
    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }
    public int getOpenCents() { return openCents; }
    public void setOpenCents(int openCents) { this.openCents = openCents; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public boolean isEstimated() { return estimated; }
    public void setEstimated(boolean estimated) { this.estimated = estimated; }
    public boolean isDialMoved() { return dialMoved; }
    public void setDialMoved(boolean dialMoved) { this.dialMoved = dialMoved; }
    public boolean isServiceOff() { return serviceOff; }
    public void setServiceOff(boolean serviceOff) { this.serviceOff = serviceOff; }
    public boolean isActualRequested() { return actualRequested; }
    public void setActualRequested(boolean actualRequested) { this.actualRequested = actualRequested; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
