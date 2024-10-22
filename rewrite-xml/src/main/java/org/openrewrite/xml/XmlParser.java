/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.xml;

import org.antlr.v4.runtime.*;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.marker.TypeReference;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;
import org.openrewrite.xml.internal.XmlParserVisitor;
import org.openrewrite.xml.internal.grammar.XMLLexer;
import org.openrewrite.xml.internal.grammar.XMLParser;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class XmlParser implements Parser {
    private static final Set<String> ACCEPTED_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "xml",
            "wsdl",
            "xhtml",
            "xsd",
            "xsl",
            "xslt",
            "xmi",
            "tld",
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
            "fsproj"));

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).map(input -> {
            parsingListener.startedParsing(input);
            Path path = input.getRelativePath(relativeTo);
            try (EncodingDetectingInputStream is = input.getSource(ctx)) {
                String sourceStr = is.readFully();

                XMLLexer lexer = new XMLLexer(CharStreams.fromString(sourceStr));
                lexer.removeErrorListeners();
                lexer.addErrorListener(new ForwardingErrorListener(input.getPath(), ctx));

                XMLParser parser = new XMLParser(new CommonTokenStream(lexer));
                parser.removeErrorListeners();
                parser.addErrorListener(new ForwardingErrorListener(input.getPath(), ctx));

                Xml.Document document = new XmlParserVisitor(
                        path,
                        input.getFileAttributes(),
                        sourceStr,
                        is.getCharset(),
                        is.isCharsetBomMarked()
                ).visitDocument(parser.document());
                parsingListener.parsed(input, document);
                document = (Xml.Document) addJavaTypeOrPackageMarkers().visitDocument(document, ctx);
                return requirePrintEqualsInput(document, input, relativeTo, ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    @Override
    public Stream<SourceFile> parse(@Language("xml") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public boolean accept(Path path) {
        String p = path.toString();
        int dot = p.lastIndexOf('.');
        if (0 < dot && dot < (p.length() - 1)) {
            if (ACCEPTED_FILE_EXTENSIONS.contains(p.substring(dot + 1))) {
                return true;
            }
        }
        return path.endsWith("packages.config");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.xml");
    }

    private static class ForwardingErrorListener extends BaseErrorListener {
        private final Path sourcePath;
        private final ExecutionContext ctx;

        private ForwardingErrorListener(Path sourcePath, ExecutionContext ctx) {
            this.sourcePath = sourcePath;
            this.ctx = ctx;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            ctx.getOnError().accept(new XmlParsingException(sourcePath,
                    String.format("Syntax error in %s at line %d:%d %s.", sourcePath, line, charPositionInLine, msg), e));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends org.openrewrite.Parser.Builder {

        public Builder() {
            super(Xml.Document.class);
        }

        @Override
        public XmlParser build() {
            return new XmlParser();
        }

        @Override
        public String getDslName() {
            return "xml";
        }
    }

    private XmlVisitor<ExecutionContext> addJavaTypeOrPackageMarkers() {
        XPathMatcher classXPath = new XPathMatcher("//@class[contains(., '.')]");
        XPathMatcher typeXPath = new XPathMatcher("//@type[contains(., '.')]");
        XPathMatcher tags = new XPathMatcher("//value[contains(text(), '.')]");

        return new XmlVisitor<ExecutionContext>() {

            @Override
            public Xml visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                Xml.Attribute attrib = (Xml.Attribute) super.visitAttribute(attribute, ctx);
                if (classXPath.matches(getCursor()) || typeXPath.matches(getCursor())) {
                    return attrib.withMarkers(attrib.getMarkers().withMarkers(Collections.singletonList(new TypeReference(attrib.getId(), "Java"))));

                }
                return attrib;
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag tg = (Xml.Tag) super.visitTag(tag, ctx);
                if (tags.matches(getCursor())) {
                    if (tg.getValue().isPresent()) {
                        return tg.withMarkers(tg.getMarkers().withMarkers(Collections.singletonList(new TypeReference(tg.getId(), "Java"))));
                    }
                }
                return tg;
            }
        };
    }
}
