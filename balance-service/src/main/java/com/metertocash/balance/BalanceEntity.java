package com.metertocash.balance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "balances")
public class BalanceEntity {
    @Id
    @Column(name = "bill_id")
    private String id;

    @Column(name = "open_cents")
    private int openCents;

    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getOpenCents() { return openCents; }
    public void setOpenCents(int openCents) { this.openCents = openCents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
