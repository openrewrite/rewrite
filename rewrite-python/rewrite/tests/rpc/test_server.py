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
    import json
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
        # Simulate pip honoring --report by writing a minimal install report.
        report_path = cmd[cmd.index("--report") + 1]
        with open(report_path, "w") as f:
            json.dump({
                "install": [
                    {"download_info": {"url": "https://files.pythonhosted.org/packages/aa/bb/foo-1.0-py3-none-any.whl"}},
                    {"download_info": {"url": "https://internal.example.com/artifactory/api/pypi/pypi/simple/foo/foo-1.0.tar.gz"}},
                    {"download_info": {"url": "file:///local/wheel.whl"}},
                ]
            }, f)
        return FakeCompletedProcess()

    monkeypatch.setattr(subprocess, "run", fake_run)

    repos = server._pip_install_recipe_package(
        "openrewrite-recipes-python", "1.2.3", install_dir
    )

    assert install_dir.exists()
    assert captured["cmd"][1] == "-m"
    assert captured["cmd"][2:5] == ["pip", "install", "--target"]
    assert captured["cmd"][5] == str(install_dir)
    assert "--report" in captured["cmd"]
    assert captured["cmd"][-1] == "openrewrite-recipes-python==1.2.3"
    assert str(install_dir.resolve()) in __import__("sys").path
    # Local file:// download skipped; http(s) downloads reduced to origin.
    assert repos == {
        "https://files.pythonhosted.org",
        "https://internal.example.com",
    }


def test_parse_pip_install_report_handles_missing_or_malformed_file(tmp_path):
    import rewrite.rpc.server as server

    missing = tmp_path / "does-not-exist.json"
    assert server._parse_pip_install_report(str(missing)) == set()

    malformed = tmp_path / "malformed.json"
    malformed.write_text("{not valid json")
    assert server._parse_pip_install_report(str(malformed)) == set()

    empty_install = tmp_path / "empty.json"
    empty_install.write_text("{}")
    assert server._parse_pip_install_report(str(empty_install)) == set()


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
