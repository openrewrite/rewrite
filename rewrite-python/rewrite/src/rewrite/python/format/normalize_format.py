from __future__ import annotations

from typing import cast, Optional

from rewrite import Tree, P, Cursor
from rewrite.java import MethodDeclaration, J, Space, ClassDeclaration
from rewrite.python import PythonVisitor
from rewrite.python.format._helpers import concatenate_prefix
from rewrite.visitor import T


class NormalizeFormatVisitor(PythonVisitor):
    def __init__(self, stop_after: Optional[Tree] = None):
        self._stop_after = stop_after
        self._stop = False

    def visit_class_declaration(self, class_decl: ClassDeclaration, p: P) -> J:
        cd = cast(ClassDeclaration, super().visit_class_declaration(class_decl, p))
        if cd.leading_annotations:
            cd = concatenate_prefix(cd, Space.first_prefix(cd.leading_annotations))
            cd = cd.replace(leading_annotations=Space.format_first_prefix(cd.leading_annotations, Space.EMPTY))
            return cd

        cd = concatenate_prefix(cd, cd.padding.kind.prefix)
        cd = cd.padding.replace(kind=cd.padding.kind.with_prefix(Space.EMPTY))
        return cd

    def visit_method_declaration(self, method: MethodDeclaration, p: P) -> J:
        md = cast(MethodDeclaration, super().visit_method_declaration(method, p))
        if md.leading_annotations:
            md = concatenate_prefix(md, Space.first_prefix(md.leading_annotations))
            md = md.replace(leading_annotations=Space.format_first_prefix(md.leading_annotations, Space.EMPTY))
            return md

        if md.modifiers:
            md = concatenate_prefix(md, Space.first_prefix(md.modifiers))
            md = md.replace(modifiers=Space.format_first_prefix(md.modifiers, Space.EMPTY))
            return md

        return md

    def post_visit(self, tree: T, p: P) -> Optional[T]:
        if self._stop_after and tree == self._stop_after:
            self._stop = True
        return tree

    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        return cast(Optional[T], tree if self._stop else super().visit(tree, p, parent))
