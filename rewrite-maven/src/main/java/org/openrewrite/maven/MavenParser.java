package org.openrewrite.maven;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XmlParser;

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
