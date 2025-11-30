package com.example.bestmlawi.ui.orders;

public class OrderLineItem {
    private String id;
    private String menuItemId;
    private String orderId; // ✅ Changé de int à String
    private int quantity;

    // Default constructor
    public OrderLineItem() {
    }

    // Parameterized constructor
    public OrderLineItem(String id, String menuItemId, String orderId, int quantity) { // ✅ Paramètre String
        this.id = id;
        this.menuItemId = menuItemId;
        this.orderId = orderId;
        this.quantity = quantity;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(String menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getOrderId() { // ✅ Retourne String
        return orderId;
    }

    public void setOrderId(String orderId) { // ✅ Prend String
        this.orderId = orderId;
    }

    public int getQuantity() {
        return quantity;
    }


    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "OrderLineItem{" +
                "id=" + id +
                ", menuItemId=" + menuItemId +
                ", orderId='" + orderId + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}