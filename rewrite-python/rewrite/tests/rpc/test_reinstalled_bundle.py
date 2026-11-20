import os
import sys
import textwrap
import venv
from types import SimpleNamespace

from rewrite.rpc import venv_manager
from rewrite.rpc.bundle_children import BundleChildren

_MODULE = textwrap.dedent('''
    from rewrite import CategoryDescriptor, Recipe


    class _R(Recipe):
        @property
        def name(self): return "org.example.{name}"
        @property
        def display_name(self): return "r"
        @property
        def description(self): return "r"


    def activate(marketplace):
        marketplace.install(_R, [CategoryDescriptor(display_name="Test")])
''')


def _ops_installing(recipe_names):
    """The real venv operations, except that an install writes the next recipe name's module into
    the venv, the way pip re-copies a local source whose version is unchanged."""
    def install_into_venv(venv_dir, spec, force=False):
        site_packages = venv_manager.site_packages(venv_dir)
        (site_packages / "one_ep.py").write_text(_MODULE.format(name=recipe_names.pop(0)))
        dist_info = site_packages / "one_ep-0.1.0.dist-info"
        dist_info.mkdir(exist_ok=True)
        (dist_info / "METADATA").write_text("Metadata-Version: 2.1\nName: one-ep\nVersion: 0.1.0\n")
        (dist_info / "entry_points.txt").write_text("[openrewrite.recipes]\none = one_ep:activate\n")

    return SimpleNamespace(
        is_usable_venv=venv_manager.is_usable_venv,
        create_venv=lambda py, d, clear=False: venv.create(d, with_pip=False, symlinks=os.name != "nt"),
        install_into_venv=install_into_venv,
        installed_version=venv_manager.installed_version,
        remove_venv=venv_manager.remove_venv)


def test_a_reinstalled_bundle_serves_the_code_and_attribution_it_was_reinstalled_with(tmp_path):
    replaced = []
    children = BundleChildren(sys.executable, tmp_path / "venvs", upstream=lambda *a: None, activate=None,
                              venv_ops=_ops_installing(["Before", "After"]), on_child_replaced=replaced.append)
    try:
        children.install("one-ep", "/src/before", force=True, attribution_name="/src/before")
        rows = children.install("one-ep", "/src/after", force=True, attribution_name="/src/after")
    finally:
        children.shutdown()

    assert [(r["descriptor"]["name"], r["packageName"]) for r in rows] == [("org.example.After", "/src/after")]
    assert replaced == ["one_ep"]   # the ref maps mirroring the first child are dropped with it
