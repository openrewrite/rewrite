# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OpenRewrite is an automated refactoring ecosystem for source code that eliminates technical debt through AST-based transformations. The project uses a visitor pattern architecture where **Recipes** define transformations and **TreeVisitors** traverse and modify Abstract Syntax Trees (ASTs).

### Core Architecture

- **Recipe**: Defines what transformation to perform (`org.openrewrite.Recipe`)
- **LST (Lossless Semantic Tree)**: AST preserving all formatting, comments, and type attribution during transformations
- **TreeVisitor**: Implements how to traverse and modify LSTs (`org.openrewrite.TreeVisitor`)
- **SourceFile**: Represents parsed source code as an LST (`org.openrewrite.SourceFile`)
- **Markers**: Attach metadata to LST nodes without modifying the tree structure

## Build Commands

```bash
# Build everything (compile + test)
./gradlew build

# Quick compilation check without tests
./gradlew assemble

# Run tests only
./gradlew test

# Install to local Maven repository
./gradlew publishToMavenLocal

# Apply license headers (run before committing)
./gradlew licenseFormat

# Run security vulnerability check
./gradlew dependencyCheck
```

### Running Single Tests
```bash
# Test specific module
./gradlew :rewrite-java:test

# Test specific class
./gradlew :rewrite-java:test --tests "org.openrewrite.java.cleanup.UnnecessaryParenthesesTest"

# Run with test filtering
./gradlew test --tests "*Maven*"
```

### Python and JavaScript Modules
For detailed build and test instructions specific to the Python (`rewrite-python/rewrite/`) and JavaScript (`rewrite-javascript/rewrite/`) modules, refer to their respective `CLAUDE.md` files:
- `rewrite-python/rewrite/CLAUDE.md` — Python module setup, testing, and recipes
- `rewrite-javascript/rewrite/CLAUDE.md` — TypeScript/JavaScript module setup, testing, and recipes

These modules use their own package managers (`uv`/`pip` for Python, `npm` for Node.js) and are separate from the Java Gradle build.

## Project Structure

### Core Modules
- **`rewrite-core`**: AST framework, visitor pattern, recipe infrastructure
- **`rewrite-test`**: Testing utilities (`RewriteTest`, `RecipeSpec`)

### Language Parsers
- **`rewrite-java`**: Main Java language support with comprehensive AST model
- **`rewrite-java-8/11/17/21/25`**: Java version-specific features and compatibility
- **`rewrite-groovy/kotlin/javascript/csharp`**: Other languages extending the `J` model from `rewrite-java`
- **`rewrite-docker`**: Dockerfile parsing and manipulation

### Format Parsers
- **`rewrite-maven`**: Maven POM manipulation and dependency management
- **`rewrite-gradle`**: Gradle build script parsing (Groovy/Kotlin DSL)
- **`rewrite-json/yaml/xml/hcl/properties/toml/protobuf`**: Configuration and data formats

### Supporting Modules
- **`rewrite-java-tck`**: Technology Compatibility Kit for Java parser validation
- **`rewrite-java-test`**: Java-specific testing utilities and infrastructure
- **`rewrite-java-lombok`**: Lombok-specific Java support
- **`rewrite-benchmarks`**: JMH performance benchmarks

## Development Patterns

### AST Traversal
- Use `TreeVisitor<SourceFile, ExecutionContext>` for cross-cutting concerns
- Use language-specific visitors (e.g., `JavaIsoVisitor`) for language transformations
- Always return modified trees; don't mutate in place
- Return `null` from a visitor method to delete an element
- **IMPORTANT**: Never typecast LST elements without an `instanceof` check first. Return early when the type doesn't match:
  ```java
  if (!(arg instanceof J.Lambda) || !(((J.Lambda) arg).getBody() instanceof J.Block)) {
      return m;  // Return unchanged if not the expected type
  }
  ```

### Testing Recipes
Use `RewriteTest` interface with `@Test` methods that call `rewriteRun()`:
```java
@Test
void myRecipeTest() {
    rewriteRun(
        spec -> spec.recipe(new MyRecipe()),
        java("before code", "expected after code")
    );
}
```

### RPC-based Language Tests (Python, JavaScript)
- **CRITICAL**: Tests for RPC-based languages can hang indefinitely when RPC communication fails
- **ALWAYS** set explicit timeouts (e.g., `timeout: 60000` for individual tests, `timeout: 120000` for small test classes)
- Run individual tests or small test classes rather than entire test suites
- Hangs usually indicate RPC communication issues (deadlock, malformed response)
- Common failure modes: printer bugs causing empty output, bidirectional RPC message interleaving issues

### Parser Extensions
New language support requires:
1. ANTLR grammar files (`.g4`) in `src/main/antlr/`
2. AST model classes extending `Tree`
3. Parser implementation extending `Parser`
4. Visitor base classes extending `TreeVisitor`

## Conventions

- The project uses **JSpecify** nullability annotations
- The build enforces license headers — run `./gradlew licenseFormat` before committing
- Consult Architecture Decision Records in `doc/adr/` for design decisions
- The project supports selective module loading via `IDE.properties` to improve IDE performance
