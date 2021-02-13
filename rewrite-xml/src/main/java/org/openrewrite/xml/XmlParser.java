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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.internal.XmlParserVisitor;
import org.openrewrite.xml.internal.grammar.XMLLexer;
import org.openrewrite.xml.internal.grammar.XMLParser;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class XmlParser implements Parser<Xml.Document> {
    private final Listener onParse;

    protected XmlParser(Listener onParse) {
        this.onParse = onParse;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<Xml.Document> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        return acceptedInputs(sourceFiles).stream()
                .map(sourceFile -> {
                    try {
                        onParse.onParseStart(sourceFile.getPath());
                        XMLParser parser = new XMLParser(new CommonTokenStream(new XMLLexer(
                                CharStreams.fromStream(sourceFile.getSource()))));

                        Xml.Document document = new XmlParserVisitor(
                                sourceFile.getRelativePath(relativeTo),
                                StringUtils.readFully(sourceFile.getSource())
                        ).visitDocument(parser.document());

                        onParse.onParseSucceeded(sourceFile.getPath());
                        return document;
                    } catch (IOException e) {
                        onParse.onParseFailed(sourceFile.getPath());
                        ctx.getOnError().accept(e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".xml");
    }

    public static class Builder implements Parser.Builder<Xml.Document> {
        private Listener onParse = Listener.NOOP;

        @Override
        public XmlParser.Builder doOnParse(Listener onParse) {
            this.onParse = onParse;
            return this;
        }

        @Override
        public XmlParser build() {
            return new XmlParser(onParse);
        }
    }
}
