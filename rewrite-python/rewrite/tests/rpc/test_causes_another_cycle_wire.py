# Copyright 2026 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Older Java hosts reject unknown PrepareRecipe response fields, so ``causesAnotherCycle``
must only appear when the host asked for it."""

import pytest

from rewrite import Recipe, RecipeMarketplace
from rewrite.marketplace import Python


class _CausesAnotherCycle(Recipe):
    @property
    def name(self):
        return "test.cycle.CausesAnotherCycle"

    @property
    def display_name(self):
        return "Causes another cycle"

    @property
    def description(self):
        return "Causes another cycle."

    @property
    def causes_another_cycle(self) -> bool:
        return True


class _Plain(Recipe):
    @property
    def name(self):
        return "test.cycle.Plain"

    @property
    def display_name(self):
        return "Plain"

    @property
    def description(self):
        return "Does not cause another cycle."


@pytest.fixture
def server():
    import rewrite.rpc.server as server

    saved_marketplace = server._marketplace
    server._marketplace = RecipeMarketplace()
    server._marketplace.install(_CausesAnotherCycle, Python)
    server._marketplace.install(_Plain, Python)
    try:
        yield server
    finally:
        server._marketplace = saved_marketplace
        server._prepared_recipes.clear()
        server._prepared_editor_overrides.clear()
        server._prepared_edit_preconditions.clear()


def test_omitted_for_hosts_that_did_not_ask(server):
    response = server.handle_prepare_recipe({"id": "test.cycle.CausesAnotherCycle"})
    assert "causesAnotherCycle" not in response


def test_omitted_when_false(server):
    response = server.handle_prepare_recipe({"id": "test.cycle.Plain", "acceptsCausesAnotherCycle": True})
    assert "causesAnotherCycle" not in response


def test_sent_when_requested(server):
    response = server.handle_prepare_recipe(
        {"id": "test.cycle.CausesAnotherCycle", "acceptsCausesAnotherCycle": True})
    assert response["causesAnotherCycle"] is True
