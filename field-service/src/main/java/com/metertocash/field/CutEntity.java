package com.metertocash.field;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cuts")
public class CutEntity {
    @Id
    private String id;

    @Column(name = "seal_token")
    private String sealToken;

    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSealToken() { return sealToken; }
    public void setSealToken(String sealToken) { this.sealToken = sealToken; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
