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

"""Tests for the secret flag on recipe options."""

from dataclasses import dataclass, field
from typing import Any

from rewrite import Recipe, OptionDescriptor, option
from rewrite.rpc.server import _recipe_descriptor_to_dict


@dataclass
class RecipeWithSecret(Recipe):
    """A recipe with a secret option."""

    api_token: str = field(
        default="",
        metadata=option(
            display_name="API token",
            description="API token used by the recipe.",
            secret=True,
        ),
    )

    @property
    def name(self) -> str:
        return "org.example.RecipeWithSecret"

    @property
    def display_name(self) -> str:
        return "Recipe with secret"

    @property
    def description(self) -> str:
        return "Recipe with secret."


class TestSecretOption:
    """Tests for the secret flag on OptionDescriptor."""

    def test_option_descriptor_defaults_to_non_secret(self):
        desc = OptionDescriptor(display_name="x", description="x")
        assert desc.secret is False

    def test_option_factory_can_mark_secret(self):
        meta = option(display_name="API token", description="API token.", secret=True)
        desc: OptionDescriptor = meta["option"]
        assert desc.secret is True

    def test_recipe_field_with_secret_option_exposes_flag(self):
        recipe = RecipeWithSecret(api_token="hunter2")
        recipe_descriptor = recipe.descriptor()
        # options is List[tuple[str, Any, OptionDescriptor]]
        name, value, opt = recipe_descriptor.options[0]
        assert name == "api_token"
        assert value == "hunter2"
        assert opt.secret is True
        # Raw value is preserved on the Python-side descriptor; redaction is a
        # persistence-boundary concern, not a source-level concern.

    def test_rpc_serialization_includes_secret_flag(self):
        """The Java peer must learn that an option is secret over the wire.

        `_recipe_descriptor_to_dict` is the canonical Python -> JSON serializer
        consumed by the Java-side RPC bridge."""
        recipe = RecipeWithSecret(api_token="hunter2")
        d: dict[str, Any] = _recipe_descriptor_to_dict(recipe.descriptor())

        opt = d["options"][0]
        assert opt["name"] == "api_token"
        assert opt["secret"] is True
        # The Python serializer ships the raw value; the Java side is responsible
        # for redaction at its persistence boundaries (RecipeMarketplaceWriter,
        # moderne-cli trace.json, moderne-saas event log).
        assert opt["value"] == "hunter2"
