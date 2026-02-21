using OpenRewrite.Core;

namespace OpenRewrite.Java.Search;

/// <summary>
/// Simple local UsesType that walks the tree checking JavaType attributes.
/// Used as fallback when not connected to Java via RPC.
/// Marks the compilation unit with a SearchResult marker if the type is found.
/// </summary>
internal class LocalUsesType<P> : JavaVisitor<P>
{
    private readonly string _fullyQualifiedTypeName;
    private bool _found;

    public LocalUsesType(string fullyQualifiedTypeName)
    {
        _fullyQualifiedTypeName = fullyQualifiedTypeName;
    }

    public override J VisitIdentifier(Identifier identifier, P p)
    {
        if (!_found && IsMatchingType(identifier.Type))
        {
            _found = true;
        }
        return identifier;
    }

    public override J VisitFieldAccess(FieldAccess fieldAccess, P p)
    {
        if (!_found && IsMatchingType(fieldAccess.Type))
        {
            _found = true;
        }
        return base.VisitFieldAccess(fieldAccess, p);
    }

    public override J? PostVisit(J tree, P p)
    {
        if (_found && tree is SourceFile)
        {
            return SearchResult.Found(tree);
        }
        return tree;
    }

    private bool IsMatchingType(JavaType? type)
    {
        if (type is JavaType.Class cls)
            return cls.FullyQualifiedName == _fullyQualifiedTypeName;
        return false;
    }
}
