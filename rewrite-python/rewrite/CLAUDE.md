# CLAUDE.md

This file provides guidance to Claude Code when working with the OpenRewrite Python implementation.

## Module Overview

Pure-Python implementation of OpenRewrite for Python source code transformations. Communicates with the Java runtime via an RPC bridge for cross-language recipes and integrations.

Separate from the Java monorepo build system — has its own package management, testing, and development workflows.

## Project Setup

From `rewrite-python/rewrite/`:
```bash
uv pip install -e ".[dev]"
```

Requires Python 3.10+ (`pyproject.toml` specifies `>=3.10`).

## Running Tests

```bash
# All tests with timeout
pytest tests/ -v --timeout=60

# Specific test file
pytest tests/python/all/tree/import_test.py -v --timeout=60

# RPC tests (need Java classpath generated first)
# From repo root: ./gradlew :rewrite-python:generateTestClasspath
pytest tests/rpc/ --timeout=120
```

**RPC tests can hang indefinitely** if communication fails (deadlock, malformed response, printer bugs). Always use explicit `--timeout`.

## Directory Structure

```
rewrite-python/rewrite/
├── src/rewrite/
│   ├── tree.py, visitor.py, recipe.py    # Core framework
│   ├── markers.py, parser.py             # Metadata, base parser
│   ├── execution.py, style.py            # Execution context, style detection
│   ├── java/                             # Java LST model (J nodes)
│   │   ├── tree.py                       # J namespace (Java AST)
│   │   ├── visitor.py                    # JavaVisitor
│   │   └── support_types.py, extensions.py
│   ├── python/                           # Python-specific
│   │   ├── tree.py                       # Python AST nodes
│   │   ├── visitor.py                    # PythonVisitor
│   │   ├── printer.py                    # Python-to-source printer
│   │   ├── _parser_visitor.py            # Python 3 parser visitor
│   │   ├── _py2_parser_visitor.py        # Python 2 parser visitor
│   │   ├── add_import.py                 # Import addition logic
│   │   ├── remove_import.py              # Import removal logic
│   │   ├── recipes/                      # Built-in recipes
│   │   ├── format/                       # Formatting visitors (auto_format, blank_lines, etc.)
│   │   └── template/                     # Template engine (coordinates, patterns, etc.)
│   ├── rpc/                              # RPC bridge (Java ↔ Python)
│   │   ├── python_sender.py              # Serialize tree, send to Java
│   │   ├── python_receiver.py            # Receive from Java, rebuild tree
│   │   ├── send_queue.py, receive_queue.py  # Queue abstractions
│   │   ├── server.py                     # RPC server entry point
│   │   └── java_rpc_client.py, java_recipe.py  # Java-side integration
│   └── test/                             # Testing infrastructure
│       ├── rewrite_test.py               # RecipeSpec class, rewrite_run()
│       └── spec.py                       # SourceSpec, python() helper
├── tests/
│   ├── conftest.py                       # Top-level pytest fixtures
│   ├── python/all/tree/                  # Parse/print round-trip tests
│   ├── python/all/format/                # Format round-trip tests
│   ├── python/format/                    # Format-specific tests
│   ├── python/template/                  # Template tests
│   ├── python/py311-py314/               # Python version-specific tests
│   ├── recipes/                          # Recipe tests
│   └── rpc/                              # RPC integration tests
└── pyproject.toml                        # Package config, dependencies
```

## Development Patterns

### Frozen Dataclasses with Padding

All LST nodes are immutable frozen dataclasses. Private fields (prefixed `_`) store padded versions; public `@property` accessors return unwrapped values.

```python
# Updating a node
from rewrite.visitor import replace_if_changed
new_node = replace_if_changed(old_node, _statements=new_padded_statements)

# Accessing/modifying padding (whitespace, comments)
padded_stmts = cu.padding.statements  # List[JRightPadded[Statement]]
cu = cu.padding.replace(_statements=new_padded_list)
```

### Statement and Import Newlines

**Critical convention**: The `\n` between statements belongs in the **next statement's `.prefix`**, NOT in the preceding element's `JRightPadded.after`.

```python
from os.path import join
x = 1
```
- Statement 0 (MultiImport): `prefix=''`; names: `join(after='')`
- Statement 1 (x=1): `prefix='\n'`, `after=''`

When inserting after existing imports, the new import needs `prefix=Space([], '\n')` as a newline separator.

### Import Detection

- `from datetime import datetime` → `MultiImport` with `from_=Identifier("datetime")`, names contain `Import` with qualid `"datetime"`
- `import datetime` → `MultiImport` with `from_=None`, names contain `Import` with qualid `"datetime"`

### Padding and Whitespace

- `JRightPadded[T]`: element T with `.after` (whitespace before trailing delimiter)
- `JLeftPadded[T]`: before-space + element T (used for operators like `=`)
- `JContainer[T]`: leading `(`, elements, trailing `)` with full padding control
- Always access public properties (`.statements`, `.names`); use `.padding` for modifications
- All tree nodes are frozen. Use `replace_if_changed()` or `.padding.replace()` for modifications.

### Recipe Pattern

```python
from dataclasses import dataclass, field
from rewrite.recipe import Recipe
from rewrite.python.visitor import PythonVisitor
from rewrite.execution import ExecutionContext

@dataclass
class MyRecipe(Recipe):
    @property
    def name(self) -> str:
        return "org.openrewrite.python.MyRecipe"

    @property
    def description(self) -> str:
        return "Brief description of what this recipe does"

    def editor(self) -> PythonVisitor:
        class _Visitor(PythonVisitor[ExecutionContext]):
            def visit_multi_import(self, multi, p):
                return multi  # or modified version
        return _Visitor()
```

### Test Pattern

```python
from rewrite.test import RecipeSpec
from rewrite.test.spec import python

def test_my_recipe():
    RecipeSpec(recipe=MyRecipe()).rewrite_run(
        python("import os", "import pathlib")
    )
```

`python(before, after)` creates a source spec. `rewrite_run()` parses `before`, applies the recipe, and asserts the result matches `after`.

## RPC Communication

Sender/Receiver implementations (`python_sender.py`, `python_receiver.py`) must stay aligned with the Java equivalents. Any mismatch causes deadlocks or data corruption.

If an RPC test hangs, verify the Java classpath is generated (`./gradlew :rewrite-python:generateTestClasspath`) and run a single test with verbose output: `pytest tests/rpc/test_case.py -v -s --timeout=120`.

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