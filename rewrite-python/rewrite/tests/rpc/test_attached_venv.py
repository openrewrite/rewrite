import os
import sys
import textwrap
import venv

from rewrite.rpc import venv_manager
from rewrite.rpc.bundle_children import BundleChildren
from rewrite.rpc.facade import Facade

_MODULE = textwrap.dedent('''
    from rewrite import CategoryDescriptor, Recipe


    def _recipe(recipe_name):
        class _R(Recipe):
            @property
            def name(self): return recipe_name
            @property
            def display_name(self): return recipe_name
            @property
            def description(self): return "r"
        return _R


    def activate_first(marketplace):
        marketplace.install(_recipe("org.example.First"), [CategoryDescriptor(display_name="Test")])


    def activate_second(marketplace):
        marketplace.install(_recipe("org.example.Second"), [CategoryDescriptor(display_name="Test")])
''')


def _prebuilt_venv(venv_dir):
    """A venv holding one distribution that declares two ``openrewrite.recipes`` entry points."""
    venv.create(venv_dir, with_pip=False, symlinks=os.name != "nt")
    site_packages = venv_manager._site_packages(venv_dir)
    (site_packages / "two_eps.py").write_text(_MODULE)
    dist_info = site_packages / "two_eps-0.1.0.dist-info"
    dist_info.mkdir()
    (dist_info / "METADATA").write_text("Metadata-Version: 2.1\nName: two-eps\nVersion: 0.1.0\n")
    (dist_info / "entry_points.txt").write_text(
        "[openrewrite.recipes]\nfirst = two_eps:activate_first\nsecond = two_eps:activate_second\n")


def test_an_attached_venv_lists_every_entry_point_of_its_distribution(tmp_path):
    src = tmp_path / "two-eps"
    src.mkdir()
    (src / "pyproject.toml").write_text('[project]\nname = "two-eps"\nversion = "0.1.0"\n')
    prebuilt = tmp_path / "prebuilt"
    _prebuilt_venv(prebuilt)

    children = BundleChildren(sys.executable, tmp_path / "venvs", upstream=lambda *a: None)
    try:
        resp = Facade(children).install_recipes({"recipes": str(src), "venv": str(prebuilt)})
        rows = children.marketplace()
    finally:
        children.shutdown()

    assert resp == {"recipesInstalled": 2, "version": "0.1.0"}
    assert sorted((r["descriptor"]["name"], r["packageName"]) for r in rows) == [
        ("org.example.First", str(src)), ("org.example.Second", str(src))]
    assert not (tmp_path / "venvs").exists()
