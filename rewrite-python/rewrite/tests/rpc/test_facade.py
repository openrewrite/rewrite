from rewrite.rpc.facade import Facade


class _FakeChildren:
    def __init__(self):
        self.calls = []
        self.data_table_store = None
        self._owner = {"pkg.R": "pkg"}

    def set_data_table_store(self, params):
        self.data_table_store = params

    def install(self, bundle, spec, force=False, attribution_name=None):
        self.calls.append(("install", bundle, spec, force, attribution_name))
        return [{"descriptor": {"name": "pkg.R"}}]

    def resolved_version(self, bundle):
        return "1.0.5"          # what pip resolved — deliberately != the requested "1.0"

    def marketplace(self):
        return [{"descriptor": {"name": "pkg.R"}}]

    def owner(self, name):
        return self._owner.get(name)

    def request(self, bundle, method, params):
        self.calls.append(("request", bundle, method))
        if method == "PrepareRecipe":
            return {
                "id": "prep-1",
                "editVisitor": "edit:prep-1",
                "scanVisitor": "scan:prep-1",
                "recipeList": [{"id": "prep-2", "editVisitor": "edit:prep-2", "recipeList": []}],
            }
        if method == "BatchVisit":
            # record which visitors this child received, and echo one result per visitor
            vs = [v["visitor"] for v in params.get("visitors", [])]
            self.calls.append(("batch", bundle, vs))
            return {"results": [{"visitor": v} for v in vs]}
        return {"ok": method}


def test_facade_install_marketplace_prepare_visit_generate_routing():
    children = _FakeChildren()
    f = Facade(children)

    # install a package spec -> bundle keyed by package name; version reported is the RESOLVED one
    resp = f.install_recipes({"recipes": {"packageName": "pkg", "version": "1.0"}})
    assert resp == {"recipesInstalled": 1, "version": "1.0.5"}
    # registry spec: no force, no attribution override (recipes carry the distribution's own name)
    assert children.calls[0] == ("install", "pkg", "pkg==1.0", False, None)

    # marketplace served from the merged cache
    assert f.get_marketplace({}) == [{"descriptor": {"name": "pkg.R"}}]

    # prepare routes to the owning child and registers visitor/recipe ownership (root + nested)
    prep = f.prepare_recipe({"id": "pkg.R"})
    assert prep["id"] == "prep-1"

    # visit routes by visitor name to the child that prepared it (nested included)
    assert f.visit({"visitor": "edit:prep-1"}) == {"ok": "Visit"}
    assert f.visit({"visitor": "edit:prep-2"}) == {"ok": "Visit"}

    # generate routes by prepared-recipe id
    assert f.generate({"id": "prep-2"}) == {"ok": "Generate"}

    # SetDataTableStore is broadcast to the children (recipes emit rows there), not kept on the facade
    assert f.set_data_table_store({"kind": "CSV", "outputDir": "/out"}) is True
    assert children.data_table_store == {"kind": "CSV", "outputDir": "/out"}


def test_facade_installs_a_local_path_by_resolved_distribution_name(tmp_path):
    src = tmp_path / "my-recipes"
    src.mkdir()
    (src / "pyproject.toml").write_text('[project]\nname = "my-local-recipes"\nversion = "0.1.0"\n')

    children = _FakeChildren()
    f = Facade(children)
    resp = f.install_recipes({"recipes": str(src)})

    # filtered by the resolved dist name, but force-reinstalled and attributed to the supplied path
    assert children.calls[0] == ("install", "my-local-recipes", str(src), True, str(src))
    assert resp == {"recipesInstalled": 1, "version": "1.0.5"}


def test_facade_rejects_a_local_path_without_a_resolvable_name(tmp_path):
    empty = tmp_path / "no-metadata"
    empty.mkdir()
    f = Facade(_FakeChildren())
    try:
        f.install_recipes({"recipes": str(empty)})
        assert False, "expected a ValueError for an unresolvable local path"
    except ValueError as e:
        assert "distribution name" in str(e)


