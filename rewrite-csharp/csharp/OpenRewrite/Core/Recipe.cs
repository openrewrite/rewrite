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
using JetBrains.Annotations;
using OpenRewrite.Java;

namespace OpenRewrite.Core;

/// <summary>
/// Defines a transformation to be applied to source files.
/// Recipes provide a visitor that traverses and modifies LSTs.
/// </summary>
public abstract class Recipe
{
    private RecipeDescriptor? _descriptor;

    /// <summary>
    /// Unique name for this recipe, typically the fully qualified type name.
    /// </summary>
    public string Name => GetType().FullName ?? GetType().Name;

    [LanguageInjection("markdown")]
    public abstract string DisplayName { get; }

    [LanguageInjection("markdown")]
    public abstract string Description { get; }

    public virtual IReadOnlySet<string> Tags => new HashSet<string>();

    public virtual TimeSpan? EstimatedEffortPerOccurrence => TimeSpan.FromMinutes(5);

    public virtual List<Recipe> GetRecipeList() => [];

    public virtual ITreeVisitor<ExecutionContext> GetVisitor() => ITreeVisitor<ExecutionContext>.Noop();

    public RecipeDescriptor GetDescriptor()
    {
        return _descriptor ??= CreateRecipeDescriptor();
    }

    protected virtual RecipeDescriptor CreateRecipeDescriptor()
    {
        var options = GetOptionDescriptors();
        var recipeList = GetRecipeList()
            .Select(r => r.GetDescriptor())
            .ToList();

        return new RecipeDescriptor(
            Name, DisplayName, Description,
            Tags, EstimatedEffortPerOccurrence,
            options, recipeList
        );
    }

    private List<OptionDescriptor> GetOptionDescriptors()
    {
        var options = new List<OptionDescriptor>();
        foreach (var prop in GetType().GetProperties())
        {
            var attr = prop.GetCustomAttribute<OptionAttribute>();
            if (attr == null) continue;

            object? value = null;
            try { value = prop.GetValue(this); }
            catch { /* inaccessible property */ }

            options.Add(new OptionDescriptor(
                prop.Name,
                prop.PropertyType.Name,
                attr.DisplayName,
                attr.Description,
                string.IsNullOrEmpty(attr.Example) ? null : attr.Example,
                attr.Valid is { Length: > 0 } v && !(v.Length == 1 && v[0] == "")
                    ? v.ToList() : null,
                attr.Required,
                value
            ));
        }
        return options;
    }
}

/// <summary>
/// Marker interface for recipes that delegate entirely to a Java recipe.
/// When a recipe implements this interface, the Java host loads the recipe
/// locally instead of wrapping it in an RpcRecipe, eliminating per-file
/// RPC round trips.
/// </summary>
public interface IDelegatesTo
{
    /// <summary>
    /// The fully-qualified Java recipe name, e.g. "org.openrewrite.java.ChangeType".
    /// </summary>
    string JavaRecipeName { get; }

    /// <summary>
    /// Options to configure the Java recipe, keyed by option name.
    /// </summary>
    Dictionary<string, object?> Options { get; }
}

/// <summary>
/// Non-generic interface for scanning recipes, allowing the scheduler to
/// manage the scan/generate/edit lifecycle without knowing the accumulator type.
/// </summary>
public interface IScanningRecipe
{
    object InitialValue(ExecutionContext ctx);
    ITreeVisitor<ExecutionContext> Scanner(object acc);
    ITreeVisitor<ExecutionContext> Editor(object acc);
    IEnumerable<SourceFile> Generate(object acc, ExecutionContext ctx);
}

/// <summary>
/// A recipe that first scans source files to accumulate data, then transforms them.
/// </summary>
/// <typeparam name="TAccumulator">The type of the accumulator for scanning data.</typeparam>
public abstract class ScanningRecipe<TAccumulator> : Recipe, IScanningRecipe
{
    public abstract TAccumulator GetInitialValue(ExecutionContext ctx);

    public abstract ITreeVisitor<ExecutionContext> GetScanner(TAccumulator acc);

    public virtual ITreeVisitor<ExecutionContext> GetVisitor(TAccumulator acc) => ITreeVisitor<ExecutionContext>.Noop();

    public virtual IEnumerable<SourceFile> Generate(TAccumulator acc, ExecutionContext ctx) => [];

    public sealed override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        throw new InvalidOperationException(
            "ScanningRecipe.GetVisitor() should not be called directly.");
    }

    // IScanningRecipe explicit implementation — type-erased bridge for the scheduler
    object IScanningRecipe.InitialValue(ExecutionContext ctx) => GetInitialValue(ctx)!;
    ITreeVisitor<ExecutionContext> IScanningRecipe.Scanner(object acc) => GetScanner((TAccumulator)acc);
    ITreeVisitor<ExecutionContext> IScanningRecipe.Editor(object acc) => GetVisitor((TAccumulator)acc);
    IEnumerable<SourceFile> IScanningRecipe.Generate(object acc, ExecutionContext ctx) => Generate((TAccumulator)acc, ctx);
}

/// <summary>
/// Represents the before and after state of a source file transformation.
/// </summary>
public record Result(SourceFile? Before, SourceFile? After);
