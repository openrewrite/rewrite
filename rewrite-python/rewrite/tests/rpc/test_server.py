def test_require_tree_rejects_dict_fallback():
    """A top-level SourceFile must be a Tree. The receiver returns a generic
    ``{'kind': value_type, ...}`` dict for value_types it has no codec for —
    that fallback is fine for nested fragments but lets unknown source files
    leak into the visitor framework, where they crash with
    ``AttributeError: 'dict' object has no attribute 'is_acceptable'``.
    Verify the guard converts that into the same "No RPC codec" error the
    ADD path raises so the failure mode is consistent."""
    import pytest
    from rewrite.rpc.server import _require_tree

    with pytest.raises(RuntimeError, match="No RPC codec registered"):
        _require_tree(
            {"kind": "org.openrewrite.docker.tree.Docker$Document"},
            "org.openrewrite.docker.tree.Docker$Document",
        )

    with pytest.raises(RuntimeError, match="No RPC codec registered"):
        _require_tree(None, "org.openrewrite.text.PlainText")


def test_handle_parse_preserves_empty_text(tmp_path, monkeypatch):
    import rewrite.rpc.server as server

    observed = {}

    # given
    def fake_parse_python_source(source, path="<unknown>", relative_to=None, ty_client=None, **_):
        observed["source"] = source
        observed["path"] = path
        return {"id": "empty-file"}

    monkeypatch.setattr(server, "parse_python_source", fake_parse_python_source)

    # when
    result = server.handle_parse(
        {
            "relativeTo": str(tmp_path),
            "inputs": [{"text": "", "sourcePath": "pkg/__init__.py"}],
        }
    )

    # then
    assert result == ["empty-file"]
    assert observed["source"] == ""
    assert observed["path"] == str(tmp_path / "pkg" / "__init__.py")
    assert (tmp_path / "pkg" / "__init__.py").read_text(encoding="utf-8") == ""


def test_pip_install_recipe_package_shape(tmp_path, monkeypatch):
    import rewrite.rpc.server as server
    import subprocess

    install_dir = tmp_path / "recipes"
    captured = {}

    class FakeCompletedProcess:
        returncode = 0
        stdout = ""
        stderr = ""

    def fake_run(cmd, capture_output=False, text=False):
        captured["cmd"] = cmd
        return FakeCompletedProcess()

    monkeypatch.setattr(subprocess, "run", fake_run)

    server._pip_install_recipe_package(
        "openrewrite-recipes-python", "1.2.3", install_dir
    )

    assert install_dir.exists()
    assert captured["cmd"][1:] == [
        "-m", "pip", "install",
        "--upgrade",
        "--target", str(install_dir),
        "openrewrite-recipes-python==1.2.3",
    ]
    assert str(install_dir.resolve()) in __import__("sys").path


def test_pip_install_recipe_package_passes_upgrade(tmp_path, monkeypatch):
    # `pip install --target` refuses to replace an already-populated package
    # directory without --upgrade, silently leaving stale files from a prior
    # version. Recipe versions are immutable, so reinstalling a version *change*
    # into the shared install dir must overwrite cleanly.
    import subprocess

    import rewrite.rpc.server as server

    install_dir = tmp_path / "recipes"
    captured = {}

    class FakeCompletedProcess:
        returncode = 0
        stdout = ""
        stderr = ""

    def fake_run(cmd, capture_output=False, text=False):
        captured["cmd"] = cmd
        return FakeCompletedProcess()

    monkeypatch.setattr(subprocess, "run", fake_run)

    server._pip_install_recipe_package(
        "openrewrite-recipes-python", "1.2.3", install_dir
    )

    assert "--upgrade" in captured["cmd"]


def test_pip_install_recipe_package_comparator_spec(tmp_path, monkeypatch):
    import rewrite.rpc.server as server
    import subprocess

    install_dir = tmp_path / "recipes"

    class FakeCompletedProcess:
        returncode = 0
        stdout = ""
        stderr = ""

    for version, expected_spec in [
        (">=1.0", "openrewrite-recipes-python>=1.0"),
        ("~=1.4", "openrewrite-recipes-python~=1.4"),
    ]:
        captured = {}

        def fake_run(cmd, capture_output=False, text=False):
            captured["cmd"] = cmd
            return FakeCompletedProcess()

        monkeypatch.setattr(subprocess, "run", fake_run)

        server._pip_install_recipe_package(
            "openrewrite-recipes-python", version, install_dir
        )

        assert captured["cmd"][1:] == [
            "-m", "pip", "install",
            "--upgrade",
            "--target", str(install_dir),
            expected_spec,
        ], f"version {version!r}: expected spec {expected_spec!r}"


