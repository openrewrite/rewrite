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
