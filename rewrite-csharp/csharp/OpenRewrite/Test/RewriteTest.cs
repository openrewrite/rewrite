using OpenRewrite.Core;
using OpenRewrite.CSharp;
using Rewrite.Core;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Test;

/// <summary>
/// Base class for rewrite tests with round-trip validation and recipe execution.
/// </summary>
public abstract class RewriteTest
{
    protected void RewriteRun(params SourceSpec[] specs)
    {
        RewriteRun(_ => { }, specs);
    }

    protected void RewriteRun(Action<RecipeSpec> configure, params SourceSpec[] specs)
    {
        var recipeSpec = new RecipeSpec();
        configure(recipeSpec);

        var parser = new CSharpParser();
        var printer = new CSharpPrinter<object>();

        // 1. Parse all sources and validate round-trip
        var parsed = new List<(SourceSpec Spec, SourceFile Source)>();
        foreach (var spec in specs)
        {
            var source = parser.Parse(spec.Before);

            // Verify round-trip: printed should match input
            var printed = printer.Print(source);
            Assert.Equal(spec.Before, printed);

            // Verify idempotence: reparse and reprint should match
            var reparsed = parser.Parse(printed);
            var reprinted = printer.Print(reparsed);
            Assert.Equal(printed, reprinted);

            parsed.Add((spec, source));
        }

        // 2. If recipe configured, run it and verify results
        if (recipeSpec.Recipe != null)
        {
            var sources = parsed.Select(p => p.Source).ToList();
            var results = recipeSpec.Recipe.Run(sources, new ExecutionContext());

            foreach (var (spec, source) in parsed)
            {
                var result = results.FirstOrDefault(r =>
                    r.Before != null && r.Before.Id == source.Id);

                if (spec.After != null)
                {
                    // Expected a change
                    Assert.True(result != null && result.After != null,
                        $"Recipe was expected to make changes but did not modify the source file.");
                    var afterPrinted = printer.Print(result.After);
                    Assert.Equal(spec.After, afterPrinted);
                }
                else
                {
                    // Expected no change
                    Assert.True(result == null || result.After == null || result.Before == result.After,
                        "Recipe made unexpected changes to a source file that was not expected to change.");
                }
            }
        }
    }

    protected static SourceSpec CSharp(string before, string? after = null)
    {
        return new SourceSpec(before, after);
    }
}

/// <summary>
/// Specification for a source file in a test.
/// </summary>
public record SourceSpec(string Before, string? After = null);

/// <summary>
/// Specification for recipe configuration in a test.
/// </summary>
public class RecipeSpec
{
    public Recipe? Recipe { get; private set; }

    public RecipeSpec SetRecipe(Recipe recipe)
    {
        Recipe = recipe;
        return this;
    }
}
