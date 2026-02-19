# Python Language Support Planning Document

## Executive Summary

This document outlines the design decisions for adding Python language support to OpenRewrite, following the pattern established by rewrite-javascript. The goal is to create a native Python recipe authorship experience while maximizing reuse of the Java LST model (`J` types) for cross-language recipe applicability.

## Current State Analysis

### Existing rewrite-python Implementation

Located at `/Users/jon/conductor/workspaces/rewrite-python/lome`, the existing implementation uses:

1. **LST Model**: Frozen dataclasses with `with_*` methods for immutability
2. **32 Python-specific types** extending `Py(J)` marker class
3. **Extensive J model reuse**: Uses Java types for common constructs (MethodDeclaration, ClassDeclaration, If, While, etc.)
4. **Verbose boilerplate**: Each dataclass requires ~50 lines for a simple type due to property accessors and `with_*` methods

### TypeScript/JavaScript Implementation (Reference)

Located at `rewrite-javascript/rewrite/src/`, the TypeScript implementation uses:

1. **LST Model**: TypeScript interfaces with `readonly` properties
2. **`kind` string discriminant**: Each type has a unique `kind` constant for type identification
3. **Namespace pattern**: `JS.Kind.Alias`, `JS.ArrowFunction`, etc.
4. **Immutable updates via Mutative library**: `produce()` function for structural sharing
5. **Recipe options via decorators**: `@Option({ displayName, description, example })`

---

## Decision 1: Python LST Type Modeling

### Options Considered

| Approach | Pros | Cons |
|----------|------|------|
| **A. Frozen dataclasses (current)** | Standard library, IDE support, type hints | Extremely verbose, requires `with_*` boilerplate |
| **B. attrs library** | Less verbose, good IDE support | External dependency, still needs some boilerplate |
| **C. Named tuples with replace()** | Minimal, built-in | No good IDE autocomplete, less readable |
| **D. Pydantic v2** | Validation, JSON serialization built-in | Heavy dependency, mutation-focused API |
| **E. Custom metaclass + slots** | Minimal memory, fast | Complex, hard to maintain |

### Recommendation: **Frozen dataclasses with `.pyi` stub generation**

