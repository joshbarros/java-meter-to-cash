package com.metertocash.bill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tariff_directory")
public class TariffDirectory {
    @Id
    private String id;

    private String code;

    @Column(name = "rate_cents")
    private int rateCents;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public int getRateCents() { return rateCents; }
    public void setRateCents(int rateCents) { this.rateCents = rateCents; }
}
