import sys

import pytest

import rewrite.rpc.server as server
from rewrite import CategoryDescriptor, Recipe
from rewrite.discovery import RecipeAttribution


class _Demo(Recipe):
    @property
    def name(self): return "org.example.Demo"

    @property
    def display_name(self): return "Demo"

    @property
    def description(self): return "d"


class _FakeOverlay:
    """Stands in for a bundle's module table, recording that activation ran inside it."""

    def __init__(self, dist):
        self.dist = dist
        self.entered = 0

    def active(self):
        overlay = self

        class _Ctx:
            def __enter__(self):
                overlay.entered += 1
                return overlay

            def __exit__(self, *exc):
                return False

        return _Ctx()


def test_a_host_discovers_only_the_bundles_installed_into_it(monkeypatch, tmp_path):
    """A host's marketplace holds exactly what its bundles contributed — never whatever the
    process happens to have on its path — and each bundle is activated under its own table."""
    activated = {}

    def fake_root(root_dist_name, marketplace=None, attribution=None, attribution_name=None):
        activated["root"] = root_dist_name
        activated["attribution_name"] = attribution_name
        marketplace.install(_Demo, [CategoryDescriptor(display_name="Test")])
        attribution.record(attribution_name or root_dist_name, {"org.example.Demo"})
        return marketplace

    monkeypatch.setattr("rewrite.discovery.discover_root_recipes", fake_root)
    monkeypatch.setattr("rewrite.discovery.discover_recipes",
                        lambda *a, **k: pytest.fail("a host discovers no ambient recipes"))
    monkeypatch.setattr(server, "_marketplace", None)
    monkeypatch.setattr(server, "_attribution", RecipeAttribution())
    monkeypatch.setattr(server, "_recipe_install_dir", tmp_path)   # bundle-host mode on
    monkeypatch.setattr(sys, "path", list(sys.path))

    overlay = _FakeOverlay("demo-recipes")
    rows = server.activate_bundle_in_process(overlay, "demo-recipes", None)

    assert overlay.entered == 1          # discovery ran inside the bundle's table
    assert activated["root"] == "demo-recipes"
    assert [row["descriptor"]["name"] for row in rows] == ["org.example.Demo"]
