package com.pharmacy.model;

import java.time.LocalDateTime;

public class StockMovement {

    public enum MovementType { IN, OUT }

    private Long id;
    private Long medicineId;
    private MovementType type;
    private int quantity;
    private LocalDateTime occurredAt;
    private String note;
    private String medicineName; // θα γεμίζει από JOIN queries

    public StockMovement() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMedicineId() { return medicineId; }
    public void setMedicineId(Long medicineId) { this.medicineId = medicineId; }

    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
}