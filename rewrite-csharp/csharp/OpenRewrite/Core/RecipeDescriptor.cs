using JetBrains.Annotations;

namespace OpenRewrite.Core;

/// <summary>
/// Complete metadata description of a recipe, including its options
/// and any nested recipes it composes. Mirrors Java's <c>RecipeDescriptor</c>.
/// </summary>
public record RecipeDescriptor(
    string Name,
    [property: LanguageInjection("markdown")] string DisplayName,
    [property: LanguageInjection("markdown")] string Description,
    IReadOnlySet<string> Tags,
    TimeSpan? EstimatedEffortPerOccurrence,
    IReadOnlyList<OptionDescriptor> Options,
    IReadOnlyList<RecipeDescriptor> RecipeList
);
