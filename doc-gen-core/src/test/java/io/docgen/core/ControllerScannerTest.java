package io.docgen.core;

import io.docgen.core.analyzer.ControllerScanner;
import io.docgen.core.model.Operation;
import io.docgen.core.model.Parameter;
import io.docgen.core.model.PathItem;
import io.docgen.core.model.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerScannerTest {

    private Map<String, Schema> registry;
    private ControllerScanner scanner;

    @BeforeEach
    void setUp() {
        registry = new LinkedHashMap<>();
        scanner = new ControllerScanner(registry);
    }


    @SuppressWarnings("unused")
    static class ItemDto {
        public Long id;
        public String name;
    }

    @SuppressWarnings("unused")
    static class NestedDto {
        public String value;
        public ItemDto item;
    }

    @RestController
    @RequestMapping("/api/items")
    @SuppressWarnings("unused")
    static class SampleController {

        @GetMapping
        public List<ItemDto> getAll(@RequestParam(required = false) String filter) {
            return List.of();
        }

        @GetMapping("/{id}")
        public ItemDto getById(@PathVariable Long id) {
            return null;
        }

        @PostMapping
        public ItemDto create(@RequestBody ItemDto item) {
            return item;
        }

        @PutMapping("/{id}")
        public ItemDto update(@PathVariable Long id, @RequestBody ItemDto item) {
            return item;
        }

        @DeleteMapping("/{id}")
        public void delete(@PathVariable Long id) {
        }

        @GetMapping("/nested")
        public NestedDto getNested() {
            return null;
        }
    }


    @Test
    void detectsRestController() {
        Map<String, PathItem> paths = scanner.scan(List.of(SampleController.class));
        assertThat(paths).isNotEmpty();
    }

    @Test
    void mergesClassAndMethodPaths() {
        Map<String, PathItem> paths = scanner.scan(List.of(SampleController.class));
        assertThat(paths).containsKey("/api/items");
        assertThat(paths).containsKey("/api/items/{id}");
    }

    @Test
    void extractsGetOperation() {
        Map<String, PathItem> paths = scanner.scan(List.of(SampleController.class));
        Operation getAll = paths.get("/api/items").getOperation("get");
        assertThat(getAll).isNotNull();
        assertThat(getAll.getTags()).contains("Sample");
    }

    @Test
    void extractsQueryParameter() {
        Map<String, PathItem> paths = scanner.scan(List.of(SampleController.class));
        Operation getAll = paths.get("/api/items").getOperation("get");
        assertThat(getAll.getParameters()).isNotNull();

        Parameter filterParam = getAll.getParameters().stream()
                .filter(p -> "filter".equals(p.getName()))
                .findFirst().orElse(null);
        assertThat(filterParam).isNotNull();
        assertThat(filterParam.getIn()).isEqualTo(Parameter.In.query);
        assertThat(filterParam.isRequired()).isFalse();
    }

    @Test
    void extractsPathParameter() {
        Map<String, PathItem> paths = scanner.scan(List.of(SampleController.class));
        Operation getById = paths.get("/api/items/{id}").getOperation("get");
        assertThat(getById.getParameters()).isNotNull();

        Parameter idParam = getById.getParameters().stream()
                .filter(p -> "id".equals(p.getName()))
                .findFirst().orElse(null);
        assertThat(idParam).isNotNull();
        assertThat(idParam.getIn()).isEqualTo(Parameter.In.path);
        assertThat(idParam.isRequired()).isTrue();
    }

    @Test
    void extractsRequestBody() {
        Map<String, PathItem> paths = scanner.scan(List.of(SampleController.class));
        Operation create = paths.get("/api/items").getOperation("post");
        assertThat(create.getRequestBody()).isNotNull();
        assertThat(create.getRequestBody().isRequired()).isTrue();
        assertThat(create.getRequestBody().getContent()).containsKey("application/json");
    }

    @Test
    void deleteReturns204() {
        Map<String, PathItem> paths = scanner.scan(List.of(SampleController.class));
        Operation delete = paths.get("/api/items/{id}").getOperation("delete");
        assertThat(delete.getResponses()).containsKey("204");
    }

    @Test
    void registersSchemas() {
        scanner.scan(List.of(SampleController.class));
        assertThat(registry).containsKey("ItemDto");
    }

    @Test
    void registersNestedDtoSchema() {
        scanner.scan(List.of(SampleController.class));
        assertThat(registry).containsKey("NestedDto");
        assertThat(registry).containsKey("ItemDto");

        Schema nested = registry.get("NestedDto");
        assertThat(nested.getProperties()).containsKey("item");
        assertThat(nested.getProperties().get("item").getRef()).isEqualTo("#/components/schemas/ItemDto");
    }

    @Test
    void ignoresNonControllerClasses() {
        Map<String, PathItem> paths = scanner.scan(List.of(String.class, Integer.class));
        assertThat(paths).isEmpty();
    }
}

