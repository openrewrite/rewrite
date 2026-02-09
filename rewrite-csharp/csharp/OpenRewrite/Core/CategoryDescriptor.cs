namespace Rewrite.Core;

/// <summary>
/// Describes a category in the recipe marketplace hierarchy.
/// Categories are used to organize recipes and are fully decoupled from the recipes themselves.
/// </summary>
public record CategoryDescriptor(string DisplayName, string? Description = null);
