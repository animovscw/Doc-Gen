package io.docgen.core;

import io.docgen.core.analyzer.TypeResolver;
import io.docgen.core.model.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class TypeResolverTest {

    private Map<String, Schema> registry;
    private TypeResolver resolver;

    @BeforeEach
    void setUp() {
        registry = new LinkedHashMap<>();
        resolver = new TypeResolver(registry);
    }


    @Test
    void resolvesStringType() {
        Schema s = resolver.resolve(String.class);
        assertThat(s.getType()).isEqualTo("string");
    }

    @Test
    void resolvesIntPrimitive() {
        Schema s = resolver.resolve(int.class);
        assertThat(s.getType()).isEqualTo("integer");
        assertThat(s.getFormat()).isEqualTo("int32");
    }

    @Test
    void resolvesLongWrapper() {
        Schema s = resolver.resolve(Long.class);
        assertThat(s.getType()).isEqualTo("integer");
        assertThat(s.getFormat()).isEqualTo("int64");
    }

    @Test
    void resolvesBooleanPrimitive() {
        Schema s = resolver.resolve(boolean.class);
        assertThat(s.getType()).isEqualTo("boolean");
    }

    @Test
    void resolvesDoublePrimitive() {
        Schema s = resolver.resolve(double.class);
        assertThat(s.getType()).isEqualTo("number");
        assertThat(s.getFormat()).isEqualTo("double");
    }

    @Test
    void resolvesLocalDate() {
        Schema s = resolver.resolve(LocalDate.class);
        assertThat(s.getType()).isEqualTo("string");
        assertThat(s.getFormat()).isEqualTo("date");
    }

    @Test
    void resolvesLocalDateTime() {
        Schema s = resolver.resolve(LocalDateTime.class);
        assertThat(s.getType()).isEqualTo("string");
        assertThat(s.getFormat()).isEqualTo("date-time");
    }

    @Test
    void resolvesVoidToNull() {
        assertThat(resolver.resolve(void.class)).isNull();
        assertThat(resolver.resolve(Void.class)).isNull();
    }

    enum Color { RED, GREEN, BLUE }

    @Test
    void resolvesEnumToRefAndRegistersSchema() {
        Schema s = resolver.resolve(Color.class);
        assertThat(s.getRef()).isEqualTo("#/components/schemas/Color");
        assertThat(registry).containsKey("Color");

        Schema enumSchema = registry.get("Color");
        assertThat(enumSchema.getType()).isEqualTo("string");
        assertThat(enumSchema.getEnumValues()).containsExactly("RED", "GREEN", "BLUE");
    }

    @SuppressWarnings("unused")
    static class TypeHolder {
        List<String> stringList;
        Set<Integer> intSet;
        Map<String, Double> stringDoubleMap;
        Optional<String> optionalString;
        String[] stringArray;
    }

    private Type fieldType(String name) {
        try {
            Field f = TypeHolder.class.getDeclaredField(name);
            return f.getGenericType();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void resolvesListOfString() {
        Schema s = resolver.resolve(fieldType("stringList"));
        assertThat(s.getType()).isEqualTo("array");
        assertThat(s.getItems().getType()).isEqualTo("string");
    }

    @Test
    void resolvesSetOfInteger() {
        Schema s = resolver.resolve(fieldType("intSet"));
        assertThat(s.getType()).isEqualTo("array");
        assertThat(s.getItems().getType()).isEqualTo("integer");
    }

    @Test
    void resolvesMapOfStringDouble() {
        Schema s = resolver.resolve(fieldType("stringDoubleMap"));
        assertThat(s.getType()).isEqualTo("object");
        assertThat(s.getAdditionalProperties()).isNotNull();
        assertThat(s.getAdditionalProperties().getType()).isEqualTo("number");
    }

    @Test
    void resolvesOptionalUnwrapped() {
        Schema s = resolver.resolve(fieldType("optionalString"));
        assertThat(s.getType()).isEqualTo("string");
    }

    @Test
    void resolvesStringArray() {
        Schema s = resolver.resolve(fieldType("stringArray"));
        assertThat(s.getType()).isEqualTo("array");
        assertThat(s.getItems().getType()).isEqualTo("string");
    }

    @SuppressWarnings("unused")
    static class SimpleDto {
        Long id;
        String name;
    }

    @Test
    void resolvesDtoToRefAndRegistersSchema() {
        Schema s = resolver.resolve(SimpleDto.class);
        assertThat(s.getRef()).isEqualTo("#/components/schemas/SimpleDto");
        assertThat(registry).containsKey("SimpleDto");

        Schema dto = registry.get("SimpleDto");
        assertThat(dto.getType()).isEqualTo("object");
        assertThat(dto.getProperties()).containsKeys("id", "name");
    }

    @SuppressWarnings("unused")
    static class ParentDto {
        String value;
        ChildDto child;
    }

    @SuppressWarnings("unused")
    static class ChildDto {
        int score;
    }

    @Test
    void resolvesNestedDto() {
        resolver.resolve(ParentDto.class);
        assertThat(registry).containsKeys("ParentDto", "ChildDto");

        Schema parent = registry.get("ParentDto");
        Schema childProp = parent.getProperties().get("child");
        assertThat(childProp.getRef()).isEqualTo("#/components/schemas/ChildDto");
    }

    @SuppressWarnings("unused")
    static class NodeA {
        NodeB b;
    }

    @SuppressWarnings("unused")
    static class NodeB {
        NodeA a;
    }

    @Test
    void handlesCyclicReferencesWithoutStackOverflow() {
        Schema s = resolver.resolve(NodeA.class);
        assertThat(s.getRef()).isEqualTo("#/components/schemas/NodeA");
        assertThat(registry).containsKeys("NodeA", "NodeB");
    }
}

