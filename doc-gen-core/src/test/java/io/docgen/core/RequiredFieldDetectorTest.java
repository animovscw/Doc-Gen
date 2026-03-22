package io.docgen.core;

import io.docgen.core.analyzer.RequiredFieldDetector;
import jakarta.validation.constraints.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequiredFieldDetectorTest {

    static class TestDtoWithValidation {
        @NotNull
        public String id;

        @NotBlank
        public String name;

        @Email
        public String email;

        public String optionalField;
    }

    @Test
    void detectsRequiredFieldsFromValidationAnnotations() {
        List<String> required = RequiredFieldDetector.detectRequiredFields(TestDtoWithValidation.class);

        assertThat(required)
                .isNotNull()
                .contains("id", "name", "email")
                .doesNotContain("optionalField")
                .hasSize(3);
    }

    @Test
    void returnsEmptyListForClassWithoutValidation() {
        List<String> required = RequiredFieldDetector.detectRequiredFields(String.class);
        assertThat(required).isEmpty();
    }

    static class ComplexDto {
        @NotNull
        @Size(min = 1, max = 100)
        public String username;

        @DecimalMin("0.0")
        @DecimalMax("100.0")
        public Double score;

        public String description;
    }

    @Test
    void detectsFieldsWithMultipleValidationAnnotations() {
        List<String> required = RequiredFieldDetector.detectRequiredFields(ComplexDto.class);
        assertThat(required).contains("username", "score");
    }
}

