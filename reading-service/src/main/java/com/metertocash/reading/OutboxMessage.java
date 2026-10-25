package com.metertocash.reading;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {
    @Id
    private String id;
    private String transport;
    private String destination;
    @Column(name = "message_key")
    private String messageKey;
    @Column(name = "type_name")
    private String typeName;
    @Column(columnDefinition = "text")
    private String payload;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "sent_at")
    private Instant sentAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTransport() { return transport; }
    public void setTransport(String transport) { this.transport = transport; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getMessageKey() { return messageKey; }
    public void setMessageKey(String messageKey) { this.messageKey = messageKey; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
