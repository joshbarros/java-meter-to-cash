package com.metertocash.meter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inbox")
public class InboxMessage {
    @Id
    private String id;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
