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
package org.openrewrite.maven;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenModel;
import org.openrewrite.xml.XmlParser;

import java.io.File;
import java.nio.file.Path;
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