**Rationale:**
- Python's idiomatic choice for immutable value objects is `@dataclass(frozen=True)`
- The idiomatic way to update frozen dataclasses is `dataclasses.replace()` (similar to attrs' `evolve()`)
- The `with_*` pattern from the current implementation is a Java/Scala idiom, not Pythonic
- However, `replace()` has poor IDE autocomplete for kwargs
- **Solution**: Generate `.pyi` stub files that declare typed `replace()` method signatures

**Key Design Elements:**

**`tree.py`** (runtime source - clean and readable):
```python
from dataclasses import dataclass, replace as _replace
from typing import Optional, ClassVar
from uuid import UUID

@dataclass(frozen=True, eq=False, slots=True)
class Await(Py, Expression):
    """Represents an await expression: `await some_coroutine()`"""

    kind: ClassVar[str] = "org.openrewrite.python.tree.Py$Await"
    id: UUID
    prefix: Space
    markers: Markers
    expression: Expression
    type: Optional[JavaType] = None

    def replace(self, **changes) -> "Await":
        """Create a copy with the specified fields replaced."""
        return _replace(self, **changes)
```

**`tree.pyi`** (generated stub - provides IDE autocomplete):
```python
from typing import Optional, Self, ClassVar
from uuid import UUID

class Await(Py, Expression):
    kind: ClassVar[str]
    id: UUID
    prefix: Space
    markers: Markers
    expression: Expression
    type: Optional[JavaType]

    def replace(
        self,
        *,
        id: UUID = ...,
        prefix: Space = ...,
        markers: Markers = ...,
        expression: Expression = ...,
        type: Optional[JavaType] = ...,
    ) -> Self: ...
```

**How This Works:**
1. IDEs (PyCharm, VS Code with Pylance) prefer `.pyi` files over `.py` when both exist
2. The stub file declares the typed `replace()` signature with all fields as keyword-only args
3. Recipe authors get full autocomplete: `await_expr.replace(expr` shows `expression: Expression`
4. A simple build script (~50 lines) generates `.pyi` from `.py` using Python's `ast` module

**Changes from Current Implementation:**

1. **Remove `_` prefix on fields** - Use direct property names
2. **Remove verbose `with_*` methods** - Use single `replace()` method
3. **Add `kind` class variable** - For type discrimination in RPC
4. **Add `slots=True`** - Memory efficiency
5. **Keep `eq=False`** - Identity-based equality (same as current)
6. **Generate `.pyi` stubs** - For IDE autocomplete support

**Example Usage:**

```python
# Before (current verbose style with Java idiom)
await_expr = await_expr.with_expression(new_expr).with_type(new_type)

# After (idiomatic Python with full IDE autocomplete)
await_expr = await_expr.replace(expression=new_expr, type=new_type)
```

**Stub Generation:**

A simple script parses `tree.py` and generates `tree.pyi`:

```python
import ast
from pathlib import Path

def generate_stubs(source_path: Path) -> str:
    """Generate .pyi stub with typed replace() methods for each dataclass."""
    tree = ast.parse(source_path.read_text())
    # ... extract fields from each @dataclass
    # ... generate typed replace() signature
    # See .context/metaprogramming_options.py for full implementation
```

This can run as a pre-commit hook or build step.

---

## Decision 2: Python Recipe Type Design

### TypeScript Pattern (Reference)

```typescript
export class ChangeImport extends Recipe {
    readonly name = "org.openrewrite.javascript.change-import";
    readonly displayName = "Change import";
    readonly description = "Changes an import...";

    @Option({ displayName: "Old module", description: "...", example: "react" })
    oldModule!: string;

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {
            // ...
        }();
    }
}
```

### Recommendation: **Python dataclass with field metadata**

**Rationale:**
- Python's `dataclasses.field()` with `metadata` parameter is the idiomatic equivalent of decorators for option configuration
- Avoids additional decorator complexity while achieving the same result
- Integrates naturally with type annotations

```python
from dataclasses import dataclass, field
from typing import Optional
from abc import ABC, abstractmethod

def option(
    display_name: str,
    description: str,
    example: Optional[str] = None,
    required: bool = True,
    valid: Optional[list[str]] = None
) -> dict:
    """Creates option metadata for recipe fields."""
    return {
        "option": {
            "display_name": display_name,
            "description": description,
            "example": example,
            "required": required,
            "valid": valid
        }
    }


@dataclass
class Recipe(ABC):
    """Base class for all Python recipes."""

    @property
    @abstractmethod
    def name(self) -> str:
        """Fully qualified recipe name, e.g. 'org.openrewrite.python.cleanup.RemoveUnusedImports'"""
        ...

    @property
    @abstractmethod
    def display_name(self) -> str:
        """Human-readable name, initial capped, no period."""
        ...

    @property
    @abstractmethod
    def description(self) -> str:
        """Full description with markdown support."""
        ...

    @property
    def tags(self) -> list[str]:
        return []

    @property
    def estimated_effort_per_occurrence(self) -> int:
        """Estimated minutes to fix manually."""
        return 5

    def editor(self) -> "TreeVisitor":
        """Returns the visitor that performs transformations."""
        return TreeVisitor.noop()

    def recipe_list(self) -> list["Recipe"]:
        """Returns child recipes for composite recipes."""
        return []

    def descriptor(self) -> "RecipeDescriptor":
        """Returns the recipe descriptor for marketplace."""
        return RecipeDescriptor.from_recipe(self)


@dataclass
class ChangeImport(Recipe):
    """Changes an import from one module to another."""

    old_module: str = field(metadata=option(
        display_name="Old module",
        description="The module to change imports from",
        example="flask"
    ))

    new_module: str = field(metadata=option(
        display_name="New module",
        description="The module to change imports to",
        example="flask_restful"
    ))

    old_name: Optional[str] = field(default=None, metadata=option(
        display_name="Old name",
        description="The specific name to change (or None for all)",
        required=False
    ))

    @property
    def name(self) -> str:
        return "org.openrewrite.python.ChangeImport"

    @property
    def display_name(self) -> str:
        return "Change import"

    @property
    def description(self) -> str:
        return "Changes an import from one module to another, updating all references."

    def editor(self) -> "TreeVisitor":
        old_module = self.old_module
        new_module = self.new_module
        old_name = self.old_name

        class ChangeImportVisitor(PythonVisitor):
            def visit_import(self, imp: Import, ctx: ExecutionContext):
                # Implementation...
                pass

        return ChangeImportVisitor()
```

### Alternative: Decorator-based (closer to TypeScript)

```python
from rewrite import Recipe, option

class ChangeImport(Recipe):
    name = "org.openrewrite.python.ChangeImport"
    display_name = "Change import"
    description = "Changes an import..."

    @option(display_name="Old module", description="...", example="flask")
    old_module: str

    @option(display_name="New module", description="...", example="flask_restful")
    new_module: str
```

This requires a custom descriptor protocol but is visually cleaner.

**Recommendation: Start with dataclass + field metadata**, as it's more Pythonic and requires less magic. We can add decorator sugar later if desired.

---

## Decision 3: Python LST Types vs J Model Reuse

### Current State in rewrite-python

The existing implementation already reuses J model types extensively:

**Fully Reused (no Python-specific version):**
- `J.Identifier`
- `J.Literal`
- `J.MethodDeclaration`
- `J.ClassDeclaration`
- `J.Block`
- `J.If`, `J.While`, `J.DoWhile`
- `J.Try`, `J.Catch`
- `J.Return`, `J.Break`, `J.Continue`
- `J.Assignment`, `J.AssignmentOperation`
- `J.Lambda`
- `J.Ternary`
- `J.Parentheses`
- And ~50+ more

**Python-Specific (32 types defined in tree.py):**

| Type | Reason for Python-Specific |
|------|---------------------------|
| `Py.Async` | `async def`, `async with` syntax |
| `Py.Await` | `await expr` syntax |
| `Py.Binary` | Python-specific operators: `in`, `is`, `is not`, `not in`, `//`, `@`, `**` |
| `Py.ChainedAssignment` | `a = b = c = 1` |
| `Py.CollectionLiteral` | `[1, 2]` lists and `{1, 2}` sets |
| `Py.CompilationUnit` | Module-level structure |
| `Py.ComprehensionExpression` | `[x for x in y]`, dict/set comprehensions |
| `Py.Del` | `del x` statement |
| `Py.DictLiteral` / `Py.DictEntry` | `{a: b}` dict literals |
| `Py.ExceptionType` | `except Type as var` |
| `Py.FormattedString` | f-strings: `f"x={x}"` |
| `Py.ForLoop` | Different from J.ForLoop (no init/condition/update) |
| `Py.MatchCase` | Pattern matching (`match`/`case`) |
| `Py.MultiImport` | `from x import a, b, c` |
| `Py.NamedArgument` | `func(name=value)` |
| `Py.Pass` | `pass` statement |
| `Py.Slice` | `a[1:2:3]` |
| `Py.SpecialParameter` | `*args`, `**kwargs` |
| `Py.Star` | `*` unpacking |
| `Py.TrailingElseWrapper` | `else` on `for`/`while`/`try` |
| `Py.TypeAlias` | `type X = Y` |
| `Py.TypeHint` | `: Type` annotations |
| `Py.UnionType` | `Type | Type` |
| `Py.YieldFrom` | `yield from x` |
| `Py.VariableScope` | Comprehension scope handling |

### Opportunities for More J Reuse

After analysis, most Python-specific types are justified. However, these could potentially be consolidated:

1. **`Py.NamedArgument`** - Could potentially extend `J.Assignment` with a marker
2. **`Py.ExpressionStatement`** / **`Py.StatementExpression`** - Review if `J.ExpressionStatement` could be reused

### Recommendation: **Keep current split, minimal changes**

The existing reuse is well-designed. The Python-specific types genuinely represent Python-only syntax. The split enables:

1. **Cross-language recipes**: Recipes targeting `J.MethodDeclaration` work on Python methods
2. **Python-specific recipes**: Can target `Py.Async` for async-specific transformations
3. **Clear semantics**: No confusion about what operators/constructs are supported

---

## Implementation Plan

### Phase 1: Core Infrastructure (Week 1-2)

1. **Set up rewrite-python module structure** in openrewrite/rewrite
   ```
   rewrite-python/
   ├── build.gradle.kts
   └── rewrite/
       ├── pyproject.toml
       └── src/
           ├── rewrite/
           │   ├── __init__.py
           │   ├── tree.py          # Base Tree, SourceFile
           │   ├── markers.py       # Markers system
           │   ├── visitor.py       # TreeVisitor base
           │   ├── execution.py     # ExecutionContext
           │   ├── recipe.py        # Recipe base class
           │   └── python/
           │       ├── tree.py      # Py types
           │       ├── visitor.py   # PythonVisitor
           │       ├── parser.py    # PythonParser
           │       ├── printer.py   # PythonPrinter
           │       └── rpc/
           │           ├── sender.py
           │           └── receiver.py
           └── tests/
   ```

2. **Port core infrastructure** from lome:
   - `Tree`, `SourceFile`, `Cursor`
   - `Markers` system
   - `TreeVisitor` base class
   - `ExecutionContext`

3. **Implement Recipe base class** with option metadata pattern

### Phase 2: LST Model (Week 2-3)

1. **Port J model types** needed for Python:
   - Import from Java definitions or code-generate
   - `J.Space`, `J.RightPadded`, `J.LeftPadded`, `J.Container`
   - Core expression/statement types

2. **Refactor Py types** with cleaner dataclass pattern:
   - Remove `with_*` boilerplate
   - Add `kind` discriminant
   - Use `slots=True`

3. **Port PythonVisitor** with all visit methods

### Phase 3: RPC Integration (Week 3-4)

1. **Implement PythonSender** extending Sender pattern
2. **Implement PythonReceiver** extending Receiver pattern
3. **Register Python codecs** for RPC serialization
4. **Integration testing** with Java process

### Phase 4: Parser & Printer (Week 4-5)

1. **Port PythonParser** using Python `ast` module
2. **Port PythonPrinter** for lossless printing
3. **Round-trip testing** (parse -> print == original)

### Phase 5: Test Framework & Recipes (Week 5-6)

1. **Port test framework** (`RecipeSpec`, `rewrite_run()`)
2. **Create initial recipes**:
   - `ChangeImport`
   - `RemoveUnusedImports`
   - `OrderImports`
3. **Documentation**

---

## Open Questions

1. ~~**Code Generation**: Should we generate LST types from a schema like TypeScript does?~~
   - **Resolved**: No. J types rarely change, so we hand-write them once. We only generate `.pyi` stub files for IDE autocomplete support.

2. **Async Visitors**: TypeScript visitors are async for RPC. Should Python visitors be async?
   - Pro: Native async/await for RPC calls
   - Con: Complexity for simple in-process recipes

3. **Package Name**: `openrewrite` vs `rewrite` for PyPI package?

4. **Python Version Support**: Minimum Python 3.10 (for `match`/`case`) or 3.9?

---

## Appendix: Kind String Convention

Following the Java/TypeScript pattern, Python types use fully-qualified Java-style names:

```python
# Pattern: org.openrewrite.python.tree.Py$<TypeName>
Py.Await.kind = "org.openrewrite.python.tree.Py$Await"
Py.Binary.kind = "org.openrewrite.python.tree.Py$Binary"
Py.CompilationUnit.kind = "org.openrewrite.python.tree.Py$CompilationUnit"
```

This enables seamless RPC communication with Java processes.
