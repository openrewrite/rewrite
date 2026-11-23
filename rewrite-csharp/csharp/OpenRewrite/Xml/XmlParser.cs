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
using Antlr4.Runtime;
using OpenRewrite.Core;
using OpenRewrite.Xml.Grammar;

namespace OpenRewrite.Xml;

/// <summary>
/// Parses XML source text into an OpenRewrite XML LST (Document).
/// Direct port of Java's org.openrewrite.xml.XmlParser.
/// </summary>
public class XmlParser
{
    private static readonly HashSet<string> AcceptedFileExtensions = new(StringComparer.OrdinalIgnoreCase)
    {
        "xml",
        "wsdl",
        "xhtml",
        "xsd",
        "xsl",
        "xslt",
        "xmi",
        "tld",
        "jxb",
        "xjb",
        "jsp",
        // Datastage file formats that are all xml under the hood
        "det",
        "pjb",
        "qjb",
        "sjb",
        "prt",
        "srt",
        "psc",
        "ssc",
        "tbd",
        "tfm",
        "dqs",
        "stp",
        "dcn",
        "pst",
        // .NET project files
        "csproj",
        "vbproj",
        "fsproj",
        "props",
        // JasperReports files
        "jrxml"
    };

    /// <summary>
    /// Parse XML source text into a Document LST.
    /// </summary>
    /// <param name="sourceStr">The XML source text.</param>
    /// <param name="sourcePath">The file path (used for error reporting and the Document's SourcePath). Defaults to "file.xml".</param>
    /// <returns>The parsed Document.</returns>
    public Document Parse(string sourceStr, string sourcePath = "file.xml")
    {
        var charsetName = "UTF-8";
        var charsetBomMarked = false;

        // Detect and strip UTF-8 BOM (matches Java EncodingDetectingInputStream behavior)
        if (sourceStr.Length > 0 && sourceStr[0] == '\uFEFF')
        {
            charsetBomMarked = true;
            sourceStr = sourceStr.Substring(1);
        }

        var lexer = new XMLLexer(new AntlrInputStream(sourceStr));
        lexer.RemoveErrorListeners();
        lexer.AddErrorListener(new ForwardingErrorListener(sourcePath));

        var parser = new Grammar.XMLParser(new CommonTokenStream(lexer));
        parser.RemoveErrorListeners();
        parser.AddErrorListener(new ForwardingErrorListener(sourcePath));

        var document = (Document)new XmlParserVisitor(
            sourcePath,
            null,
            sourceStr,
            charsetName,
            charsetBomMarked
        ).VisitDocument(parser.document());

        return document;
    }

    /// <summary>
    /// Parse and validate round-trip: printed output must match original input.
    /// </summary>
    /// <param name="sourceStr">The XML source text.</param>
    /// <param name="sourcePath">The file path. Defaults to "file.xml".</param>
    /// <returns>The parsed Document.</returns>
    /// <exception cref="InvalidOperationException">If the round-trip validation fails.</exception>
    public Document ParseAndValidate(string sourceStr, string sourcePath = "file.xml")
    {
        var document = Parse(sourceStr, sourcePath);
        var printed = Print(document);
        if (printed != sourceStr)
        {
            throw new InvalidOperationException(
                $"Round-trip validation failed for {sourcePath}: printed output does not match input.");
        }
        return document;
    }

    /// <summary>
    /// Print a Document back to its source text.
    /// </summary>
    public static string Print(Document document)
    {
        var capture = new PrintOutputCapture<object>(null!);
        new XmlPrinter<object>().Visit(document, capture);
        return capture.ToString();
    }

    /// <summary>
    /// Check whether a file path is accepted by this parser.
    /// </summary>
    public bool Accept(string path)
    {
        var dot = path.LastIndexOf('.');
        if (dot > 0 && dot < path.Length - 1)
        {
            if (AcceptedFileExtensions.Contains(path.Substring(dot + 1)))
            {
                return true;
            }
        }
        return path.EndsWith("nuget.config", StringComparison.OrdinalIgnoreCase) ||
               path.EndsWith("packages.config", StringComparison.OrdinalIgnoreCase);
    }

    private class ForwardingErrorListener : IAntlrErrorListener<IToken>, IAntlrErrorListener<int>
    {
        private readonly string _sourcePath;

        public ForwardingErrorListener(string sourcePath)
        {
            _sourcePath = sourcePath;
        }

        public void SyntaxError(TextWriter output, IRecognizer recognizer, IToken offendingSymbol, int line,
            int charPositionInLine, string msg, RecognitionException e)
        {
            throw new XmlParsingException(_sourcePath,
                $"Syntax error in {_sourcePath} at line {line}:{charPositionInLine} {msg}.", e);
        }

        public void SyntaxError(TextWriter output, IRecognizer recognizer, int offendingSymbol, int line,
            int charPositionInLine, string msg, RecognitionException e)
        {
            throw new XmlParsingException(_sourcePath,
                $"Syntax error in {_sourcePath} at line {line}:{charPositionInLine} {msg}.", e);
        }
    }
}

/// <summary>
/// Exception thrown when XML parsing fails.
/// </summary>
public class XmlParsingException : Exception
{
    public string SourcePath { get; }

    public XmlParsingException(string sourcePath, string message, Exception? innerException = null)
        : base(message, innerException)
    {
        SourcePath = sourcePath;
    }
}
