package io.docgen.core.analyzer;

import jakarta.validation.constraints.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

public class RequiredFieldDetector {

    private static final Set<String> REQUIRED_ANNOTATIONS = Set.of(
            "NotNull", "NotEmpty", "NotBlank",
            "Min", "Max", "Size",
            "Pattern", "Email",
            "Positive", "PositiveOrZero",
            "Negative", "NegativeOrZero",
            "DecimalMin", "DecimalMax",
            "Digits", "Length"
    );

    public static List<String> detectRequiredFields(Class<?> dtoClass) {
        List<String> required = new ArrayList<>();
        List<Field> allFields = getAllFields(dtoClass);

        for (Field field : allFields) {
            if (hasRequiredAnnotation(field)) {
                required.add(field.getName());
            }
        }

        return required;
    }

    public static boolean hasRequiredAnnotation(Field field) {
        for (Annotation annotation : field.getAnnotations()) {
            String annotationName = annotation.annotationType().getSimpleName();
            if (REQUIRED_ANNOTATIONS.contains(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private static List<Field> getAllFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }
}

