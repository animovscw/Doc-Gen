package com.example.advanced;

import java.util.List;
import java.util.Map;

public class Product {
    public Long id;
    public String name;
    public String description;
    public Double price;
    public ProductStatus status;
    public Category category;
    public List<String> tags;
    public Map<String, String> attributes;
    public Integer stock;

    public Product() {}

    public Product(Long id, String name, String description, Double price,
                   ProductStatus status, Category category, List<String> tags,
                   Map<String, String> attributes, Integer stock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = status;
        this.category = category;
        this.tags = tags;
        this.attributes = attributes;
        this.stock = stock;
    }
}

