# CLAUDE.md

This file provides guidance to Claude Code when working with the OpenRewrite C# implementation.

## Module Overview

C# implementation of OpenRewrite using Roslyn (`Microsoft.CodeAnalysis`) for parsing. Communicates with the Java runtime via a bidirectional JSON-RPC bridge (StreamJsonRpc) over stdin/stdout.

Self-contained .NET project, separate from the Java monorepo build system but orchestrated by Gradle.

## Project Setup

Requires .NET 10.0+ SDK (`net10.0` target framework).

From `rewrite-csharp/csharp/`:
```bash
dotnet build
```

Via Gradle (from repo root):
```bash
./gradlew :rewrite-csharp:csharpBuild
```

## Running Tests

```bash
# All C# xUnit tests
dotnet test --verbosity normal

# Specific test class
dotnet test --filter "FullyQualifiedName~OpenRewrite.Tests.Tree.AttributeListTests"

# Specific test method
dotnet test --filter "FullyQualifiedName~OpenRewrite.Tests.Tree.AttributeListTests.SimpleAttribute"

# Working-set tests (excluded by default, requires WORKING_SET_ROOT env var)
dotnet test --filter "Category=WorkingSet"
```

Via Gradle (from repo root):
```bash
# Run C# xUnit tests (outputs JUnit XML to csharp/build/test-results/xunit/junit.xml)
./gradlew :rewrite-csharp:csharpTest

# Run Java-side RPC integration tests
./gradlew :rewrite-csharp:test

# Include working-set tests
./gradlew :rewrite-csharp:test -PincludeWorkingSet
```

Java-side tests run single-threaded (`maxParallelForks = 1`) with a 30-second timeout per test to avoid RPC deadlocks.

## Directory Structure

```
rewrite-csharp/csharp/
├── OpenRewrite/                      # Main SDK library (OpenRewrite.CSharp NuGet package)
│   ├── Core/                         # AST framework
│   │   ├── TreeVisitor.cs            # Base visitor with cursor, pre/post hooks
│   │   ├── Recipe.cs                 # Abstract recipe class
│   │   ├── ExecutionContext.cs       # Context passed through visitors
│   │   ├── Space.cs                  # Whitespace/comment representation
│   │   ├── Markers.cs               # Metadata attached to LST nodes
│   │   └── Rpc/                      # RPC infrastructure (queues, serialization)
│   ├── CSharp/                       # C#-specific language support
│   │   ├── CSharpParser.cs           # Main parser (Roslyn CSharpSyntaxVisitor)
│   │   ├── CSharpVisitor.cs          # C# AST visitor (extends JavaVisitor)
│   │   ├── Cs.cs                     # C#-specific LST element definitions
│   │   ├── Linq.cs                   # LINQ query expression support
│   │   ├── SolutionParser.cs         # MSBuild .sln/.csproj parser
│   │   ├── Rpc/                      # RPC server, receiver, plugin loader
│   │   ├── Template/                 # Template engine & pattern matching
│   │   └── Format/                   # Roslyn formatter, whitespace reconciliation
│   ├── Java/                         # Java interop layer (J types, JavaVisitor)
│   │   ├── J.cs                      # Java LST element definitions
│   │   ├── JavaVisitor.cs            # Java visitor base class
│   │   └── Rpc/                      # JavaSender/JavaReceiver for RPC
│   ├── Test/                         # Test infrastructure
│   │   └── RewriteTest.cs            # Base class: RewriteRun(), CSharp() helper
│   └── Tests/                        # xUnit test suite
│       ├── Tree/                     # Parser round-trip tests
│       ├── Core/                     # Core infrastructure tests
│       ├── Java/                     # Java interop tests
│       └── CSharp/                   # C#-specific recipe tests
├── OpenRewrite.Tool/                 # RPC server CLI executable
│   └── Program.cs                    # Entry point (warmup parse, start RPC server)
└── OpenRewrite.sln                   # Visual Studio solution
```

## Development Patterns

### Sealed Records with Padding

All LST nodes are sealed records with immutable properties. Whitespace/formatting is stored in `JRightPadded<T>`, `JLeftPadded<T>`, and `JContainer<T>` wrappers.

- `JRightPadded<T>`: element T with `.After` space (whitespace before trailing delimiter)
- `JLeftPadded<T>`: `.Before` space + element T (used for operators like `=`)
- `JContainer<T>`: opening delimiter space, list of padded elements, closing delimiter

### Visitor Pattern

```csharp
public class MyVisitor : CSharpVisitor<ExecutionContext>
{
    public override J? VisitClassDeclaration(ClassDeclaration classDecl, ExecutionContext ctx)
    {
        // Transform and return
        return base.VisitClassDeclaration(classDecl, ctx);
    }
}
```

Hierarchy: `TreeVisitor<T, P>` → `JavaVisitor<P>` → `CSharpVisitor<P>`

Key visitor methods:
- `PreVisit()` — called before children; `StopAfterPreVisit()` skips descent
- `PostVisit()` — called after children
- `DoAfterVisit()` — registers one-off visitors (e.g., auto-formatters) after tree walk

### Test Pattern

Tests use xUnit with the `RewriteTest` base class. `RewriteRun()` parses the input, prints it back, and asserts print idempotency. When an `after` string is provided, it applies the recipe and asserts the result matches.

```csharp
public class MyTests : RewriteTest
{
    [Fact]
    public void SimpleRoundTrip()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void WithRecipe()
    {
        RewriteRun(
            spec => spec.Recipe = new MyRecipe(),
            CSharp(
                """
                class Foo { }
                """,
                """
                class Bar { }
                """
            )
        );
    }
}
```

### Type Naming Conventions

- C#-specific LST types are defined in `Cs.cs` (e.g., `Cs.Lambda`, `Cs.Binary`, `Cs.UsingDirective`)
- Shared Java AST types live in `J.cs` (e.g., `J.ClassDeclaration`, `J.MethodDeclaration`)
- LINQ types live in `Linq.cs` (e.g., `Linq.Query`, `Linq.Select`)

## RPC Architecture

Bidirectional JSON-RPC over stdin/stdout using StreamJsonRpc. The `OpenRewrite.Tool` runs as a .NET process spawned by the Java side.

**Core RPC methods** (C# side):
- `ParseSolution(request)` — loads .sln, parses projects, returns SourceFile objects
- `Visit(request)` — runs a visitor on a cached tree
- `GetObject(objectId)` — Java requests a serialized object from C# cache
- `Reset()` — clears all cached state

**Type mapping**: `RpcSendQueue.RegisterJavaTypeName()` maps C# types to Java fully-qualified names for serialization (e.g., `Cs.Lambda` → `org.openrewrite.csharp.tree.Cs$Lambda`).

### Debugging RPC Issues

- RPC tests can hang indefinitely if communication fails. Always use timeouts.
- Non-concurrent synchronization context prevents deadlocks on reentrant requests.
- Check that Sender/Receiver methods stay aligned between C# and Java sides.
- The RPC server logs to a file when `--log-file=` is passed to the tool.

## License Headers

Run from repo root before committing:
```bash
./gradlew licenseFormatCsharp
```

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