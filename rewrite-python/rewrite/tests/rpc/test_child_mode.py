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


def _activate_demo_bundle(monkeypatch, tmp_path):
    """Host a bundle from ``tmp_path/venvs/demo`` whose one recipe is ``org.example.Demo``."""
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
    monkeypatch.setattr(sys, "path", [str(tmp_path), *sys.path])

    return server.activate_bundle_in_process(tmp_path / "venvs" / "demo", "demo-recipes", None)


def test_a_hosted_bundle_activates_from_its_venv_into_the_facades_marketplace(monkeypatch, tmp_path):
    site_packages = tmp_path / "venvs" / "demo" / "lib" / "python3.12" / "site-packages"
    site_packages.mkdir(parents=True)

    rows = _activate_demo_bundle(monkeypatch, tmp_path)

    assert str(site_packages) in sys.path      # the bundle's venv is how its recipes are importable
    assert str(tmp_path) not in sys.path
    assert [row["descriptor"]["name"] for row in rows] == ["org.example.Demo"]


def test_templates_see_a_hosted_bundles_packages_while_its_venv_exists(monkeypatch, tmp_path):
    import rewrite.python.ty_client as ty_client
    from rewrite.python.template import engine

    venvs = []

    class _RecordingTyClient:
        def __init__(self, virtual_env=None, **kwargs):
            venvs.append(virtual_env)

        def initialize(self, project_root):
            raise RuntimeError("only the venv ty would start in matters here")

    monkeypatch.setattr(ty_client, "TyTypesClient", _RecordingTyClient)
    monkeypatch.setattr(engine, "_installed_packages_venv", None)
    venv = tmp_path / "venvs" / "demo"
    venv.mkdir(parents=True)
    (venv / "pyvenv.cfg").write_text("home = /usr/bin\n")

    _activate_demo_bundle(monkeypatch, tmp_path)
    engine.TemplateEngine._parse_code("x = 1\n")

    (venv / "pyvenv.cfg").unlink()  # as uninstalling the bundle leaves it
    engine.TemplateEngine._parse_code("x = 1\n")

    assert venvs == [str(venv), None]
