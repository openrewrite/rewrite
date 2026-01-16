from rewrite.test import RecipeSpec, python


def test_comment():
    # language=python
    RecipeSpec().rewrite_run(python("assert 1 # type: foo"))


def test_windows_line_endings():
    RecipeSpec().rewrite_run(python("assert 1 # type: foo\r\n"))


def test_multiline_comment():
    # language=python
    RecipeSpec().rewrite_run(python("""
    '''
    This is a
    multiline comment
    '''
    assert 1
    """))


def test_multiline_comment_with_code():
    # language=python
    RecipeSpec().rewrite_run(python("""
    # This is a
    # multiline comment
    assert 1
    # This is another
    # multiline comment
    """))


def test_multiline_comment_in_class():
    # language=python
    RecipeSpec().rewrite_run(python("""
    class ExampleClass:
        '''
        This is a
        multiline comment
        inside a class
        '''
        def example_method(self):
            def example_function():
                '''
                This is a
                multiline comment
                inside a function
                '''
                assert 1
    """))


def test_multiline_comment_with_hash():
    # language=python
    RecipeSpec().rewrite_run(python("""
    class ExampleClass:
        # This is a
        # multiline comment
        # inside a class
        def example_method(self):
            def example_function():
                # This is a
                # multiline comment
                # inside a function
                assert 1
    """))
