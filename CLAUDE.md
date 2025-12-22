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

## General Guidelines

### Code Organization
- Code should be organized into modules based on functionality
- Each module should have a clear responsibility
- Follow the package structure conventions established in the project
- Keep files focused on a single responsibility

### Documentation
- Public APIs should be documented with a brief summary at the type level and method level documentation for interfaces
- Include examples in documentation where appropriate
- Document complex algorithms and non-obvious code
- Keep documentation up-to-date with code changes

### Testing
- Write tests for all new features and bug fixes
- Aim for high test coverage
- Tests should be comprehensive and cover edge cases
- Use appropriate testing frameworks (JUnit for Java, Jest for TypeScript)

### Error Handling
- Handle errors appropriately and provide meaningful error messages
- Use exceptions for exceptional conditions, not for control flow
- Validate inputs and fail fast
- Log errors with appropriate context

### Task Management
- Always check tasks in the task list when they are completed
- Update task lists in `docs/tasks.md` to reflect current progress
- Mark tasks as completed by changing `- [ ]` to `- [x]`
- Keep task lists up-to-date to help track project progress
- Ensure all subtasks are checked before marking a parent task as complete

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

When testing OpenRewrite parsers or recipes use `RewriteTest` and its `rewriteRun()` methods with "before" and "after" state to indicate what changes are expected and not expected to be made to source files.

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

## Java Guidelines

### Code Style
- Follow standard Java naming conventions
  - Classes: PascalCase
  - Methods and variables: camelCase
  - Constants: UPPER_SNAKE_CASE
- Use 4 spaces for indentation
- Keep lines under 120 characters
- Use meaningful variable and method names

### Java Patterns
- Use Lombok annotations to reduce boilerplate (@Getter, @RequiredArgsConstructor, etc.)
- Use interfaces with default methods where appropriate
- Use @Nullable annotations for null safety
- Use functional programming style with streams and lambdas where it improves readability
- Prefer immutable objects where possible
- Use the Builder pattern for complex object creation
- Use the Visitor pattern for tree traversal and transformation

### Java Best Practices
- Favor composition over inheritance
- Use dependency injection for better testability
- Write small, focused methods
- Avoid mutable state where possible
- Use appropriate data structures for the task
- Follow the principle of least surprise

## TypeScript Guidelines

### Organization
- The Node project root for the TypeScript code is `rewrite-javascript/rewrite`. So when running `npm`, make sure to add `--prefix rewrite-javascript/rewrite` to the command or change into that directory before running the command.
- The TypeScript code represents an implementation of OpenRewrite Java in TypeScript
  - The modules directly inside `src`, `src/rpc`, `src/text` roughly correspond to the repo-level Gradle project `rewrite-core`
  - The modules in `src/java` correspond to the Java code in the Gradle project `rewrite-java`
  - The modules in `src/javascript` correspond to the Java code in the Gradle project `rewrite-javascript`
  - The modules in `src/json` correspond to the Java code in the Gradle project `rewrite-json`
  - The modules in `src/test` correspond to the Java code in the Gradle project `rewrite-test`
- Specifically, there are a lot of types which have the exact same names and structures in both the Java and the TypeScript code (e.g. `JavaVisitor` or `Markers`).
  - A lot of types (specifically those in `tree.ts` and `markers.ts` files) represent data types which need to have matching definitions in Java and TypeScript to support a custom serialization mechanism
  - The serialization mechanism is generally referred to as RPC and implemented in `src/rpc` (and inside the Java package `org.openrewrite.rpc` of `rewrite-core`)
  - Further, the serialization mechanism is visitor-based and thus for each of the supported languages there is a "sender" and a "receiver" (e.g. `JavaSender` and `JavaReceiver`) which each needs an implementation in both Java and TypeScript and at the same time this must be fully aligned with the corresponding model (e.g. `src/java/tree.ts`)

### Code Style
- Follow standard TypeScript naming conventions
  - Classes and interfaces: PascalCase
  - Methods, properties, and variables: camelCase
  - Constants: UPPER_SNAKE_CASE
- Use 4 spaces for indentation
- Keep lines under 120 characters
- Use meaningful variable and method names

### TypeScript Patterns
- Use TypeScript interfaces and classes for type safety
- Use generics for reusable code
- Use async/await for asynchronous operations (including the visitor which is async)
- Use Immer for immutable state management
- Use the visitor pattern for tree traversal and transformation
- Use optional chaining for null/undefined handling

### TypeScript Best Practices
- Explicitly type function parameters and return values
- Use readonly for immutable properties
- Use union types instead of inheritance where appropriate
- Avoid any type where possible
- Use type guards for runtime type checking
- Use async/await instead of raw promises

## Version Control Guidelines

### Commits
- Write clear, concise commit messages
- Each commit should represent a logical change
- Keep commits focused on a single task
- Reference issue numbers in commit messages where applicable

### Pull Requests
- Write a clear description of the changes
- Include tests for new features and bug fixes
- Ensure all tests pass before submitting
- Address review comments promptly

## Important Conventions

### Nullability Annotations
The project uses JSpecify nullability annotations.

## Conclusion

Following these guidelines will help maintain code quality and consistency across the OpenRewrite project. These guidelines are not exhaustive, and common sense should be applied when making decisions not covered here.