package com.example.advanced;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    public CategoryController() {}

    @GetMapping
    public List<Category> listCategories() {
        return List.of(
            new Category(1L, "Electronics", "Electronic devices"),
            new Category(2L, "Books", "Books and publications"),
            new Category(3L, "Clothing", "Clothing and accessories")
        );
    }

    @GetMapping("/{id}")
    public Category getCategory(@PathVariable(value = "id") Long categoryId) {
        return new Category(categoryId, "Category " + categoryId, "Description");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category createCategory(@RequestBody Category category) {
        return category;
    }

    @PutMapping("/{id}")
    public Category updateCategory(
            @PathVariable(value = "id") Long categoryId,
            @RequestBody Category category) {
        category.id = categoryId;
        return category;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable(value = "id") Long categoryId) {
    }
}
