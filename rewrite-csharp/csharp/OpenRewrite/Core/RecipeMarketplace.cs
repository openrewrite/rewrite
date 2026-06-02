/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
using System.Reflection;

namespace OpenRewrite.Core;

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
    /// Discover and install all <see cref="Recipe"/> types in the given assembly that declare
    /// their marketplace placement via <see cref="CategoryAttribute"/> + <see cref="CategoryDescriptorAttribute"/>.
    /// <para>
    /// Attributes are read in declaration order. Every <see cref="CategoryAttribute"/> opens a new
    /// path bucket; subsequent <see cref="CategoryDescriptorAttribute"/> instances (until the next
    /// marker) form that path from root to leaf. Multiple <c>[Category, ...]</c> stacks on the same
    /// recipe install it under multiple paths. Recipes with no <c>[Category]</c> marker are skipped.
    /// </para>
    /// <para>
    /// Relies on <see cref="MemberInfo.GetCustomAttributesData"/> preserving metadata declaration
    /// order, which is the de-facto behavior of CoreCLR and Mono runtimes.
    /// </para>
    /// </summary>
    public void InstallAssembly(Assembly assembly)
    {
        Type[] types;
        try
        {
            types = assembly.GetTypes();
        }
        catch (ReflectionTypeLoadException ex)
        {
            types = ex.Types.Where(t => t != null).ToArray()!;
        }

        foreach (var type in types)
        {
            if (type.IsAbstract || type.IsInterface || !typeof(Recipe).IsAssignableFrom(type))
                continue;

            var paths = ReadCategoryPaths(type);
            if (paths.Count == 0)
                continue;

            if (type.GetConstructor(Type.EmptyTypes) == null)
            {
                throw new InvalidOperationException(
                    $"Recipe '{type.FullName}' is decorated with [Category] but has no parameterless constructor.");
            }

            var recipe = (Recipe)Activator.CreateInstance(type)!;
            foreach (var path in paths)
            {
                Install(recipe, path.ToArray());
            }
        }
    }

    private static readonly Dictionary<Type, CategoryDescriptor> _descriptorCache = new();

    private static List<List<CategoryDescriptor>> ReadCategoryPaths(Type recipeType)
    {
        var paths = new List<List<CategoryDescriptor>>();
        List<CategoryDescriptor>? current = null;

        foreach (var attrData in recipeType.GetCustomAttributesData())
        {
            var attrType = attrData.AttributeType;
            if (attrType == typeof(CategoryAttribute))
            {
                current = new List<CategoryDescriptor>();
                paths.Add(current);
                continue;
            }

            if (typeof(CategoryDescriptorAttribute).IsAssignableFrom(attrType))
            {
                if (current == null)
                {
                    throw new InvalidOperationException(
                        $"Recipe '{recipeType.FullName}' applies '{attrType.Name}' without a preceding [Category] marker. " +
                        $"Start each path with [Category, ...].");
                }
                current.Add(ResolveDescriptor(attrType));
            }
        }

        return paths;
    }

    private static CategoryDescriptor ResolveDescriptor(Type attrType)
    {
        if (_descriptorCache.TryGetValue(attrType, out var cached))
            return cached;

        if (attrType.GetConstructor(Type.EmptyTypes) == null)
        {
            throw new InvalidOperationException(
                $"CategoryDescriptorAttribute subclass '{attrType.FullName}' must expose a parameterless constructor.");
        }

        var instance = (CategoryDescriptorAttribute)Activator.CreateInstance(attrType)!;
        _descriptorCache[attrType] = instance.Descriptor;
        return instance.Descriptor;
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
