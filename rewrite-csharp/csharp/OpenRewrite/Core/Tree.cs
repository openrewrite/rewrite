namespace Rewrite.Core;

/// <summary>
/// The base interface for all LST (Lossless Semantic Tree) elements.
/// </summary>
public interface Tree
{
    Guid Id { get; }

    Tree WithId(Guid id);
}

/// <summary>
/// Represents a source file in the LST.
/// </summary>
public interface SourceFile : Tree
{
    string SourcePath { get; }

    SourceFile WithSourcePath(string sourcePath);
}
