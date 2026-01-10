import ast
from typing import Optional

from pytype import config
from pytype.pytd.pytd import CallableType, GenericType, ClassType, UnionType, NothingType, TypeParameter
from pytype.tools.annotate_ast.annotate_ast import AnnotateAstVisitor, PytypeError, infer_types

from ..java import JavaType


class PythonTypeMapping:
    __enabled = False
    __cache: dict[str, JavaType] = {}

    def __init__(self, source: str):
        pytype_options = config.Options.create(python_version='3.12', check=False, precise_return=True,
                                               output_debug=False)
        try:
            self._source_with_types = infer_types(source, pytype_options) if self.__enabled else None
        except PytypeError:
            self._source_with_types = None

    def resolve_types(self, node):
        if self._source_with_types:
            type_visitor = MyAnnotateAstVisitor(self._source_with_types, ast)
            type_visitor.visit(node)

    def type(self, node) -> Optional[JavaType]:
        if hasattr(node, 'resolved_annotation') and (result := self.__cache.get(node.resolved_annotation)):
            return result
        if isinstance(node, ast.Constant):
            if isinstance(node.value, str):
                return JavaType.Primitive.String
            elif isinstance(node.value, bool):
                return JavaType.Primitive.Boolean
            elif isinstance(node.value, int):
                return JavaType.Primitive.Int
            elif isinstance(node.value, float):
                return JavaType.Primitive.Double
            else:
                return JavaType.Primitive.None_
        elif isinstance(node, ast.Call):
            return self.method_invocation_type(node)
        elif self.__enabled and hasattr(node, 'resolved_type'):
            return self.__map_type(node.resolved_type, node)
        return None

    def method_invocation_type(self, node) -> Optional[JavaType.Method]:
        return self.__map_type(node.func.resolved_type, node) if self.__enabled else None

    def __map_type(self, type, node):
        signature = get_type_string(type)
        result = self.__cache.get(signature)
        if result:
            return result

        if isinstance(type, ClassType):
            self.__cache[signature] = (result := JavaType.Class())
            result._kind = JavaType.FullyQualified.Kind.Class
            result._fully_qualified_name = type.name
            result._interfaces = [self.__map_type(i, node) for i in type.cls.bases]
        elif isinstance(type, CallableType):
            if isinstance(node, ast.Name):
                name = node.id
            elif isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute):
                name = node.func.attr
            else:
                name = ''
            return JavaType.Method(_name=name)
        elif isinstance(type, GenericType):
            self.__cache[signature] = (result := JavaType.Parameterized)
            result._type = self.__map_type(type.base_type, node)
            result._type_parameters = [self.__map_type(t, node) for t in type.parameters]
        elif isinstance(type, NothingType):
            self.__cache[signature] = (result := JavaType.Unknown())
        return result


def get_type_string(typ):
    if typ is None:
        return "None"
    elif isinstance(typ, ClassType):
        return typ.name
    elif isinstance(typ, GenericType):
        base = get_type_string(typ.base_type)
        params = [get_type_string(p) for p in typ.parameters]
        return f"{base}[{', '.join(params)}]"
    elif isinstance(typ, CallableType):
        args = [get_type_string(a) for a in typ.args]
        ret = get_type_string(typ.ret)
        return f"[{', '.join(args)}] -> {ret}"
    elif isinstance(typ, UnionType):
        types = [get_type_string(t) for t in typ.type_list]
        return '|'.join(types)
    elif isinstance(typ, TypeParameter):
        types = [get_type_string(t) for t in typ.constraints]
        return f"{typ.full_name}{[{', '.join(types)}] if types else ''}"
    elif hasattr(typ, "name"):
        return typ.name
    else:
        return str(typ)


class MyAnnotateAstVisitor(AnnotateAstVisitor):
    # TODO check if we really should have this
    def visit_Call(self, node):
        self._maybe_annotate(node)
