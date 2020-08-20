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

import org.eclipse.aether.repository.RemoteRepository;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenModel;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MavenParser implements Parser<Maven.Pom> {
    private final XmlParser xmlParser = new XmlParser();
    private final boolean resolveDependencies;
    private final File localRepository;
    private final List<RemoteRepository> remoteRepositories;

    private MavenParser(boolean resolveDependencies, File localRepository, List<RemoteRepository> remoteRepositories) {
        this.resolveDependencies = resolveDependencies;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<Maven.Pom> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo) {
        Iterable<Input> pomSourceFiles = acceptedInputs(sourceFiles);

        List<MavenModel> modules = new MavenModuleLoader(resolveDependencies, localRepository, remoteRepositories)
                .load(pomSourceFiles);

        List<Maven.Pom> poms = new ArrayList<>();
        Iterator<Xml.Document> xmlDocuments = xmlParser.parseInputs(pomSourceFiles, relativeTo).iterator();
        for (MavenModel module : modules) {
            poms.add(new Maven.Pom(module, xmlDocuments.next()));
        }

        return poms;
    }

    @Override
    public boolean accept(Path path) {
        return path.getFileName().toString().equals("pom.xml");
    }

    public static class Builder {
        private boolean resolveDependencies = true;
        private File localRepository = new File(System.getProperty("user.home") + "/.m2/rewrite");
        private List<RemoteRepository> remoteRepositories = new ArrayList<>();

        public Builder() {
            remoteRepositories.add(new RemoteRepository.Builder("central", "default",
                    "https://repo1.maven.org/maven2/").build()
            );
        }

        public Builder localRepository(File localRepository) {
            this.localRepository = localRepository;
            return this;
        }

        public Builder resolveDependencies(boolean resolveDependencies) {
            this.resolveDependencies = resolveDependencies;
            return this;
        }

        public Builder remoteRepositories(List<RemoteRepository> remoteRepositories) {
            this.remoteRepositories = remoteRepositories;
            return this;
        }

        public Builder addRemoteRepository(RemoteRepository remoteRepository) {
            this.remoteRepositories.add(remoteRepository);
            return this;
        }

        public MavenParser build() {
            return new MavenParser(resolveDependencies, localRepository, remoteRepositories);
        }
    }
}
