package com.example.bestmlawi.ui.delivery;

public class OrderItem {
    private String itemId;
    private String name;
    private int quantity;
    private double price;
    private String notes;

    public OrderItem() {}

    public OrderItem(String itemId, String name, int quantity, double price) {
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getTotalPrice() {
        return quantity * price;
    }

    public String getFormattedPrice() {
        return String.format("%.2f DT", price);
    }

    public String getFormattedTotalPrice() {
        return String.format("%.2f DT", getTotalPrice());
    }
}