def test_pip_install_local_path_shape(tmp_path, monkeypatch):
    # Local recipe sources are mutable, so --force-reinstall picks up changed
    # content even when the version is unchanged, and --upgrade lets pip replace
    # the existing files under --target. `pip install <path>` also resolves and
    # installs the local package's dependencies — local sources don't carry them.
    import subprocess

    import rewrite.rpc.server as server

    install_dir = tmp_path / "recipes"
    local = tmp_path / "my-recipe"
    captured = {}

    class FakeCompletedProcess:
        returncode = 0
        stdout = ""
        stderr = ""

    def fake_run(cmd, capture_output=False, text=False):
        captured["cmd"] = cmd
        return FakeCompletedProcess()

    monkeypatch.setattr(subprocess, "run", fake_run)

    server._pip_install_local_path(local, install_dir)

    assert install_dir.exists()
    assert captured["cmd"][1:] == [
        "-m", "pip", "install",
        "--force-reinstall", "--upgrade",
        "--target", str(install_dir),
        str(local),
    ]
    assert str(install_dir.resolve()) in __import__("sys").path


def test_handle_install_recipes_local_path_installs_with_deps(tmp_path, monkeypatch):
    # A local-path install must pip-install the path (resolving its deps) when a
    # recipe-install dir is configured. A Python source dir doesn't carry its
    # dependencies, so importing it directly would fail for any recipe with
    # third-party deps — unlike the prepackaged npm/NuGet local artifacts.
    import subprocess

    import rewrite.rpc.server as server
    from rewrite.discovery import RecipeAttribution
    from rewrite.marketplace import RecipeMarketplace

    monkeypatch.setattr(server, "_marketplace", RecipeMarketplace())
    monkeypatch.setattr(server, "_attribution", RecipeAttribution())
    monkeypatch.setattr(server, "_recipe_install_dir", tmp_path / "install")

    captured = {}

    class FakeCompletedProcess:
        returncode = 0
        stdout = ""
        stderr = ""

    def fake_run(cmd, capture_output=False, text=False):
        captured["cmd"] = cmd
        return FakeCompletedProcess()

    monkeypatch.setattr(subprocess, "run", fake_run)

    # A bare directory (no pyproject/setup) makes _find_package_name return None,
    # so activation is skipped — but the install must still have run.
    local = tmp_path / "my-recipe"
    local.mkdir()

    server.handle_install_recipes({"recipes": str(local)})

    assert "cmd" in captured, "expected a pip install for a local-path recipe"
    assert "--force-reinstall" in captured["cmd"]
    assert str(local) in captured["cmd"]


def test_handle_install_recipes_local_path_attributes_to_the_supplied_path(monkeypatch, tmp_path):
    # A local install is attributed to the supplied path (the identity the host keys the bundle by),
    # not the resolved distribution name — matching the facade's local install.
    import rewrite.rpc.server as server
    from rewrite import CategoryDescriptor, Recipe
    from rewrite.discovery import RecipeAttribution
    from rewrite.marketplace import RecipeMarketplace

    class _Sample(Recipe):
        @property
        def name(self): return "org.local.Sample"
        @property
        def display_name(self): return "Sample"
        @property
        def description(self): return "s"

    attribution = RecipeAttribution()
    monkeypatch.setattr(server, "_marketplace", RecipeMarketplace())
    monkeypatch.setattr(server, "_attribution", attribution)
    monkeypatch.setattr(server, "_recipe_install_dir", None)          # no pip; just activate
    monkeypatch.setattr(server, "_find_package_name", lambda p: "sample-dist")
    monkeypatch.setattr(server, "_import_and_activate_package",
                        lambda name, mkt, local_path=None: mkt.install(_Sample, [CategoryDescriptor(display_name="Local")]))

    supplied = str(tmp_path / "my-recipe-src")
    server.handle_install_recipes({"recipes": supplied})

    assert attribution.package_for("org.local.Sample") == supplied   # the path, not "sample-dist"


def test_recipe_descriptor_to_dict_emits_all_collection_keys():
    from rewrite.recipe import RecipeDescriptor
    from rewrite.rpc.server import _recipe_descriptor_to_dict

    descriptor = RecipeDescriptor(
        name="org.example.Foo",
        display_name="Foo",
        description="A recipe.",
        tags=[],
        estimated_effort_per_occurrence=0,
        options=[],
        data_tables=[],
        recipe_list=[],
    )

    result = _recipe_descriptor_to_dict(descriptor)

    for key in ("tags", "options", "preconditions", "recipeList",
                "dataTables", "maintainers", "contributors", "examples"):
        assert key in result, f"missing key: {key}"
        assert result[key] == [], f"{key} should be empty list, got {result[key]!r}"


