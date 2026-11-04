from rewrite import CategoryDescriptor, RecipeMarketplace, Recipe
from rewrite.rpc.server import _collect_marketplace_rows


def _recipe(name: str, description: str):
    class _R(Recipe):
        @property
        def name(self): return name
        @property
        def display_name(self): return name
        @property
        def description(self): return description
    return _R


def test_registry_is_first_wins_on_duplicate_name():
    m = RecipeMarketplace()
    m.install(_recipe("org.example.X", "first"), [CategoryDescriptor(display_name="Test")])
    m.install(_recipe("org.example.X", "second"), [CategoryDescriptor(display_name="Test")])

    rows = _collect_marketplace_rows(m)
    row = next(r for r in rows if r["descriptor"]["name"] == "org.example.X")
    assert row["descriptor"]["description"] == "first"  # first registration wins, not overwritten
