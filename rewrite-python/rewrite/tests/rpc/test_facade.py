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
    # A local install arrives as a path string; the facade resolves the distribution name from the
    # source, keys the venv/child by it, and force-reinstalls the mutable source.
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


def test_facade_routes_print_and_getobject_to_the_child_that_visited_the_tree():
    children = _FakeChildren()
    f = Facade(children)
    f._bundle_by_visitor["edit:r"] = "pkg"

    # Before any visit (build time) there is no active child, so objects stay facade-local.
    assert f.tree_owner({"treeId": "T1"}) is None

    # A visit records treeId -> bundle and marks the active child. A later Print/GetObject reaches
    # the child that holds the modified tree and its RPC ref-state — by exact id, and (because a
    # modifying visit re-keys the tree and Java also fetches the context/cursors by id) by the
    # active-child fallback for ids the map never recorded.
    f.visit({"visitor": "edit:r", "treeId": "T1"})
    assert f.tree_owner({"treeId": "T1"}) == "pkg"          # Print carries 'treeId'
    assert f.tree_owner({"id": "T1"}) == "pkg"              # GetObject carries 'id'
    assert f.tree_owner({"treeId": "re-keyed-id"}) == "pkg" # untracked during a run -> active child
    assert f.route_to_child("pkg", "Print", {"treeId": "T1"}) == {"ok": "Print"}


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
    # tree ownership follows the last child to touch it (for a later Print/GetObject)
    assert f.tree_owner({"treeId": "T"}) == "B"


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
    """The facade owns the in-flight tree, so it answers Java itself instead of round-tripping to a
    child -- whose copy carries only its own bundle's edit. It also acquires at the top level: a
    fetch from inside a child's callback deadlocks (child waits on us, Java waits on this request)."""
    import rewrite.rpc.server as server

    class _FakeFacade:
        # facade_handlers is built eagerly from these; stubs keep the dict construction happy
        def get_marketplace(self, params): ...
        def install_recipes(self, params): ...
        def prepare_recipe(self, params): ...
        def set_data_table_store(self, params): ...
        def visit(self, params): ...
        def batch_visit(self, params): ...
        def generate(self, params): ...

        def route_to_child(self, bundle, method, params):
            raise AssertionError("Print/GetObject must never round-trip to a child")

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
    """Java fetches the execution context and cursors by id too, and those have no Python codec.

    `GetObject.sourceFileType` is nullable and set only for trees, so it is what distinguishes
    them. Acquiring a non-tree hands its property messages to a receiver that cannot consume
    them, which desynchronizes the queue for every object that follows -- observed end-to-end as
    "No RPC codec registered on the Python side for 'org.openrewrite.InMemoryExecutionContext'".
    """
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
    """Evict drops the refs a file introduced from the child's receive map. If the facade kept them
    in its send map it would later emit GET_REF for a ref the child no longer has -- the
    "Received reference to unknown object" flood."""
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
    """PrepareRecipe returns a Preconditions.check(...) editor: the precondition names a Python
    visitor of its own, which the host will later call back like any other visitor."""

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
    # A precondition's visitor is prepared inside the child but appears in neither editVisitor nor
    # recipeList. Without registering it, the host's Visit for it would find no owner.
    children = _PreconditionChildren()
    f = Facade(children)
    f.prepare_recipe({"id": "pkg.R"})

    assert f.visit({"visitor": "edit:precond-1"}) == {"ok": "Visit"}
    assert f.visit({"visitor": "scan:precond-2"}) == {"ok": "Visit"}
