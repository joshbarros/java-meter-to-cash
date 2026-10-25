package com.metertocash.bill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "meter_directory")
public class MeterDirectory {
    @Id
    private String id;

    @Column(name = "premise_id")
    private String premiseId;

    private String serial;

    @Column(name = "last_read")
    private int lastRead;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPremiseId() { return premiseId; }
    public void setPremiseId(String premiseId) { this.premiseId = premiseId; }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public int getLastRead() { return lastRead; }
    public void setLastRead(int lastRead) { this.lastRead = lastRead; }
}