def test_get_marketplace_row_carries_package_name(monkeypatch):
    """A GetMarketplace row is tagged with the package that contributed the recipe,
    so the host attributes it to its own bundle instead of the requested one."""
    import rewrite.rpc.server as server
    from rewrite.discovery import RecipeAttribution
    from rewrite import CategoryDescriptor, RecipeMarketplace, Recipe

    class _MyRecipe(Recipe):
        @property
        def name(self): return "org.example.MyRecipe"
        @property
        def display_name(self): return "My Recipe"
        @property
        def description(self): return "A recipe."

    marketplace = RecipeMarketplace()
    marketplace.install(_MyRecipe, [CategoryDescriptor(display_name="Test")])

    attribution = RecipeAttribution()
    attribution.record("my-recipes-package", {"org.example.MyRecipe"})
    monkeypatch.setattr(server, "_attribution", attribution)

    rows = server._collect_marketplace_rows(marketplace)

    row = next(r for r in rows if r["descriptor"]["name"] == "org.example.MyRecipe")
    assert row["packageName"] == "my-recipes-package"


def test_handle_install_recipes_local_path_attributes_to_the_path(tmp_path, monkeypatch):
    """A local-path install attributes its recipes to the install path (the host's bundle identity),
    not the distribution name — the host's marketplace filter keys on that path."""
    import rewrite.rpc.server as server
    from rewrite.discovery import RecipeAttribution
    from rewrite import CategoryDescriptor, RecipeMarketplace, Recipe

    class _MyRecipe(Recipe):
        @property
        def name(self): return "org.example.MyRecipe"
        @property
        def display_name(self): return "My Recipe"
        @property
        def description(self): return "A recipe."

    marketplace = RecipeMarketplace()
    monkeypatch.setattr(server, "_marketplace", marketplace)
    monkeypatch.setattr(server, "_attribution", RecipeAttribution())
    monkeypatch.setattr(server, "_recipe_install_dir", None)
    # The distribution name deliberately differs from the install path — attributing to it was the bug.
    monkeypatch.setattr(server, "_find_package_name", lambda path: "some-distribution-name")
    monkeypatch.setattr(server, "_import_and_activate_package",
                        lambda pkg, mkt, local_path=None: mkt.install(_MyRecipe, [CategoryDescriptor(display_name="Test")]))

    local = tmp_path / "my-recipe"
    local.mkdir()

    response = server.handle_install_recipes({"recipes": str(local)})

    assert response["recipesInstalled"] == 1
    # Attributed to the path, not the distribution name.
    assert server._attribution.package_for("org.example.MyRecipe") == str(local)
    # And GetMarketplace surfaces that path as the row's origin, so the host keeps the recipe.
    rows = server._collect_marketplace_rows(marketplace)
    row = next(r for r in rows if r["descriptor"]["name"] == "org.example.MyRecipe")
    assert row["packageName"] == str(local)


def test_get_marketplace_row_unattributed_has_none_package_name(monkeypatch):
    """An unattributed recipe (e.g. a built-in the server recorded no origin for)
    leaves packageName None so the host falls back to the requested bundle."""
    import rewrite.rpc.server as server
    from rewrite.discovery import RecipeAttribution
    from rewrite import CategoryDescriptor, RecipeMarketplace, Recipe

    class _Unattributed(Recipe):
        @property
        def name(self): return "org.example.Unattributed"
        @property
        def display_name(self): return "Unattributed"
        @property
        def description(self): return "A recipe."

    marketplace = RecipeMarketplace()
    marketplace.install(_Unattributed, [CategoryDescriptor(display_name="Test")])
    monkeypatch.setattr(server, "_attribution", RecipeAttribution())

    rows = server._collect_marketplace_rows(marketplace)

    row = next(r for r in rows if r["descriptor"]["name"] == "org.example.Unattributed")
    assert row["packageName"] is None


def test_prepare_recipe_rejects_missing_required_option(monkeypatch):
    """The server validates required options when preparing a recipe, rather than silently
    preparing a broken recipe."""
    import rewrite.rpc.server as server
    from rewrite.marketplace import RecipeMarketplace
    from rewrite import Recipe, CategoryDescriptor
    from rewrite.recipe import option
    from dataclasses import dataclass, field

    @dataclass
    class _RequiresText(Recipe):
        text: str = field(default=None, metadata=option(
            display_name="Text", description="Required text."))

        @property
        def name(self): return "org.example.RequiresText"
        @property
        def display_name(self): return "Requires text"
        @property
        def description(self): return "A recipe with a required option."

    marketplace = RecipeMarketplace()
    marketplace.install(_RequiresText, [CategoryDescriptor(display_name="Test")])
    monkeypatch.setattr(server, "_marketplace", marketplace)

    try:
        server.handle_prepare_recipe({"id": "org.example.RequiresText"})
        assert False, "expected a missing-required-option error"
    except ValueError as e:
        assert "Missing required option `text`" in str(e)


