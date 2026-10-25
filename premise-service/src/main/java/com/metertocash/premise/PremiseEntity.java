package com.metertocash.premise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "premises")
public class PremiseEntity {
    @Id
    private String id;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "premise_code", unique = true)
    private String premiseCode;

    private String address;

    @Column(name = "service_state")
    private String serviceState;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getPremiseCode() { return premiseCode; }
    public void setPremiseCode(String premiseCode) { this.premiseCode = premiseCode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getServiceState() { return serviceState; }
    public void setServiceState(String serviceState) { this.serviceState = serviceState; }
}
