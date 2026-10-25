package com.metertocash.bill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bill_actions")
public class BillActionEntity {
    @Id
    private String id;

    @Column(name = "bill_id")
    private String billId;

    private String kind;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
}