def test_prepare_recipe_validates_required_options_of_children(monkeypatch):
    """Validation recurses through the whole prepared tree (like the C# and JS servers), so a
    composite whose child is missing a required option is rejected."""
    import rewrite.rpc.server as server
    from rewrite.marketplace import RecipeMarketplace
    from rewrite import Recipe, CategoryDescriptor
    from rewrite.recipe import option
    from dataclasses import dataclass, field

    @dataclass
    class _RequiresText(Recipe):
        text: str = field(default=None, metadata=option(
            display_name="Text", description="Required text."))

        @property
        def name(self): return "org.example.ChildRequiresText"
        @property
        def display_name(self): return "Child requires text"
        @property
        def description(self): return "A child recipe with a required option."

    class _CompositeWithInvalidChild(Recipe):
        @property
        def name(self): return "org.example.CompositeWithInvalidChild"
        @property
        def display_name(self): return "Composite with an invalid child"
        @property
        def description(self): return "A composite whose child lacks a required option."

        def recipe_list(self):
            return [_RequiresText()]

    marketplace = RecipeMarketplace()
    marketplace.install(_CompositeWithInvalidChild, [CategoryDescriptor(display_name="Test")])
    monkeypatch.setattr(server, "_marketplace", marketplace)

    try:
        server.handle_prepare_recipe({"id": "org.example.CompositeWithInvalidChild"})
        assert False, "expected a missing-required-option error for the child"
    except ValueError as e:
        assert "Missing required option `text`" in str(e)


def test_prepare_recipe_returns_whole_child_tree(monkeypatch):
    """PrepareRecipe prepares and returns the whole tree (recipeList), so the host builds it
    locally instead of a round trip per child."""
    import rewrite.rpc.server as server
    from rewrite.marketplace import RecipeMarketplace
    from rewrite import Recipe, CategoryDescriptor

    class _Leaf(Recipe):
        @property
        def name(self): return "org.example.Leaf"
        @property
        def display_name(self): return "Leaf"
        @property
        def description(self): return "A leaf recipe."

    class _Composite(Recipe):
        @property
        def name(self): return "org.example.Composite"
        @property
        def display_name(self): return "Composite"
        @property
        def description(self): return "A composite recipe."

        def recipe_list(self):
            return [_Leaf()]

    marketplace = RecipeMarketplace()
    marketplace.install(_Composite, [CategoryDescriptor(display_name="Test")])
    monkeypatch.setattr(server, "_marketplace", marketplace)

    response = server.handle_prepare_recipe({"id": "org.example.Composite"})

    assert "recipeList" in response
    assert [c["descriptor"]["name"] for c in response["recipeList"]] == ["org.example.Leaf"]


def test_hub_release_rewinds_send_refs_in_lockstep_with_the_child():
    """A child drops its receive refs for a file when the broadcast Evict reaches it, so the facade
    must return its send-ref numbering to exactly the pre-file value. If the facade kept advancing,
    it would emit a GET_REF for a ref the child no longer holds; if it rewound while the child did
    not, it would reuse a number still bound to the old object and serve a wrong tree silently."""
    import rewrite.rpc.server as server

    bundle, first, second = "pkg", "file-1", "file-2"
    server._hub_send_refs[bundle] = {}
    server._hub_send_next[bundle] = 0

    # Serving the first file advances this child's numbering and records where it started.
    server._hub_send_checkpoint.setdefault((bundle, first), server._hub_send_next[bundle])
    server._hub_send_refs[bundle].update({"obj-a": (object(), 1), "obj-b": (object(), 2)})
    server._hub_send_next[bundle] = 2
    server._hub_served[(bundle, first)] = object()
    server._hub_tree[first] = object()

    server._hub_release(first)

    # Everything that file introduced is gone, and the counter is back where it began.
    assert server._hub_send_next[bundle] == 0
    assert server._hub_send_refs[bundle] == {}
    assert (bundle, first) not in server._hub_served
    assert (bundle, first) not in server._hub_send_checkpoint
    assert first not in server._hub_tree

    # So the next file reuses the same ref numbers rather than continuing past them.
    server._hub_send_checkpoint.setdefault((bundle, second), server._hub_send_next[bundle])
    assert server._hub_send_checkpoint[(bundle, second)] == 0
