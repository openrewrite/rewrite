using Rewrite.CSharp;
using Xunit;

namespace Rewrite.Test;

/// <summary>
/// Base class for rewrite tests with round-trip validation.
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

        foreach (var spec in specs)
        {
            // 1. Parse
            var parsed = parser.Parse(spec.Before);

            // 2. Print
            var printed = printer.Print(parsed);

            // 3. Verify round-trip: printed should match input
            Assert.Equal(spec.Before, printed);

            // 4. Verify idempotence: reparse and reprint should match
            var reparsed = parser.Parse(printed);
            var reprinted = printer.Print(reparsed);
            Assert.Equal(printed, reprinted);

            // 5. If recipe configured and expected output provided, apply and verify
            if (recipeSpec.Recipe != null && spec.After != null)
            {
                // TODO: Apply recipe
                // var result = recipeSpec.Recipe.Run(parsed);
                // Assert.Equal(spec.After, printer.Print(result));
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
    public object? Recipe { get; private set; }

    public RecipeSpec SetRecipe(object recipe)
    {
        Recipe = recipe;
        return this;
    }
}
