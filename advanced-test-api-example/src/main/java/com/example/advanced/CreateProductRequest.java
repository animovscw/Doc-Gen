package com.example.advanced;

public class CreateProductRequest {
    public String name;
    public String description;
    public Double price;
    public Long categoryId;
    public java.util.List<String> tags;

    public CreateProductRequest() {}

    public CreateProductRequest(String name, String description, Double price,
                               Long categoryId, java.util.List<String> tags) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.tags = tags;
    }
}

