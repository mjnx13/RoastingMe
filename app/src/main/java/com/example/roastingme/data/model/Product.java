package com.example.roastingme.data.model;

public class Product {

    private final String id;
    private final String name;
    private final String mallName;
    private final long price;
    private final String imageUrl;
    private final String productUrl;

    public Product(
            String id,
            String name,
            String mallName,
            long price,
            String imageUrl,
            String productUrl
    ) {
        this.id = id;
        this.name = name;
        this.mallName = mallName;
        this.price = price;
        this.imageUrl = imageUrl;
        this.productUrl = productUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMallName() {
        return mallName;
    }

    public long getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getProductUrl() {
        return productUrl;
    }
}