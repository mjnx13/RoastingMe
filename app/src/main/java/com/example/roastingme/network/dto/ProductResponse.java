package com.example.roastingme.network.dto;

public class ProductResponse {
    private String id;
    private String name;
    private String mallName;
    private int price;
    private String imageUrl;
    private String productUrl;

    public ProductResponse() {}

    public ProductResponse(String id, String name, String mallName, int price, String imageUrl, String productUrl) {
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

    public int getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getProductUrl() {
        return productUrl;
    }
}