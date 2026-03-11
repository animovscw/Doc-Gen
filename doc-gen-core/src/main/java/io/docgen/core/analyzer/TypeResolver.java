package io.docgen.core.analyzer;

import io.docgen.core.model.Schema;

import java.lang.reflect.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;

public class TypeResolver {

    private static final Set<String> WRAPPER_CONTAINERS = Set.of(
            "Optional", "ResponseEntity", "CompletableFuture", "Callable", "DeferredResult"
    );

    private final Map<String, Schema> schemaRegistry;
    private final Set<String> inProgress = new HashSet<>();

    public TypeResolver(Map<String, Schema> schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public Schema resolve(Type type) {
        return doResolve(type);
    }

    private Schema doResolve(Type type) {
        if (type == null) return null;

        if (type instanceof ParameterizedType pt) {
            return resolveParameterized(pt);
        }

        if (type instanceof GenericArrayType gat) {
            Schema itemSchema = doResolve(gat.getGenericComponentType());
            return Schema.ofArray(itemSchema != null ? itemSchema : Schema.ofType("object"));
        }

        if (type instanceof WildcardType wt) {
            Type[] upper = wt.getUpperBounds();
            return (upper != null && upper.length > 0) ? doResolve(upper[0]) : Schema.ofType("object");
        }

        if (type instanceof TypeVariable<?>) {
            return Schema.ofType("object");
        }

        if (type instanceof Class<?> cls) {
            return resolveClass(cls);
        }

        return Schema.ofType("object");
    }

    private Schema resolveParameterized(ParameterizedType pt) {
        Class<?> raw = (Class<?>) pt.getRawType();
        Type[] args = pt.getActualTypeArguments();

        if (WRAPPER_CONTAINERS.contains(raw.getSimpleName()) && args.length == 1) {
            return doResolve(args[0]);
        }

        if (List.class.isAssignableFrom(raw)
                || Set.class.isAssignableFrom(raw)
                || Collection.class.isAssignableFrom(raw)) {
            Schema itemSchema = args.length > 0 ? doResolve(args[0]) : Schema.ofType("object");
            return Schema.ofArray(itemSchema != null ? itemSchema : Schema.ofType("object"));
        }

        if (Map.class.isAssignableFrom(raw)) {
            Schema valueSchema = args.length > 1 ? doResolve(args[1]) : Schema.ofType("object");
            return Schema.ofMap(valueSchema != null ? valueSchema : Schema.ofType("object"));
        }

        return resolveClass(raw);
    }

    private Schema resolveClass(Class<?> cls) {
        if (cls == void.class || cls == Void.class) return null;

        Schema prim = resolvePrimitive(cls);
        if (prim != null) return prim;

        if (cls.isArray()) {
            Schema itemSchema = resolveClass(cls.getComponentType());
            return Schema.ofArray(itemSchema != null ? itemSchema : Schema.ofType("object"));
        }

        if (cls.isEnum()) {
            return resolveEnum(cls);
        }

        if (Collection.class.isAssignableFrom(cls)) {
            return Schema.ofArray(Schema.ofType("object"));
        }
        if (Map.class.isAssignableFrom(cls)) {
            return Schema.ofMap(Schema.ofType("object"));
        }

        return resolveDto(cls);
    }

    private Schema resolvePrimitive(Class<?> cls) {
        if (cls == String.class || cls == CharSequence.class || cls == char.class || cls == Character.class) {
            return Schema.ofType("string");
        }
        if (cls == int.class || cls == Integer.class) return Schema.ofType("integer", "int32");
        if (cls == long.class || cls == Long.class) return Schema.ofType("integer", "int64");
        if (cls == short.class || cls == Short.class) return Schema.ofType("integer", "int32");
        if (cls == byte.class || cls == Byte.class) return Schema.ofType("integer", "int32");
        if (cls == float.class || cls == Float.class) return Schema.ofType("number", "float");
        if (cls == double.class || cls == Double.class) return Schema.ofType("number", "double");
        if (cls == boolean.class || cls == Boolean.class) return Schema.ofType("boolean");
        if (cls == java.math.BigDecimal.class) return Schema.ofType("number");
        if (cls == java.math.BigInteger.class) return Schema.ofType("integer");

        if (cls == LocalDate.class || cls == java.sql.Date.class) return Schema.ofType("string", "date");
        if (cls == LocalDateTime.class || cls == OffsetDateTime.class
                || cls == ZonedDateTime.class || cls == java.util.Date.class
                || cls == java.sql.Timestamp.class) {
            return Schema.ofType("string", "date-time");
        }

        if (cls == java.util.UUID.class) return Schema.ofType("string", "uuid");
        if (cls == java.net.URI.class || cls == java.net.URL.class) return Schema.ofType("string", "uri");
        if (cls == Object.class) return Schema.ofType("object");
        return null;
    }

    private Schema resolveEnum(Class<?> cls) {
        String name = cls.getSimpleName();

        if (schemaRegistry.containsKey(name)) {
            return Schema.ofRef("#/components/schemas/" + name);
        }

        List<String> values = new ArrayList<>();
        for (Object constant : cls.getEnumConstants()) {
            values.add(constant.toString());
        }
        Schema enumSchema = Schema.ofEnum(values);
        schemaRegistry.put(name, enumSchema);
        return Schema.ofRef("#/components/schemas/" + name);
    }

    private Schema resolveDto(Class<?> cls) {
        String name = cls.getSimpleName();

        if (schemaRegistry.containsKey(name)) {
            return Schema.ofRef("#/components/schemas/" + name);
        }

        if (inProgress.contains(name)) {
            return Schema.ofRef("#/components/schemas/" + name);
        }
        inProgress.add(name);

        Schema dto = new Schema();
        dto.setType("object");
        schemaRegistry.put(name, dto);

        Map<String, Schema> props = new LinkedHashMap<>();
        for (Field field : getAllFields(cls)) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;

            Schema fieldSchema = doResolve(field.getGenericType());
            if (fieldSchema != null) {
                props.put(field.getName(), fieldSchema);
            }
        }

        if (!props.isEmpty()) {
            dto.setProperties(props);
        }

        inProgress.remove(name);
        return Schema.ofRef("#/components/schemas/" + name);
    }

    private List<Field> getAllFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }
}

