# Doc-Gen: REST API Documentation Generator

[![Java Version](https://img.shields.io/badge/java-21+-blue)]()

Doc-Gen is a powerful tool that automatically generates interactive REST API documentation from Spring Web MVC controllers. It produces both machine-readable OpenAPI 3.0.3 specifications and interactive HTML documentation.

## Features

**Core Features:**
- Automatic Spring controller scanning from JAR files
- OpenAPI 3.0.3 specification generation
- Interactive HTML documentation UI
- JavaDoc extraction for operation and parameter descriptions
- Validation annotation support for required fields detection
- Support for all Spring MVC annotations (path variables, query params, headers, request bodies)
- Complex type resolution (generics, enums, nested DTOs)
- Clean, responsive HTML interface
- JSON output for integration with other tools

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+

This creates a fat JAR at: `doc-gen-cli/target/doc-gen-cli-1.0.0-SNAPSHOT-fat.jar`

### Basic Usage

```bash
java -jar doc-gen-cli-1.0.0-SNAPSHOT-fat.jar your-api.jar
```

This generates:
- `docs-output/api-docs.json` - OpenAPI 3.0.3 specification
- `docs-output/index.html` - Interactive documentation

### Advanced Options

```bash
java -jar doc-gen-cli-1.0.0-SNAPSHOT-fat.jar your-api.jar \
  -t "My API" \
  -v "2.0.0" \
  -s "https://api.example.com" \
  --description "Production API" \
  -o /custom/output/path \
  --with-javadoc
```

## Future Enhancements

Planned features:
- Multiple HTML themes (light/dark mode)
- Security scheme support (OAuth2, API Key, JWT)
- Export to Swagger UI
- Search and filter functionality in HTML
- Mobile-responsive UI improvements
- Plugin system for custom generators
- Gradle plugin
- Spring Security annotation support
- Support for request/response examples
