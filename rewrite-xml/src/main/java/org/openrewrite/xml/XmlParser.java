/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.xml;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.internal.XmlParserVisitor;
import org.openrewrite.xml.internal.grammar.XMLLexer;
import org.openrewrite.xml.internal.grammar.XMLParser;
import org.openrewrite.xml.tree.Xml;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class XmlParser {
    public Xml.Document parse(String source) {
        try {
            Path temp = Files.createTempDirectory("sources");

            try {
                var file = temp.resolve("file.xml");
                try {
                    Files.writeString(file, source);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                return parse(file, null);
            } finally {
                // delete temp recursively
                //noinspection ResultOfMethodCallIgnored
                Files.walk(temp)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Xml.Document> parse(List<Path> sourceFiles, @Nullable Path relativeTo) {
        return sourceFiles.stream().map(source -> parse(source, relativeTo)).collect(toList());
    }

    public Xml.Document parse(Path sourceFile, @Nullable Path relativeTo) {
        try {
            var parser = new XMLParser(new CommonTokenStream(new XMLLexer(
                    CharStreams.fromPath(sourceFile))));

            return new XmlParserVisitor(relativeTo == null ? sourceFile : relativeTo.relativize(sourceFile),
                    Files.readString(sourceFile, StandardCharsets.UTF_8)).visitDocument(parser.document());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Xml.Document parseFromString(Path sourceFileLocation, String xmlSource) {
        var parser = new XMLParser(new CommonTokenStream(new XMLLexer(
                CharStreams.fromString(xmlSource))));

        return new XmlParserVisitor(sourceFileLocation, xmlSource).visitDocument(parser.document());
    }

    public Xml.Tag parseTag(String snippet) {
        var parser = new XMLParser(new CommonTokenStream(new XMLLexer(
                CharStreams.fromString(snippet))));
        return (Xml.Tag) new XmlParserVisitor(null, snippet).visitContent(parser.content());
    }
}
