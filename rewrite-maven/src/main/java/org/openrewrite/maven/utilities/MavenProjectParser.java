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
import java.util.Objects;
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

        // sort maven projects so that multi-module build dependencies parse before the dependent projects
        mavens.sort((m1, m2) -> {
            Pom m1Model = m1.getModel();
            List<Pom.Dependency> m1Dependencies = m1Model.getDependencies().stream().filter(d -> d.getRepository() == null).collect(Collectors.toList());
            Pom m2Model = m2.getModel();
            List<Pom.Dependency> m2Dependencies = m2Model.getDependencies().stream().filter(d -> d.getRepository() == null).collect(Collectors.toList());
            if (m1Dependencies.stream().anyMatch(m1Dependency -> m1Dependency.getGroupId().equals(m2Model.getGroupId()) && m1Dependency.getArtifactId().equals(m2Model.getArtifactId()))) {
                return 1;
            } else if (m2Dependencies.stream().anyMatch(m2Dependency -> m2Dependency.getGroupId().equals(m1Model.getGroupId()) && m2Dependency.getArtifactId().equals(m1Model.getArtifactId()))) {
                return -1;
            } else {
                return 0;
            }
        });

        JavaParser javaParser = javaParserBuilder
                .build();

        for (Maven maven : mavens) {
            javaParser.setClasspath(downloadArtifacts(maven.getModel().getDependencies(Scope.Compile)));
            sourceFiles.addAll(
                    javaParser.parse(maven.getJavaSources(projectDirectory, ctx), projectDirectory, ctx)
            );

            javaParser.setClasspath(downloadArtifacts(maven.getModel().getDependencies(Scope.Test)));
            sourceFiles.addAll(
                    javaParser.parse(maven.getTestJavaSources(projectDirectory, ctx), projectDirectory, ctx)
            );

            List<Path> resources = new ArrayList<>(maven.getResources(projectDirectory, ctx));
            resources.addAll(maven.getTestResources(projectDirectory, ctx));

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
                .filter(d -> d.getRepository() != null)
                .map(artifactDownloader::downloadArtifact)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
