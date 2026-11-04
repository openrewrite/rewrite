from rewrite.java.tree import FieldAccess
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


# noinspection PyUnresolvedReferences
def test_attribute():
    # language=python
    RecipeSpec().rewrite_run(python("a = foo.bar"))


# noinspection PyUnresolvedReferences
def test_nested_attribute():
    # language=python
    RecipeSpec().rewrite_run(python("a = foo.bar.baz"))


def test_field_access_type_attribution():
    """Verify that os.path has a non-None FieldAccess.type."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_field_access(self, field_access, p):
                if not isinstance(field_access, FieldAccess):
                    return field_access
                if field_access.name.simple_name != 'path':
                    return field_access
                if field_access.type is None:
                    errors.append("FieldAccess.type is None for os.path")
                return field_access

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        """\
        import os
        x = os.path
        """,
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
