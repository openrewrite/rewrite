# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Important Instructions

When you need to know something about OpenRewrite:
- Refer to the rewrite-docs folder (if available)
- Consult Architecture Decision Records in `docs/adr/` for design decisions

## Project Overview

OpenRewrite is an automated refactoring ecosystem for source code that eliminates technical debt through AST-based transformations. The project uses a visitor pattern architecture where **Recipes** define transformations and **TreeVisitors** traverse and modify Abstract Syntax Trees (ASTs).

### Core Architecture

- **Recipe**: Defines what transformation to perform (`org.openrewrite.Recipe`)
- **LST (Lossless Semantic Tree)**: AST preserving all formatting, comments, and type attribution during transformations
- **TreeVisitor**: Implements how to traverse and modify LSTs (`org.openrewrite.TreeVisitor`)  
- **SourceFile**: Represents parsed source code as an LST (`org.openrewrite.SourceFile`)
- **Markers**: Attach metadata to LST nodes without modifying the tree structure

## Essential Build Commands

```bash
# Build everything (compile + test)
./gradlew build

# Quick compilation check without tests
./gradlew assemble

# Run tests only
./gradlew test

# Run all quality checks (tests, license, etc.)
./gradlew check

# Clean and rebuild
./gradlew clean build

# Install to local Maven repository
./gradlew publishToMavenLocal

# Apply license headers to files
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

### JavaScript Module (rewrite-javascript)
```bash
# Install Node.js dependencies
./gradlew :rewrite-javascript:npmInstall

# Run JavaScript tests
./gradlew :rewrite-javascript:npm_test

# Build JavaScript components
./gradlew :rewrite-javascript:npm_run_build
```

## Project Structure

### Core Modules
- **`rewrite-core`**: AST framework, visitor pattern, recipe infrastructure
- **`rewrite-test`**: Testing utilities (`RewriteTest`, `RecipeSpec`)

### Language Parsers
- **`rewrite-java`**: Main Java language support with comprehensive AST model
- **`rewrite-java-8/11/17/21`**: Java version-specific features and compatibility
- **`rewrite-groovy/kotlin/javascript`**: Other JVM and web languages extending the `J` model from `rewrite-java`

### Format Parsers  
- **`rewrite-maven`**: Maven POM manipulation and dependency management
- **`rewrite-gradle`**: Gradle build script parsing (Groovy/Kotlin DSL)
- **`rewrite-json/yaml/xml/hcl/properties/toml/protobuf`**: Configuration and data formats

### Supporting Modules
- **`rewrite-java-tck`**: Technology Compatibility Kit for Java parser validation
- **`rewrite-java-test`**: Java-specific testing utilities and infrastructure
- **`rewrite-java-lombok`**: Lombok-specific Java support
- **`rewrite-benchmarks`**: JMH performance benchmarks
- **`tools/language-parser-builder`**: Template tool for generating new language parsers

## Architecture Decision Records (ADRs)

Significant architectural decisions are documented in `docs/adr/`. When working on features that involve architectural decisions, consult existing ADRs and create new ones as needed following the standard ADR format (Context, Decision, Consequences).

## Development Patterns

### Recipe Development
Recipes extend `org.openrewrite.Recipe` and typically contain one or more `TreeVisitor` implementations. The visitor pattern allows type-safe traversal of language-specific ASTs.

### AST Traversal
- Use `TreeVisitor<SourceFile, ExecutionContext>` for cross-cutting concerns
- Use language-specific visitors (e.g., `JavaIsoVisitor`) for language transformations
- Always return modified trees; don't mutate in place
- Return `null` from a visitor method to delete an element
- **IMPORTANT**: Never typecast LST elements without an `instanceof` check first. The typical pattern when an LST element is not of the expected type is to return early, making no changes. For example:
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

### Parser Extensions
New language support for grammars that don't have type attribution require:
1. ANTLR grammar files (`.g4`) in `src/main/antlr/`
2. AST model classes extending `Tree`
3. Parser implementation extending `Parser`
4. Visitor base classes extending `TreeVisitor`

### IDE Configuration
The project supports selective module loading via `IDE.properties` to improve IDE performance. This allows developers to work on specific modules without loading the entire multi-project build.

### Quality Checks
The build enforces license headers on all source files and runs OWASP dependency vulnerability scanning. Always run `./gradlew licenseFormat` before committing code changes.

## Important Conventions

### Nullability Annotations
The project uses JSpecify nullability annotations.
