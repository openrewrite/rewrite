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
    def fake_parse_python_source(source, path="<unknown>", relative_to=None, ty_client=None):
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
        "--target", str(install_dir),
        "openrewrite-recipes-python==1.2.3",
    ]
    assert str(install_dir.resolve()) in __import__("sys").path


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
