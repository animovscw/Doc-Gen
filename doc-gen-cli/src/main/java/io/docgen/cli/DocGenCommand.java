package io.docgen.cli;

import io.docgen.core.analyzer.ApiSpecBuilder;
import io.docgen.core.analyzer.JarClassLoader;
import io.docgen.core.generator.HtmlGenerator;
import io.docgen.core.model.ApiSpec;
import io.docgen.core.serializer.JsonSerializer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "doc-gen",
        mixinStandardHelpOptions = true,
        version = "Doc-Gen 1.0.0",
        description = "Generates interactive REST API documentation from a Spring Web MVC JAR."
)
public class DocGenCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the JAR file containing Spring controllers.")
    private File inputJar;

    @Option(names = {"-o", "--output"}, description = "Output directory for generated files.", defaultValue = "docs-output")
    private File outputDir;

    @Option(names = {"-t", "--title"}, description = "API title.", defaultValue = "API Documentation")
    private String title;

    @Option(names = {"-v", "--api-version"}, description = "API version.", defaultValue = "1.0.0")
    private String apiVersion;

    @Option(names = {"-s", "--server"}, description = "Base server URL (e.g. http://localhost:8080).")
    private String serverUrl;

    @Option(names = {"--description"}, description = "API description.")
    private String description;

    @Override
    public Integer call() throws Exception {
        if (!inputJar.exists() || !inputJar.isFile()) {
            System.err.println("ERROR: JAR file not found: " + inputJar.getAbsolutePath());
            return 1;
        }

        System.out.println("Doc-Gen: Scanning " + inputJar.getName() + " ...");

        List<Class<?>> classes;
        try (JarClassLoader loader = new JarClassLoader(inputJar)) {
            classes = loader.loadAllClasses();
        }
        System.out.println("  Found " + classes.size() + " classes.");

        ApiSpecBuilder builder = new ApiSpecBuilder()
                .title(title)
                .version(apiVersion);
        if (serverUrl != null) builder.serverUrl(serverUrl);
        if (description != null) builder.description(description);

        ApiSpec spec = builder.build(classes);

        int endpointCount = spec.getPaths() != null ? spec.getPaths().size() : 0;
        int schemaCount = (spec.getComponents() != null && spec.getComponents().getSchemas() != null)
                ? spec.getComponents().getSchemas().size() : 0;
        System.out.println("  Discovered " + endpointCount + " path(s), " + schemaCount + " schema(s).");

        Path outPath = outputDir.toPath();
        Files.createDirectories(outPath);

        JsonSerializer serializer = new JsonSerializer();
        String json = serializer.serialize(spec);
        Files.writeString(outPath.resolve("api-docs.json"), json);
        System.out.println("  Written: " + outPath.resolve("api-docs.json"));

        HtmlGenerator htmlGenerator = new HtmlGenerator();
        String html = htmlGenerator.generate(spec);
        Files.writeString(outPath.resolve("index.html"), html);
        System.out.println("  Written: " + outPath.resolve("index.html"));

        System.out.println("Done!");
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new DocGenCommand()).execute(args);
        System.exit(exitCode);
    }
}

