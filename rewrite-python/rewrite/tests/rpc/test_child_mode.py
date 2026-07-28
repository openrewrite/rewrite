import rewrite.rpc.server as server


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
