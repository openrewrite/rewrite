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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.marker.JavaProvenance;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.yaml.YamlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

/**
 * Parse a Maven project on disk into a list of {@link org.openrewrite.SourceFile} including
 * Maven, Java, YAML, properties, and XML AST representations of sources and resources found.
 */
public class MavenProjectParser {

    private static final Pattern mavenWrapperVersionPattern = Pattern.compile(".*apache-maven/(.*?)/.*");
    private static final Logger logger = LoggerFactory.getLogger(MavenProjectParser.class);

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
        GitProvenance gitProvenance = GitProvenance.fromProjectDirectory(projectDirectory);
        List<Maven> mavens = mavenParser.parse(Maven.getMavenPoms(projectDirectory, ctx), projectDirectory, ctx);
        List<SourceFile> sourceFiles = new ArrayList<>(mavens);
        Path rootPomPath = Paths.get("pom.xml");
        Maven rootMaven = sourceFiles.stream().filter(f -> f.getSourcePath().equals(rootPomPath))
                .map(Maven.class::cast).findAny()
                .orElseThrow(() -> new RuntimeException("Unable to locate root pom source file"));
        Pom rootMavenModel = rootMaven.getModel();

        // sort maven projects so that multi-module build dependencies parse before the dependent projects
        mavens.sort((m1, m2) -> {
            Pom m1Model = m1.getModel();
            Collection<Pom.Dependency> m1Dependencies = m1Model.getDependencies();
            Pom m2Model = m2.getModel();
            Collection<Pom.Dependency> m2Dependencies = m2Model.getDependencies();
            if (m1Dependencies.stream().anyMatch(m1Dependency ->
                    m1Dependency.getGroupId().equals(m2Model.getGroupId()) &&
                            m1Dependency.getArtifactId().equals(m2Model.getArtifactId()))) {
                return 1;
            } else if (m2Dependencies.stream().anyMatch(m2Dependency ->
                    m2Dependency.getGroupId().equals(m1Model.getGroupId()) &&
                            m2Dependency.getArtifactId().equals(m1Model.getArtifactId()))) {
                return -1;
            } else {
                if (m1.getModel().getGroupId().equals(m2.getModel().getGroupId())) {
                    return m1.getModel().getArtifactId().compareTo(m2.getModel().getArtifactId());
                }
                return m1.getModel().getGroupId().compareTo(m2.getModel().getGroupId());
            }
        });

        JavaParser javaParser = javaParserBuilder
                .build();

        String javaRuntimeVersion = System.getProperty("java.runtime.version");
        String javaVendor = System.getProperty("java.vm.vendor");
        String sourceCompatibility = javaRuntimeVersion;
        String targetCompatibility = javaRuntimeVersion;
        String propertiesSourceCompatibility = rootMavenModel.getValue(rootMavenModel.getEffectiveProperties().get("maven.compiler.source"));
        if (propertiesSourceCompatibility != null) {
            sourceCompatibility = propertiesSourceCompatibility;
        }
        String propertiesTargetCompatibility = rootMavenModel.getValue(rootMavenModel.getEffectiveProperties().get("maven.compiler.target"));
        if (propertiesTargetCompatibility != null) {
            targetCompatibility = propertiesTargetCompatibility;
        }

        Path wrapperPropertiesPath = projectDirectory.resolve(".mvn/wrapper/maven-wrapper.properties");
        String mavenVersion = "3.6";
        if (Files.exists(wrapperPropertiesPath)) {
            try {
                Properties wrapperProperties = new Properties();
                wrapperProperties.load(new FileReader(wrapperPropertiesPath.toFile()));
                String distributionUrl = (String) wrapperProperties.get("distributionUrl");
                if (distributionUrl != null) {
                    Matcher wrapperVersionMatcher = mavenWrapperVersionPattern.matcher(distributionUrl);
                    if (wrapperVersionMatcher.matches()) {
                        mavenVersion = wrapperVersionMatcher.group(1);
                    }
                }
            } catch (IOException e) {
                ctx.getOnError().accept(e);
            }
        }

