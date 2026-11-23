package com.expiryguard.app;

public class Product {
    public long id;
    public String name;
    public String category;
    public String expiryDate; // yyyy-MM-dd
    public String photoPath;  // absolute path in private storage, or null

    public Product() { }

    public Product(long id, String name, String category, String expiryDate, String photoPath) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.expiryDate = expiryDate;
        this.photoPath = photoPath;
    }

    public boolean hasPhoto() {
        return photoPath != null && !photoPath.isEmpty();
    }
}
