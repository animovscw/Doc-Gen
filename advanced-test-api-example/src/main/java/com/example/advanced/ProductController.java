package com.example.advanced;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    public ProductController() {}

    @GetMapping
    public PaginatedResponse<Product> listProducts(
            @RequestParam(value = "status", required = false) ProductStatus status,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "sort", required = false) String sort) {

        List<Product> products = List.of(
                new Product(1L, "Laptop", "High-performance laptop", 999.99, ProductStatus.ACTIVE,
                        new Category(1L, "Electronics", "Electronic devices"),
                        List.of("tech", "computer"), Map.of("brand", "Dell"), 50),
                new Product(2L, "Mouse", "Wireless mouse", 29.99, ProductStatus.ACTIVE,
                        new Category(1L, "Electronics", "Electronic devices"),
                        List.of("tech", "accessory"), Map.of("brand", "Logitech"), 150)
        );

        return new PaginatedResponse<>(products, page, size, 2L, 1, false);
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable(value = "id") Long productId) {
        return new Product(productId, "Product " + productId, "Description", 99.99,
                ProductStatus.ACTIVE,
                new Category(1L, "Electronics", "Electronic devices"),
                List.of("tag1", "tag2"),
                Map.of("attr1", "value1"),
                100);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestBody CreateProductRequest request) {

        return new Product(1L, request.name, request.description, request.price,
                ProductStatus.ACTIVE,
                new Category(request.categoryId, "Category", "Desc"),
                request.tags,
                new HashMap<>(),
                0);
    }

    @PutMapping("/{id}")
    public Product updateProduct(
            @PathVariable(value = "id") Long productId,
            @RequestBody CreateProductRequest request) {

        return new Product(productId, request.name, request.description, request.price,
                ProductStatus.ACTIVE,
                new Category(request.categoryId, "Category", "Desc"),
                request.tags,
                new HashMap<>(),
                0);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable(value = "id") Long productId) {
    }

    @PatchMapping("/{id}/status")
    public Product updateProductStatus(
            @PathVariable(value = "id") Long productId,
            @RequestParam(value = "status") ProductStatus status) {

        return new Product(productId, "Product", "Description", 99.99, status,
                new Category(1L, "Category", "Desc"),
                List.of(),
                new HashMap<>(),
                100);
    }

    @GetMapping("/search")
    public List<Product> searchProducts(
            @RequestParam(value = "query") String query,
            @RequestParam(value = "tags", required = false) List<String> tags) {

        return List.of(
                new Product(1L, "Product 1", "Description", 99.99, ProductStatus.ACTIVE,
                        new Category(1L, "Category", "Desc"),
                        tags != null ? tags : List.of(),
                        new HashMap<>(),
                        100)
        );
    }
}
