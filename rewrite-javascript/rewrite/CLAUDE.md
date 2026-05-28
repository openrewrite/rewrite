# CLAUDE.md

This file provides guidance to Claude Code when working with the OpenRewrite TypeScript implementation.

## Module Overview

TypeScript implementation of OpenRewrite for JavaScript/TypeScript source code transformations, plus JSON and YAML support. Includes parsers, AST models, visitors, printers, and an RPC bridge for Java communication.

Self-contained Node.js project, separate from the Java monorepo build system.

## Project Setup

From `rewrite-javascript/rewrite/`:
```bash
npm install
```

Via Gradle (from repo root):
```bash
./gradlew :rewrite-javascript:npmInstall
./gradlew :rewrite-javascript:npm_test
./gradlew :rewrite-javascript:npm_run_build
```

Requires Node.js 18+.

## Running Tests

```bash
# Full suite (typecheck + build + test)
npm test

# Fast iteration (skip typecheck)
npm run testhelper

# Type-checking only
npm run typecheck

# Individual test file
npm run testhelper -- test/javascript/recipes/order-imports.test.ts
```

Available npm scripts: `prebuild`, `build`, `postbuild`, `typecheck`, `dev`, `test`, `testhelper`, `build:fixtures`, `ci:test`, `start`.

### Java RPC Integration Tests

Tests under `test/rpc/` that exercise real Java recipes spawn `org.openrewrite.maven.rpc.JavaRewriteRpc` via `JavaRpcTestServer` (see `src/rpc/java-rpc-client.ts`). They need a classpath file generated from the Java side:

```bash
# From repo root
./gradlew :rewrite-javascript:generateTestClasspath
```

This writes `rewrite-javascript/rewrite/test-classpath.txt` (gitignored). Alternatively set `REWRITE_JAVASCRIPT_CLASSPATH` to override. Tests in `test/rpc/java-recipe-via-rpc.test.ts` skip cleanly with a one-line warning when neither is configured.

## Directory Structure

```
rewrite-javascript/rewrite/
├── src/
│   ├── index.ts                         # Main entry point / re-exports
│   ├── tree.ts, visitor.ts, recipe.ts   # Core framework
│   ├── markers.ts, execution.ts         # Metadata, execution context
│   ├── print.ts, parser.ts              # Base printer, base parser
│   ├── util.ts, uuid.ts                 # Utilities
│   ├── java/                            # Java LST model
│   │   ├── tree.ts                      # J namespace (Java AST)
│   │   ├── visitor.ts                   # JavaVisitor
│   │   ├── print.ts                     # Java-to-source printer
│   │   ├── rpc.ts                       # RPC sender/receiver for Java
│   │   └── type.ts, type-visitor.ts     # Java type system
│   ├── javascript/                      # JavaScript/TypeScript
│   │   ├── tree.ts                      # JS namespace (JavaScript AST)
│   │   ├── visitor.ts                   # JavaScriptVisitor
│   │   ├── print.ts                     # JS-to-source printer
│   │   ├── parser.ts                    # JS/TS parser
│   │   ├── rpc.ts                       # RPC sender/receiver for JS
│   │   ├── assertions.ts               # Test helpers: typescript(), javascript(), jsx(), tsx(), packageJson()
│   │   ├── add-import.ts, remove-import.ts  # Import manipulation
│   │   ├── recipes/                     # Built-in recipes (order-imports, change-import, add-dependency, etc.)
│   │   ├── format/                      # Formatting visitors
│   │   ├── cleanup/                     # Cleanup recipes (add-parse-int-radix, prefer-optional-chain, etc.)
│   │   ├── migrate/                     # Migration recipes (es6/, typescript/)
│   │   ├── search/                      # Search patterns
│   │   └── templating/                  # Template engine
│   ├── json/                            # JSON support (tree, visitor, print, rpc, recipes)
│   ├── yaml/                            # YAML support (tree, visitor, print, rpc, recipes)
│   ├── search/                          # Cross-language search utilities
│   ├── text/                            # Plain text support
│   ├── rpc/                             # RPC infrastructure
│   │   ├── queue.ts                     # Message queue
│   │   ├── rewrite-rpc.ts              # Core RPC protocol
│   │   ├── server.ts                    # RPC server
│   │   ├── recipe.ts                    # Recipe RPC bridge
│   │   ├── trace.ts                     # RPC tracing/debugging
│   │   └── request/                     # Request types (parse, visit, get-object, etc.)
│   └── test/                            # Testing infrastructure
│       └── rewrite-test.ts              # RecipeSpec class, rewriteRun()
├── test/                                # Vitest tests (mirrors src/ structure)
│   ├── javascript/                      # JS/TS tests
│   │   ├── recipes/                     # Recipe tests
│   │   ├── fixtures/                    # Test npm projects
│   │   ├── parser/, format/, cleanup/   # Category tests
│   │   └── search/, templating/, migrate/
│   ├── java/                            # Java model tests
│   ├── json/, yaml/                     # JSON/YAML tests
│   └── rpc/                             # RPC integration tests
├── tsconfig.json
├── vitest.config.mts
└── package.json                         # name: @openrewrite/rewrite
```

