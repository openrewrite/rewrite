namespace Rewrite.Core;

/// <summary>
/// A hierarchical marketplace of recipes organized by categories.
/// Recipes are installed into category paths, and the same recipe can appear
/// in multiple categories by calling <see cref="Install"/> multiple times.
/// </summary>
public class RecipeMarketplace
{
    private readonly Category _root = new(new CategoryDescriptor(
        "Root",
        "This is the root of all categories. When displaying the category hierarchy, this is typically not shown."
    ));

    /// <summary>
    /// Install a recipe into the marketplace under the specified category path.
    /// Categories are specified top-down (shallowest to deepest).
    /// Intermediate categories are created as needed.
    /// </summary>
    public void Install(Recipe recipe, params CategoryDescriptor[] categoryPath)
    {
        _root.Install(recipe, categoryPath);
    }

    /// <summary>
    /// Install a recipe by type. The recipe is instantiated via its parameterless constructor
    /// to extract its descriptor.
    /// </summary>
    public void Install<TRecipe>(params CategoryDescriptor[] categoryPath) where TRecipe : Recipe, new()
    {
        Install(new TRecipe(), categoryPath);
    }

    /// <summary>
    /// Install a recipe descriptor without a live recipe instance (for client-side hydration from RPC).
    /// </summary>
    public void Install(RecipeDescriptor descriptor, params CategoryDescriptor[] categoryPath)
    {
        _root.Install(descriptor, categoryPath);
    }

    /// <summary>
    /// Top-level categories (root's children).
    /// </summary>
    public IReadOnlyList<Category> Categories => _root.SubCategories;

    /// <summary>
    /// Find a recipe by its fully qualified name. Returns the descriptor and optionally the live recipe instance.
    /// </summary>
    public (RecipeDescriptor Descriptor, Recipe? Recipe)? FindRecipe(string name)
    {
        return _root.FindRecipe(name);
    }

    /// <summary>
    /// Recursively collect all recipe descriptors in the marketplace.
    /// </summary>
    public List<RecipeDescriptor> AllRecipes()
    {
        return _root.AllRecipes();
    }

    /// <summary>
    /// A node in the category hierarchy, containing subcategories and recipes.
    /// </summary>
    public class Category
    {
        public CategoryDescriptor Descriptor { get; }
        private readonly List<Category> _categories = [];
        private readonly Dictionary<RecipeDescriptor, Recipe?> _recipes = new();

        public Category(CategoryDescriptor descriptor)
        {
            Descriptor = descriptor;
        }

        public IReadOnlyList<Category> SubCategories => _categories;
        public IReadOnlyDictionary<RecipeDescriptor, Recipe?> Recipes => _recipes;

        internal void Install(Recipe recipe, ReadOnlySpan<CategoryDescriptor> categoryPath)
        {
            if (categoryPath.IsEmpty)
            {
                _recipes[recipe.GetDescriptor()] = recipe;
                return;
            }

            var target = FindOrCreateCategory(categoryPath[0]);
            target.Install(recipe, categoryPath[1..]);
        }

        internal void Install(RecipeDescriptor descriptor, ReadOnlySpan<CategoryDescriptor> categoryPath)
        {
            if (categoryPath.IsEmpty)
            {
                _recipes[descriptor] = null;
                return;
            }

            var target = FindOrCreateCategory(categoryPath[0]);
            target.Install(descriptor, categoryPath[1..]);
        }

        private Category FindOrCreateCategory(CategoryDescriptor descriptor)
        {
            foreach (var category in _categories)
            {
                if (category.Descriptor.DisplayName == descriptor.DisplayName)
                    return category;
            }

            var newCategory = new Category(descriptor);
            _categories.Add(newCategory);
            return newCategory;
        }

        public (RecipeDescriptor Descriptor, Recipe? Recipe)? FindRecipe(string name)
        {
            foreach (var (descriptor, recipe) in _recipes)
            {
                if (descriptor.Name == name)
                    return (descriptor, recipe);
            }

            foreach (var category in _categories)
            {
                var found = category.FindRecipe(name);
                if (found != null)
                    return found;
            }

            return null;
        }

        public List<RecipeDescriptor> AllRecipes()
        {
            var result = new List<RecipeDescriptor>(_recipes.Keys);
            foreach (var category in _categories)
            {
                result.AddRange(category.AllRecipes());
            }
            return result;
        }
    }
}
