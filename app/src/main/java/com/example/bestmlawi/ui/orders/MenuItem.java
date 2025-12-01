package com.example.bestmlawi.ui.orders;

import java.time.LocalDateTime;
import java.util.Objects;

public class MenuItem {
    private String id;
    private String name;
    private double price;
    private String imageUrl;
    private String category;
    private double rating;
    private String description;
    private String ingredients;
    private LocalDateTime createdAt;

    // Default constructor
    public MenuItem() {
    }

    // Parameterized constructor
    public MenuItem(String id, String name, double price, String imageUrl,
                    String category, double rating, String description,
                    String ingredients, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.rating = rating;
        this.description = description;
        this.ingredients = ingredients;
        this.createdAt = createdAt;
    }

    public MenuItem(String id, String name, double price, String imageUrl, String category, double rating, String description, String ingredients) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.rating = rating;
        this.description = description;
        this.ingredients = ingredients;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // toString method for easy printing
    @Override
    public String toString() {
        return "MenuItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", imageUrl='" + imageUrl + '\'' +
                ", category='" + category + '\'' +
                ", rating=" + rating +
                ", description='" + description + '\'' +
                ", ingredients='" + ingredients + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    // equals and hashCode methods for object comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItem menuItem = (MenuItem) o;
        return Double.compare(price, menuItem.price) == 0 &&
                Double.compare(rating, menuItem.rating) == 0 &&
                Objects.equals(id, menuItem.id) &&
                Objects.equals(name, menuItem.name) &&
                Objects.equals(imageUrl, menuItem.imageUrl) &&
                Objects.equals(category, menuItem.category) &&
                Objects.equals(description, menuItem.description) &&
                Objects.equals(ingredients, menuItem.ingredients) &&
                Objects.equals(createdAt, menuItem.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, imageUrl, category, rating, description, ingredients, createdAt);
    }
}