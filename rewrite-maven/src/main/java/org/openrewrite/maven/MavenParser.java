package org.openrewrite.maven;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XmlParser;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class MavenParser {
    private final XmlParser xmlParser = new XmlParser();
    private final boolean resolveDependencies;
    private final File localRepository;

    private MavenParser(boolean resolveDependencies, File localRepository) {
        this.resolveDependencies = resolveDependencies;
        this.localRepository = localRepository;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Maven.Pom> parse(List<Path> sourceFiles, @Nullable Path relativeTo) {
        Map<Path, MavenModel> modules = new MavenModuleLoader(resolveDependencies, localRepository)
                .load(sourceFiles);

        return sourceFiles.stream()
                .map(sourceFile -> new Maven.Pom(
                        modules.get(sourceFile),
                        xmlParser.parse(sourceFile, relativeTo)))
                .collect(toList());
    }

    public Maven.Pom parse(Path sourceFile, @Nullable Path relativeTo) {
        return parse(singletonList(sourceFile), relativeTo).get(0);
    }

    public static class Builder {
        private boolean resolveDependencies = true;
        private File localRepository = new File(System.getProperty("user.home") + "/.m2");

        public Builder localRepository(File localRepository) {
            this.localRepository = localRepository;
            return this;
        }

        public Builder resolveDependencies(boolean resolveDependencies) {
            this.resolveDependencies = resolveDependencies;
            return this;
        }

        public MavenParser build() {
            return new MavenParser(resolveDependencies, localRepository);
        }
    }
}
