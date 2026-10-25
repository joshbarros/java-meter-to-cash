package com.metertocash.reading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "readings")
public class ReadingEntity {
    @Id
    private String id;

    @Column(name = "previous_read")
    private int previousRead;

    @Column(name = "read_value")
    private int readValue;

    private int consumption;

    private String route;

    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getPreviousRead() { return previousRead; }
    public void setPreviousRead(int previousRead) { this.previousRead = previousRead; }
    public int getReadValue() { return readValue; }
    public void setReadValue(int readValue) { this.readValue = readValue; }
    public int getConsumption() { return consumption; }
    public void setConsumption(int consumption) { this.consumption = consumption; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
