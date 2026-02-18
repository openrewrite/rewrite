using JetBrains.Annotations;

namespace OpenRewrite.Core;

/// <summary>
/// Describes a single configurable option on a recipe.
/// Built via reflection from properties annotated with <see cref="OptionAttribute"/>.
/// </summary>
public record OptionDescriptor(
    string Name,
    string Type,
    [property: LanguageInjection("markdown")] string DisplayName,
    [property: LanguageInjection("markdown")] string Description,
    string? Example,
    IReadOnlyList<string>? Valid,
    bool Required,
    object? Value
);
