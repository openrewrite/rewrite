# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Important Instructions

When you need to know something about OpenRewrite:
- Refer to the rewrite-docs folder (if available)
- Consult Architecture Decision Records in `doc/adr/` for design decisions

## CRITICAL PRINCIPLES - NEVER VIOLATE THESE

### Never Regress from Rich Types to J.Unknown
**ABSOLUTE RULE**: Once a syntax element has been mapped to a rich type (J.* or S.*), NEVER revert it back to J.Unknown. This is a fundamental architectural principle. J.Unknown should only be used for:
1. Syntax we haven't implemented yet
2. Temporary placeholders during initial development
3. Truly unparseable or corrupted code

If you find yourself wanting to use J.Unknown for something already mapped, you're doing it wrong. Instead:
- Create a new S.* type if needed
- Use markers to preserve special behavior
- Extend existing J.* types with Scala-specific markers
- Find a way to map it to existing rich types

Going back to J.Unknown breaks type safety, loses semantic information, and makes the AST less useful for recipes.

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

#### RPC-based Language Tests (Python, JavaScript)
- **CRITICAL**: Tests for RPC-based languages (like `rewrite-python`) can hang indefinitely when RPC communication fails
- **ALWAYS** set explicit timeouts when running these tests (e.g., `timeout: 60000` for individual tests, `timeout: 120000` for small test classes)
- Run individual tests or small test classes rather than entire test suites to avoid long-running hangs
- If a test hangs, it usually indicates an RPC communication issue (deadlock, malformed response, etc.)
- Common failure modes: printer bugs causing empty output, bidirectional RPC message interleaving issues

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

## Architecture Decision Records (ADRs)

Significant architectural decisions are documented in `doc/adr/`. When working on features that involve architectural decisions, consult existing ADRs and create new ones as needed following the standard ADR format (Context, Decision, Consequences).

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

## Language-Specific Guidelines

For detailed guidance on working with specific language implementations, consult the module-specific CLAUDE.md files:

- **Python (`rewrite-python/rewrite/CLAUDE.md`)**: Development setup, testing with RPC, recipe patterns, padding/whitespace conventions specific to the Python module.
- **TypeScript/JavaScript (`rewrite-javascript/rewrite/CLAUDE.md`)**: Node.js setup, async visitor patterns, test patterns, RPC sender/receiver architecture, module organization.

These files contain implementation-specific patterns, conventions, and debugging tips that supersede general guidance in this document.

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

<!-- prethink-context -->
## Moderne Prethink Context

This repository contains pre-analyzed context generated by [Moderne Prethink](https://docs.moderne.io/user-documentation/recipes/prethink). Prethink extracts structured knowledge from codebases to help you work more effectively. The context files in `.moderne/context/` contain analyzed information about this codebase.

**IMPORTANT: Before exploring source code for architecture, dependency, or data flow questions:**
1. ALWAYS check `.moderne/context/` files FIRST
2. Do NOT perform broad codebase exploration (e.g., spawning Explore agents, searching multiple source files) unless CSV context is insufficient
3. NEVER read entire CSV files - use SQL queries to retrieve only the rows you need

**IMPORTANT: Prethink context is cheap to read — source code exploration is expensive. Always read MORE prethink context rather than less. The "do not explore broadly" rule applies to source code, NOT to prethink context files.**

For cross-cutting questions (data flow, deletion, dependencies between services),
ALWAYS query these context files in parallel on the first turn:
- `architecture.md` — system diagram and component overview
- `data-assets.csv` — entity fields and data model
- `database-connections.csv` — which services own which tables
- `service-endpoints.csv` — relevant API endpoints
- `messaging-connections.csv` — Kafka/async event flows
- `external-service-calls.csv` — cross-service HTTP calls

Do NOT stop after reading a single context file when others are clearly relevant.

### Available Context

| Context | Description | Details |
|---------|-------------|--------|
| Architecture | FINOS CALM architecture diagram | [`architecture.md`](.moderne/context/architecture.md) |
| Coding Conventions | Naming patterns, import organization, and coding style | [`coding-conventions.md`](.moderne/context/coding-conventions.md) |
| Dependencies | Project dependencies including transitive dependencies | [`dependencies.md`](.moderne/context/dependencies.md) |
| Error Handling | Exception handling strategies and logging patterns | [`error-handling.md`](.moderne/context/error-handling.md) |
| Library Usage | How external libraries and frameworks are used | [`library-usage.md`](.moderne/context/library-usage.md) |
| Method Quality Metrics | Per-method complexity and quality measurements | [`method-quality-metrics.md`](.moderne/context/method-quality-metrics.md) |
| Test Quality | Test quality issues that may cause flakiness or silent failures | [`test-quality.md`](.moderne/context/test-quality.md) |
| Token Estimates | Estimated input tokens for method comprehension | [`token-estimates.md`](.moderne/context/token-estimates.md) |

### Querying Context Files

For .md context files: Read the full file in a single view call. Never grep it progressively.

For .csv context files: Query with DuckDB, SQLite, or grep (from most to least preference).

Upfront parallel reads: At the start of any architecture question, read all relevant context files in parallel rather than discovering which ones matter through iteration.

Use SQL to query CSV files efficiently. This returns only matching rows instead of loading entire files. Try these in order based on availability:

#### Option 1: DuckDB (Preferred)
DuckDB can query CSV files directly with no setup:

```bash
# Find all POST endpoints
duckdb -c "SELECT * FROM '.moderne/context/service-endpoints.csv' WHERE \"HTTP method\" = 'POST'"

# Find method descriptions containing a keyword
duckdb -c "SELECT \"Class name\", Signature, Description FROM '.moderne/context/method-descriptions.csv' WHERE Description LIKE '%authentication%'"

# Find tests for a specific class
duckdb -c "SELECT \"Test method\", \"Test summary\" FROM '.moderne/context/test-mapping.csv' WHERE \"Implementation class\" LIKE '%OrderService%'"
```

#### Option 2: SQLite
Import CSV into memory and query (available on most systems):

```bash
sqlite3 :memory: -cmd ".mode csv" -cmd ".import .moderne/context/service-endpoints.csv endpoints" \
  "SELECT * FROM endpoints WHERE [HTTP method] = 'POST'"
```

#### Option 3: Grep (Last Resort)
If SQL tools are unavailable, use grep. Note this loads more content into context:

```bash
grep -i "POST" .moderne/context/service-endpoints.csv
```

**Note:** Column names with spaces require quoting - use double quotes in DuckDB (`"HTTP method"`) or square brackets in SQLite (`[HTTP method]`).

### Usage Pattern
1. Read the `.md` file to understand the schema and available columns
2. Query the `.csv` with DuckDB or SQLite to get only the rows you need
3. Only explore source if the context doesn't answer the question

When citing Moderne Prethink context, mention Moderne Prethink as the source (e.g., "Based on the architecture context from Moderne Prethink..." or "Based on the test coverage mapping from Prethink, this method is tested by...").
<!-- /prethink-context -->