def test_facade_batch_visit_splits_consecutive_runs_across_bundles():
    children = _FakeChildren()
    f = Facade(children)
    # visitors owned by two different bundles; the scheduler batches them across recipes
    f._bundle_by_visitor.update({"edit:a1": "A", "edit:a2": "A", "edit:b1": "B"})

    resp = f.batch_visit({"treeId": "T", "visitors": [
        {"visitor": "edit:a1"}, {"visitor": "edit:a2"}, {"visitor": "edit:b1"}]})

    # per-visitor results returned in the original order
    assert resp == {"results": [{"visitor": "edit:a1"}, {"visitor": "edit:a2"}, {"visitor": "edit:b1"}]}
    # dispatched as maximal consecutive same-owner runs: A gets [a1,a2], then B gets [b1]
    batches = [c for c in children.calls if c[0] == "batch"]
    assert batches == [("batch", "A", ["edit:a1", "edit:a2"]), ("batch", "B", ["edit:b1"])]


def test_facade_batch_visit_re_splits_when_owners_alternate():
    children = _FakeChildren()
    f = Facade(children)
    f._bundle_by_visitor.update({"edit:a": "A", "edit:b": "B"})
    f.batch_visit({"treeId": "T", "visitors": [
        {"visitor": "edit:a"}, {"visitor": "edit:b"}, {"visitor": "edit:a"}]})
    batches = [c for c in children.calls if c[0] == "batch"]
    assert batches == [("batch", "A", ["edit:a"]), ("batch", "B", ["edit:b"]), ("batch", "A", ["edit:a"])]


class _EditingChildren(_FakeChildren):
    """Every BatchVisit reports that its visitors modified the tree."""
    def __init__(self, modified=True):
        super().__init__()
        self._modified = modified

    def request(self, bundle, method, params):
        if method == "BatchVisit":
            vs = [v["visitor"] for v in params.get("visitors", [])]
            self.calls.append(("batch", bundle, vs))
            return {"results": [{"visitor": v, "modified": self._modified} for v in vs]}
        if method == "Visit":
            self.calls.append(("visit", bundle, params.get("visitor")))
            return {"modified": self._modified}
        return super().request(bundle, method, params)


def test_cross_bundle_batch_pulls_each_bundles_edit_into_the_hub_in_order():
    children = _EditingChildren()
    pulls = []
    f = Facade(children, hub_pull=lambda ch, b, tid, sft: pulls.append((b, tid, sft)))
    f._bundle_by_visitor.update({"edit:a": "A", "edit:b": "B"})

    f.batch_visit({"treeId": "T", "sourceFileType": "py",
                   "visitors": [{"visitor": "edit:a"}, {"visitor": "edit:b"}]})

    # dispatched as consecutive same-owner runs
    assert [c for c in children.calls if c[0] == "batch"] == [
        ("batch", "A", ["edit:a"]), ("batch", "B", ["edit:b"])]
    # each run's edit is pulled into the facade's tree in order, so B starts from A's result
    assert pulls == [("A", "T", "py"), ("B", "T", "py")]


def test_single_visit_pulls_its_edit_into_the_hub():
    children = _EditingChildren()
    pulls = []
    f = Facade(children, hub_pull=lambda ch, b, tid, sft: pulls.append((b, tid, sft)))
    f._bundle_by_visitor["edit:a"] = "A"

    f.visit({"visitor": "edit:a", "treeId": "T", "sourceFileType": "py"})

    assert pulls == [("A", "T", "py")]


def test_single_visit_does_not_pull_when_nothing_changed():
    children = _EditingChildren(modified=False)
    pulls = []
    f = Facade(children, hub_pull=lambda ch, b, tid, sft: pulls.append((b, tid, sft)))
    f._bundle_by_visitor["edit:a"] = "A"

    f.visit({"visitor": "edit:a", "treeId": "T", "sourceFileType": "py"})

    assert pulls == []


def test_single_bundle_batch_still_pulls_its_edit_into_the_hub():
    children = _EditingChildren()
    pulls = []
    f = Facade(children, hub_pull=lambda ch, b, tid, sft: pulls.append((b, tid, sft)))
    f._bundle_by_visitor.update({"edit:a1": "A", "edit:a2": "A"})

    f.batch_visit({"treeId": "T", "sourceFileType": "py",
                   "visitors": [{"visitor": "edit:a1"}, {"visitor": "edit:a2"}]})

    # one run, but the facade still owns the tree so it pulls the result back
    assert [c for c in children.calls if c[0] == "batch"] == [("batch", "A", ["edit:a1", "edit:a2"])]
    assert pulls == [("A", "T", "py")]


