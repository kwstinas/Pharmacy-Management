package com.pharmacy.model;

public class MedicineIngredient {

    private Long id;
    private Long medicineId;
    private String ingredientName;

    public MedicineIngredient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMedicineId() { return medicineId; }
    public void setMedicineId(Long medicineId) { this.medicineId = medicineId; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
}