## Development Patterns

### Async Visitor Pattern

**All visitor methods are async.** This supports the RPC nature of the framework.

```typescript
export class MyVisitor extends JavaScriptVisitor<ExecutionContext> {
    protected async visitJsCompilationUnit(
        cu: JS.CompilationUnit,
        p: ExecutionContext
    ): Promise<JS.CompilationUnit> {
        // Transform and return
        return cu;
    }
}
```

Hierarchy: `TreeVisitor` → `JavaVisitor` → `JavaScriptVisitor`

### Immutability and Structural Sharing

LST nodes are immutable. Use `updateIfChanged()` for single-field updates:

```typescript
const updated = updateIfChanged(node, 'field', newValue);
```

For multiple updates, use `produceAsync()` (Mutative-based draft mutations):
```typescript
import { produceAsync } from '../../visitor';

const updated = await produceAsync(cu, async draft => {
    draft.statements = newStatements;
});
```

Note: The project uses [Mutative](https://github.com/unadlib/mutative) (not Immer) for immutable state management.

### RPC Kind Constants

LST nodes have a `kind` property that must exactly match Java FQN strings:

```typescript
// These must stay in sync with Java — any mismatch breaks RPC serialization
CompilationUnit: "org.openrewrite.javascript.tree.JS$CompilationUnit"
```

## Recipe Pattern

```typescript
import { Recipe, ExecutionContext, TreeVisitor } from '../../';
import { JavaScriptVisitor } from '../visitor';
import { JS } from '../tree';

export class MyRecipe extends Recipe {
    readonly name = 'org.openrewrite.javascript.MyRecipe';
    readonly displayName = 'My Recipe';
    readonly description = 'Describes what this recipe does';

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitJsCompilationUnit(cu: JS.CompilationUnit, p: ExecutionContext) {
                return cu;
            }
        };
    }
}
```

## Test Pattern

Tests use relative imports. Source spec factories (`typescript()`, `javascript()`, `jsx()`, `tsx()`, `packageJson()`) are in `src/javascript/assertions.ts`.

```typescript
import { RecipeSpec } from "../../../src/test";
import { typescript } from "../../../src/javascript";
import { OrderImports } from "../../../src/javascript/recipes/order-imports";

describe('OrderImports', () => {
    test('sorts imports', () =>
        new RecipeSpec({ recipe: new OrderImports() }).rewriteRun(
            typescript(
                `import {z} from 'zebra';\nimport {a} from 'alpha';`,
                `import {a} from 'alpha';\nimport {z} from 'zebra';`
            )
        )
    );
});
```

## RPC Sender/Receiver

Each language module has `rpc.ts` with a Sender (visit tree → serialize to queue) and Receiver (read queue → reconstruct tree). These must stay aligned with each other AND with the Java equivalents. Any mismatch causes deadlocks or corrupted trees.

## Debugging Tips

### RPC Hangs
1. Check that both Java and TypeScript RPC methods are implemented
2. Verify Kind constants match between Java and TypeScript
3. Use `vitest run --reporter=hanging-process` or `--test-timeout` to detect hanging tests
4. Check `src/rpc/queue.ts` for deadlock in read/write operations

### Type Checking
Run `npm run typecheck` frequently to catch type mismatches early.

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