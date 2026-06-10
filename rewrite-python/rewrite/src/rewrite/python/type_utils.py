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

"""Type-comparison helpers for the shared ``JavaType`` model.

A small, Python-flavoured counterpart to rewrite-java's
``org.openrewrite.java.tree.TypeUtils``. Provides the pieces recipes most
commonly need:

* :func:`is_assignable_to` — is a value of ``from_`` assignable to ``to``
  (a fully-qualified name *or* a :class:`JavaType`), walking the supertype and
  interface hierarchy.
* :func:`is_of_type` — exact structural type equality.
* :func:`is_of_class_type` — does a type match a given fully-qualified name.

Generic variance and full type inference are intentionally out of scope for
now; assignability of parameterized types is reduced to their raw fully
qualified name.
"""


from rewrite.java.support_types import JavaType

__all__ = [
    "is_assignable_to",
    "is_of_type",
    "is_of_class_type",
    "is_string",
    "is_object",
    "as_fully_qualified",
    "fully_qualified_names_are_equal",
]

# Fully-qualified names that denote the universal supertype. Python's root is
# ``object``; ``java.lang.Object`` is accepted too since the model is shared.
_OBJECT_NAMES = frozenset({"object", "builtins.object", "java.lang.Object"})

# Mirrors the primitive <-> name mapping used by ``python.type_mapping``. Kept
# local so this module stays free of the (heavyweight) type-mapping imports.
_PRIMITIVE_KEYWORDS = {
    JavaType.Primitive.String: "str",
    JavaType.Primitive.Int: "int",
    JavaType.Primitive.Double: "float",
    JavaType.Primitive.Boolean: "bool",
    JavaType.Primitive.None_: "None",
}
_KEYWORD_TO_PRIMITIVE = {name: prim for prim, name in _PRIMITIVE_KEYWORDS.items()}

# Permitted primitive widenings (``to`` accepts these ``from_`` primitives). In
# Python ``bool`` is a subclass of ``int`` and ``int`` widens to ``float``.
_PRIMITIVE_WIDENING = {
    JavaType.Primitive.Int: frozenset({JavaType.Primitive.Boolean}),
    JavaType.Primitive.Double: frozenset({JavaType.Primitive.Boolean, JavaType.Primitive.Int}),
}


def fully_qualified_names_are_equal(fqn1: str | None, fqn2: str | None) -> bool:
    """Compare two fully-qualified names, treating ``$`` and ``.`` as equivalent."""
    if fqn1 is not None and fqn2 is not None:
        if fqn1 == fqn2:
            return True
        if len(fqn1) != len(fqn2):
            return False
        for c1, c2 in zip(fqn1, fqn2):
            if c1 != c2 and not ((c1 == "$" and c2 == ".") or (c1 == "." and c2 == "$")):
                return False
        return True
    return fqn1 is None and fqn2 is None


def as_fully_qualified(type_: JavaType | None) -> JavaType.FullyQualified | None:
    """Return ``type_`` as a :class:`JavaType.FullyQualified`, or ``None``.

    ``JavaType.Unknown`` is treated as *not* fully qualified.
    """
    if isinstance(type_, JavaType.FullyQualified) and not isinstance(type_, JavaType.Unknown):
        return type_
    return None


def _fqn(type_: JavaType | None) -> str | None:
    """Best-effort fully-qualified name for a type, or ``None``."""
    fq = as_fully_qualified(type_)
    return getattr(fq, "fully_qualified_name", None) if fq is not None else None


def is_string(type_: JavaType | None) -> bool:
    """True for the ``str`` primitive or a ``str`` class."""
    if type_ is JavaType.Primitive.String:
        return True
    return _fqn(type_) in ("str", "java.lang.String")


def is_object(type_: JavaType | None) -> bool:
    """True for the universal supertype (``object``)."""
    return _fqn(type_) in _OBJECT_NAMES


def is_of_class_type(type_: JavaType | None, fqn: str) -> bool:
    """True if ``type_`` (or what it wraps) matches the fully-qualified name ``fqn``.

    This is an *exact* match; it does not walk the supertype hierarchy. Use
    :func:`is_assignable_to` for that.
    """
    if isinstance(type_, JavaType.FullyQualified):
        return fully_qualified_names_are_equal(_fqn(type_), fqn)
    if isinstance(type_, JavaType.Variable):
        return is_of_class_type(type_.type, fqn)
    if isinstance(type_, JavaType.Method):
        return is_of_class_type(type_.return_type, fqn)
    if isinstance(type_, JavaType.Array):
        return is_of_class_type(type_.elem_type, fqn)
    if isinstance(type_, JavaType.Primitive):
        return _PRIMITIVE_KEYWORDS.get(type_) == fqn
    return False


