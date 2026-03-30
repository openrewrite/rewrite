/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
using OpenRewrite.Core;
using OpenRewrite.CSharp;

namespace OpenRewrite.Java.Search;

/// <summary>
/// Simple local UsesType that walks the tree checking JavaType attributes.
/// Used as fallback when not connected to Java via RPC.
/// Marks the compilation unit with a SearchResult marker if the type is found.
/// </summary>
internal class LocalUsesType<P> : CSharpVisitor<P>
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
