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

"""Tests for recipe descriptor content validation."""

from dataclasses import dataclass, field

import pytest

from rewrite import Recipe, option
from rewrite.category import CategoryDescriptor
from rewrite.marketplace import Python, RecipeMarketplace
from rewrite.test import validate_recipe_content


def _recipe(cls_name, *, display_name="Remove unnecessary parentheses",
            description="Removes unnecessary parentheses from code.",
            name=None):
    """Build a concrete Recipe subclass with the given descriptor content."""
    ns = {
        "name": property(lambda self, n=name or f"org.openrewrite.python.{cls_name}": n),
        "display_name": property(lambda self: display_name),
        "description": property(lambda self: description),
    }
    return type(cls_name, (Recipe,), ns)


def _messages(findings):
    return [f.message for f in findings]


def test_well_formed_recipe_has_no_findings():
    @dataclass
    class Configured(Recipe):
        pattern: str = field(default="", metadata=option(
            display_name="Pattern",
            description="The pattern to search for.",
        ))

        @property
        def name(self) -> str:
            return "org.openrewrite.python.Configured"

        @property
        def display_name(self) -> str:
            return "Remove unnecessary parentheses"

        @property
        def description(self) -> str:
            return "Removes unnecessary parentheses from code."

    assert validate_recipe_content(Configured, namespace="org.openrewrite.python") == []


def test_display_name_must_not_end_with_a_period_but_description_must():
    findings = validate_recipe_content(_recipe(
        "Inverted",
        display_name="Remove unnecessary parentheses.",
        description="Removes unnecessary parentheses from code",
    ))
    assert _messages(findings) == [
        "Display name must not end with a period",
        "Description must end with a period",
    ]


@pytest.mark.parametrize("text", ["remove unnecessary parentheses", " Remove parentheses"])
def test_text_not_starting_with_a_capital_is_not_sentence_cased(text):
    findings = validate_recipe_content(_recipe("Cased", display_name=text))
    assert _messages(findings) == ["Display name must be sentence cased"]


def test_markup_before_the_first_word_is_sentence_cased():
    assert validate_recipe_content(_recipe("Markup", display_name="`Remove` parentheses")) == []


def test_empty_display_name_and_description_are_reported():
    findings = validate_recipe_content(_recipe("Blank", display_name="", description=""))
    assert _messages(findings) == [
        "Display name must not be empty",
        "Description must not be empty",
    ]


def test_option_display_name_and_description_are_validated():
    @dataclass
    class WithOptions(Recipe):
        pattern: str = field(default="", metadata=option(
            display_name="pattern.",
            description="the pattern to search for",
        ))

        @property
        def name(self) -> str:
            return "org.openrewrite.python.WithOptions"

        @property
        def display_name(self) -> str:
            return "Remove unnecessary parentheses"

        @property
        def description(self) -> str:
            return "Removes unnecessary parentheses from code."

    findings = validate_recipe_content(WithOptions)
    assert [f.property for f in findings] == [
        "org.openrewrite.python.WithOptions.options[pattern].displayName",
        "org.openrewrite.python.WithOptions.options[pattern].displayName",
        "org.openrewrite.python.WithOptions.options[pattern].description",
        "org.openrewrite.python.WithOptions.options[pattern].description",
    ]


def test_option_metadata_on_a_non_dataclass_is_dropped():
    class NotADataclass(Recipe):
        pattern: str = field(default="", metadata=option(
            display_name="Pattern",
            description="The pattern to search for.",
        ))

        @property
        def name(self) -> str:
            return "org.openrewrite.python.NotADataclass"

        @property
        def display_name(self) -> str:
            return "Remove unnecessary parentheses"

        @property
        def description(self) -> str:
            return "Removes unnecessary parentheses from code."

    assert _messages(validate_recipe_content(NotADataclass)) == [
        "Option metadata on 'pattern' is ignored because the class is not a dataclass"
    ]


def test_name_must_end_with_the_class_name_and_stay_inside_the_namespace():
    findings = validate_recipe_content(
        _recipe("Misnamed", name="org.example.python.SomethingElse"),
        namespace="org.openrewrite.python",
    )
    assert _messages(findings) == [
        "Recipe name must end with the class name 'Misnamed'",
        "Recipe name must start with the namespace 'org.openrewrite.python.'",
    ]


def test_two_recipes_sharing_a_name_collide():
    first = _recipe("First", name="org.openrewrite.python.Shared")
    second = _recipe("Second", name="org.openrewrite.python.Shared")
    findings = validate_recipe_content([first, second])
    assert "Recipe name is not unique" in _messages(findings)[-1]


def test_a_recipe_requiring_constructor_arguments_is_undiscoverable():
    class NeedsArgs(Recipe):
        def __init__(self, required):
            self.required = required

        @property
        def name(self) -> str:
            return "org.openrewrite.python.NeedsArgs"

        @property
        def display_name(self) -> str:
            return "Remove unnecessary parentheses"

        @property
        def description(self) -> str:
            return "Removes unnecessary parentheses from code."

    findings = validate_recipe_content(NeedsArgs)
    assert len(findings) == 1
    assert findings[0].message.startswith("Recipe must be instantiable with no arguments")


def test_marketplace_findings_name_the_category_path():
    marketplace = RecipeMarketplace()
    marketplace.install(
        _recipe("Categorized", display_name="remove parentheses"),
        [*Python, CategoryDescriptor(display_name="Cleanup")],
    )
    findings = validate_recipe_content(marketplace)
    assert [f.property for f in findings] == [
        "org.openrewrite.python.Categorized [Python > Cleanup].displayName"
    ]


def test_abstract_recipe_bases_are_not_marketplace_entries():
    class AbstractBase(Recipe):
        @property
        def display_name(self) -> str:
            return "Remove unnecessary parentheses"

    assert validate_recipe_content(AbstractBase) == []


def test_built_in_python_recipes_satisfy_the_house_style():
    import rewrite
    import rewrite.python.recipes

    marketplace = RecipeMarketplace()
    rewrite.activate(marketplace)
    findings = validate_recipe_content(marketplace, namespace="org.openrewrite.python")
    assert not findings, "\n".join(map(str, findings))

    findings = validate_recipe_content(rewrite.python.recipes, namespace="org.openrewrite.python")
    assert not findings, "\n".join(map(str, findings))
