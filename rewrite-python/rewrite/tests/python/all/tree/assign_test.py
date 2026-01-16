from rewrite.test import RecipeSpec, python


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
