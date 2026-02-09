from __future__ import annotations

from typing import TypeVar, Optional, Any

from rewrite.tree import Tree, SourceFile
from rewrite.visitor import TreeVisitor
from rewrite.toml.tree import (
    Document, Array, Table, KeyValue, Literal, Identifier, Empty,
    TomlSpace, TomlRightPadded
)

P = TypeVar('P')


class TomlVisitor(TreeVisitor[Tree, P]):

    def is_acceptable(self, source_file: SourceFile, p: P) -> bool:
        return isinstance(source_file, Document)

    def visit_document(self, document: Document, p: P) -> Document:
        d = document
        d = d.replace(prefix=self.visit_space(d.prefix, p))
        d = d.replace(markers=self.visit_markers(d.markers, p))
        new_values = []
        changed = False
        for v in d.values:
            result = self.visit(v, p)
            new_values.append(result)
            if result is not v:
                changed = True
        if changed:
            d = d.replace(values=new_values)
        d = d.replace(eof=self.visit_space(d.eof, p))
        return d

    def visit_array(self, array: Array, p: P) -> Array:
        a = array
        a = a.replace(prefix=self.visit_space(a.prefix, p))
        a = a.replace(markers=self.visit_markers(a.markers, p))
        new_values = []
        changed = False
        for v in a.values:
            result = self.visit_right_padded(v, p)
            new_values.append(result)
            if result is not v:
                changed = True
        if changed:
            a = a.replace(values=new_values)
        return a

    def visit_table(self, table: Table, p: P) -> Table:
        t = table
        t = t.replace(prefix=self.visit_space(t.prefix, p))
        t = t.replace(markers=self.visit_markers(t.markers, p))
        new_values = []
        changed = False
        for v in t.values:
            result = self.visit_right_padded(v, p)
            new_values.append(result)
            if result is not v:
                changed = True
        if changed:
            t = t.replace(values=new_values)
        return t

    def visit_key_value(self, key_value: KeyValue, p: P) -> KeyValue:
        kv = key_value
        kv = kv.replace(prefix=self.visit_space(kv.prefix, p))
        kv = kv.replace(markers=self.visit_markers(kv.markers, p))
        new_key = self.visit_right_padded(kv.key, p)
        if new_key is not kv.key:
            kv = kv.replace(key=new_key)
        new_value = self.visit(kv.value, p)
        if new_value is not kv.value:
            kv = kv.replace(value=new_value)
        return kv

    def visit_literal(self, literal: Literal, p: P) -> Literal:
        l = literal
        l = l.replace(prefix=self.visit_space(l.prefix, p))
        l = l.replace(markers=self.visit_markers(l.markers, p))
        return l

    def visit_identifier(self, identifier: Identifier, p: P) -> Identifier:
        i = identifier
        i = i.replace(prefix=self.visit_space(i.prefix, p))
        i = i.replace(markers=self.visit_markers(i.markers, p))
        return i

    def visit_empty(self, empty: Empty, p: P) -> Empty:
        e = empty
        e = e.replace(prefix=self.visit_space(e.prefix, p))
        e = e.replace(markers=self.visit_markers(e.markers, p))
        return e

    def visit_space(self, space: Optional[TomlSpace], p: P) -> Optional[TomlSpace]:
        return space

    def visit_right_padded(self, right: Optional[TomlRightPadded], p: P) -> Optional[TomlRightPadded]:
        if right is None:
            return None

        t = right.element
        if isinstance(t, Tree):
            new_t = self.visit(t, p)
        else:
            new_t = t

        after = self.visit_space(right.after, p)
        if after is right.after and new_t is right.element:
            return right
        return TomlRightPadded(new_t, after, right.markers)
