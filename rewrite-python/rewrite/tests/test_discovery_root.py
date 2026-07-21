import rewrite.discovery as discovery
from rewrite import CategoryDescriptor, RecipeMarketplace, Recipe


def _recipe(name):
    class _R(Recipe):
        @property
        def name(self): return name
        @property
        def display_name(self): return name
        @property
        def description(self): return "r"
    return _R


class _Dist:
    def __init__(self, name): self.name = name


class _EP:
    def __init__(self, dist_name, recipe_name):
        self.dist = _Dist(dist_name)
        self._recipe_name = recipe_name

    def load(self):
        rn = self._recipe_name

        class _Mod:
            @staticmethod
            def activate(marketplace):
                marketplace.install(_recipe(rn), [CategoryDescriptor(display_name="Test")])
        return _Mod


class _AttributeEP:
    """An ``pkg = "pkg:activate"`` entry point: ``load()`` yields the function, not the module.

    This is the form the engine itself declares (``openrewrite = rewrite:activate``) and the form
    the discovery docstring advertises, so a bundle using it must still activate.
    """

    def __init__(self, dist_name, recipe_name):
        self.dist = _Dist(dist_name)
        self._recipe_name = recipe_name

    def load(self):
        rn = self._recipe_name

        def activate(marketplace):
            marketplace.install(_recipe(rn), [CategoryDescriptor(display_name="Test")])

        return activate


def _names(marketplace):
    return {n for cat in marketplace.categories() for n in cat.recipes.keys()}


def test_root_only_activation_hides_transitive_recipe_packages(monkeypatch):
    # Root bundle "root-pkg" plus a transitive recipe package "dep-pkg" both present in the venv.
    eps = [_EP("root-pkg", "org.example.Root"), _EP("dep-pkg", "org.example.Dep")]
    monkeypatch.setattr(discovery, "entry_points", lambda group: eps)

    marketplace = RecipeMarketplace()
    attribution = discovery.RecipeAttribution()
    discovery.discover_root_recipes("root_pkg", marketplace, attribution)  # normalized spelling

    assert _names(marketplace) == {"org.example.Root"}          # only the root's direct recipe
    assert attribution.package_for("org.example.Root") == "root-pkg"
    assert attribution.package_for("org.example.Dep") is None


def test_root_activation_supports_attribute_form_entry_points(monkeypatch):
    eps = [_AttributeEP("root-pkg", "org.example.Root")]
    monkeypatch.setattr(discovery, "entry_points", lambda group: eps)

    marketplace = RecipeMarketplace()
    attribution = discovery.RecipeAttribution()
    discovery.discover_root_recipes("root-pkg", marketplace, attribution)

    assert _names(marketplace) == {"org.example.Root"}
    assert attribution.package_for("org.example.Root") == "root-pkg"


def test_root_activation_attributes_to_the_supplied_name(monkeypatch):
    # A local install is filtered by its resolved distribution name but must be attributed to the
    # path the host supplied and keys the bundle by — not the distribution's own name.
    eps = [_AttributeEP("my-local-recipes", "org.example.Local")]
    monkeypatch.setattr(discovery, "entry_points", lambda group: eps)

    marketplace = RecipeMarketplace()
    attribution = discovery.RecipeAttribution()
    discovery.discover_root_recipes("my-local-recipes", marketplace, attribution,
                                    attribution_name="/abs/path/to/my-recipes")

    assert _names(marketplace) == {"org.example.Local"}
    assert attribution.package_for("org.example.Local") == "/abs/path/to/my-recipes"


def test_distribution_name_from_pyproject_and_setup_py(tmp_path):
    proj = tmp_path / "pep621"; proj.mkdir()
    (proj / "pyproject.toml").write_text('[project]\nname = "my-recipes"\nversion = "1.0"\n')
    assert discovery.distribution_name_from_source(proj) == "my-recipes"

    poetry = tmp_path / "poetry"; poetry.mkdir()
    (poetry / "pyproject.toml").write_text('[tool.poetry]\nname = "poetry-recipes"\n')
    assert discovery.distribution_name_from_source(poetry) == "poetry-recipes"

    legacy = tmp_path / "legacy"; legacy.mkdir()
    (legacy / "setup.py").write_text('setup(name="setup-recipes", version="1.0")\n')
    assert discovery.distribution_name_from_source(legacy) == "setup-recipes"

    assert discovery.distribution_name_from_source(tmp_path / "absent") is None
    (tmp_path / "empty").mkdir()
    assert discovery.distribution_name_from_source(tmp_path / "empty") is None


def test_flat_discovery_supports_attribute_form_entry_points(monkeypatch):
    # discover_recipes shares _activate_entry_point with discover_root_recipes, so the engine's own
    # `openrewrite = rewrite:activate` (function-form) and any recipe bundle's entry points activate
    # in the non-facade path too — not silently skipped as they were before.
    eps = [_AttributeEP("pkg-a", "org.example.A"), _EP("pkg-b", "org.example.B")]
    monkeypatch.setattr(discovery, "entry_points", lambda group: eps)

    marketplace = RecipeMarketplace()
    attribution = discovery.RecipeAttribution()
    discovery.discover_recipes(marketplace, attribution)

    assert _names(marketplace) == {"org.example.A", "org.example.B"}   # both forms activate
    assert attribution.package_for("org.example.A") == "pkg-a"
    assert attribution.package_for("org.example.B") == "pkg-b"