def is_of_type(type1: JavaType | None, type2: JavaType | None) -> bool:
    """True if the two types are exactly the same type.

    Parameterized types are compared by their raw name *and* type parameters.
    Generic variance is not considered.
    """
    if type1 is None or type2 is None:
        return False
    if type1 is type2 and not isinstance(type1, JavaType.Unknown):
        return True
    # ``str`` is special: it can show up as either a primitive or a class.
    if is_string(type1) and is_string(type2):
        return True
    if isinstance(type1, JavaType.Primitive) or isinstance(type2, JavaType.Primitive):
        return type1 is type2
    if isinstance(type1, JavaType.Array) and isinstance(type2, JavaType.Array):
        return is_of_type(type1.elem_type, type2.elem_type)

    fqn1 = _fqn(type1)
    fqn2 = _fqn(type2)
    if fqn1 is not None and fqn2 is not None:
        if not fully_qualified_names_are_equal(fqn1, fqn2):
            return False
        is_p1 = isinstance(type1, JavaType.Parameterized)
        is_p2 = isinstance(type2, JavaType.Parameterized)
        if is_p1 != is_p2:
            return False
        if is_p1 and is_p2:
            tp1 = type1.type_parameters or []
            tp2 = type2.type_parameters or []
            if len(tp1) != len(tp2):
                return False
            return all(is_of_type(a, b) for a, b in zip(tp1, tp2))
        return True
    return False


def is_assignable_to(to: str | JavaType, from_: JavaType | None) -> bool:
    """True if a value of type ``from_`` can be assigned to ``to``.

    ``to`` may be a fully-qualified name (the common case for recipes that don't
    hold a :class:`JavaType` handle) or a :class:`JavaType`. The ``from_``
    hierarchy is walked through its supertype and interfaces.
    """
    try:
        if isinstance(to, str):
            return _is_assignable_to_fqn(to, from_)
        return _is_assignable_to_type(to, from_)
    except Exception:
        return False


def _is_assignable_to_fqn(to: str, from_: JavaType | None) -> bool:
    if isinstance(from_, JavaType.FullyQualified) and not isinstance(from_, JavaType.Unknown):
        if fully_qualified_names_are_equal(to, _fqn(from_)):
            return True
        if _is_assignable_to_fqn(to, getattr(from_, "_supertype", None)):
            return True
        for i in getattr(from_, "_interfaces", None) or []:
            if _is_assignable_to_fqn(to, i):
                return True
        return False
    elif isinstance(from_, JavaType.GenericTypeVariable):
        for bound in from_.bounds:
            if _is_assignable_to_fqn(to, bound):
                return True
    elif isinstance(from_, JavaType.Primitive):
        to_primitive = _KEYWORD_TO_PRIMITIVE.get(to)
        if to_primitive is not None and _is_assignable_to_primitive(to_primitive, from_):
            return True
    elif isinstance(from_, JavaType.Variable):
        return _is_assignable_to_fqn(to, from_.type)
    elif isinstance(from_, JavaType.Method):
        return _is_assignable_to_fqn(to, from_.return_type)
    elif isinstance(from_, (JavaType.Intersection, JavaType.Union)):
        for bound in from_.bounds:
            if _is_assignable_to_fqn(to, bound):
                return True
    # Everything is ultimately an ``object``.
    return to in _OBJECT_NAMES


def _is_assignable_to_type(to: JavaType | None, from_: JavaType | None) -> bool:
    if to is from_ and not isinstance(to, JavaType.Unknown):
        return True
    if to is None or from_ is None:
        return False
    if is_string(to) and is_string(from_):
        return True

    # Unwrap variables and methods to the type they carry.
    if isinstance(to, JavaType.Variable):
        return _is_assignable_to_type(to.type, from_)
    if isinstance(to, JavaType.Method):
        return _is_assignable_to_type(to.return_type, from_)
    if isinstance(from_, JavaType.Variable):
        return _is_assignable_to_type(to, from_.type)
    if isinstance(from_, JavaType.Method):
        return _is_assignable_to_type(to, from_.return_type)

    if isinstance(to, JavaType.Primitive):
        return _is_assignable_to_primitive(to, from_)
    if isinstance(to, JavaType.Array):
        return isinstance(from_, JavaType.Array) and is_of_type(to.elem_type, from_.elem_type)
    if isinstance(to, JavaType.GenericTypeVariable):
        bounds = to.bounds
        if not bounds:
            return True
        return all(_is_assignable_to_type(bound, from_) for bound in bounds)

    # Reduce a fully-qualified ``to`` (including parameterized) to its raw name
    # and walk the ``from_`` hierarchy.
    to_fqn = _fqn(to)
    if to_fqn is not None:
        return _is_assignable_to_fqn(to_fqn, from_)
    return False


def _is_assignable_to_primitive(to: JavaType.Primitive, from_: JavaType | None) -> bool:
    if not isinstance(from_, JavaType.Primitive):
        return False
    if to is from_:
        return True
    return from_ in _PRIMITIVE_WIDENING.get(to, frozenset())
