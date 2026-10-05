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
/// Structured C# XML documentation comment (<c>///</c>) LST, mirroring the Java
/// <c>org.openrewrite.csharp.tree.CsDocComment</c> model node-for-node so a doc comment can be
/// traversed and rewritten with full fidelity and decomposed over RPC without flattening to text.
/// </summary>
public interface CsDocComment
{
    Guid Id { get; }
    Markers Markers { get; }

    /// <summary>
    /// Root of an XML documentation comment. Implements <see cref="Comment"/> so it can live in
    /// <see cref="Space.Comments"/> alongside <see cref="TextComment"/>.
    /// </summary>
    public sealed class DocComment : Comment, CsDocComment
    {
        public Guid Id { get; }
        public Markers Markers { get; }
        public IList<CsDocComment> Body { get; }

        // Comment.Suffix already carries the trailing whitespace after the comment.
        public new string Suffix => base.Suffix;

        public DocComment(Guid id, Markers markers, IList<CsDocComment> body, string suffix)
            : base("", suffix, true)
        {
            Id = id;
            Markers = markers;
            Body = body;
        }

        public DocComment WithId(Guid id) => id == Id ? this : new(id, Markers, Body, Suffix);
        public DocComment WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, markers, Body, Suffix);
        public DocComment WithBody(IList<CsDocComment> body) =>
            ReferenceEquals(body, Body) ? this : new(Id, Markers, body, Suffix);
        public DocComment WithSuffix(string suffix) =>
            suffix == Suffix ? this : new(Id, Markers, Body, suffix);
    }

    /// <summary>An XML element with opening and closing tags: <c>&lt;tag attr="val"&gt;content&lt;/tag&gt;</c>.</summary>
    public sealed class XmlElement : CsDocComment
    {
        public Guid Id { get; }
        public Markers Markers { get; }
        public string Name { get; }
        public IList<CsDocComment> Attributes { get; }
        public IList<CsDocComment> SpaceBeforeClose { get; }
        public IList<CsDocComment> Content { get; }
        public IList<CsDocComment> ClosingTagSpaceBeforeClose { get; }

        public XmlElement(Guid id, Markers markers, string name, IList<CsDocComment> attributes,
            IList<CsDocComment> spaceBeforeClose, IList<CsDocComment> content,
            IList<CsDocComment> closingTagSpaceBeforeClose)
        {
            Id = id;
            Markers = markers;
            Name = name;
            Attributes = attributes;
            SpaceBeforeClose = spaceBeforeClose;
            Content = content;
            ClosingTagSpaceBeforeClose = closingTagSpaceBeforeClose;
        }

        public XmlElement WithId(Guid id) =>
            id == Id ? this : new(id, Markers, Name, Attributes, SpaceBeforeClose, Content, ClosingTagSpaceBeforeClose);
        public XmlElement WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, markers, Name, Attributes, SpaceBeforeClose, Content, ClosingTagSpaceBeforeClose);
        public XmlElement WithName(string name) =>
            name == Name ? this : new(Id, Markers, name, Attributes, SpaceBeforeClose, Content, ClosingTagSpaceBeforeClose);
        public XmlElement WithAttributes(IList<CsDocComment> attributes) =>
            ReferenceEquals(attributes, Attributes) ? this : new(Id, Markers, Name, attributes, SpaceBeforeClose, Content, ClosingTagSpaceBeforeClose);
        public XmlElement WithSpaceBeforeClose(IList<CsDocComment> spaceBeforeClose) =>
            ReferenceEquals(spaceBeforeClose, SpaceBeforeClose) ? this : new(Id, Markers, Name, Attributes, spaceBeforeClose, Content, ClosingTagSpaceBeforeClose);
        public XmlElement WithContent(IList<CsDocComment> content) =>
            ReferenceEquals(content, Content) ? this : new(Id, Markers, Name, Attributes, SpaceBeforeClose, content, ClosingTagSpaceBeforeClose);
        public XmlElement WithClosingTagSpaceBeforeClose(IList<CsDocComment> closingTagSpaceBeforeClose) =>
            ReferenceEquals(closingTagSpaceBeforeClose, ClosingTagSpaceBeforeClose) ? this : new(Id, Markers, Name, Attributes, SpaceBeforeClose, Content, closingTagSpaceBeforeClose);
    }

    /// <summary>A self-closing XML element: <c>&lt;tag attr="val"/&gt;</c>.</summary>
    public sealed class XmlEmptyElement : CsDocComment
    {
        public Guid Id { get; }
        public Markers Markers { get; }
        public string Name { get; }
        public IList<CsDocComment> Attributes { get; }
        public IList<CsDocComment> SpaceBeforeSlashClose { get; }

        public XmlEmptyElement(Guid id, Markers markers, string name,
            IList<CsDocComment> attributes, IList<CsDocComment> spaceBeforeSlashClose)
        {
            Id = id;
            Markers = markers;
            Name = name;
            Attributes = attributes;
            SpaceBeforeSlashClose = spaceBeforeSlashClose;
        }

        public XmlEmptyElement WithId(Guid id) =>
            id == Id ? this : new(id, Markers, Name, Attributes, SpaceBeforeSlashClose);
        public XmlEmptyElement WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, markers, Name, Attributes, SpaceBeforeSlashClose);
        public XmlEmptyElement WithName(string name) =>
            name == Name ? this : new(Id, Markers, name, Attributes, SpaceBeforeSlashClose);
        public XmlEmptyElement WithAttributes(IList<CsDocComment> attributes) =>
            ReferenceEquals(attributes, Attributes) ? this : new(Id, Markers, Name, attributes, SpaceBeforeSlashClose);
        public XmlEmptyElement WithSpaceBeforeSlashClose(IList<CsDocComment> spaceBeforeSlashClose) =>
            ReferenceEquals(spaceBeforeSlashClose, SpaceBeforeSlashClose) ? this : new(Id, Markers, Name, Attributes, spaceBeforeSlashClose);
    }

    /// <summary>Plain text content within an XML documentation comment.</summary>
    public sealed class XmlText : CsDocComment
    {
        public Guid Id { get; }
        public Markers Markers { get; }
        public string Text { get; }

        public XmlText(Guid id, Markers markers, string text)
        {
            Id = id;
            Markers = markers;
            Text = text;
        }

        public XmlText WithId(Guid id) => id == Id ? this : new(id, Markers, Text);
        public XmlText WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, markers, Text);
        public XmlText WithText(string text) => text == Text ? this : new(Id, Markers, text);
    }

    /// <summary>A generic XML attribute: <c>attr="value"</c>.</summary>
    public sealed class XmlAttribute : CsDocComment
    {
        public Guid Id { get; }
        public Markers Markers { get; }
        public string Name { get; }
        public IList<CsDocComment>? SpaceBeforeEquals { get; }
        public IList<CsDocComment>? Value { get; }

        public XmlAttribute(Guid id, Markers markers, string name,
            IList<CsDocComment>? spaceBeforeEquals, IList<CsDocComment>? value)
        {
            Id = id;
            Markers = markers;
            Name = name;
            SpaceBeforeEquals = spaceBeforeEquals;
            Value = value;
        }

        public XmlAttribute WithId(Guid id) => id == Id ? this : new(id, Markers, Name, SpaceBeforeEquals, Value);
        public XmlAttribute WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, markers, Name, SpaceBeforeEquals, Value);
        public XmlAttribute WithName(string name) =>
            name == Name ? this : new(Id, Markers, name, SpaceBeforeEquals, Value);
        public XmlAttribute WithSpaceBeforeEquals(IList<CsDocComment>? spaceBeforeEquals) =>
            ReferenceEquals(spaceBeforeEquals, SpaceBeforeEquals) ? this : new(Id, Markers, Name, spaceBeforeEquals, Value);
        public XmlAttribute WithValue(IList<CsDocComment>? value) =>
            ReferenceEquals(value, Value) ? this : new(Id, Markers, Name, SpaceBeforeEquals, value);
    }

    /// <summary>A <c>cref</c> attribute referencing a type or member: <c>cref="System.String"</c>.</summary>
    public sealed class XmlCrefAttribute : CsDocComment
    {
        public Guid Id { get; }
        public Markers Markers { get; }
        public IList<CsDocComment>? SpaceBeforeEquals { get; }
        public IList<CsDocComment>? Value { get; }
        public J? Reference { get; }

        public XmlCrefAttribute(Guid id, Markers markers,
            IList<CsDocComment>? spaceBeforeEquals, IList<CsDocComment>? value, J? reference)
        {
            Id = id;
            Markers = markers;
            SpaceBeforeEquals = spaceBeforeEquals;
            Value = value;
            Reference = reference;
        }

        public XmlCrefAttribute WithId(Guid id) => id == Id ? this : new(id, Markers, SpaceBeforeEquals, Value, Reference);
        public XmlCrefAttribute WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, markers, SpaceBeforeEquals, Value, Reference);
        public XmlCrefAttribute WithSpaceBeforeEquals(IList<CsDocComment>? spaceBeforeEquals) =>
            ReferenceEquals(spaceBeforeEquals, SpaceBeforeEquals) ? this : new(Id, Markers, spaceBeforeEquals, Value, Reference);
        public XmlCrefAttribute WithValue(IList<CsDocComment>? value) =>
            ReferenceEquals(value, Value) ? this : new(Id, Markers, SpaceBeforeEquals, value, Reference);
        public XmlCrefAttribute WithReference(J? reference) =>
            ReferenceEquals(reference, Reference) ? this : new(Id, Markers, SpaceBeforeEquals, Value, reference);
    }

    /// <summary>A <c>name</c> attribute binding to a parameter or type parameter: <c>name="paramName"</c>.</summary>
    public sealed class XmlNameAttribute : CsDocComment
    {
        public Guid Id { get; }
        public Markers Markers { get; }
        public IList<CsDocComment>? SpaceBeforeEquals { get; }
        public IList<CsDocComment>? Value { get; }
        public J? ParamName { get; }

        public XmlNameAttribute(Guid id, Markers markers,
            IList<CsDocComment>? spaceBeforeEquals, IList<CsDocComment>? value, J? paramName)
        {
            Id = id;
            Markers = markers;
            SpaceBeforeEquals = spaceBeforeEquals;
            Value = value;
            ParamName = paramName;
        }

        public XmlNameAttribute WithId(Guid id) => id == Id ? this : new(id, Markers, SpaceBeforeEquals, Value, ParamName);
        public XmlNameAttribute WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, markers, SpaceBeforeEquals, Value, ParamName);
        public XmlNameAttribute WithSpaceBeforeEquals(IList<CsDocComment>? spaceBeforeEquals) =>
            ReferenceEquals(spaceBeforeEquals, SpaceBeforeEquals) ? this : new(Id, Markers, spaceBeforeEquals, Value, ParamName);
        public XmlNameAttribute WithValue(IList<CsDocComment>? value) =>
            ReferenceEquals(value, Value) ? this : new(Id, Markers, SpaceBeforeEquals, value, ParamName);
        public XmlNameAttribute WithParamName(J? paramName) =>
            ReferenceEquals(paramName, ParamName) ? this : new(Id, Markers, SpaceBeforeEquals, Value, paramName);
    }

    /// <summary>A line break within a documentation comment, including the margin (e.g. <c>"\n///"</c>).</summary>
    public sealed class LineBreak : CsDocComment
    {
        public Guid Id { get; }
        public string Margin { get; }
        public Markers Markers { get; }

        public LineBreak(Guid id, string margin, Markers markers)
        {
            Id = id;
            Margin = margin;
            Markers = markers;
        }

        public LineBreak WithId(Guid id) => id == Id ? this : new(id, Margin, Markers);
        public LineBreak WithMargin(string margin) => margin == Margin ? this : new(Id, margin, Markers);
        public LineBreak WithMarkers(Markers markers) =>
            ReferenceEquals(markers, Markers) ? this : new(Id, Margin, markers);
    }
}