def test_unmodified_run_is_not_pulled():
    children = _EditingChildren(modified=False)
    pulls = []
    f = Facade(children, hub_pull=lambda ch, b, tid, sft: pulls.append((b, tid, sft)))
    f._bundle_by_visitor.update({"edit:a": "A"})

    f.batch_visit({"treeId": "T", "visitors": [{"visitor": "edit:a"}]})

    assert pulls == []  # nothing changed -> nothing to fetch back


def test_handle_request_answers_print_from_the_facades_own_tree(monkeypatch, tmp_path):
    import rewrite.rpc.server as server

    class _FakeFacade:
        def get_marketplace(self, params): ...
        def install_recipes(self, params): ...
        def prepare_recipe(self, params): ...
        def set_data_table_store(self, params): ...
        def visit(self, params): ...
        def batch_visit(self, params): ...
        def generate(self, params): ...

    monkeypatch.setattr(server, "_recipe_install_dir", tmp_path)  # facade mode on
    monkeypatch.setattr(server, "_child_bundle", None)
    monkeypatch.setattr(server, "_facade", _FakeFacade())
    monkeypatch.setattr(server, "handle_print", lambda p: "local-print")

    # a tree the facade already holds is served from its own copy, no acquire needed
    acquired = []
    monkeypatch.setattr(server, "_hub_acquire", lambda oid, sft: acquired.append(oid))
    monkeypatch.setitem(server._hub_tree, "T1", object())
    assert server.handle_request("Print", {"treeId": "T1", "sourceFileType": "py"}) == "local-print"
    assert acquired == []

    # one it doesn't hold yet is acquired here, at the top level, then served locally
    assert server.handle_request("Print", {"treeId": "other", "sourceFileType": "py"}) == "local-print"
    assert acquired == ["other"]


def test_handle_request_does_not_acquire_non_tree_objects(monkeypatch, tmp_path):
    import rewrite.rpc.server as server

    class _FakeFacade:
        def get_marketplace(self, params): ...
        def install_recipes(self, params): ...
        def prepare_recipe(self, params): ...
        def set_data_table_store(self, params): ...
        def visit(self, params): ...
        def batch_visit(self, params): ...
        def generate(self, params): ...

    monkeypatch.setattr(server, "_recipe_install_dir", tmp_path)  # facade mode on
    monkeypatch.setattr(server, "_child_bundle", None)
    monkeypatch.setattr(server, "_facade", _FakeFacade())
    monkeypatch.setattr(server, "handle_get_object", lambda p: "local-get")

    acquired = []
    monkeypatch.setattr(server, "_hub_acquire", lambda oid, sft: acquired.append(oid))

    # The execution context: fetched by id, no sourceFileType.
    assert server.handle_request("GetObject", {"id": "ctx-1"}) == "local-get"
    assert acquired == []

    # A tree carries its type and is still acquired.
    assert server.handle_request("GetObject", {"id": "tree-1", "sourceFileType": "py"}) == "local-get"
    assert acquired == ["tree-1"]


def test_handle_request_routes_to_facade_in_facade_mode(monkeypatch, tmp_path):
    import rewrite.rpc.server as server

    routed = {}

    class _FakeFacade:
        def get_marketplace(self, params):
            routed["marketplace"] = True
            return ["facade-rows"]

        def install_recipes(self, params):
            routed["install"] = params
            return {"recipesInstalled": 0, "version": None}

        def prepare_recipe(self, params): ...
        def batch_visit(self, params): ...
        def set_data_table_store(self, params):
            routed["store"] = params
            return True
        def visit(self, params): ...
        def generate(self, params): ...

    monkeypatch.setattr(server, "_recipe_install_dir", tmp_path)  # facade mode on
    monkeypatch.setattr(server, "_child_bundle", None)
    monkeypatch.setattr(server, "_facade", _FakeFacade())

    assert server.handle_request("GetMarketplace", {}) == ["facade-rows"]
    # SetDataTableStore routes to the facade (which broadcasts to children) in facade mode
    assert server.handle_request("SetDataTableStore", {"kind": "CSV"}) is True
    assert routed["store"] == {"kind": "CSV"}
    assert routed.get("marketplace") is True

    r = {"recipes": {"packageName": "pkg"}}
    assert server.handle_request("InstallRecipes", r) == {"recipesInstalled": 0, "version": None}
    assert routed["install"] == r


