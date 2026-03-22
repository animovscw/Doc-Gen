package io.docgen.core;

import io.docgen.core.analyzer.ApiSpecBuilder;
import io.docgen.core.model.ApiSpec;
import io.docgen.core.serializer.JsonSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiSpecBuilderTest {

    @Test
    void buildsCompleteApiSpec() {
        ApiSpec spec = new ApiSpecBuilder()
                .title("Test API")
                .version("1.0.0")
                .description("Test API Description")
                .serverUrl("http://localhost:8080")
                .build(List.of());

        assertThat(spec).isNotNull();
        assertThat(spec.getInfo()).isNotNull();
        assertThat(spec.getInfo().getTitle()).isEqualTo("Test API");
        assertThat(spec.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(spec.getInfo().getDescription()).isEqualTo("Test API Description");
        assertThat(spec.getServers()).isNotNull().hasSize(1);
        assertThat(spec.getServers().get(0).getUrl()).isEqualTo("http://localhost:8080");
    }

    @Test
    void serializesApiSpec() {
        ApiSpec spec = new ApiSpecBuilder()
                .title("Test API")
                .version("1.0.0")
                .build(List.of());

        JsonSerializer serializer = new JsonSerializer();
        String json = serializer.serialize(spec);

        assertThat(json).isNotNull().isNotEmpty();
        assertThat(json).contains("\"openapi\"");
        assertThat(json).contains("\"title\"");
        assertThat(json).contains("Test API");
        assertThat(json).contains("\"version\"");
        assertThat(json).contains("1.0.0");
    }
}
