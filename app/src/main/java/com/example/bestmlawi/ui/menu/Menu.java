package com.example.bestmlawi.ui.menu;

import com.google.firebase.Timestamp;

public class Menu {
    private String id;
    private String name;
    private String description;
    private double price;
    private String category;
    private String ingredients;
    private String imageBase64; // Change from imageUrl to imageBase64
    private double rating;
    private Timestamp createdAt;

    // Empty constructor required for Firestore
    public Menu() {}

    public Menu(String name, String description, double price, String category,
                String ingredients, String imageBase64, double rating) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.ingredients = ingredients;
        this.imageBase64 = imageBase64;
        this.rating = rating;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    // Updated getter/setter for Base64 image
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}