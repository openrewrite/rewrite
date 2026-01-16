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
