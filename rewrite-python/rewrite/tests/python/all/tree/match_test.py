from rewrite.test import RecipeSpec, python


def test_simple_match():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        match x:
            case 1:
                pass
        """
    ))


def test_match_with_or_pattern():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        match x:
            case "a" | "b":
                pass
        """
    ))


def test_match_with_sequence_pattern():
    # language=python
    RecipeSpec().rewrite_run(python(
        """\
        match command.split():
            case ["go", direction]:
                pass
        """
    ))


def test_match_with_parenthesized_or_pattern():
    # language=python - OR pattern with parentheses for grouping
    RecipeSpec().rewrite_run(python(
        """\
        match command.split():
            case ["go", ("north" | "south")]:
                pass
        """
    ))


def test_match_with_parenthesized_or_pattern_with_as():
    # language=python - OR pattern with parentheses and as binding
    RecipeSpec().rewrite_run(python(
        """\
        match command.split():
            case ["go", ("north" | "south" | "east" | "west") as direction]:
                current_room = current_room.neighbor(direction)
        """
    ))


def test_match_with_multi_line_body():
    # language=python - match with multi-line body
    RecipeSpec().rewrite_run(python(
        """\
        match command.split():
            case ["go", ("north" | "south" | "east" | "west")]:
                current_room = current_room.neighbor(...)
                # how do I know which direction to go?
        """
    ))


def test_match_multiple_cases():
    # language=python - match with multiple cases
    RecipeSpec().rewrite_run(python(
        """\
        match command.split():
            case ["go", direction] if direction in current_room.exits:
                current_room = current_room.neighbor(direction)
            case ["go", _]:
                print("Sorry, you can't go that way")
        """
    ))


def test_match_with_star_pattern():
    # language=python - star pattern capturing remaining items
    RecipeSpec().rewrite_run(python(
        """\
        match command.split():
            case ["go", *rest]:
                pass
        """
    ))


def test_match_with_star_wildcard():
    # language=python - star wildcard pattern (discarding remaining items)
    RecipeSpec().rewrite_run(python(
        """\
        match command.split():
            case ["go", *_]:
                pass
        """
    ))


def test_match_with_parenthesized_or_pattern_multiline():
    # language=python - multi-line parenthesized OR pattern
    RecipeSpec().rewrite_run(python(
        """\
def f(x):
    match x:
        case (
            A() | B()
        ):
            pass
"""
    ))


def test_match_with_star_wildcard_and_capture():
    # language=python - star wildcard followed by a capture variable
    RecipeSpec().rewrite_run(python(
        """\
match x:
    case [*_, stmt]:
        pass
"""
    ))


def test_match_with_star_capture_and_variable():
    # language=python - star capture followed by a variable
    RecipeSpec().rewrite_run(python(
        """\
match x:
    case [*prev, stmt]:
        pass
"""
    ))


def test_match_with_star_wildcard_expression_not_none():
    # Verify that the Star node for *_ has a non-None expression
    import ast as stdlib_ast
    from rewrite.python._parser_visitor import ParserVisitor

    code = """\
match x:
    case [*_, stmt]:
        pass
"""
    tree = stdlib_ast.parse(code)
    cu = ParserVisitor(code, 'test.py').visit(tree)

    # Walk the LST to find Star nodes
    stars = []
    _collect_stars(cu, stars)
    assert len(stars) > 0, "Expected at least one Star node"
    for star in stars:
        assert star.expression is not None, "Star expression should not be None for *_ pattern"


def test_match_with_nested_star_wildcard_expression_not_none():
    # Verify that Star nodes in nested match-class patterns have non-None expression
    # This is the pattern from refurb that triggers the PythonValidator error
    import ast as stdlib_ast
    from rewrite.python._parser_visitor import ParserVisitor

    code = """\
match node:
    case IfStmt(else_body=Block(body=[*_, stmt])) | WithStmt(body=Block(body=[*_, stmt])):
        pass
    case ForStmt(body=Block(body=[*prev, stmt])) | WhileStmt(body=Block(body=[*prev, stmt])):
        pass
"""
    tree = stdlib_ast.parse(code)
    cu = ParserVisitor(code, 'test.py').visit(tree)

    stars = []
    _collect_stars(cu, stars)
    assert len(stars) > 0, f"Expected Star nodes, found none"
    for star in stars:
        assert star.expression is not None, f"Star expression should not be None"


def _collect_stars(node, result, visited=None):
    """Recursively collect Star nodes from an LST."""
    from rewrite.python.tree import Star

    if visited is None:
        visited = set()
    node_id = id(node)
    if node_id in visited:
        return
    visited.add(node_id)

    if isinstance(node, Star):
        result.append(node)

    # Check dataclass fields
    if hasattr(node, '__dataclass_fields__'):
        for field_name in node.__dataclass_fields__:
            val = getattr(node, field_name, None)
            if val is None or isinstance(val, (str, int, float, bool, bytes)):
                continue
            if hasattr(val, '__dataclass_fields__'):
                _collect_stars(val, result, visited)
            elif isinstance(val, list):
                for item in val:
                    if hasattr(item, '__dataclass_fields__'):
                        _collect_stars(item, result, visited)
                    elif hasattr(item, 'element'):
                        _collect_stars(item.element, result, visited)


def test_match_tuple_with_sequence_pattern():
    # language=python - implicit tuple match with sequence pattern [c] as first element
    RecipeSpec().rewrite_run(python(
        """\
match (y, z):
    case _, b:
        pass
    case [c], _:
        pass
"""
    ))


def test_match_with_or_pattern_in_tuple():
    # language=python - OR pattern as first element of implicit tuple
    RecipeSpec().rewrite_run(python(
        """\
def f(x, y):
    match x, y:
        case A | B, C:
            pass
"""
    ))
