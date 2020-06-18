package org.openrewrite.xml.maven;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.internal.XmlParserVisitor;
import org.openrewrite.xml.internal.grammar.XMLLexer;
import org.openrewrite.xml.internal.grammar.XMLParser;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MavenParser {
    private final XmlParser xmlParser = new XmlParser();

    public List<Maven.Pom> parse(List<Path> sourceFiles, @Nullable Path relativeTo) {
        return sourceFiles.stream().map(source -> parse(source, relativeTo)).collect(toList());
    }

    public Maven.Pom parse(Path sourceFile, @Nullable Path relativeTo) {
        return new Maven.Pom(xmlParser.parse(sourceFile, relativeTo));
    }
}
