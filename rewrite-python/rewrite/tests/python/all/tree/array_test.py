from rewrite.test import RecipeSpec, python


def test_empty():
    # language=python
    RecipeSpec().rewrite_run(python("a = []"))


def test_trailing_comma():
    # language=python
    RecipeSpec().rewrite_run(python("a = [1, 2,  ]"))


def test_array_subscript():
    # language=python
    RecipeSpec().rewrite_run(python("a = [1, 2][0]"))


def test_array_slice():
    # language=python
    RecipeSpec().rewrite_run(python("a = [1, 2][0:1]"))


def test_array_slice_no_upper():
    # language=python
    RecipeSpec().rewrite_run(python("a = [1, 2][0:]"))


def test_array_slice_all_empty():
    # language=python
    RecipeSpec().rewrite_run(python("a = [1, 2][ :  :   ]"))


def test_comment():
    # language=python
    RecipeSpec().rewrite_run(python(r'''
        a = d[:0]
        a = d[0:]
        '''
           ))


def test_array_slice_empty_upper_and_step():
    # language=python
    RecipeSpec().rewrite_run(python("a = [1, 2][0::]"))


def test_array_slice_no_lower():
    # language=python
    RecipeSpec().rewrite_run(python("a = [1, 2][:1]"))


def test_array_slice_no_lower_no_upper():
    # language=python
    RecipeSpec().rewrite_run(python("a = [1, 2][::1]"))


def test_array_slice_full():
    # language=python
    RecipeSpec().rewrite_run(python("a = [1, 2][0:1:1]"))


def test_array_slice_tuple_index_1():
    # language=python
    RecipeSpec().rewrite_run(python("a = [1, 2][0,1]"))


def test_array_slice_tuple_index_2():
    # language=python
    RecipeSpec().rewrite_run(python("a = [1, 2][(0,1)]"))
