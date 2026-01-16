# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from typing import Any, ClassVar, List, Optional
from typing_extensions import Self
from uuid import UUID

from rewrite import TreeVisitor
from rewrite.java.tree import J

class Py(J):
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...
    def accept_python(self, v: 'PythonVisitor[P]', p: P) -> Optional['J']: ...
    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
