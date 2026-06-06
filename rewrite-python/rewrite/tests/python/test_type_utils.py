# Copyright 2025 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Unit tests for ``rewrite.python.type_utils``.

Pure-Python tests that construct ``JavaType`` instances directly (no parsing or
RPC) and exercise the type-comparison helpers.
"""


from rewrite.java import JavaType
from rewrite.python.type_utils import (
    as_fully_qualified,
    fully_qualified_names_are_equal,
    is_assignable_to,
    is_object,
    is_of_class_type,
    is_of_type,
    is_string,
)

# ---------------------------------------------------------------------------
# Construction helpers
# ---------------------------------------------------------------------------

def _cls(
    fqn: str,
    supertype: JavaType.FullyQualified | None = None,
    interfaces: list[JavaType.FullyQualified] | None = None,
    kind: JavaType.FullyQualified.Kind | None = None,
) -> JavaType.Class:
    """Build a ``JavaType.Class`` the way the type mapping does (set fields directly).

    Deliberately leaves ``_supertype``/``_interfaces`` *unset* when not provided,
    to exercise the missing-attribute path that real instances often hit.
    """
    c = JavaType.Class()
    c._flags_bit_map = 0
    c._fully_qualified_name = fqn
    c._kind = kind if kind is not None else JavaType.FullyQualified.Kind.Class
    if supertype is not None:
        c._supertype = supertype
    if interfaces is not None:
        c._interfaces = interfaces
    return c


def _param(base: JavaType.FullyQualified, params: list[JavaType]) -> JavaType.Parameterized:
    p = JavaType.Parameterized()
    p._type = base
    p._type_parameters = params
    return p


def _var(name: str, type_: JavaType, owner: JavaType | None = None) -> JavaType.Variable:
    v = JavaType.Variable()
    v._flags_bit_map = 0
    v._name = name
    v._type = type_
    v._owner = owner
    v._annotations = None
    return v


def _method(name: str, declaring: JavaType.FullyQualified, return_type: JavaType) -> JavaType.Method:
    m = JavaType.Method()
    m._flags_bit_map = 0
    m._declaring_type = declaring
    m._name = name
    m._return_type = return_type
    m._parameter_names = None
    m._parameter_types = None
    m._thrown_exceptions = None
    m._annotations = None
    m._default_value = None
    m._declared_formal_type_names = None
    return m


def _generic(name: str, bounds: list[JavaType] | None = None) -> JavaType.GenericTypeVariable:
    return JavaType.GenericTypeVariable(
        _name=name,
        _variance=JavaType.GenericTypeVariable.Variance.Invariant,
        _bounds=bounds,
    )


# A small inheritance fixture:
#   object
#     ^
#   Animal
#     ^
#   Dog  (also implements Comparable)
OBJECT = _cls("object")
ANIMAL = _cls("zoo.Animal", supertype=OBJECT)
COMPARABLE = _cls("typing.Comparable", kind=JavaType.FullyQualified.Kind.Interface)
DOG = _cls("zoo.Dog", supertype=ANIMAL, interfaces=[COMPARABLE])


# ---------------------------------------------------------------------------
# fully_qualified_names_are_equal
# ---------------------------------------------------------------------------

class TestFqnEquality:
    def test_equal(self):
        assert fully_qualified_names_are_equal("a.B", "a.B")

    def test_not_equal(self):
        assert not fully_qualified_names_are_equal("a.B", "a.C")

    def test_both_none(self):
        assert fully_qualified_names_are_equal(None, None)

    def test_one_none(self):
        assert not fully_qualified_names_are_equal("a.B", None)
        assert not fully_qualified_names_are_equal(None, "a.B")


# ---------------------------------------------------------------------------
# as_fully_qualified
# ---------------------------------------------------------------------------

class TestAsFullyQualified:
    def test_class(self):
        assert as_fully_qualified(DOG) is DOG

    def test_parameterized(self):
        p = _param(_cls("list"), [JavaType.Primitive.String])
        assert as_fully_qualified(p) is p

    def test_unknown_is_none(self):
        assert as_fully_qualified(JavaType.Unknown()) is None

    def test_primitive_is_none(self):
        assert as_fully_qualified(JavaType.Primitive.Int) is None

    def test_none_is_none(self):
        assert as_fully_qualified(None) is None


# ---------------------------------------------------------------------------
# is_string / is_object
# ---------------------------------------------------------------------------

class TestIsString:
    def test_primitive_string(self):
        assert is_string(JavaType.Primitive.String)

    def test_str_class(self):
        assert is_string(_cls("str"))

    def test_not_string(self):
        assert not is_string(JavaType.Primitive.Int)
        assert not is_string(_cls("zoo.Dog"))
        assert not is_string(None)


class TestIsObject:
    def test_object_class(self):
        assert is_object(_cls("object"))
        assert is_object(_cls("builtins.object"))

    def test_not_object(self):
        assert not is_object(_cls("zoo.Dog"))
        assert not is_object(JavaType.Primitive.Int)
        assert not is_object(None)


# ---------------------------------------------------------------------------
# is_of_class_type
# ---------------------------------------------------------------------------

class TestIsOfClassType:
    def test_direct_match(self):
        assert is_of_class_type(DOG, "zoo.Dog")

    def test_no_supertype_match(self):
        # is_of_class_type is an *exact* match; it does not walk the hierarchy
        assert not is_of_class_type(DOG, "zoo.Animal")

    def test_parameterized_matches_raw_fqn(self):
        assert is_of_class_type(_param(_cls("list"), [JavaType.Primitive.String]), "list")

    def test_variable_unwrapped(self):
        assert is_of_class_type(_var("d", DOG), "zoo.Dog")

    def test_method_uses_return_type(self):
        assert is_of_class_type(_method("get", DOG, DOG), "zoo.Dog")

    def test_array_uses_elem_type(self):
        arr = JavaType.Array(_elem_type=DOG, _annotations=None)
        assert is_of_class_type(arr, "zoo.Dog")

    def test_primitive_keyword(self):
        assert is_of_class_type(JavaType.Primitive.String, "str")
        assert is_of_class_type(JavaType.Primitive.Int, "int")
        assert not is_of_class_type(JavaType.Primitive.Int, "str")

    def test_unknown(self):
        assert not is_of_class_type(JavaType.Unknown(), "object")

    def test_none(self):
        assert not is_of_class_type(None, "zoo.Dog")


# ---------------------------------------------------------------------------
# is_of_type
# ---------------------------------------------------------------------------

class TestIsOfType:
    def test_identity(self):
        assert is_of_type(DOG, DOG)

    def test_same_fqn(self):
        assert is_of_type(_cls("zoo.Dog"), _cls("zoo.Dog"))

    def test_different_fqn(self):
        assert not is_of_type(_cls("zoo.Dog"), _cls("zoo.Animal"))

    def test_does_not_walk_hierarchy(self):
        # exact type, not assignability
        assert not is_of_type(DOG, ANIMAL)

    def test_primitive(self):
        assert is_of_type(JavaType.Primitive.Int, JavaType.Primitive.Int)
        assert not is_of_type(JavaType.Primitive.Int, JavaType.Primitive.Double)

    def test_string_primitive_and_class(self):
        # str is special: primitive String and a `str` class are the same type
        assert is_of_type(JavaType.Primitive.String, _cls("str"))

    def test_parameterized_same_params(self):
        a = _param(_cls("list"), [JavaType.Primitive.String])
        b = _param(_cls("list"), [JavaType.Primitive.String])
        assert is_of_type(a, b)

    def test_parameterized_different_params(self):
        a = _param(_cls("list"), [JavaType.Primitive.String])
        b = _param(_cls("list"), [JavaType.Primitive.Int])
        assert not is_of_type(a, b)

    def test_parameterized_vs_raw(self):
        a = _param(_cls("list"), [JavaType.Primitive.String])
        assert not is_of_type(a, _cls("list"))

    def test_array(self):
        a = JavaType.Array(_elem_type=DOG, _annotations=None)
        b = JavaType.Array(_elem_type=_cls("zoo.Dog"), _annotations=None)
        assert is_of_type(a, b)
        assert not is_of_type(a, JavaType.Array(_elem_type=ANIMAL, _annotations=None))

    def test_none(self):
        assert not is_of_type(None, DOG)
        assert not is_of_type(DOG, None)
        assert not is_of_type(None, None)

    def test_unknown_not_equal_to_itself(self):
        u = JavaType.Unknown()
        assert not is_of_type(u, u)


# ---------------------------------------------------------------------------
# is_assignable_to — string (FQN) form
# ---------------------------------------------------------------------------

class TestIsAssignableToFqn:
    def test_direct(self):
        assert is_assignable_to("zoo.Dog", DOG)

    def test_supertype(self):
        assert is_assignable_to("zoo.Animal", DOG)

    def test_transitive_supertype(self):
        assert is_assignable_to("object", DOG)

    def test_interface(self):
        assert is_assignable_to("typing.Comparable", DOG)

    def test_not_assignable(self):
        assert not is_assignable_to("zoo.Cat", DOG)

    def test_object_fallback_when_no_supertype(self):
        # A class with no _supertype attribute is still assignable to object
        assert is_assignable_to("object", _cls("loose.Thing"))

    def test_not_object_when_no_supertype(self):
        assert not is_assignable_to("other.Thing", _cls("loose.Thing"))

    def test_variable_unwrapped(self):
        assert is_assignable_to("zoo.Animal", _var("d", DOG))

    def test_method_return_type(self):
        assert is_assignable_to("zoo.Animal", _method("get", DOG, DOG))

    def test_generic_bound(self):
        t = _generic("T", bounds=[DOG])
        assert is_assignable_to("zoo.Animal", t)
        assert not is_assignable_to("zoo.Cat", t)

    def test_str_primitive(self):
        assert is_assignable_to("str", JavaType.Primitive.String)

    def test_primitive_widening_bool_to_int(self):
        assert is_assignable_to("int", JavaType.Primitive.Boolean)

    def test_primitive_widening_int_to_float(self):
        assert is_assignable_to("float", JavaType.Primitive.Int)

    def test_primitive_no_narrowing(self):
        assert not is_assignable_to("int", JavaType.Primitive.Double)

    def test_primitive_is_object(self):
        assert is_assignable_to("object", JavaType.Primitive.Int)

    def test_none_from(self):
        assert not is_assignable_to("zoo.Dog", None)

    def test_unknown_from_only_object(self):
        assert is_assignable_to("object", JavaType.Unknown())
        assert not is_assignable_to("zoo.Dog", JavaType.Unknown())


# ---------------------------------------------------------------------------
# is_assignable_to — JavaType form
# ---------------------------------------------------------------------------

class TestIsAssignableToType:
    def test_direct(self):
        assert is_assignable_to(_cls("zoo.Dog"), DOG)

    def test_supertype(self):
        assert is_assignable_to(ANIMAL, DOG)

    def test_interface(self):
        assert is_assignable_to(COMPARABLE, DOG)

    def test_not_assignable(self):
        assert not is_assignable_to(_cls("zoo.Cat"), DOG)

    def test_parameterized_to_reduced_to_raw(self):
        # `to` parameterized list; from a `list` subtype — reduced to raw fqn walk
        list_param = _param(_cls("list"), [JavaType.Primitive.String])
        my_list = _cls("mymod.MyList", supertype=_cls("list"))
        assert is_assignable_to(list_param, my_list)

    def test_primitive_identity(self):
        assert is_assignable_to(JavaType.Primitive.Int, JavaType.Primitive.Int)

    def test_primitive_widening(self):
        assert is_assignable_to(JavaType.Primitive.Double, JavaType.Primitive.Int)

    def test_string_primitive_and_class(self):
        assert is_assignable_to(JavaType.Primitive.String, _cls("str"))
        assert is_assignable_to(_cls("str"), JavaType.Primitive.String)

    def test_object_to_accepts_anything(self):
        assert is_assignable_to(OBJECT, DOG)
        assert is_assignable_to(_cls("object"), JavaType.Primitive.Int)

    def test_variable_to_unwrapped(self):
        assert is_assignable_to(_var("a", ANIMAL), DOG)

    def test_none(self):
        assert not is_assignable_to(DOG, None)
