using JetBrains.Annotations;

namespace Rewrite.Core;

/// <summary>
/// Marks a property as a configurable recipe option.
/// Mirrors Java's <c>@Option</c> annotation.
/// </summary>
[AttributeUsage(AttributeTargets.Property)]
public sealed class OptionAttribute : Attribute
{
    [LanguageInjection("markdown")]
    public string DisplayName { get; set; } = "";

    [LanguageInjection("markdown")]
    public string Description { get; set; } = "";

    public string Example { get; set; } = "";

    public string[]? Valid { get; set; }

    public bool Required { get; set; } = true;
}
