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
├── test/                                # Jest tests (mirrors src/ structure)
│   ├── javascript/                      # JS/TS tests
│   │   ├── recipes/                     # Recipe tests
│   │   ├── fixtures/                    # Test npm projects
│   │   ├── parser/, format/, cleanup/   # Category tests
│   │   └── search/, templating/, migrate/
│   ├── java/                            # Java model tests
│   ├── json/, yaml/                     # JSON/YAML tests
│   └── rpc/                             # RPC integration tests
├── tsconfig.json
├── jest.config.js
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
3. Run with `--detectOpenHandles` to find unclosed promises: `npm run testhelper -- --detectOpenHandles`
4. Check `src/rpc/queue.ts` for deadlock in read/write operations

### Type Checking
Run `npm run typecheck` frequently to catch type mismatches early.
