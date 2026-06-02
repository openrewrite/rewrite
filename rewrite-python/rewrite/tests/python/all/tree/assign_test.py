from rewrite.java.support_types import JavaType
from rewrite.java.tree import Assignment, Identifier, MethodInvocation
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python

Parameterized = JavaType.Parameterized


def test_assign():
    # language=python
    RecipeSpec().rewrite_run(python("a = 1"))


def test_assign_2():
    # language=python
    RecipeSpec().rewrite_run(python("a.b: int = 1"))


def test_assign_no_init():
    # language=python
    RecipeSpec().rewrite_run(python("a : int"))


def test_chained_assign():
    # language=python
    RecipeSpec().rewrite_run(python("a = b = c = 3"))


def test_assign_expression():
    # language=python
    RecipeSpec().rewrite_run(python("(a := 1)"))


def test_assign_in_if():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        if True:
            a = 1
        elif True:
            a = 2
        else:
            a = 3
        """
    ))


def test_assign_in_while_loop():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        while True:
            a = 1
        """
    ))


def test_assign_in_for_loop():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        for i in range(10):
            a = 2
        """
    ))


def test_assign_in_try():
    # language=python
    RecipeSpec().rewrite_run(python(
        """
        try:
            a = 1
        except Exception:
            a = 2
        """
    ))


def test_assign_op():
    # language=python
    RecipeSpec().rewrite_run(python("a += 1"))
    # language=python
    RecipeSpec().rewrite_run(python("a -= 1"))
    # language=python
    RecipeSpec().rewrite_run(python("a *= 1"))
    # language=python
    RecipeSpec().rewrite_run(python("a /= 1"))
    # language=python
    RecipeSpec().rewrite_run(python("a %= 1"))
    # language=python
    RecipeSpec().rewrite_run(python("a |= 1"))
    # language=python
    RecipeSpec().rewrite_run(python("a &= 1"))
    # language=python
    RecipeSpec().rewrite_run(python("a ^= 1"))
    # language=python
    RecipeSpec().rewrite_run(python("a **= 1"))
    # language=python
    RecipeSpec().rewrite_run(python("a //= 1"))
    # language=python
    RecipeSpec().rewrite_run(python("a @= 1"))


def test_assign_type_attribution():
    """Verify that type attribution is populated on assignment AST nodes."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_assignment(self, assignment, p):
                if not isinstance(assignment, Assignment):
                    return assignment
                # The assignment type should be int (the type of the value)
                if assignment.type is None:
                    errors.append("Assignment.type is None")
                elif assignment.type != JavaType.Primitive.Int:
                    errors.append(f"Assignment.type is {assignment.type}, expected Primitive.Int")
                # The LHS identifier should have type int
                if isinstance(assignment.variable, Identifier):
                    ident = assignment.variable
                    if ident.type is None:
                        errors.append("LHS Identifier.type is None")
                return assignment

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        "x: int = 42",
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)


def test_assign_method_call_type_attribution():
    """Verify type attribution on an assignment from a method call like str.split()."""
    errors = []

    def check_types(source_file):
        assert isinstance(source_file, CompilationUnit)

        class TypeChecker(PythonVisitor):
            def visit_assignment(self, assignment, p):
                if not isinstance(assignment, Assignment):
                    return assignment
                # The assignment type should be list (possibly Parameterized)
                if assignment.type is None:
                    errors.append("Assignment.type is None")
                elif isinstance(assignment.type, Parameterized):
                    # Parameterized: check base type is list
                    if assignment.type._type._fully_qualified_name != 'list':
                        errors.append(f"Parameterized._type fqn is '{assignment.type._type._fully_qualified_name}', expected 'list'")
                    # Check type parameters include str
                    if not assignment.type._type_parameters:
                        errors.append("Parameterized._type_parameters is empty")
                elif isinstance(assignment.type, JavaType.Class):
                    if not assignment.type._fully_qualified_name.startswith('list'):
                        errors.append(f"Assignment.type fqn is '{assignment.type._fully_qualified_name}', expected to start with 'list'")
                else:
                    errors.append(f"Assignment.type is {type(assignment.type).__name__}, expected Parameterized or Class")
                return assignment

            def visit_method_invocation(self, method, p):
                if not isinstance(method, MethodInvocation):
                    return method
                if method.name.simple_name != 'split':
                    return method
                # method_type should be populated
                if method.method_type is None:
                    errors.append("MethodInvocation.method_type is None")
                elif method.method_type.name != 'split':
                    errors.append(f"method_type.name is '{method.method_type.name}', expected 'split'")
                # declaring type should be str
                if method.method_type is not None and method.method_type.declaring_type is not None:
                    dt = method.method_type.declaring_type
                    if dt._fully_qualified_name != 'str':
                        errors.append(f"declaring_type fqn is '{dt._fully_qualified_name}', expected 'str'")
                # return type should be list (possibly Parameterized)
                if method.method_type is not None and method.method_type._return_type is not None:
                    rt = method.method_type._return_type
                    if isinstance(rt, Parameterized):
                        if not rt._type._fully_qualified_name.startswith('list'):
                            errors.append(f"return_type fqn is '{rt._type._fully_qualified_name}', expected to start with 'list'")
                    elif isinstance(rt, JavaType.Class):
                        if not rt._fully_qualified_name.startswith('list'):
                            errors.append(f"return_type fqn is '{rt._fully_qualified_name}', expected to start with 'list'")
                    else:
                        errors.append(f"return_type is {type(rt).__name__}, expected Parameterized or Class")
                return method

        TypeChecker().visit(source_file, None)

    # language=python
    RecipeSpec(type_attribution=True).rewrite_run(python(
        'parts = "a-b-c".split("-")',
        after_recipe=check_types,
    ))
    assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)
