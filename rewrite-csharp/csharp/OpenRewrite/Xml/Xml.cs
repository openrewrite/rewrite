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

namespace OpenRewrite.Xml;

/// <summary>
/// Base interface for all XML LST elements.
/// Uses string prefixes (not Space) for whitespace tracking.
/// </summary>
public interface Xml : Tree
{
    string Prefix { get; }

    Markers Markers { get; }
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

    public Tag(Guid id, string prefix, Markers markers, string name, IList<Attribute> attributes, IList<Content>? contentList, Closing? closingTag, string beforeTagDelimiterPrefix)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Name = name;
        Attributes = attributes;
        ContentList = contentList;
        ClosingTag = closingTag;
        BeforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Tag(id, Prefix, Markers, Name, Attributes, ContentList, ClosingTag, BeforeTagDelimiterPrefix);
    public Tag WithPrefix(string prefix) => prefix == Prefix ? this : new Tag(Id, prefix, Markers, Name, Attributes, ContentList, ClosingTag, BeforeTagDelimiterPrefix);
    public Tag WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Tag(Id, Prefix, markers, Name, Attributes, ContentList, ClosingTag, BeforeTagDelimiterPrefix);
    public Tag WithName(string name) => name == Name ? this : new Tag(Id, Prefix, Markers, name, Attributes, ContentList, ClosingTag, BeforeTagDelimiterPrefix);
    public Tag WithAttributes(IList<Attribute> attributes) => ReferenceEquals(attributes, Attributes) ? this : new Tag(Id, Prefix, Markers, Name, attributes, ContentList, ClosingTag, BeforeTagDelimiterPrefix);
    public Tag WithContentList(IList<Content>? contentList) => ReferenceEquals(contentList, ContentList) ? this : new Tag(Id, Prefix, Markers, Name, Attributes, contentList, ClosingTag, BeforeTagDelimiterPrefix);
    public Tag WithClosingTag(Closing? closingTag) => ReferenceEquals(closingTag, ClosingTag) ? this : new Tag(Id, Prefix, Markers, Name, Attributes, ContentList, closingTag, BeforeTagDelimiterPrefix);
    public Tag WithBeforeTagDelimiterPrefix(string beforeTagDelimiterPrefix) => beforeTagDelimiterPrefix == BeforeTagDelimiterPrefix ? this : new Tag(Id, Prefix, Markers, Name, Attributes, ContentList, ClosingTag, beforeTagDelimiterPrefix);

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
    public Value Val { get; }

    public Attribute(Guid id, string prefix, Markers markers, Ident key, string beforeEquals, Value val)
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
    public Attribute WithVal(Value val) => ReferenceEquals(val, Val) ? this : new Attribute(Id, Prefix, Markers, Key, BeforeEquals, val);

    public sealed class Value : Xml
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

    public Ident(Guid id, string prefix, Markers markers, string name)
    {
        Id = id;
        Prefix = prefix;
        Markers = markers;
        Name = name;
    }

    public Tree WithId(Guid id) => id == Id ? this : new Ident(id, Prefix, Markers, Name);
    public Ident WithPrefix(string prefix) => prefix == Prefix ? this : new Ident(Id, prefix, Markers, Name);
    public Ident WithMarkers(Markers markers) => ReferenceEquals(markers, Markers) ? this : new Ident(Id, Prefix, markers, Name);
    public Ident WithName(string name) => name == Name ? this : new Ident(Id, Prefix, Markers, name);
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
