package com.example.bestmlawi.ui.orders;

import java.util.Date;

public class Order {
    private String id;                 // ID document Firestore
    private String user_id;            // ID utilisateur
    private String sales_point_id;     // ID point de vente
    private String status;             // Statut: "En cours", "Prêt", "Livré"
    private String address;            // Adresse du client
    private Date orderDate;            // Date de la commande
    private String deliverId;           // ID du chauffeur
    private double totalAmount;        // 🔥 Montant total de la commande

    public Order() {}

    // ✅ Constructeur mis à jour avec totalAmount
    public Order(String id, String user_id, String sales_point_id, String status,
                 String address, Date orderDate, String driverId, double totalAmount) {
        this.id = id;
        this.user_id = user_id;
        this.sales_point_id = sales_point_id;
        this.status = status;
        this.address = address;
        this.orderDate = orderDate;
        this.deliverId = driverId;
        this.totalAmount = totalAmount;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getUser_id() { return user_id; }
    public String getSales_point_id() { return sales_point_id; }
    public String getStatus() { return status; }
    public String getAddress() { return address; }
    public Date getOrderDate() { return orderDate; }
    public String getDeliverId() { return deliverId; }
    public double getTotalAmount() { return totalAmount; }  // ✅ Getter ajouté

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setSales_point_id(String sales_point_id) { this.sales_point_id = sales_point_id; }
    public void setStatus(String status) { this.status = status; }
    public void setAddress(String address) { this.address = address; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }
    public void setDeliverId(String deliverId) { this.deliverId = deliverId; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; } // ✅ Setter ajouté
}