        JavaProvenance.BuildTool buildTool = new JavaProvenance.BuildTool(JavaProvenance.BuildTool.Type.Maven,
                mavenVersion);

        JavaProvenance.JavaVersion javaVersion = new JavaProvenance.JavaVersion(
                javaRuntimeVersion,
                javaVendor,
                sourceCompatibility,
                targetCompatibility
        );

        JavaProvenance.Publication publication = new JavaProvenance.Publication(
                rootMavenModel.getGroupId(),
                rootMavenModel.getArtifactId(),
                rootMavenModel.getVersion()
        );

        JavaProvenance mainProvenance = new JavaProvenance(
                randomId(),
                rootMavenModel.getName(),
                "main",
                buildTool,
                javaVersion,
                publication
        );

        JavaProvenance testProvenance = new JavaProvenance(
                randomId(),
                rootMavenModel.getName(),
                "test",
                buildTool,
                javaVersion,
                publication
        );

        logger.info("The order in which projects are being parsed is:");
        for (Maven maven : mavens) {
            logger.info("  {}:{}", maven.getModel().getGroupId(), maven.getModel().getArtifactId());
        }

        for (Maven maven : mavens) {
            List<Path> dependencies = downloadArtifacts(maven.getModel().getDependencies(Scope.Compile));
            javaParser.setClasspath(dependencies);
            sourceFiles.addAll(
                    ListUtils.map(javaParser.parse(maven.getJavaSources(projectDirectory, ctx), projectDirectory, ctx),
                            s -> s.withMarkers(s.getMarkers().addIfAbsent(mainProvenance))
                    ));

            List<Path> testDependencies = downloadArtifacts(maven.getModel().getDependencies(Scope.Test));
            javaParser.setClasspath(testDependencies);
            sourceFiles.addAll(
                    ListUtils.map(javaParser.parse(maven.getTestJavaSources(projectDirectory, ctx), projectDirectory, ctx),
                            s -> s.withMarkers(s.getMarkers().addIfAbsent(testProvenance))
                    ));

            parseResources(maven.getResources(projectDirectory, ctx), projectDirectory, sourceFiles, mainProvenance);
            parseResources(maven.getTestResources(projectDirectory, ctx), projectDirectory, sourceFiles, testProvenance);
        }

        return ListUtils.map(sourceFiles, s -> s.withMarkers(s.getMarkers().addIfAbsent(gitProvenance)));
    }

    private void parseResources(List<Path> resources, Path projectDirectory, List<SourceFile> sourceFiles, JavaProvenance javaProvenance) {
        sourceFiles.addAll(
                ListUtils.map(
                        new XmlParser().parse(
                                resources.stream()
                                        .filter(p -> p.getFileName().toString().endsWith(".xml"))
                                        .collect(Collectors.toList()),
                                projectDirectory,
                                ctx
                        ), s -> s.withMarkers(s.getMarkers().addIfAbsent(javaProvenance))
                ));

        sourceFiles.addAll(
                ListUtils.map(new YamlParser().parse(
                        resources.stream()
                                .filter(p -> p.getFileName().toString().endsWith(".yml") || p.getFileName().toString().endsWith(".yaml"))
                                .collect(Collectors.toList()),
                        projectDirectory,
                        ctx
                        ), s -> s.withMarkers(s.getMarkers().addIfAbsent(javaProvenance))
                ));

        sourceFiles.addAll(
                ListUtils.map(new PropertiesParser().parse(
                        resources.stream()
                                .filter(p -> p.getFileName().toString().endsWith(".properties"))
                                .collect(Collectors.toList()),
                        projectDirectory,
                        ctx
                        ), s -> s.withMarkers(s.getMarkers().addIfAbsent(javaProvenance))
                ));
    }

    private List<Path> downloadArtifacts(Set<Pom.Dependency> dependencies) {
        return dependencies.stream()
                .filter(d -> d.getRepository() != null)
                .map(artifactDownloader::downloadArtifact)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
