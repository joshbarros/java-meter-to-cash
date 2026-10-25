package com.metertocash.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id
    private String id;

    @Column(name = "bill_id")
    private String billId;

    @Column(name = "amount_cents")
    private int amountCents;

    @Column(name = "pay_token")
    private String payToken;

    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }
    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }
    public String getPayToken() { return payToken; }
    public void setPayToken(String payToken) { this.payToken = payToken; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
