using Newtonsoft.Json;

namespace Rewrite.Core;

/// <summary>
/// Marker that records which recipes made changes to a source file.
/// This is an opaque marker — C# stores the raw recipe data without interpreting it.
/// </summary>
public sealed class RecipesThatMadeChanges : Marker
{
    [JsonProperty("id")]
    public Guid Id { get; init; }

    [JsonProperty("recipes")]
    public object? Recipes { get; init; }
}
