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
package org.openrewrite.maven.utilities;

import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.yaml.YamlParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parse a Maven project on disk into a list of {@link org.openrewrite.SourceFile} including
 * Maven, Java, YAML, properties, and XML AST representations of sources and resources found.
 */
public class MavenProjectParser {
    private final MavenParser mavenParser;
    private final MavenArtifactDownloader artifactDownloader;
    private final JavaParser.Builder<?, ?> javaParserBuilder;
    private final ExecutionContext ctx;

    public MavenProjectParser(MavenArtifactDownloader artifactDownloader,
                              MavenParser.Builder mavenParserBuilder,
                              JavaParser.Builder<?, ?> javaParserBuilder,
                              ExecutionContext ctx) {
        this.mavenParser = mavenParserBuilder.build();
        this.artifactDownloader = artifactDownloader;
        this.javaParserBuilder = javaParserBuilder;
        this.ctx = ctx;
    }

    public List<SourceFile> parse(Path projectDirectory) {
        List<Maven> mavens = mavenParser.parse(Maven.getMavenPoms(projectDirectory, ctx), projectDirectory, ctx);
        List<SourceFile> sourceFiles = new ArrayList<>(mavens);

        for (Maven maven : mavens) {
            sourceFiles.addAll(
                    javaParserBuilder
                            .classpath(downloadArtifacts(maven.getModel().getDependencies(Scope.Compile)))
                            .build()
                            .parse(maven.getJavaSources(ctx), projectDirectory, ctx)
            );

            sourceFiles.addAll(
                    javaParserBuilder
                            .classpath(downloadArtifacts(maven.getModel().getDependencies(Scope.Test)))
                            .build()
                            .parse(maven.getTestJavaSources(ctx), projectDirectory, ctx)
            );

            List<Path> resources = new ArrayList<>(maven.getResources(ctx));
            resources.addAll(maven.getTestResources(ctx));

            sourceFiles.addAll(
                    XmlParser.builder().build().parse(
                            resources.stream()
                                    .filter(p -> p.getFileName().toString().endsWith(".xml"))
                                    .collect(Collectors.toList()),
                            projectDirectory,
                            ctx
                    )
            );

            sourceFiles.addAll(
                    YamlParser.builder().build().parse(
                            resources.stream()
                                    .filter(p -> p.getFileName().toString().endsWith(".yml") || p.getFileName().toString().endsWith(".yaml"))
                                    .collect(Collectors.toList()),
                            projectDirectory,
                            ctx
                    )
            );

            sourceFiles.addAll(
                    PropertiesParser.builder().build().parse(
                            resources.stream()
                                    .filter(p -> p.getFileName().toString().endsWith(".properties"))
                                    .collect(Collectors.toList()),
                            projectDirectory,
                            ctx
                    )
            );
        }

        return sourceFiles;
    }

    private List<Path> downloadArtifacts(Set<Pom.Dependency> dependencies) {
        return dependencies.stream()
                .map(artifactDownloader::downloadArtifact)
                .collect(Collectors.toList());
    }
}
