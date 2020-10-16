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
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.internal.XmlParserVisitor;
import org.openrewrite.xml.internal.grammar.XMLLexer;
import org.openrewrite.xml.internal.grammar.XMLParser;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class XmlParser implements Parser<Xml.Document> {
    @Override
    public List<Xml.Document> parseInputs(Iterable<Input> sourceFiles, @Nullable URI relativeTo) {
        return acceptedInputs(sourceFiles).stream()
                .map(sourceFile -> {
                    try {
                        XMLParser parser = new XMLParser(new CommonTokenStream(new XMLLexer(
                                CharStreams.fromStream(sourceFile.getSource()))));

                        return new XmlParserVisitor(
                                sourceFile.getRelativePath(relativeTo),
                                StringUtils.readFully(sourceFile.getSource())
                        ).visitDocument(parser.document());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .collect(toList());
    }

    public Xml.Tag parseTag(String tag) {
        XMLParser parser = new XMLParser(new CommonTokenStream(new XMLLexer(
                CharStreams.fromString(tag))));
        return (Xml.Tag) new XmlParserVisitor(null, tag).visitContent(parser.content());
    }

    @Override
    public boolean accept(URI path) {
        return path.toString().endsWith(".xml");
    }
}
