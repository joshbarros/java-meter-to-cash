package com.metertocash.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class AccountEntity {
    @Id
    private String id;

    @Column(name = "account_number", unique = true)
    private String accountNumber;

    private String name;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
