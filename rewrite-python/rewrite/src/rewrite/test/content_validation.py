# Copyright 2026 the original author or authors.
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

"""Content validation for the recipe descriptors a package publishes.

Recipes reach the same marketplace and published docs whichever language they are written
in, so they follow the house style of ``RecipeMarketplaceContentValidator`` in rewrite-core.
Findings are returned, so a package asserts on them from its own test suite.
"""

from __future__ import annotations

import inspect
from dataclasses import Field, dataclass, is_dataclass
from types import ModuleType
from typing import Dict, Iterable, List, Optional, Sequence, Type, Union

from rewrite.marketplace import RecipeMarketplace
from rewrite.recipe import OptionDescriptor, Recipe, RecipeDescriptor

__all__ = ["ContentFinding", "validate_recipe_content"]


@dataclass(frozen=True)
class ContentFinding:
    """A single house-style violation on a recipe or option descriptor."""

    recipe: str
    property: str
    value: str
    message: str

    def __str__(self) -> str:
        return f"{self.property}: {self.message} (got {self.value!r})"


ValidationSource = Union[
    RecipeMarketplace,
    ModuleType,
    Type[Recipe],
    Iterable[Type[Recipe]],
]


def validate_recipe_content(
    source: ValidationSource,
    namespace: Optional[str] = None,
) -> List[ContentFinding]:
    """Validate the recipes a package publishes against the marketplace house style.

    ``source`` is a :class:`RecipeMarketplace`, a module whose ``@categorize``-decorated
    recipes are inspected, or one or more ``Recipe`` classes. ``namespace`` is the dotted
    prefix every recipe name must sit under. Only the package knows its namespace, so the
    check runs only when one is given.
    """
    entries = _entries(source)
    findings: List[ContentFinding] = []
    for entry in entries:
        findings.extend(entry.validate(namespace))
    findings.extend(_duplicate_findings(entries))
    return findings


class _Entry:
    """One candidate marketplace listing: a descriptor, the class behind it, or both."""

    def __init__(
        self,
        descriptor: Optional[RecipeDescriptor],
        recipe_class: Optional[Type[Recipe]],
        category_path: Sequence[str] = (),
    ):
        self.descriptor = descriptor
        self.recipe_class = recipe_class
        self.category_path = list(category_path)
        self._construction_error: Optional[BaseException] = None
        if descriptor is None and recipe_class is not None:
            try:
                self.descriptor = recipe_class().descriptor()
            except Exception as e:
                self._construction_error = e

    @property
    def name(self) -> str:
        if self.descriptor is not None:
            return self.descriptor.name
        return f"{self.recipe_class.__module__}.{self.recipe_class.__qualname__}"

    @property
    def _prefix(self) -> str:
        path = f" [{' > '.join(self.category_path)}]" if self.category_path else ""
        return f"{self.name}{path}"

    def _finding(self, prop: str, value: str, message: str) -> ContentFinding:
        return ContentFinding(self.name, f"{self._prefix}.{prop}", value, message)

    def validate(self, namespace: Optional[str]) -> List[ContentFinding]:
        if self._construction_error is not None:
            # A package's entry point activates by instantiating every recipe it registers,
            # so one that needs arguments takes the whole entry point down with it.
            return [self._finding(
                "name", self.name,
                f"Recipe must be instantiable with no arguments, but raised "
                f"{type(self._construction_error).__name__}: {self._construction_error}",
            )]

        assert self.descriptor is not None
        findings = self._validate_name(namespace)
        findings.extend(self._validate_display_name("displayName", self.descriptor.display_name))
        findings.extend(self._validate_description("description", self.descriptor.description))
        findings.extend(self._validate_orphaned_option_metadata())
        for field_name, _value, opt in self.descriptor.options:
            findings.extend(self._validate_option(field_name, opt))
        return findings

    def _validate_name(self, namespace: Optional[str]) -> List[ContentFinding]:
        findings: List[ContentFinding] = []
        name = self.descriptor.name
        if self.recipe_class is not None and not name.endswith("." + self.recipe_class.__name__):
            findings.append(self._finding(
                "name", name,
                f"Recipe name must end with the class name '{self.recipe_class.__name__}'",
            ))
        if namespace is not None and not name.startswith(namespace.rstrip(".") + "."):
            findings.append(self._finding(
                "name", name,
                f"Recipe name must start with the namespace '{namespace.rstrip('.')}.'",
            ))
        return findings

    def _validate_orphaned_option_metadata(self) -> List[ContentFinding]:
        """Report ``field(metadata=option(...))`` on a class that is not a dataclass.

        Such a field is a plain class attribute, so the descriptor carries no options at
        all and the recipe reaches the marketplace unconfigurable.
        """
        cls = self.recipe_class
        if cls is None or is_dataclass(cls):
            return []
        return [
            self._finding(
                f"options[{attr}]", attr,
                f"Option metadata on '{attr}' is ignored because the class is not a dataclass",
            )
            for attr, value in vars(cls).items()
            if isinstance(value, Field) and "option" in value.metadata
        ]

    def _validate_option(self, field_name: str, opt: OptionDescriptor) -> List[ContentFinding]:
        findings = self._validate_display_name(
            f"options[{field_name}].displayName", opt.display_name)
        findings.extend(self._validate_description(
            f"options[{field_name}].description", opt.description))
        return findings

    def _validate_display_name(self, prop: str, display_name: str) -> List[ContentFinding]:
        if not display_name:
            return [self._finding(prop, display_name, "Display name must not be empty")]
        findings: List[ContentFinding] = []
        if not _starts_with_uppercase(display_name):
            findings.append(self._finding(
                prop, display_name, "Display name must be sentence cased"))
        if display_name.endswith("."):
            findings.append(self._finding(
                prop, display_name, "Display name must not end with a period"))
        return findings

    def _validate_description(self, prop: str, description: str) -> List[ContentFinding]:
        if not description:
            return [self._finding(prop, description, "Description must not be empty")]
        findings: List[ContentFinding] = []
        if not _starts_with_uppercase(description):
            findings.append(self._finding(
                prop, description, "Description must be sentence cased"))
        if not description.endswith("."):
            findings.append(self._finding(
                prop, description, "Description must end with a period"))
        return findings


