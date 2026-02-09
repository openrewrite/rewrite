namespace Rewrite.Core;

/// <summary>
/// Implemented by recipe packages (NuGet assemblies) to register their recipes
/// into the marketplace with explicit category placement.
/// <para>
/// This mirrors the JavaScript <c>activate(marketplace)</c> function pattern.
/// When an assembly is loaded, types implementing this interface are discovered
/// via reflection and their <see cref="Activate"/> method is called.
/// </para>
/// </summary>
public interface IRecipeActivator
{
    void Activate(RecipeMarketplace marketplace);
}
