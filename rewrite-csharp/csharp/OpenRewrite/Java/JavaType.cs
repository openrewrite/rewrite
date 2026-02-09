namespace Rewrite.Java;

/// <summary>
/// Type attribution for LST elements.
/// </summary>
public abstract record JavaType
{
    /// <summary>
    /// Primitive types (int, string, bool, etc.)
    /// </summary>
    public sealed record Primitive(PrimitiveKind Kind) : JavaType;

    /// <summary>
    /// A fully qualified class/interface type.
    /// </summary>
    public sealed record FullyQualified(string FullyQualifiedName) : JavaType;

    /// <summary>
    /// A method type with return type and parameter types.
    /// </summary>
    public sealed record Method(string Name, JavaType? ReturnType, IList<JavaType>? ParameterTypes) : JavaType;

    public enum PrimitiveKind
    {
        Boolean,
        Byte,
        Char,
        Double,
        Float,
        Int,
        Long,
        Short,
        Void,
        String,
        Null,
        None
    }
}
