package org.openrewrite.xml;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.xml.internal.XmlParserVisitor;
import org.openrewrite.xml.internal.grammar.XMLLexer;
import org.openrewrite.xml.internal.grammar.XMLParser;
import org.openrewrite.xml.tree.Xml;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
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

                return parse(file);
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

    public List<Xml.Document> parse(List<Path> sourceFiles) {
        return sourceFiles.stream().map(this::parse).collect(toList());
    }

    public Xml.Document parse(Path sourceFile) {
        try {
            var parser = new XMLParser(new CommonTokenStream(new XMLLexer(
                    CharStreams.fromPath(sourceFile))));

            return new XmlParserVisitor(sourceFile).visitDocument(parser.document());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
