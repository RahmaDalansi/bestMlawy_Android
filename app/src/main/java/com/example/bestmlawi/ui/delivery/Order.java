package com.example.bestmlawi.ui.delivery;

import java.util.Date;
import java.util.List;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Order {
    private String orderId;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private String status; // pending, assigned, in_progress, delivered, cancelled
    private String deliverId; // ← Champ
    private String deliveryPersonName;
    private Date orderDate;
    private Date deliveryDate;
    private double totalAmount;
    private List<Object> items;
    private String notes;
    private double deliveryFee;
    private double latitude;
    private double longitude;

    public Order() {
        // Constructeur vide requis pour Firestore
    }

    public Order(String orderId, String customerName, String deliveryAddress, double totalAmount) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.deliveryAddress = deliveryAddress;
        this.totalAmount = totalAmount;
        this.status = "pending";
        this.orderDate = new Date();
    }

    // Getters et Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // CORRECTION : Getters/setters cohérents avec le champ deliverId
    public String getDeliverId() { return deliverId; } // ← Changé à getDeliverId
    public void setDeliverId(String deliverId) { this.deliverId = deliverId; } // ← Changé à setDeliverId

    public String getDeliveryPersonName() { return deliveryPersonName; }
    public void setDeliveryPersonName(String deliveryPersonName) { this.deliveryPersonName = deliveryPersonName; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public Date getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Date deliveryDate) { this.deliveryDate = deliveryDate; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public List<Object> getItems() { return items; }
    public void setItems(List<Object> items) { this.items = items; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // Méthodes utilitaires
    public String getFormattedTotal() {
        return String.format("%.2f €", totalAmount);
    }

    public String getFormattedOrderDate() {
        return android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", orderDate).toString();
    }

    public boolean isAssigned() {
        return "assigned".equals(status) || "in_progress".equals(status);
    }

    public boolean canBeDelivered() {
        return "assigned".equals(status) || "in_progress".equals(status);
    }
}