# OpenRewrite Python

OpenRewrite automated refactoring for Python source code.

## Installation

```bash
pip install openrewrite
```

## Quick Start

```python
from rewrite.python import PythonParser
from rewrite import ExecutionContext

# Parse Python source code
parser = PythonParser()
ctx = ExecutionContext()
source_files = parser.parse(ctx, "example.py")

# Apply recipes to transform code
# ...
```

## Writing Recipes

```python
from dataclasses import dataclass, field
from rewrite import Recipe, option
from rewrite.python import PythonVisitor

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

    @property
    def name(self) -> str:
        return "org.openrewrite.python.ChangeImport"

    @property
    def display_name(self) -> str:
        return "Change import"

    @property
    def description(self) -> str:
        return "Changes an import from one module to another."

    def editor(self) -> PythonVisitor:
        # Implementation...
        pass
```

## Documentation

See [docs.openrewrite.org](https://docs.openrewrite.org) for full documentation.

## License

Moderne Source Available License - see [LICENSE.md](../LICENSE.md)
