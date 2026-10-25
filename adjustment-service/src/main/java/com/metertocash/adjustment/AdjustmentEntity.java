package com.metertocash.adjustment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "adjustments")
public class AdjustmentEntity {
    @Id
    private String id;

    @Column(name = "delta_cents")
    private int deltaCents;

    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getDeltaCents() { return deltaCents; }
    public void setDeltaCents(int deltaCents) { this.deltaCents = deltaCents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
