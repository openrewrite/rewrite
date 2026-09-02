import sys

import pytest

import rewrite.rpc.server as server
from rewrite.discovery import RecipeAttribution


def test_child_mode_scopes_discovery_to_the_root_bundle(monkeypatch):
    called = {}

    def fake_root(root_dist_name, marketplace=None, attribution=None, attribution_name=None):
        called["root"] = root_dist_name
        called["attribution_name"] = attribution_name
        return marketplace

    def fake_flat(*a, **k):
        called["flat"] = True

    monkeypatch.setattr("rewrite.discovery.discover_root_recipes", fake_root)
    monkeypatch.setattr("rewrite.discovery.discover_recipes", fake_flat)
    monkeypatch.setattr(server, "_marketplace", None)
    monkeypatch.setattr(server, "_child_bundle", "my-recipes")

    server._get_marketplace()

    assert called.get("root") == "my-recipes"   # root-scoped discovery
    assert "flat" not in called                 # flat discovery + built-in activate skipped


def test_a_hosted_bundle_activates_from_its_venv_into_the_facades_marketplace(monkeypatch, tmp_path):
    """A facade's marketplace holds exactly the bundles installed into it, the way a child's holds
    exactly ``--child-bundle`` — nothing ambient, and the bundle's venv is what makes it reachable.
    """
    from rewrite import CategoryDescriptor, Recipe

    class _Demo(Recipe):
        @property
        def name(self): return "org.example.Demo"
        @property
        def display_name(self): return "Demo"
        @property
        def description(self): return "d"

    def fake_root(root_dist_name, marketplace=None, attribution=None, attribution_name=None):
        marketplace.install(_Demo, [CategoryDescriptor(display_name="Test")])
        attribution.record(attribution_name or root_dist_name, {"org.example.Demo"})
        return marketplace

    monkeypatch.setattr("rewrite.discovery.discover_root_recipes", fake_root)
    monkeypatch.setattr("rewrite.discovery.discover_recipes",
                        lambda *a, **k: pytest.fail("a facade discovers no ambient recipes"))
    monkeypatch.setattr(server, "_marketplace", None)
    monkeypatch.setattr(server, "_attribution", RecipeAttribution())
    monkeypatch.setattr(server, "_child_bundle", None)
    monkeypatch.setattr(server, "_recipe_install_dir", tmp_path)   # facade mode on

    site_packages = tmp_path / "venvs" / "demo" / "lib" / "python3.12" / "site-packages"
    site_packages.mkdir(parents=True)
    monkeypatch.setattr(sys, "path", list(sys.path))

    rows = server.activate_bundle_in_process(tmp_path / "venvs" / "demo", "demo-recipes", None)

    assert str(site_packages) in sys.path      # the bundle's venv is how its recipes are importable
    assert [row["descriptor"]["name"] for row in rows] == ["org.example.Demo"]
