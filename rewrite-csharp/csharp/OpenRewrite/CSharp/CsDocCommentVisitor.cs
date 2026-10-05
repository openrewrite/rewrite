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
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Visitor over the structured C# XML documentation comment tree, mirroring the Java
/// <c>CsDocCommentVisitor</c>. Bridges to a <see cref="CSharpVisitor{P}"/> so <c>cref</c>/<c>name</c>
/// attribute references (which are ordinary <see cref="J"/> nodes) can be visited.
/// </summary>
public class CsDocCommentVisitor<P>
{
    private readonly CSharpVisitor<P> _csharpVisitor;

    public CsDocCommentVisitor(CSharpVisitor<P> csharpVisitor)
    {
        _csharpVisitor = csharpVisitor;
    }

    public Cursor Cursor { get; set; } = new();

    protected J? CsharpVisitorVisit(Tree? tree, P p)
    {
        if (tree == null) return null;
        var previous = _csharpVisitor.Cursor;
        var j = _csharpVisitor.Visit(tree, p, Cursor);
        _csharpVisitor.Cursor = previous;
        return j;
    }

    protected virtual Markers VisitMarkers(Markers markers, P p) => _csharpVisitor.VisitMarkers(markers, p);

    public virtual CsDocComment? Visit(CsDocComment? node, P p) => node switch
    {
        CsDocComment.DocComment d => VisitDocComment(d, p),
        CsDocComment.XmlElement e => VisitXmlElement(e, p),
        CsDocComment.XmlEmptyElement e => VisitXmlEmptyElement(e, p),
        CsDocComment.XmlText t => VisitXmlText(t, p),
        CsDocComment.XmlCrefAttribute a => VisitXmlCrefAttribute(a, p),
        CsDocComment.XmlNameAttribute a => VisitXmlNameAttribute(a, p),
        CsDocComment.XmlAttribute a => VisitXmlAttribute(a, p),
        CsDocComment.LineBreak l => VisitLineBreak(l, p),
        _ => node
    };

    protected IList<CsDocComment> VisitList(IList<CsDocComment> nodes, P p)
    {
        // Identity-preserving, mirroring Java's ListUtils.map: return the SAME list instance when
        // no element changed, so With*/change-detection upstream can short-circuit unchanged trees.
        List<CsDocComment>? result = null;
        for (int i = 0; i < nodes.Count; i++)
        {
            var original = nodes[i];
            var visited = Visit(original, p);
            if (result == null && !ReferenceEquals(visited, original))
            {
                result = new List<CsDocComment>(nodes.Count);
                for (int j = 0; j < i; j++) result.Add(nodes[j]);
            }
            if (result != null && visited != null) result.Add(visited);
        }
        return result ?? nodes;
    }

    protected IList<CsDocComment>? VisitListNullable(IList<CsDocComment>? nodes, P p) =>
        nodes == null ? null : VisitList(nodes, p);

    public virtual CsDocComment VisitDocComment(CsDocComment.DocComment docComment, P p) =>
        docComment.WithMarkers(VisitMarkers(docComment.Markers, p))
            .WithBody(VisitList(docComment.Body, p));

    public virtual CsDocComment VisitXmlElement(CsDocComment.XmlElement element, P p) =>
        element.WithMarkers(VisitMarkers(element.Markers, p))
            .WithAttributes(VisitList(element.Attributes, p))
            .WithSpaceBeforeClose(VisitList(element.SpaceBeforeClose, p))
            .WithContent(VisitList(element.Content, p))
            .WithClosingTagSpaceBeforeClose(VisitList(element.ClosingTagSpaceBeforeClose, p));

    public virtual CsDocComment VisitXmlEmptyElement(CsDocComment.XmlEmptyElement element, P p) =>
        element.WithMarkers(VisitMarkers(element.Markers, p))
            .WithAttributes(VisitList(element.Attributes, p))
            .WithSpaceBeforeSlashClose(VisitList(element.SpaceBeforeSlashClose, p));

    public virtual CsDocComment VisitXmlText(CsDocComment.XmlText text, P p) =>
        text.WithMarkers(VisitMarkers(text.Markers, p));

    public virtual CsDocComment VisitXmlAttribute(CsDocComment.XmlAttribute attribute, P p) =>
        attribute.WithMarkers(VisitMarkers(attribute.Markers, p))
            .WithSpaceBeforeEquals(VisitListNullable(attribute.SpaceBeforeEquals, p))
            .WithValue(VisitListNullable(attribute.Value, p));

    public virtual CsDocComment VisitXmlCrefAttribute(CsDocComment.XmlCrefAttribute attribute, P p) =>
        attribute.WithMarkers(VisitMarkers(attribute.Markers, p))
            .WithSpaceBeforeEquals(VisitListNullable(attribute.SpaceBeforeEquals, p))
            .WithValue(VisitListNullable(attribute.Value, p))
            .WithReference(CsharpVisitorVisit(attribute.Reference, p));

    public virtual CsDocComment VisitXmlNameAttribute(CsDocComment.XmlNameAttribute attribute, P p) =>
        attribute.WithMarkers(VisitMarkers(attribute.Markers, p))
            .WithSpaceBeforeEquals(VisitListNullable(attribute.SpaceBeforeEquals, p))
            .WithValue(VisitListNullable(attribute.Value, p))
            .WithParamName(CsharpVisitorVisit(attribute.ParamName, p));

    public virtual CsDocComment VisitLineBreak(CsDocComment.LineBreak lineBreak, P p) =>
        lineBreak.WithMarkers(VisitMarkers(lineBreak.Markers, p));
}
