/*
 * Copyright 2025 the original author or authors.
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

namespace OpenRewrite.Xml;

/// <summary>
/// Base interface for all XML LST elements.
/// Uses string prefixes (not Space) for whitespace tracking.
/// </summary>
public interface Xml : Tree
{
    string Prefix { get; }

}

/// <summary>Marker interface for nodes that can appear as tag content.</summary>
public interface Content : Xml;

/// <summary>Marker interface for nodes that can appear in the document prolog.</summary>
public interface Misc : Xml;

/// <summary>
/// Root of an XML document.
/// </summary>
public sealed class Document : Xml, SourceFile
{
    public Guid Id { get; }
    public string SourcePath { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string? CharsetName { get; }
    public bool CharsetBomMarked { get; }
    public Checksum? Checksum { get; }
    public OpenRewrite.Core.FileAttributes? FileAttributes { get; }
    public Prolog? Prolog { get; }
    public Tag Root { get; }
    public string Eof { get; }

    public Document(
        Guid id, string sourcePath, string prefix, Markers markers,
        string? charsetName, bool charsetBomMarked, Checksum? checksum,
        OpenRewrite.Core.FileAttributes? fileAttributes, Prolog? prolog, Tag root, string eof)
    {
        Id = id;
        SourcePath = sourcePath;
        Prefix = prefix;
        Markers = markers;
        CharsetName = charsetName;
        CharsetBomMarked = charsetBomMarked;
        Checksum = checksum;
        FileAttributes = fileAttributes;
        Prolog = prolog;
        Root = root;
        Eof = eof;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Document(id, SourcePath, Prefix, Markers, CharsetName, CharsetBomMarked, Checksum, FileAttributes, Prolog, Root, Eof);
    public SourceFile WithSourcePath(string sourcePath) => sourcePath == SourcePath ? this : new Document(Id, sourcePath, Prefix, Markers, CharsetName, CharsetBomMarked, Checksum, FileAttributes, Prolog, Root, Eof);
    public Document WithPrefix(string prefix) => prefix == Prefix ? this : new Document(Id, SourcePath, prefix, Markers, CharsetName, CharsetBomMarked, Checksum, FileAttributes, Prolog, Root, Eof);
    public Document WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Document(Id, SourcePath, Prefix, markers, CharsetName, CharsetBomMarked, Checksum, FileAttributes, Prolog, Root, Eof);
    public Document WithCharsetName(string? charsetName) => charsetName == CharsetName ? this : new Document(Id, SourcePath, Prefix, Markers, charsetName, CharsetBomMarked, Checksum, FileAttributes, Prolog, Root, Eof);
    public Document WithCharsetBomMarked(bool charsetBomMarked) => charsetBomMarked == CharsetBomMarked ? this : new Document(Id, SourcePath, Prefix, Markers, CharsetName, charsetBomMarked, Checksum, FileAttributes, Prolog, Root, Eof);
    public Document WithChecksum(Checksum? checksum) => checksum == Checksum ? this : new Document(Id, SourcePath, Prefix, Markers, CharsetName, CharsetBomMarked, checksum, FileAttributes, Prolog, Root, Eof);
    public Document WithFileAttributes(OpenRewrite.Core.FileAttributes? fileAttributes) => fileAttributes == FileAttributes ? this : new Document(Id, SourcePath, Prefix, Markers, CharsetName, CharsetBomMarked, Checksum, fileAttributes, Prolog, Root, Eof);
    public Document WithProlog(Prolog? prolog) => ReferenceEquals(prolog, Prolog) ? this : new Document(Id, SourcePath, Prefix, Markers, CharsetName, CharsetBomMarked, Checksum, FileAttributes, prolog, Root, Eof);
    public Document WithRoot(Tag root) => ReferenceEquals(root, Root) ? this : new Document(Id, SourcePath, Prefix, Markers, CharsetName, CharsetBomMarked, Checksum, FileAttributes, Prolog, root, Eof);
    public Document WithEof(string eof) => eof == Eof ? this : new Document(Id, SourcePath, Prefix, Markers, CharsetName, CharsetBomMarked, Checksum, FileAttributes, Prolog, Root, eof);
}

public sealed class Prolog : Xml
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public XmlDecl? XmlDecl { get; }
    public IList<Misc> MiscList { get; }
    public IList<JspDirective> JspDirectives { get; }

    public Prolog(Guid id, string prefix, Markers markers, XmlDecl? xmlDecl, IList<Misc> miscList, IList<JspDirective> jspDirectives)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        XmlDecl = xmlDecl;
        MiscList = miscList;
        JspDirectives = jspDirectives;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Prolog(id, Prefix, Markers, XmlDecl, MiscList, JspDirectives);
    public Prolog WithPrefix(string prefix) => prefix == Prefix ? this : new Prolog(Id, prefix, Markers, XmlDecl, MiscList, JspDirectives);
    public Prolog WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Prolog(Id, Prefix, markers, XmlDecl, MiscList, JspDirectives);
    public Prolog WithXmlDecl(XmlDecl? xmlDecl) => ReferenceEquals(xmlDecl, XmlDecl) ? this : new Prolog(Id, Prefix, Markers, xmlDecl, MiscList, JspDirectives);
    public Prolog WithMiscList(IList<Misc> miscList) => ReferenceEquals(miscList, MiscList) ? this : new Prolog(Id, Prefix, Markers, XmlDecl, miscList, JspDirectives);
    public Prolog WithJspDirectives(IList<JspDirective> jspDirectives) => ReferenceEquals(jspDirectives, JspDirectives) ? this : new Prolog(Id, Prefix, Markers, XmlDecl, MiscList, jspDirectives);
}

public sealed class XmlDecl : Xml, Misc
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string Name { get; }
    public IList<Attribute> Attributes { get; }
    public string BeforeTagDelimiterPrefix { get; }

    public XmlDecl(Guid id, string prefix, Markers markers, string name, IList<Attribute> attributes, string beforeTagDelimiterPrefix)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Name = name;
        Attributes = attributes;
        BeforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
    }

    public Tree WithId(Guid id) => id == Id ? this : new XmlDecl(id, Prefix, Markers, Name, Attributes, BeforeTagDelimiterPrefix);
    public XmlDecl WithPrefix(string prefix) => prefix == Prefix ? this : new XmlDecl(Id, prefix, Markers, Name, Attributes, BeforeTagDelimiterPrefix);
    public XmlDecl WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new XmlDecl(Id, Prefix, markers, Name, Attributes, BeforeTagDelimiterPrefix);
    public XmlDecl WithName(string name) => name == Name ? this : new XmlDecl(Id, Prefix, Markers, name, Attributes, BeforeTagDelimiterPrefix);
    public XmlDecl WithAttributes(IList<Attribute> attributes) => ReferenceEquals(attributes, Attributes) ? this : new XmlDecl(Id, Prefix, Markers, Name, attributes, BeforeTagDelimiterPrefix);
    public XmlDecl WithBeforeTagDelimiterPrefix(string beforeTagDelimiterPrefix) => beforeTagDelimiterPrefix == BeforeTagDelimiterPrefix ? this : new XmlDecl(Id, Prefix, Markers, Name, Attributes, beforeTagDelimiterPrefix);
}

public sealed class ProcessingInstruction : Xml, Content, Misc
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string Name { get; }
    public CharData ProcessingInstructions { get; }
    public string BeforeTagDelimiterPrefix { get; }

    public ProcessingInstruction(Guid id, string prefix, Markers markers, string name, CharData processingInstructions, string beforeTagDelimiterPrefix)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Name = name;
        ProcessingInstructions = processingInstructions;
        BeforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
    }

    public Tree WithId(Guid id) => id == Id ? this : new ProcessingInstruction(id, Prefix, Markers, Name, ProcessingInstructions, BeforeTagDelimiterPrefix);
    public ProcessingInstruction WithPrefix(string prefix) => prefix == Prefix ? this : new ProcessingInstruction(Id, prefix, Markers, Name, ProcessingInstructions, BeforeTagDelimiterPrefix);
    public ProcessingInstruction WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new ProcessingInstruction(Id, Prefix, markers, Name, ProcessingInstructions, BeforeTagDelimiterPrefix);
    public ProcessingInstruction WithName(string name) => name == Name ? this : new ProcessingInstruction(Id, Prefix, Markers, name, ProcessingInstructions, BeforeTagDelimiterPrefix);
    public ProcessingInstruction WithProcessingInstructions(CharData processingInstructions) => ReferenceEquals(processingInstructions, ProcessingInstructions) ? this : new ProcessingInstruction(Id, Prefix, Markers, Name, processingInstructions, BeforeTagDelimiterPrefix);
    public ProcessingInstruction WithBeforeTagDelimiterPrefix(string beforeTagDelimiterPrefix) => beforeTagDelimiterPrefix == BeforeTagDelimiterPrefix ? this : new ProcessingInstruction(Id, Prefix, Markers, Name, ProcessingInstructions, beforeTagDelimiterPrefix);
}

public sealed class Tag : Xml, Content
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string Name { get; }
    public IList<Attribute> Attributes { get; }
    public IList<Content>? ContentList { get; }
    public Closing? ClosingTag { get; }
    public string BeforeTagDelimiterPrefix { get; }

    /// <summary>
    /// XAML attestation: the resolved type of the element this tag denotes. Null for
    /// plain XML documents and for unresolved XAML.
    ///
    ///   object element            &lt;Button ...&gt;         the control class
    ///   property element          &lt;Button.Content&gt;     the property on the enclosing
    ///                                                  element's type
    ///   attached property element &lt;Grid.Row&gt; (inside a  the attached member on the
    ///                             non-Grid child)      foreign owner type
    ///
    /// The name string stays verbatim (possibly xmlns-prefixed and/or dotted);
    /// attestation resolves the parts. Distinguishing the three roles is this slot's
    /// job, never a node-shape difference.
    /// </summary>
    public JavaType? Type { get; }

    public Tag(Guid id, string prefix, Markers markers, string name, IList<Attribute> attributes, IList<Content>? contentList, Closing? closingTag, string beforeTagDelimiterPrefix, JavaType? type = null)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Name = name;
        Attributes = attributes;
        ContentList = contentList;
        ClosingTag = closingTag;
        BeforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
        Type = type;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Tag(id, Prefix, Markers, Name, Attributes, ContentList, ClosingTag, BeforeTagDelimiterPrefix, Type);
    public Tag WithPrefix(string prefix) => prefix == Prefix ? this : new Tag(Id, prefix, Markers, Name, Attributes, ContentList, ClosingTag, BeforeTagDelimiterPrefix, Type);
    public Tag WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Tag(Id, Prefix, markers, Name, Attributes, ContentList, ClosingTag, BeforeTagDelimiterPrefix, Type);
    public Tag WithName(string name) => name == Name ? this : new Tag(Id, Prefix, Markers, name, Attributes, ContentList, ClosingTag, BeforeTagDelimiterPrefix, Type);
    public Tag WithAttributes(IList<Attribute> attributes) => ReferenceEquals(attributes, Attributes) ? this : new Tag(Id, Prefix, Markers, Name, attributes, ContentList, ClosingTag, BeforeTagDelimiterPrefix, Type);
    public Tag WithContentList(IList<Content>? contentList) => ReferenceEquals(contentList, ContentList) ? this : new Tag(Id, Prefix, Markers, Name, Attributes, contentList, ClosingTag, BeforeTagDelimiterPrefix, Type);
    public Tag WithClosingTag(Closing? closingTag) => ReferenceEquals(closingTag, ClosingTag) ? this : new Tag(Id, Prefix, Markers, Name, Attributes, ContentList, closingTag, BeforeTagDelimiterPrefix, Type);
    public Tag WithBeforeTagDelimiterPrefix(string beforeTagDelimiterPrefix) => beforeTagDelimiterPrefix == BeforeTagDelimiterPrefix ? this : new Tag(Id, Prefix, Markers, Name, Attributes, ContentList, ClosingTag, beforeTagDelimiterPrefix, Type);
    public Tag WithType(JavaType? type) => ReferenceEquals(type, Type) ? this : new Tag(Id, Prefix, Markers, Name, Attributes, ContentList, ClosingTag, BeforeTagDelimiterPrefix, type);

    public sealed class Closing : Xml
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public string Name { get; }
        public string BeforeTagDelimiterPrefix { get; }

        public Closing(Guid id, string prefix, Markers markers, string name, string beforeTagDelimiterPrefix)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Name = name;
            BeforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
        }

        public Tree WithId(Guid id) => id == Id ? this : new Closing(id, Prefix, Markers, Name, BeforeTagDelimiterPrefix);
        public Closing WithPrefix(string prefix) => prefix == Prefix ? this : new Closing(Id, prefix, Markers, Name, BeforeTagDelimiterPrefix);
        public Closing WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Closing(Id, Prefix, markers, Name, BeforeTagDelimiterPrefix);
        public Closing WithName(string name) => name == Name ? this : new Closing(Id, Prefix, Markers, name, BeforeTagDelimiterPrefix);
        public Closing WithBeforeTagDelimiterPrefix(string beforeTagDelimiterPrefix) => beforeTagDelimiterPrefix == BeforeTagDelimiterPrefix ? this : new Closing(Id, Prefix, Markers, Name, beforeTagDelimiterPrefix);
    }
}

public sealed class Attribute : Xml
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public Ident Key { get; }
    public string BeforeEquals { get; }
    public AttributeValue Val { get; }

    public Attribute(Guid id, string prefix, Markers markers, Ident key, string beforeEquals, AttributeValue val)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Key = key;
        BeforeEquals = beforeEquals;
        Val = val;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Attribute(id, Prefix, Markers, Key, BeforeEquals, Val);
    public Attribute WithPrefix(string prefix) => prefix == Prefix ? this : new Attribute(Id, prefix, Markers, Key, BeforeEquals, Val);
    public Attribute WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Attribute(Id, Prefix, markers, Key, BeforeEquals, Val);
    public Attribute WithKey(Ident key) => ReferenceEquals(key, Key) ? this : new Attribute(Id, Prefix, Markers, key, BeforeEquals, Val);
    public Attribute WithBeforeEquals(string beforeEquals) => beforeEquals == BeforeEquals ? this : new Attribute(Id, Prefix, Markers, Key, beforeEquals, Val);
    public Attribute WithVal(AttributeValue val) => ReferenceEquals(val, Val) ? this : new Attribute(Id, Prefix, Markers, Key, BeforeEquals, val);

    /// <summary>
    /// The polymorphic attribute-value slot — THE structural change XAML requires
    /// of the XML model. Plain XML attribute values are string leaves (Value,
    /// unchanged); XAML attribute values may instead carry a parsed expression tree
    /// (ExpressionValue) because markup extensions nest:
    ///
    ///   Text="hello"                              Value (exactly as today)
    ///   Text="{Binding Path=FirstName}"           ExpressionValue(MarkupExtension)
    ///   Text="{Binding Converter={StaticResource c}}"  nested extension tree
    ///   Text="{}{not an extension}"               Value — a value starting with the
    ///                                             {} escape is by definition plain
    ///                                             text, never an extension
    ///
    /// XML documents are untouched: their values remain Value instances, so existing
    /// serialized LSTs deserialize unchanged.
    /// </summary>
    public interface AttributeValue : Xml
    {
    }

    /// <summary>
    /// A XAML attribute value holding a parsed expression rather than a raw string.
    /// Quote style is preserved exactly as in Value; After captures the gap between
    /// the end of the expression and the closing quote:
    ///
    ///   Text="{Binding X} "     After = " "  (WPF rejects trailing whitespace at
    ///                           load — SR.WhitespaceAfterME — but files ship with
    ///                           it, so the model must round-trip it)
    /// </summary>
    public sealed class ExpressionValue : AttributeValue
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public Value.Quote QuoteStyle { get; }
        public Expression Expr { get; }
        public string After { get; }

        public ExpressionValue(Guid id, string prefix, Markers markers, Value.Quote quoteStyle, Expression expr, string after)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            QuoteStyle = quoteStyle;
            Expr = expr;
            After = after;
        }

        public Tree WithId(Guid id) => id == Id ? this : new ExpressionValue(id, Prefix, Markers, QuoteStyle, Expr, After);
        public ExpressionValue WithPrefix(string prefix) => prefix == Prefix ? this : new ExpressionValue(Id, prefix, Markers, QuoteStyle, Expr, After);
        public ExpressionValue WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new ExpressionValue(Id, Prefix, markers, QuoteStyle, Expr, After);
        public ExpressionValue WithQuoteStyle(Value.Quote quoteStyle) => quoteStyle == QuoteStyle ? this : new ExpressionValue(Id, Prefix, Markers, quoteStyle, Expr, After);
        public ExpressionValue WithExpr(Expression expr) => ReferenceEquals(expr, Expr) ? this : new ExpressionValue(Id, Prefix, Markers, QuoteStyle, expr, After);
        public ExpressionValue WithAfter(string after) => after == After ? this : new ExpressionValue(Id, Prefix, Markers, QuoteStyle, Expr, after);
    }

    public sealed class Value : AttributeValue
    {
        public enum Quote { Double, Single }

        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public Quote QuoteStyle { get; }
        public string Val { get; }

        public Value(Guid id, string prefix, Markers markers, Quote quoteStyle, string val)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            QuoteStyle = quoteStyle;
            Val = val;
        }

        public Tree WithId(Guid id) => id == Id ? this : new Value(id, Prefix, Markers, QuoteStyle, Val);
        public Value WithPrefix(string prefix) => prefix == Prefix ? this : new Value(Id, prefix, Markers, QuoteStyle, Val);
        public Value WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Value(Id, Prefix, markers, QuoteStyle, Val);
        public Value WithQuoteStyle(Quote quoteStyle) => quoteStyle == QuoteStyle ? this : new Value(Id, Prefix, Markers, quoteStyle, Val);
        public Value WithVal(string val) => val == Val ? this : new Value(Id, Prefix, Markers, QuoteStyle, val);
    }
}

public sealed class CharData : Xml, Content
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public bool Cdata { get; }
    public string Text { get; }
    public string AfterText { get; }

    public CharData(Guid id, string prefix, Markers markers, bool cdata, string text, string afterText)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Cdata = cdata;
        Text = text;
        AfterText = afterText;
    }

    public Tree WithId(Guid id) => id == Id ? this : new CharData(id, Prefix, Markers, Cdata, Text, AfterText);
    public CharData WithPrefix(string prefix) => prefix == Prefix ? this : new CharData(Id, prefix, Markers, Cdata, Text, AfterText);
    public CharData WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new CharData(Id, Prefix, markers, Cdata, Text, AfterText);
    public CharData WithCdata(bool cdata) => cdata == Cdata ? this : new CharData(Id, Prefix, Markers, cdata, Text, AfterText);
    public CharData WithText(string text) => text == Text ? this : new CharData(Id, Prefix, Markers, Cdata, text, AfterText);
    public CharData WithAfterText(string afterText) => afterText == AfterText ? this : new CharData(Id, Prefix, Markers, Cdata, Text, afterText);
}

public sealed class Comment : Xml, Content, Misc
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string Text { get; }

    public Comment(Guid id, string prefix, Markers markers, string text)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Text = text;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Comment(id, Prefix, Markers, Text);
    public Comment WithPrefix(string prefix) => prefix == Prefix ? this : new Comment(Id, prefix, Markers, Text);
    public Comment WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Comment(Id, Prefix, markers, Text);
    public Comment WithText(string text) => text == Text ? this : new Comment(Id, Prefix, Markers, text);
}

public sealed class DocTypeDecl : Xml, Misc
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public Ident Name { get; }
    public string DocumentDeclaration { get; }
    public Ident? ExternalId { get; }
    public IList<Ident> InternalSubset { get; }
    public ExternalSubsets? ExternalSubsetsNode { get; }
    public string BeforeTagDelimiterPrefix { get; }

    public DocTypeDecl(Guid id, string prefix, Markers markers, Ident name, string documentDeclaration, Ident? externalId, IList<Ident> internalSubset, ExternalSubsets? externalSubsetsNode, string beforeTagDelimiterPrefix)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Name = name;
        DocumentDeclaration = documentDeclaration;
        ExternalId = externalId;
        InternalSubset = internalSubset;
        ExternalSubsetsNode = externalSubsetsNode;
        BeforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
    }

    public Tree WithId(Guid id) => id == Id ? this : new DocTypeDecl(id, Prefix, Markers, Name, DocumentDeclaration, ExternalId, InternalSubset, ExternalSubsetsNode, BeforeTagDelimiterPrefix);
    public DocTypeDecl WithPrefix(string prefix) => prefix == Prefix ? this : new DocTypeDecl(Id, prefix, Markers, Name, DocumentDeclaration, ExternalId, InternalSubset, ExternalSubsetsNode, BeforeTagDelimiterPrefix);
    public DocTypeDecl WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new DocTypeDecl(Id, Prefix, markers, Name, DocumentDeclaration, ExternalId, InternalSubset, ExternalSubsetsNode, BeforeTagDelimiterPrefix);
    public DocTypeDecl WithName(Ident name) => ReferenceEquals(name, Name) ? this : new DocTypeDecl(Id, Prefix, Markers, name, DocumentDeclaration, ExternalId, InternalSubset, ExternalSubsetsNode, BeforeTagDelimiterPrefix);
    public DocTypeDecl WithDocumentDeclaration(string documentDeclaration) => documentDeclaration == DocumentDeclaration ? this : new DocTypeDecl(Id, Prefix, Markers, Name, documentDeclaration, ExternalId, InternalSubset, ExternalSubsetsNode, BeforeTagDelimiterPrefix);
    public DocTypeDecl WithExternalId(Ident? externalId) => ReferenceEquals(externalId, ExternalId) ? this : new DocTypeDecl(Id, Prefix, Markers, Name, DocumentDeclaration, externalId, InternalSubset, ExternalSubsetsNode, BeforeTagDelimiterPrefix);
    public DocTypeDecl WithInternalSubset(IList<Ident> internalSubset) => ReferenceEquals(internalSubset, InternalSubset) ? this : new DocTypeDecl(Id, Prefix, Markers, Name, DocumentDeclaration, ExternalId, internalSubset, ExternalSubsetsNode, BeforeTagDelimiterPrefix);
    public DocTypeDecl WithExternalSubsetsNode(ExternalSubsets? externalSubsetsNode) => ReferenceEquals(externalSubsetsNode, ExternalSubsetsNode) ? this : new DocTypeDecl(Id, Prefix, Markers, Name, DocumentDeclaration, ExternalId, InternalSubset, externalSubsetsNode, BeforeTagDelimiterPrefix);
    public DocTypeDecl WithBeforeTagDelimiterPrefix(string beforeTagDelimiterPrefix) => beforeTagDelimiterPrefix == BeforeTagDelimiterPrefix ? this : new DocTypeDecl(Id, Prefix, Markers, Name, DocumentDeclaration, ExternalId, InternalSubset, ExternalSubsetsNode, beforeTagDelimiterPrefix);

    public sealed class ExternalSubsets : Xml
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public IList<Element> Elements { get; }

        public ExternalSubsets(Guid id, string prefix, Markers markers, IList<Element> elements)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Elements = elements;
        }

        public Tree WithId(Guid id) => id == Id ? this : new ExternalSubsets(id, Prefix, Markers, Elements);
        public ExternalSubsets WithPrefix(string prefix) => prefix == Prefix ? this : new ExternalSubsets(Id, prefix, Markers, Elements);
        public ExternalSubsets WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new ExternalSubsets(Id, Prefix, markers, Elements);
        public ExternalSubsets WithElements(IList<Element> elements) => ReferenceEquals(elements, Elements) ? this : new ExternalSubsets(Id, Prefix, Markers, elements);
    }
}

public sealed class Element : Xml
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public IList<Ident> Subset { get; }
    public string BeforeTagDelimiterPrefix { get; }

    public Element(Guid id, string prefix, Markers markers, IList<Ident> subset, string beforeTagDelimiterPrefix)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Subset = subset;
        BeforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Element(id, Prefix, Markers, Subset, BeforeTagDelimiterPrefix);
    public Element WithPrefix(string prefix) => prefix == Prefix ? this : new Element(Id, prefix, Markers, Subset, BeforeTagDelimiterPrefix);
    public Element WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Element(Id, Prefix, markers, Subset, BeforeTagDelimiterPrefix);
    public Element WithSubset(IList<Ident> subset) => ReferenceEquals(subset, Subset) ? this : new Element(Id, Prefix, Markers, subset, BeforeTagDelimiterPrefix);
    public Element WithBeforeTagDelimiterPrefix(string beforeTagDelimiterPrefix) => beforeTagDelimiterPrefix == BeforeTagDelimiterPrefix ? this : new Element(Id, Prefix, Markers, Subset, beforeTagDelimiterPrefix);
}

public sealed class Ident : Xml
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string Name { get; }

    /// <summary>
    /// XAML attestation: the resolved meaning of this name token. Null for plain XML
    /// and for unresolved XAML. This slot is what gives tokens semantic identity
    /// WITHOUT node-type proliferation:
    ///
    ///   attribute key      Mode           the Binding.Mode property
    ///   attribute key      Click          the routed event
    ///   directive key      x:Name         the XAML language directive
    ///   extension name     Binding        the extension class (spelling verbatim,
    ///                                     incl. the optional "Extension" suffix)
    ///   path member        Customer       the CLR property (JavaType.Variable) —
    ///                                     the hook that propagates ViewModel
    ///                                     renames into XAML
    ///
    /// The name string is verbatim source, including any xmlns prefix and dots
    /// (local:MyControl, Grid.Column); attestation resolves the parts. Never
    /// normalized.
    /// </summary>
    public JavaType? Type { get; }

    public Ident(Guid id, string prefix, Markers markers, string name, JavaType? type = null)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Name = name;
        Type = type;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Ident(id, Prefix, Markers, Name, Type);
    public Ident WithPrefix(string prefix) => prefix == Prefix ? this : new Ident(Id, prefix, Markers, Name, Type);
    public Ident WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Ident(Id, Prefix, markers, Name, Type);
    public Ident WithName(string name) => name == Name ? this : new Ident(Id, Prefix, Markers, name, Type);
    public Ident WithType(JavaType? type) => ReferenceEquals(type, Type) ? this : new Ident(Id, Prefix, Markers, Name, type);
}

public sealed class JspDirective : Xml, Content
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string BeforeTypePrefix { get; }
    public string Type { get; }
    public IList<Attribute> Attributes { get; }
    public string BeforeDirectiveEndPrefix { get; }

    public JspDirective(Guid id, string prefix, Markers markers, string beforeTypePrefix, string type, IList<Attribute> attributes, string beforeDirectiveEndPrefix)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        BeforeTypePrefix = beforeTypePrefix;
        Type = type;
        Attributes = attributes;
        BeforeDirectiveEndPrefix = beforeDirectiveEndPrefix;
    }

    public Tree WithId(Guid id) => id == Id ? this : new JspDirective(id, Prefix, Markers, BeforeTypePrefix, Type, Attributes, BeforeDirectiveEndPrefix);
    public JspDirective WithPrefix(string prefix) => prefix == Prefix ? this : new JspDirective(Id, prefix, Markers, BeforeTypePrefix, Type, Attributes, BeforeDirectiveEndPrefix);
    public JspDirective WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new JspDirective(Id, Prefix, markers, BeforeTypePrefix, Type, Attributes, BeforeDirectiveEndPrefix);
    public JspDirective WithBeforeTypePrefix(string beforeTypePrefix) => beforeTypePrefix == BeforeTypePrefix ? this : new JspDirective(Id, Prefix, Markers, beforeTypePrefix, Type, Attributes, BeforeDirectiveEndPrefix);
    public JspDirective WithType(string type) => type == Type ? this : new JspDirective(Id, Prefix, Markers, BeforeTypePrefix, type, Attributes, BeforeDirectiveEndPrefix);
    public JspDirective WithAttributes(IList<Attribute> attributes) => ReferenceEquals(attributes, Attributes) ? this : new JspDirective(Id, Prefix, Markers, BeforeTypePrefix, Type, attributes, BeforeDirectiveEndPrefix);
    public JspDirective WithBeforeDirectiveEndPrefix(string beforeDirectiveEndPrefix) => beforeDirectiveEndPrefix == BeforeDirectiveEndPrefix ? this : new JspDirective(Id, Prefix, Markers, BeforeTypePrefix, Type, Attributes, beforeDirectiveEndPrefix);
}

public sealed class JspScriptlet : Xml, Content
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string JspContent { get; }

    public JspScriptlet(Guid id, string prefix, Markers markers, string jspContent)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        JspContent = jspContent;
    }

    public Tree WithId(Guid id) => id == Id ? this : new JspScriptlet(id, Prefix, Markers, JspContent);
    public JspScriptlet WithPrefix(string prefix) => prefix == Prefix ? this : new JspScriptlet(Id, prefix, Markers, JspContent);
    public JspScriptlet WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new JspScriptlet(Id, Prefix, markers, JspContent);
    public JspScriptlet WithJspContent(string jspContent) => jspContent == JspContent ? this : new JspScriptlet(Id, Prefix, Markers, jspContent);
}

public sealed class JspExpression : Xml, Content
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string JspContent { get; }

    public JspExpression(Guid id, string prefix, Markers markers, string jspContent)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        JspContent = jspContent;
    }

    public Tree WithId(Guid id) => id == Id ? this : new JspExpression(id, Prefix, Markers, JspContent);
    public JspExpression WithPrefix(string prefix) => prefix == Prefix ? this : new JspExpression(Id, prefix, Markers, JspContent);
    public JspExpression WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new JspExpression(Id, Prefix, markers, JspContent);
    public JspExpression WithJspContent(string jspContent) => jspContent == JspContent ? this : new JspExpression(Id, Prefix, Markers, jspContent);
}

public sealed class JspDeclaration : Xml, Content, Misc
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string JspContent { get; }

    public JspDeclaration(Guid id, string prefix, Markers markers, string jspContent)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        JspContent = jspContent;
    }

    public Tree WithId(Guid id) => id == Id ? this : new JspDeclaration(id, Prefix, Markers, JspContent);
    public JspDeclaration WithPrefix(string prefix) => prefix == Prefix ? this : new JspDeclaration(Id, prefix, Markers, JspContent);
    public JspDeclaration WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new JspDeclaration(Id, Prefix, markers, JspContent);
    public JspDeclaration WithJspContent(string jspContent) => jspContent == JspContent ? this : new JspDeclaration(Id, Prefix, Markers, jspContent);
}

public sealed class JspComment : Xml, Content, Misc
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string JspContent { get; }

    public JspComment(Guid id, string prefix, Markers markers, string jspContent)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        JspContent = jspContent;
    }

    public Tree WithId(Guid id) => id == Id ? this : new JspComment(id, Prefix, Markers, JspContent);
    public JspComment WithPrefix(string prefix) => prefix == Prefix ? this : new JspComment(Id, prefix, Markers, JspContent);
    public JspComment WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new JspComment(Id, Prefix, markers, JspContent);
    public JspComment WithJspContent(string jspContent) => jspContent == JspContent ? this : new JspComment(Id, Prefix, Markers, jspContent);
}

// ============================================================================
// XAML VALUE LAYER — the attribute-value / binding mini-language.
//
// Everything below exists to cover XAML (WPF, UWP/WinUI incl. x:Bind,
// Xamarin.Forms / .NET MAUI, Avalonia, legacy Silverlight) on top of the XML
// model. Plain XML documents never contain these nodes. This section MUST stay in
// perfect structural sync with the Java side (Xml.java, XAML VALUE LAYER).
//
// Modelling rules (same as the rest of this file): prefix = all whitespace left
// of the node's first token; interior gaps get named Before* fields; punctuation
// is implicit (an optional token's presence is a non-null Before* field). Parse
// is purely syntactic — semantics attach afterwards as JavaType attestation.
// Literal text is the RAW source slice (quotes kept, escapes unresolved); any
// value text outside the closed grammar stays a verbatim Literal, never a parse
// error (e.g. MAUI expression bindings "{Binding Price * this.TaxRate}").
// ============================================================================

/// <summary>
/// Anything legal in a XAML value position: an attribute value expression, a
/// markup-extension argument, a function-call argument. Implementations:
/// Literal, MarkupExtension, PropertyPath, Negation, TypeName.
/// </summary>
public interface Expression : Xml;

/// <summary>
/// Verbatim uninterpreted text in a value position — the most common node.
/// Text is the EXACT source slice: surrounding ' or " quotes kept, backslash
/// escapes (WPF/MAUI) and caret escapes (WPF indexer params) unresolved, leading
/// {} escape kept, interior whitespace kept. Covers plain values, enum keyword
/// tokens (Mode=TwoWay, FindAncestor — attest to the enum member), event handler
/// names (attest to the handler method), resource keys, quoted args
/// (ConverterParameter='a, b'), mixed quoting (Text=abc'def', MAUI), balanced
/// braces inside unquoted values ({Binding Foo{Bar}}, WPF v3 compat), x:Bind
/// constants, and the fallback for any value text outside the closed grammar.
/// </summary>
public sealed class Literal : Expression
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public string Text { get; }
    public JavaType? Type { get; }

    public Literal(Guid id, string prefix, Markers markers, string text, JavaType? type = null)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Text = text;
        Type = type;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Literal(id, Prefix, Markers, Text, Type);
    public Literal WithPrefix(string prefix) => prefix == Prefix ? this : new Literal(Id, prefix, Markers, Text, Type);
    public Literal WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Literal(Id, Prefix, markers, Text, Type);
    public Literal WithText(string text) => text == Text ? this : new Literal(Id, Prefix, Markers, text, Type);
    public Literal WithType(JavaType? type) => ReferenceEquals(type, Type) ? this : new Literal(Id, Prefix, Markers, Text, type);
}

/// <summary>
/// A markup extension — ONE node type for every extension kind in every dialect
/// (WPF {Binding}/{StaticResource}/{RelativeSource}/{x:Static}/..., WinUI
/// {ThemeResource}/{x:Bind}, MAUI {AppThemeBinding}/{OnPlatform}, Avalonia
/// {CompiledBinding}/{ReflectionBinding}, any third-party extension). Which one it
/// is = attestation on TypeName (xmlns resolution; WPF/MAUI try both the literal
/// name and name+"Extension" — TypeName keeps the source spelling), never a
/// subclass.
///
/// Token/field map for "{ Binding  Path=FirstName , Mode=TwoWay }": Prefix before
/// the brace (or before the quote when quoted); TypeName.Prefix after the brace;
/// each Argument owns its leading trivia via Prefix and the gap before its
/// trailing comma via BeforeComma; BeforeClosingBrace before the closing brace.
/// Nesting is direct tree recursion ({Binding Converter={StaticResource conv}});
/// a nested extension may itself be QUOTED (Converter='{...}') — non-null Quote,
/// wrapping the braces tightly. WPF enforces positional-before-named; MAUI allows
/// interleaving — the order-preserving list represents both.
///
/// Implements Content: extension syntax is legal initialization text —
/// &lt;TextBlock.Text&gt;{Binding Foo}&lt;/TextBlock.Text&gt;.
/// </summary>
public sealed class MarkupExtension : Expression, Content
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public Attribute.Value.Quote? Quote { get; }
    public Ident TypeName { get; }
    public IList<Argument> Arguments { get; }
    public string BeforeClosingBrace { get; }

    public MarkupExtension(Guid id, string prefix, Markers markers, Attribute.Value.Quote? quote, Ident typeName, IList<Argument> arguments, string beforeClosingBrace)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Quote = quote;
        TypeName = typeName;
        Arguments = arguments;
        BeforeClosingBrace = beforeClosingBrace;
    }

    public Tree WithId(Guid id) => id == Id ? this : new MarkupExtension(id, Prefix, Markers, Quote, TypeName, Arguments, BeforeClosingBrace);
    public MarkupExtension WithPrefix(string prefix) => prefix == Prefix ? this : new MarkupExtension(Id, prefix, Markers, Quote, TypeName, Arguments, BeforeClosingBrace);
    public MarkupExtension WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new MarkupExtension(Id, Prefix, markers, Quote, TypeName, Arguments, BeforeClosingBrace);
    public MarkupExtension WithQuote(Attribute.Value.Quote? quote) => quote == Quote ? this : new MarkupExtension(Id, Prefix, Markers, quote, TypeName, Arguments, BeforeClosingBrace);
    public MarkupExtension WithTypeName(Ident typeName) => ReferenceEquals(typeName, TypeName) ? this : new MarkupExtension(Id, Prefix, Markers, Quote, typeName, Arguments, BeforeClosingBrace);
    public MarkupExtension WithArguments(IList<Argument> arguments) => ReferenceEquals(arguments, Arguments) ? this : new MarkupExtension(Id, Prefix, Markers, Quote, TypeName, arguments, BeforeClosingBrace);
    public MarkupExtension WithBeforeClosingBrace(string beforeClosingBrace) => beforeClosingBrace == BeforeClosingBrace ? this : new MarkupExtension(Id, Prefix, Markers, Quote, TypeName, Arguments, beforeClosingBrace);
}

/// <summary>
/// One argument of a MarkupExtension or an x:Bind function call. Positional and
/// named forms are ONE node distinguished by Name nullability — mirroring how the
/// WPF scanner itself disambiguates (a value run that hits '=' is re-labelled
/// PropertyName; there is no parser lookahead). Positional: {Binding FirstName}
/// (Name null, Value PropertyPath), {StaticResource key} (Value Literal). Named:
/// Mode=TwoWay (Name attests to Binding.Mode, Value attests to the enum member).
/// BeforeComma is the gap before the trailing comma separator, null on the last
/// argument (the gap before the closing brace is
/// MarkupExtension.BeforeClosingBrace).
/// </summary>
public sealed class Argument : Xml
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public Ident? Name { get; }
    /// <summary>Whitespace before '='; null iff positional.</summary>
    public string? BeforeEquals { get; }
    public Expression Value { get; }
    /// <summary>Whitespace before the following ','; null on the last argument.</summary>
    public string? BeforeComma { get; }

    public Argument(Guid id, string prefix, Markers markers, Ident? name, string? beforeEquals, Expression value, string? beforeComma)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Name = name;
        BeforeEquals = beforeEquals;
        Value = value;
        BeforeComma = beforeComma;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Argument(id, Prefix, Markers, Name, BeforeEquals, Value, BeforeComma);
    public Argument WithPrefix(string prefix) => prefix == Prefix ? this : new Argument(Id, prefix, Markers, Name, BeforeEquals, Value, BeforeComma);
    public Argument WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Argument(Id, Prefix, markers, Name, BeforeEquals, Value, BeforeComma);
    public Argument WithName(Ident? name) => ReferenceEquals(name, Name) ? this : new Argument(Id, Prefix, Markers, name, BeforeEquals, Value, BeforeComma);
    public Argument WithBeforeEquals(string? beforeEquals) => beforeEquals == BeforeEquals ? this : new Argument(Id, Prefix, Markers, Name, beforeEquals, Value, BeforeComma);
    public Argument WithValue(Expression value) => ReferenceEquals(value, Value) ? this : new Argument(Id, Prefix, Markers, Name, BeforeEquals, value, BeforeComma);
    public Argument WithBeforeComma(string? beforeComma) => beforeComma == BeforeComma ? this : new Argument(Id, Prefix, Markers, Name, BeforeEquals, Value, beforeComma);
}

/// <summary>
/// The property-path mini-language: the value of Path= / the positional path of
/// {Binding}, {x:Bind}, {TemplateBinding}, {CompiledBinding}, MAUI/XF bindings.
/// A flat sequence of segments; each segment records the ACCESSOR token that
/// precedes it. There is no whole-path stream flag — the caret is a segment
/// (Avalonia allows it mid-path and repeated).
///
///   Customer.Address[0].(Validation.Errors)[0].ErrorContent
///
/// Special shapes: empty path ({Binding} — Segments empty, bind to the source);
/// bare dot (Path=. — one Property, accessor Dot, empty name); leading slash
/// (/Items/Name — WPF collection-view current item); leading dots kept in member
/// names (.Foo, WPF XLinq); Avalonia streams (Foo^.Bar^, ^, .^); null-conditional
/// (Foo?.Bar). Path text outside this closed grammar stays a verbatim Literal.
/// WPF trims segment-interior whitespace when it parses; this model stores
/// exactly what the source had.
/// </summary>
public sealed class PropertyPath : Expression
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public IList<Segment> Segments { get; }

    public PropertyPath(Guid id, string prefix, Markers markers, IList<Segment> segments)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Segments = segments;
    }

    public Tree WithId(Guid id) => id == Id ? this : new PropertyPath(id, Prefix, Markers, Segments);
    public PropertyPath WithPrefix(string prefix) => prefix == Prefix ? this : new PropertyPath(Id, prefix, Markers, Segments);
    public PropertyPath WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new PropertyPath(Id, Prefix, markers, Segments);
    public PropertyPath WithSegments(IList<Segment> segments) => ReferenceEquals(segments, Segments) ? this : new PropertyPath(Id, Prefix, Markers, segments);

    /// <summary>
    /// A step in a PropertyPath. The accessor is the token PRECEDING the step; the
    /// step's Prefix is the whitespace before that token (or before the step's
    /// first own token when None). None = first segment or direct attachment
    /// (Foo[0]); Dot = member step (all dialects); Slash = WPF collection-view
    /// current-item drill-in (may precede an Indexer: /Items/[0]);
    /// NullConditional = Avalonia ?. member access.
    /// </summary>
    public interface Segment : Xml
    {
        AccessorKind Accessor { get; }
    }

    public enum AccessorKind { None, Dot, Slash, NullConditional }

    /// <summary>
    /// A plain member step: Customer, Address. Name.Type attests to the resolved
    /// CLR property (JavaType.Variable) — the hook that propagates ViewModel
    /// renames into XAML. Exact under compiled bindings (x:Bind, x:DataType);
    /// heuristic under reflection bindings. In Avalonia a trailing member may
    /// resolve to a METHOD (command binding) — same syntax, method attestation,
    /// no parentheses involved.
    /// </summary>
    public sealed class Property : Segment
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public AccessorKind Accessor { get; }
        public Ident Name { get; }

        public Property(Guid id, string prefix, Markers markers, AccessorKind accessor, Ident name)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Accessor = accessor;
            Name = name;
        }

        public Tree WithId(Guid id) => id == Id ? this : new Property(id, Prefix, Markers, Accessor, Name);
        public Property WithPrefix(string prefix) => prefix == Prefix ? this : new Property(Id, prefix, Markers, Accessor, Name);
        public Property WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Property(Id, Prefix, markers, Accessor, Name);
        public Property WithAccessor(AccessorKind accessor) => accessor == Accessor ? this : new Property(Id, Prefix, Markers, accessor, Name);
        public Property WithName(Ident name) => ReferenceEquals(name, Name) ? this : new Property(Id, Prefix, Markers, Accessor, name);
    }

    /// <summary>
    /// An indexer step: [0], [FirstName], [a,b], [(sys:Int32)42], [15][16].
    /// Prefix is the gap before the opening bracket. Each parameter is an optional
    /// parenthesized group followed by an optional value literal — the paren
    /// group's meaning is three-way in WPF and resolved by ATTESTATION, never by
    /// node shape: [(sys:Int32)42] typed param; [(2)] parameter index; [(abc)]
    /// literal paren string; [foo] plain value. Param values are verbatim
    /// Literals: WPF's indexer escape char is CARET (a different escape rule than
    /// the extension level's backslash), nested balanced brackets are legal, and
    /// MAUI allows quoted commas (['x, y']) — the parser splits params only on
    /// commas outside quotes/brackets.
    /// </summary>
    public sealed class Indexer : Segment
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public AccessorKind Accessor { get; }
        public IList<Param> Parameters { get; }
        public string BeforeClosingBracket { get; }

        public Indexer(Guid id, string prefix, Markers markers, AccessorKind accessor, IList<Param> parameters, string beforeClosingBracket)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Accessor = accessor;
            Parameters = parameters;
            BeforeClosingBracket = beforeClosingBracket;
        }

        public Tree WithId(Guid id) => id == Id ? this : new Indexer(id, Prefix, Markers, Accessor, Parameters, BeforeClosingBracket);
        public Indexer WithPrefix(string prefix) => prefix == Prefix ? this : new Indexer(Id, prefix, Markers, Accessor, Parameters, BeforeClosingBracket);
        public Indexer WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Indexer(Id, Prefix, markers, Accessor, Parameters, BeforeClosingBracket);
        public Indexer WithAccessor(AccessorKind accessor) => accessor == Accessor ? this : new Indexer(Id, Prefix, Markers, accessor, Parameters, BeforeClosingBracket);
        public Indexer WithParameters(IList<Param> parameters) => ReferenceEquals(parameters, Parameters) ? this : new Indexer(Id, Prefix, Markers, Accessor, parameters, BeforeClosingBracket);
        public Indexer WithBeforeClosingBracket(string beforeClosingBracket) => beforeClosingBracket == BeforeClosingBracket ? this : new Indexer(Id, Prefix, Markers, Accessor, Parameters, beforeClosingBracket);

        /// <summary>One indexer parameter.</summary>
        public sealed class Param : Xml
        {
            public Guid Id { get; }
            public string Prefix { get; }
            public Markers Markers { get; }
            public ParenGroup? Paren { get; }
            public Literal? Value { get; }
            /// <summary>Before the trailing ','; null on the last parameter.</summary>
            public string? BeforeComma { get; }

            public Param(Guid id, string prefix, Markers markers, ParenGroup? paren, Literal? value, string? beforeComma)
            {
                Id = id;
                Prefix = prefix;
                Markers = markers;
                Paren = paren;
                Value = value;
                BeforeComma = beforeComma;
            }

            public Tree WithId(Guid id) => id == Id ? this : new Param(id, Prefix, Markers, Paren, Value, BeforeComma);
            public Param WithPrefix(string prefix) => prefix == Prefix ? this : new Param(Id, prefix, Markers, Paren, Value, BeforeComma);
            public Param WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Param(Id, Prefix, markers, Paren, Value, BeforeComma);
            public Param WithParen(ParenGroup? paren) => ReferenceEquals(paren, Paren) ? this : new Param(Id, Prefix, Markers, paren, Value, BeforeComma);
            public Param WithValue(Literal? value) => ReferenceEquals(value, Value) ? this : new Param(Id, Prefix, Markers, Paren, value, BeforeComma);
            public Param WithBeforeComma(string? beforeComma) => beforeComma == BeforeComma ? this : new Param(Id, Prefix, Markers, Paren, Value, beforeComma);
        }
    }

    /// <summary>
    /// A parenthesized path step — one grammar, several dialect meanings, all
    /// resolved by attestation (WPF splits on the LAST dot, so a dotted owner is
    /// legal): (Grid.Row) attached; (local:Owner.Prop) prefixed attached; (A.B.C)
    /// owner A.B + member C; (Foo) WPF simple paren member / Avalonia type CAST
    /// (owner null, attestation decides); (0) WPF parameter index; (abc) WPF
    /// literal paren string. Avalonia disambiguates cast-vs-attached the same
    /// lexical way (no dot means cast), so one node suffices; only the
    /// double-paren cast form gets its own node (Cast).
    /// </summary>
    public sealed class Paren : Segment
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public AccessorKind Accessor { get; }
        public Ident? Owner { get; }
        /// <summary>Whitespace before the owner/member dot; null when Owner is null.</summary>
        public string? BeforeDot { get; }
        public Ident Member { get; }
        public string BeforeClosingParen { get; }

        public Paren(Guid id, string prefix, Markers markers, AccessorKind accessor, Ident? owner, string? beforeDot, Ident member, string beforeClosingParen)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Accessor = accessor;
            Owner = owner;
            BeforeDot = beforeDot;
            Member = member;
            BeforeClosingParen = beforeClosingParen;
        }

        public Tree WithId(Guid id) => id == Id ? this : new Paren(id, Prefix, Markers, Accessor, Owner, BeforeDot, Member, BeforeClosingParen);
        public Paren WithPrefix(string prefix) => prefix == Prefix ? this : new Paren(Id, prefix, Markers, Accessor, Owner, BeforeDot, Member, BeforeClosingParen);
        public Paren WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Paren(Id, Prefix, markers, Accessor, Owner, BeforeDot, Member, BeforeClosingParen);
        public Paren WithAccessor(AccessorKind accessor) => accessor == Accessor ? this : new Paren(Id, Prefix, Markers, accessor, Owner, BeforeDot, Member, BeforeClosingParen);
        public Paren WithOwner(Ident? owner) => ReferenceEquals(owner, Owner) ? this : new Paren(Id, Prefix, Markers, Accessor, owner, BeforeDot, Member, BeforeClosingParen);
        public Paren WithBeforeDot(string? beforeDot) => beforeDot == BeforeDot ? this : new Paren(Id, Prefix, Markers, Accessor, Owner, beforeDot, Member, BeforeClosingParen);
        public Paren WithMember(Ident member) => ReferenceEquals(member, Member) ? this : new Paren(Id, Prefix, Markers, Accessor, Owner, BeforeDot, member, BeforeClosingParen);
        public Paren WithBeforeClosingParen(string beforeClosingParen) => beforeClosingParen == BeforeClosingParen ? this : new Paren(Id, Prefix, Markers, Accessor, Owner, BeforeDot, Member, beforeClosingParen);
    }

    /// <summary>
    /// The double-paren cast form — shared grammar of WinUI x:Bind and Avalonia
    /// bindings (Avalonia allows it mid-path in ordinary {Binding}):
    /// ((TextBox)obj).Text; $parent.((Button)DataContext).Tag. Prefix is before
    /// the OUTER paren; Type is the inner (ns:Type) group; the operand (member and
    /// optional indexer) sits inside the outer parens.
    /// </summary>
    public sealed class Cast : Segment
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public AccessorKind Accessor { get; }
        public ParenGroup Type { get; }
        public PropertyPath Operand { get; }
        public string BeforeClosingParen { get; }

        public Cast(Guid id, string prefix, Markers markers, AccessorKind accessor, ParenGroup type, PropertyPath operand, string beforeClosingParen)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Accessor = accessor;
            Type = type;
            Operand = operand;
            BeforeClosingParen = beforeClosingParen;
        }

        public Tree WithId(Guid id) => id == Id ? this : new Cast(id, Prefix, Markers, Accessor, Type, Operand, BeforeClosingParen);
        public Cast WithPrefix(string prefix) => prefix == Prefix ? this : new Cast(Id, prefix, Markers, Accessor, Type, Operand, BeforeClosingParen);
        public Cast WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Cast(Id, Prefix, markers, Accessor, Type, Operand, BeforeClosingParen);
        public Cast WithAccessor(AccessorKind accessor) => accessor == Accessor ? this : new Cast(Id, Prefix, Markers, accessor, Type, Operand, BeforeClosingParen);
        public Cast WithType(ParenGroup type) => ReferenceEquals(type, Type) ? this : new Cast(Id, Prefix, Markers, Accessor, type, Operand, BeforeClosingParen);
        public Cast WithOperand(PropertyPath operand) => ReferenceEquals(operand, Operand) ? this : new Cast(Id, Prefix, Markers, Accessor, Type, operand, BeforeClosingParen);
        public Cast WithBeforeClosingParen(string beforeClosingParen) => beforeClosingParen == BeforeClosingParen ? this : new Cast(Id, Prefix, Markers, Accessor, Type, Operand, beforeClosingParen);
    }

    /// <summary>
    /// WinUI/UWP x:Bind function binding — the one dialect where a path step takes
    /// arguments: {x:Bind ViewModel.Format(Item.Value, 'N2'), Mode=OneWay}.
    /// Arguments reuse Argument (always positional; values are PropertyPaths or
    /// Literal constants). Name.Type attests to the resolved method. Gated on the
    /// x:Bind context, which is syntactic (x: resolves via in-document xmlns). NOT
    /// valid in WPF, MAUI, or Avalonia paths — Avalonia method/command bindings
    /// are a bare identifier, never a call.
    /// </summary>
    public sealed class FunctionCall : Segment
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public AccessorKind Accessor { get; }
        public Ident Name { get; }
        public string BeforeOpenParen { get; }
        public IList<Argument> Arguments { get; }
        public string BeforeClosingParen { get; }

        public FunctionCall(Guid id, string prefix, Markers markers, AccessorKind accessor, Ident name, string beforeOpenParen, IList<Argument> arguments, string beforeClosingParen)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Accessor = accessor;
            Name = name;
            BeforeOpenParen = beforeOpenParen;
            Arguments = arguments;
            BeforeClosingParen = beforeClosingParen;
        }

        public Tree WithId(Guid id) => id == Id ? this : new FunctionCall(id, Prefix, Markers, Accessor, Name, BeforeOpenParen, Arguments, BeforeClosingParen);
        public FunctionCall WithPrefix(string prefix) => prefix == Prefix ? this : new FunctionCall(Id, prefix, Markers, Accessor, Name, BeforeOpenParen, Arguments, BeforeClosingParen);
        public FunctionCall WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new FunctionCall(Id, Prefix, markers, Accessor, Name, BeforeOpenParen, Arguments, BeforeClosingParen);
        public FunctionCall WithAccessor(AccessorKind accessor) => accessor == Accessor ? this : new FunctionCall(Id, Prefix, Markers, accessor, Name, BeforeOpenParen, Arguments, BeforeClosingParen);
        public FunctionCall WithName(Ident name) => ReferenceEquals(name, Name) ? this : new FunctionCall(Id, Prefix, Markers, Accessor, name, BeforeOpenParen, Arguments, BeforeClosingParen);
        public FunctionCall WithBeforeOpenParen(string beforeOpenParen) => beforeOpenParen == BeforeOpenParen ? this : new FunctionCall(Id, Prefix, Markers, Accessor, Name, beforeOpenParen, Arguments, BeforeClosingParen);
        public FunctionCall WithArguments(IList<Argument> arguments) => ReferenceEquals(arguments, Arguments) ? this : new FunctionCall(Id, Prefix, Markers, Accessor, Name, BeforeOpenParen, arguments, BeforeClosingParen);
        public FunctionCall WithBeforeClosingParen(string beforeClosingParen) => beforeClosingParen == BeforeClosingParen ? this : new FunctionCall(Id, Prefix, Markers, Accessor, Name, BeforeOpenParen, Arguments, beforeClosingParen);
    }

    /// <summary>
    /// Avalonia element-name step: {Binding #slider.Value}. WPF's ElementName=
    /// concept expressed inside the path grammar (root-only in Avalonia), hence a
    /// distinct-grammar node. Name.Type attests to the x:Name'd element's type.
    /// Prefix is before the hash.
    /// </summary>
    public sealed class Element : Segment
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public AccessorKind Accessor { get; }
        public Ident Name { get; }

        public Element(Guid id, string prefix, Markers markers, AccessorKind accessor, Ident name)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Accessor = accessor;
            Name = name;
        }

        public Tree WithId(Guid id) => id == Id ? this : new Element(id, Prefix, Markers, Accessor, Name);
        public Element WithPrefix(string prefix) => prefix == Prefix ? this : new Element(Id, prefix, Markers, Accessor, Name);
        public Element WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Element(Id, Prefix, markers, Accessor, Name);
        public Element WithAccessor(AccessorKind accessor) => accessor == Accessor ? this : new Element(Id, Prefix, Markers, accessor, Name);
        public Element WithName(Ident name) => ReferenceEquals(name, Name) ? this : new Element(Id, Prefix, Markers, Accessor, name);
    }

    /// <summary>
    /// Avalonia relative-source step (root-only): $self; $parent; $parent[Border]
    /// (ancestorType only); $parent[2] (ancestorLevel only — an integer token is
    /// parsed into AncestorLevel, not a type; lexical: digits cannot start a type
    /// name); $parent[local:MyControl;1] (both, SEMICOLON separated). Nullable
    /// Before* fields encode which tokens are present. WPF's equivalent
    /// ({RelativeSource FindAncestor, AncestorType={x:Type Border}}) needs NO
    /// nodes here — it is plain MarkupExtension structure; only Avalonia gave the
    /// concept its own grammar. Prefix is before the dollar sign.
    /// </summary>
    public sealed class Relative : Segment
    {
        public enum RelativeKind { Self, Parent }

        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public AccessorKind Accessor { get; }
        public RelativeKind Kind { get; }
        public string? BeforeOpenBracket { get; }
        public Ident? AncestorType { get; }
        public string? BeforeSemicolon { get; }
        public Literal? AncestorLevel { get; }
        public string? BeforeClosingBracket { get; }

        public Relative(Guid id, string prefix, Markers markers, AccessorKind accessor, RelativeKind kind, string? beforeOpenBracket, Ident? ancestorType, string? beforeSemicolon, Literal? ancestorLevel, string? beforeClosingBracket)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Accessor = accessor;
            Kind = kind;
            BeforeOpenBracket = beforeOpenBracket;
            AncestorType = ancestorType;
            BeforeSemicolon = beforeSemicolon;
            AncestorLevel = ancestorLevel;
            BeforeClosingBracket = beforeClosingBracket;
        }

        public Tree WithId(Guid id) => id == Id ? this : new Relative(id, Prefix, Markers, Accessor, Kind, BeforeOpenBracket, AncestorType, BeforeSemicolon, AncestorLevel, BeforeClosingBracket);
        public Relative WithPrefix(string prefix) => prefix == Prefix ? this : new Relative(Id, prefix, Markers, Accessor, Kind, BeforeOpenBracket, AncestorType, BeforeSemicolon, AncestorLevel, BeforeClosingBracket);
        public Relative WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Relative(Id, Prefix, markers, Accessor, Kind, BeforeOpenBracket, AncestorType, BeforeSemicolon, AncestorLevel, BeforeClosingBracket);
        public Relative WithAccessor(AccessorKind accessor) => accessor == Accessor ? this : new Relative(Id, Prefix, Markers, accessor, Kind, BeforeOpenBracket, AncestorType, BeforeSemicolon, AncestorLevel, BeforeClosingBracket);
        public Relative WithKind(RelativeKind kind) => kind == Kind ? this : new Relative(Id, Prefix, Markers, Accessor, kind, BeforeOpenBracket, AncestorType, BeforeSemicolon, AncestorLevel, BeforeClosingBracket);
        public Relative WithBeforeOpenBracket(string? beforeOpenBracket) => beforeOpenBracket == BeforeOpenBracket ? this : new Relative(Id, Prefix, Markers, Accessor, Kind, beforeOpenBracket, AncestorType, BeforeSemicolon, AncestorLevel, BeforeClosingBracket);
        public Relative WithAncestorType(Ident? ancestorType) => ReferenceEquals(ancestorType, AncestorType) ? this : new Relative(Id, Prefix, Markers, Accessor, Kind, BeforeOpenBracket, ancestorType, BeforeSemicolon, AncestorLevel, BeforeClosingBracket);
        public Relative WithBeforeSemicolon(string? beforeSemicolon) => beforeSemicolon == BeforeSemicolon ? this : new Relative(Id, Prefix, Markers, Accessor, Kind, BeforeOpenBracket, AncestorType, beforeSemicolon, AncestorLevel, BeforeClosingBracket);
        public Relative WithAncestorLevel(Literal? ancestorLevel) => ReferenceEquals(ancestorLevel, AncestorLevel) ? this : new Relative(Id, Prefix, Markers, Accessor, Kind, BeforeOpenBracket, AncestorType, BeforeSemicolon, ancestorLevel, BeforeClosingBracket);
        public Relative WithBeforeClosingBracket(string? beforeClosingBracket) => beforeClosingBracket == BeforeClosingBracket ? this : new Relative(Id, Prefix, Markers, Accessor, Kind, BeforeOpenBracket, AncestorType, BeforeSemicolon, AncestorLevel, beforeClosingBracket);
    }

    /// <summary>
    /// The Avalonia stream operator as a path STEP — it binds the value produced
    /// so far (IObservable/Task), may appear mid-path and repeatedly, so it cannot
    /// be a whole-path flag: Activity^; Foo^.Bar^; ^ (stream of the source
    /// itself); .^. Prefix is before the caret. Not valid in WPF/WinUI/MAUI.
    /// </summary>
    public sealed class Stream : Segment
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public AccessorKind Accessor { get; }

        public Stream(Guid id, string prefix, Markers markers, AccessorKind accessor)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Accessor = accessor;
        }

        public Tree WithId(Guid id) => id == Id ? this : new Stream(id, Prefix, Markers, Accessor);
        public Stream WithPrefix(string prefix) => prefix == Prefix ? this : new Stream(Id, prefix, Markers, Accessor);
        public Stream WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Stream(Id, Prefix, markers, Accessor);
        public Stream WithAccessor(AccessorKind accessor) => accessor == Accessor ? this : new Stream(Id, Prefix, Markers, accessor);
    }
}

/// <summary>
/// Avalonia logical-negation prefix on a binding value: {Binding !IsEnabled}.
/// Wraps its operand, so the to-bool double-negation !!Items.Count is two nested
/// Negations — nesting matches the operator semantics and keeps this one node
/// type. Only legal as a leading run (Avalonia rejects mid-path bangs); that is a
/// parser/validation concern, not a shape. Prefix is before the bang. Not valid
/// WPF/WinUI/MAUI grammar.
/// </summary>
public sealed class Negation : Expression
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public Expression Operand { get; }

    public Negation(Guid id, string prefix, Markers markers, Expression operand)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Operand = operand;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Negation(id, Prefix, Markers, Operand);
    public Negation WithPrefix(string prefix) => prefix == Prefix ? this : new Negation(Id, prefix, Markers, Operand);
    public Negation WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Negation(Id, Prefix, markers, Operand);
    public Negation WithOperand(Expression operand) => ReferenceEquals(operand, Operand) ? this : new Negation(Id, Prefix, Markers, operand);
}

/// <summary>
/// A XAML type-name with optional generic arguments and array subscripts — XAML's
/// generic syntax uses PARENTHESES, and WPF's GenericTypeNameParser additionally
/// accepts array subscripts folded onto the name:
///
///   x:TypeArguments="scg:Dictionary(sys:String, scg:List(sys:Int32))"  nests
///   x:TypeArguments="sys:String[]"    Subscript = "[]"
///   x:TypeArguments="sys:Int32[,][]"  Subscript = "[,][]" (verbatim; rank
///                                     derivable; interior spaces legal and kept)
///
/// Parsed as TypeName only where the grammar guarantees a type: x:TypeArguments
/// values (the x: prefix resolves syntactically via in-document xmlns). Everywhere
/// else type-valued tokens ({x:Type Grid}'s positional arg, DataType=, cast and
/// ancestor type tokens) are lexically ordinary tokens and stay Literal/Ident with
/// JavaType attestation — parse shape must not depend on resolution.
/// </summary>
public sealed class TypeName : Expression
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    /// <summary>Verbatim, possibly xmlns-prefixed: scg:List — resolved type in Name.Type.</summary>
    public Ident Name { get; }
    /// <summary>Null when non-generic.</summary>
    public GenericArguments? TypeArguments { get; }
    /// <summary>Verbatim array subscript run incl. any interior whitespace; null if none.</summary>
    public string? Subscript { get; }

    public TypeName(Guid id, string prefix, Markers markers, Ident name, GenericArguments? typeArguments, string? subscript)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Name = name;
        TypeArguments = typeArguments;
        Subscript = subscript;
    }

    public Tree WithId(Guid id) => id == Id ? this : new TypeName(id, Prefix, Markers, Name, TypeArguments, Subscript);
    public TypeName WithPrefix(string prefix) => prefix == Prefix ? this : new TypeName(Id, prefix, Markers, Name, TypeArguments, Subscript);
    public TypeName WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new TypeName(Id, Prefix, markers, Name, TypeArguments, Subscript);
    public TypeName WithName(Ident name) => ReferenceEquals(name, Name) ? this : new TypeName(Id, Prefix, Markers, name, TypeArguments, Subscript);
    public TypeName WithTypeArguments(GenericArguments? typeArguments) => ReferenceEquals(typeArguments, TypeArguments) ? this : new TypeName(Id, Prefix, Markers, Name, typeArguments, Subscript);
    public TypeName WithSubscript(string? subscript) => subscript == Subscript ? this : new TypeName(Id, Prefix, Markers, Name, TypeArguments, subscript);

    /// <summary>The parenthesized argument list: ( sys:String , sys:Int32 )</summary>
    public sealed class GenericArguments : Xml
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public IList<TypeArgument> Arguments { get; }
        public string BeforeClosingParen { get; }

        public GenericArguments(Guid id, string prefix, Markers markers, IList<TypeArgument> arguments, string beforeClosingParen)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Arguments = arguments;
            BeforeClosingParen = beforeClosingParen;
        }

        public Tree WithId(Guid id) => id == Id ? this : new GenericArguments(id, Prefix, Markers, Arguments, BeforeClosingParen);
        public GenericArguments WithPrefix(string prefix) => prefix == Prefix ? this : new GenericArguments(Id, prefix, Markers, Arguments, BeforeClosingParen);
        public GenericArguments WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new GenericArguments(Id, Prefix, markers, Arguments, BeforeClosingParen);
        public GenericArguments WithArguments(IList<TypeArgument> arguments) => ReferenceEquals(arguments, Arguments) ? this : new GenericArguments(Id, Prefix, Markers, arguments, BeforeClosingParen);
        public GenericArguments WithBeforeClosingParen(string beforeClosingParen) => beforeClosingParen == BeforeClosingParen ? this : new GenericArguments(Id, Prefix, Markers, Arguments, beforeClosingParen);
    }

    /// <summary>One generic argument with its trailing-comma gap.</summary>
    public sealed class TypeArgument : Xml
    {
        public Guid Id { get; }
        public string Prefix { get; }
        public Markers Markers { get; }
        public TypeName Type { get; }
        /// <summary>Before the trailing ','; null on the last argument.</summary>
        public string? BeforeComma { get; }

        public TypeArgument(Guid id, string prefix, Markers markers, TypeName type, string? beforeComma)
        {
            Id = id;
            Prefix = prefix;
            Markers = markers;
            Type = type;
            BeforeComma = beforeComma;
        }

        public Tree WithId(Guid id) => id == Id ? this : new TypeArgument(id, Prefix, Markers, Type, BeforeComma);
        public TypeArgument WithPrefix(string prefix) => prefix == Prefix ? this : new TypeArgument(Id, prefix, Markers, Type, BeforeComma);
        public TypeArgument WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new TypeArgument(Id, Prefix, markers, Type, BeforeComma);
        public TypeArgument WithType(TypeName type) => ReferenceEquals(type, Type) ? this : new TypeArgument(Id, Prefix, Markers, type, BeforeComma);
        public TypeArgument WithBeforeComma(string? beforeComma) => beforeComma == BeforeComma ? this : new TypeArgument(Id, Prefix, Markers, Type, beforeComma);
    }
}

/// <summary>
/// A parenthesized single token — the shared shape for contexts where parens wrap
/// one uninterpreted token whose meaning is attestation's job: the indexer param
/// prefix ([(sys:Int32)42], [(2)], [(abc)] — see Indexer.Param) and the cast
/// target type (((TextBox)obj) — see Cast). Token is a verbatim Literal
/// (attestation resolves it to a type, a parameter index, or nothing). Distinct
/// from the Paren path segment, whose content has owner-dot-member structure.
/// Prefix is before the opening paren.
/// </summary>
public sealed class ParenGroup : Xml
{
    public Guid Id { get; }
    public string Prefix { get; }
    public Markers Markers { get; }
    public Literal Token { get; }
    public string BeforeClosingParen { get; }

    public ParenGroup(Guid id, string prefix, Markers markers, Literal token, string beforeClosingParen)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Token = token;
        BeforeClosingParen = beforeClosingParen;
    }

    public Tree WithId(Guid id) => id == Id ? this : new ParenGroup(id, Prefix, Markers, Token, BeforeClosingParen);
    public ParenGroup WithPrefix(string prefix) => prefix == Prefix ? this : new ParenGroup(Id, prefix, Markers, Token, BeforeClosingParen);
    public ParenGroup WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new ParenGroup(Id, Prefix, markers, Token, BeforeClosingParen);
    public ParenGroup WithToken(Literal token) => ReferenceEquals(token, Token) ? this : new ParenGroup(Id, Prefix, Markers, token, BeforeClosingParen);
    public ParenGroup WithBeforeClosingParen(string beforeClosingParen) => beforeClosingParen == BeforeClosingParen ? this : new ParenGroup(Id, Prefix, Markers, Token, beforeClosingParen);
}
