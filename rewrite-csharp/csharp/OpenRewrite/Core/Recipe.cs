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

    public virtual JavaVisitor<ExecutionContext> GetVisitor() => new();

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

    public virtual List<Result> Run(List<SourceFile> sources, ExecutionContext ctx)
    {
        var visitor = GetVisitor();
        return EditSources(sources, visitor, ctx);
    }

    protected static List<Result> EditSources(
        List<SourceFile> sources,
        JavaVisitor<ExecutionContext> visitor,
        ExecutionContext ctx)
    {
        var results = new List<Result>();
        foreach (var source in sources)
        {
            var after = visitor.Visit((Tree)source, ctx);
            if (after == null)
            {
                results.Add(new Result(source, null));
            }
            else if (after is SourceFile sf && !ReferenceEquals(source, after))
            {
                results.Add(new Result(source, sf));
            }
        }

        return results;
    }
}

/// <summary>
/// A recipe that first scans source files to accumulate data, then transforms them.
/// </summary>
/// <typeparam name="T">The type of the accumulator for scanning data.</typeparam>
public abstract class ScanningRecipe<T> : Recipe
{
    public abstract T GetInitialValue(ExecutionContext ctx);

    public abstract JavaVisitor<ExecutionContext> GetScanner(T acc);

    public virtual JavaVisitor<ExecutionContext> GetVisitor(T acc) => new();

    public virtual IEnumerable<SourceFile> Generate(T acc, ExecutionContext ctx) => [];

    public sealed override JavaVisitor<ExecutionContext> GetVisitor()
    {
        throw new InvalidOperationException(
            "ScanningRecipe.GetVisitor() should not be called directly. Use Run() instead.");
    }

    public override List<Result> Run(List<SourceFile> sources, ExecutionContext ctx)
    {
        var acc = GetInitialValue(ctx);

        // Phase 1: Scan
        var scanner = GetScanner(acc);
        foreach (var source in sources)
        {
            scanner.Visit((Tree)source, ctx);
        }

        // Phase 2: Generate
        var generated = Generate(acc, ctx).ToList();

        // Phase 3: Edit
        var visitor = GetVisitor(acc);
        var results = EditSources(sources, visitor, ctx);

        // Add generated files
        foreach (var gen in generated)
        {
            results.Add(new Result(null, gen));
        }

        return results;
    }
}

/// <summary>
/// Represents the before and after state of a source file transformation.
/// </summary>
public record Result(SourceFile? Before, SourceFile? After);