def test_hub_release_rolls_each_childs_ref_table_back_in_lockstep():
    import rewrite.rpc.server as server

    server._hub_tree["T"] = object()
    server._hub_served[("A", "T")] = object()
    # child A had 2 refs before this file, then the file introduced refs 3 and 4
    server._hub_send_refs["A"] = {10: ("before-1", 1), 11: ("before-2", 2),
                                  12: ("from-file", 3), 13: ("from-file", 4)}
    server._hub_send_next["A"] = 4
    server._hub_send_checkpoint[("A", "T")] = 2

    server._hub_release("T")

    # only the refs this file introduced are dropped; the pre-file ones survive
    assert sorted(n for _, n in server._hub_send_refs["A"].values()) == [1, 2]
    assert server._hub_send_next["A"] == 2          # counter rewound so the next file re-ADDs
    assert "T" not in server._hub_tree
    assert ("A", "T") not in server._hub_served
    assert ("A", "T") not in server._hub_send_checkpoint


class _PreconditionChildren(_FakeChildren):
    def request(self, bundle, method, params):
        if method == "PrepareRecipe":
            return {
                "id": "prep-1",
                "editVisitor": "edit:prep-1",
                "editPreconditions": [{"visitorName": "edit:precond-1", "visitorOptions": None}],
                "scanPreconditions": [{"visitorName": "scan:precond-2", "visitorOptions": None}],
                "recipeList": [],
            }
        return super().request(bundle, method, params)


def test_precondition_visitors_route_to_the_child_that_prepared_them():
    children = _PreconditionChildren()
    f = Facade(children)
    f.prepare_recipe({"id": "pkg.R"})

    assert f.visit({"visitor": "edit:precond-1"}) == {"ok": "Visit"}
    assert f.visit({"visitor": "scan:precond-2"}) == {"ok": "Visit"}


# --- built-in (service) visitors -------------------------------------------------------------
#
# Built-in visitors like AutoFormatVisitor/AddImport belong to no recipe bundle, so instead of
# failing with "No child owns visitor" the facade runs them itself against the working tree.

_AUTO_FORMAT = "org.openrewrite.python.format.AutoFormatVisitor"
_ADD_IMPORT = "org.openrewrite.python.AddImport"


def _local_facade(children, ran):
    return Facade(
        children,
        local_visit=lambda items, params: [
            ran.append((i["visitor"], i.get("visitorOptions"), params.get("treeId")))
            or {"modified": True, "deleted": False, "hasNewMessages": False, "searchResultIds": []}
            for i in items
        ],
        is_local_visitor=lambda name: name in (_AUTO_FORMAT, _ADD_IMPORT),
    )


def test_single_visit_of_a_built_in_visitor_runs_on_the_facade():
    children = _EditingChildren()
    ran = []
    f = _local_facade(children, ran)

    resp = f.visit({"visitor": _ADD_IMPORT, "treeId": "T", "sourceFileType": "py",
                    "visitorOptions": {"module": "collections.abc", "name": "Iterable"}})

    assert resp == {"modified": True}
    # the options reach the visitor, and no child was asked to do anything
    assert ran == [(_ADD_IMPORT, {"module": "collections.abc", "name": "Iterable"}, "T")]
    assert children.calls == []


def test_batch_visit_interleaves_built_ins_with_bundle_owned_visitors_in_order():
    children = _EditingChildren()
    ran = []
    f = _local_facade(children, ran)
    f._bundle_by_visitor.update({"edit:a": "A", "edit:b": "B"})

    resp = f.batch_visit({"treeId": "T", "sourceFileType": "py", "visitors": [
        {"visitor": "edit:a"}, {"visitor": _AUTO_FORMAT}, {"visitor": "edit:b"}]})

    # one result per visitor, in the order the host asked for them
    assert len(resp["results"]) == 3
    assert [r.get("visitor") for r in resp["results"]] == ["edit:a", None, "edit:b"]
    # the built-in ran on the facade, between the two children
    assert ran == [(_AUTO_FORMAT, None, "T")]
    assert [c for c in children.calls if c[0] == "batch"] == [
        ("batch", "A", ["edit:a"]), ("batch", "B", ["edit:b"])]


def test_an_unknown_visitor_is_still_an_error():
    f = _local_facade(_EditingChildren(), [])
    try:
        f.visit({"visitor": "edit:nobody", "treeId": "T"})
        assert False, "expected a ValueError for a visitor no child owns"
    except ValueError as e:
        assert "No child owns visitor" in str(e)