def _starts_with_uppercase(text: str) -> bool:
    """Whether ``text`` reads as sentence cased.

    Markdown that precedes the first word -- a backtick, a link bracket, a list dash --
    is accepted, so only a lowercase letter or leading whitespace fails.
    """
    first = text[0]
    return first.isupper() or (not first.isspace() and not first.isalpha())


def _duplicate_findings(entries: Sequence[_Entry]) -> List[ContentFinding]:
    """Report recipe names claimed by more than one entry.

    A marketplace keeps the first recipe installed under a name and drops the rest
    without complaint, so a collision is invisible from the marketplace it produced.
    """
    by_name: Dict[str, List[_Entry]] = {}
    for entry in entries:
        if entry.descriptor is not None:
            by_name.setdefault(entry.descriptor.name, []).append(entry)
    findings: List[ContentFinding] = []
    for name, colliding in by_name.items():
        if len(colliding) > 1:
            owners = ", ".join(_owner(e) for e in colliding)
            findings.append(colliding[0]._finding(
                "name", name, f"Recipe name is not unique; claimed by {owners}"))
    return findings


def _owner(entry: _Entry) -> str:
    if entry.recipe_class is not None:
        return f"{entry.recipe_class.__module__}.{entry.recipe_class.__qualname__}"
    return " > ".join(entry.category_path) or "<root>"


def _entries(source: ValidationSource) -> List[_Entry]:
    if isinstance(source, RecipeMarketplace):
        return _marketplace_entries(source.root, [])
    if isinstance(source, ModuleType):
        from rewrite.discovery import discover_decorated_recipes_in_module

        return [
            _Entry(None, cls, [c.display_name for c in category])
            for cls, category in discover_decorated_recipes_in_module(source)
            if not inspect.isabstract(cls)
        ]
    classes: Iterable[Type[Recipe]]
    classes = [source] if isinstance(source, type) else source
    # An abstract subclass has unimplemented members, so it is a building block for
    # recipes rather than a listing of its own.
    return [_Entry(None, cls) for cls in classes if not inspect.isabstract(cls)]


def _marketplace_entries(
    category: RecipeMarketplace.Category, path: List[str]
) -> List[_Entry]:
    entries = [_Entry(desc, cls, path) for desc, cls in category.recipes.values()]
    for child in category.categories:
        entries.extend(_marketplace_entries(child, path + [child.descriptor.display_name]))
    return entries
