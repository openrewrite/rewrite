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


def test_recipe_descriptor_to_dict_emits_all_collection_keys():
    """Java's RecipeDescriptor.getXxx() collection-valued getters are
    treated as never-null by callers. Always emit the collection keys —
    including the ones the Python dataclass doesn't model — so Jackson on
    the Java side never leaves them null."""
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
