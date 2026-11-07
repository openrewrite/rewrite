/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.internal.RemoveStatementsVisitor;
import org.openrewrite.gradle.internal.SpringBomProperty;
import org.openrewrite.gradle.trait.ExtraProperty;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class SyncGradleExtPropertiesWithBom extends ScanningRecipe<SyncGradleExtPropertiesWithBom.Accumulator> {

    @Option(displayName = "Group ID",
            description = "The groupId of the BOM to sync with.",
            example = "org.springframework.boot")
    String groupId;

    @Option(displayName = "Artifact ID",
            description = "The artifactId of the BOM to sync with.",
            example = "spring-boot-dependencies")
    String artifactId;

    @Option(displayName = "Version",
            description = "The version of the BOM to sync with. An exact version, or a selector such as `3.4.x` " +
                    "which resolves to the latest matching release.",
            example = "3.4.0")
    String version;

    @Option(displayName = "Remove redundant overrides",
            description = "When enabled, ext properties whose value is lower than or equal to the BOM version " +
                    "will be removed entirely instead of updated, since the BOM default is now sufficient. " +
                    "A property that a build script reads is kept and updated instead.",
            required = false)
    @Nullable
    Boolean removeRedundantOverrides;

    String displayName = "Sync Gradle ext properties with BOM";

    String description = "Downloads a BOM and compares its properties against Gradle ext properties. " +
            "When the BOM defines a higher version for a property, the ext property is updated to match " +
            "(or removed if `removeRedundantOverrides` is enabled, unless a build script reads it). " +
            "With `removeRedundantOverrides`, a property whose value refers to an entry of `gradle.properties` " +
            "is compared by that entry's value.";

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        //noinspection ConstantValue
        if (version != null) {
            validated = validated.and(Semver.validate(version, null));
        }
        return validated;
    }

    public static class Accumulator {
        final Map<Path, Map<String, String>> gradleProperties = new HashMap<>();

        /**
         * Every name and string a build script uses other than to assign an ext property.
         */
        final Set<String> reads = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof Properties.File && ((Properties.File) tree).getSourcePath().endsWith("gradle.properties")) {
                    Properties.File file = (Properties.File) tree;
                    Map<String, String> entries = acc.gradleProperties.computeIfAbsent(directory(file.getSourcePath()), k -> new HashMap<>());
                    for (Properties.Content content : file.getContent()) {
                        if (content instanceof Properties.Entry) {
                            entries.put(((Properties.Entry) content).getKey(), ((Properties.Entry) content).getValue().getText());
                        }
                    }
                } else if (tree instanceof JavaSourceFile && IsBuildGradle.matches(((JavaSourceFile) tree).getSourcePath())) {
                    new FindReads().visit(tree, acc.reads);
                }
                return tree;
            }
        };
    }

    private static class FindReads extends JavaIsoVisitor<Set<String>> {
        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, Set<String> reads) {
            if (ExtraProperty.Matcher.assignedProperty(getCursor()) != null) {
                visit(assignment.getAssignment(), reads);
                return assignment;
            }
            return super.visitAssignment(assignment, reads);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<String> reads) {
            if (ExtraProperty.Matcher.assignedProperty(getCursor()) != null) {
                visit(method.getArguments().get(1), reads);
                return method;
            }
            return super.visitMethodInvocation(method, reads);
        }

        @Override
        public J.Literal visitLiteral(J.Literal literal, Set<String> reads) {
            if (literal.getValue() instanceof String) {
                reads.add((String) literal.getValue());
            }
            return literal;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> reads) {
            String name = identifier.getSimpleName();
            // A property read as ext.'x.version' keeps its quotes
            boolean quoted = name.length() > 1 && (name.startsWith("'") || name.startsWith("\""));
            reads.add(quoted ? name.substring(1, name.length() - 1) : name);
            return identifier;
        }
    }

    private static Path directory(Path sourcePath) {
        return sourcePath.getParent() == null ? Paths.get("") : sourcePath.getParent();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(new IsBuildGradle<>(), new RemoveStatementsVisitor<ExecutionContext>() {
            final ExtraProperty.Matcher matcher = new ExtraProperty.Matcher().matchVariableDeclarations(false);
            final LatestRelease versionComparator = new LatestRelease(null);
            @Nullable
            Map<String, String> bomProperties;

            private Map<String, String> getBomProperties(ExecutionContext ctx) {
                if (bomProperties != null) {
                    return bomProperties;
                }
                SourceFile sf = getCursor().firstEnclosing(SourceFile.class);
                List<MavenRepository> repos = sf != null
                        ? sf.getMarkers().findFirst(GradleProject.class)
                        .map(GradleProject::getMavenRepositories)
                        .orElse(singletonList(MavenRepository.MAVEN_CENTRAL))
                        : singletonList(MavenRepository.MAVEN_CENTRAL);
                try {
                    MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                    String bomVersion = selectVersion(mpd, repos);
                    if (bomVersion == null) {
                        return bomProperties = emptyMap();
                    }
                    Pom bom = mpd.download(new GroupArtifactVersion(groupId, artifactId, bomVersion), null, null, repos);
                    bomProperties = bom.resolve(emptyList(), mpd, repos, ctx).getProperties();
                } catch (MavenDownloadingException e) {
                    bomProperties = emptyMap();
                }
                return bomProperties;
            }

            private @Nullable String selectVersion(MavenPomDownloader mpd, List<MavenRepository> repos) throws MavenDownloadingException {
                VersionComparator selector = Semver.validate(version, null).getValue();
                if (selector == null || selector instanceof ExactVersion) {
                    return version;
                }
                return mpd.downloadMetadata(new GroupArtifact(groupId, artifactId), null, repos)
                        .getVersioning().getVersions().stream()
                        .filter(v -> selector.isValid(null, v))
                        .max((v1, v2) -> selector.compare(null, v1, v2))
                        .orElse(null);
            }

            // The value of an override written as `"$name"`, `"${name}"` or `name`, when gradle.properties defines it
            private @Nullable String referencedValue(Statement statement) {
                Expression value = statement instanceof J.Assignment ?
                        ((J.Assignment) statement).getAssignment() :
                        ((J.MethodInvocation) statement).getArguments().get(1);
                if (value instanceof G.GString) {
                    List<J> strings = ((G.GString) value).getStrings();
                    value = strings.size() == 1 && strings.get(0) instanceof G.GString.Value &&
                            ((G.GString.Value) strings.get(0)).getTree() instanceof Expression ?
                            (Expression) ((G.GString.Value) strings.get(0)).getTree() : null;
                }
                if (!(value instanceof J.Identifier)) {
                    return null;
                }
                String name = ((J.Identifier) value).getSimpleName();
                JavaSourceFile cu = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);
                // A variable of the script shadows the gradle.properties entry of the same name
                if (SpringBomProperty.declaredProperties(cu).contains(name) || declaresVariable(cu, name)) {
                    return null;
                }
                // A subproject inherits the properties of the projects above it
                for (Path dir = directory(cu.getSourcePath()); ; dir = directory(dir)) {
                    String referenced = acc.gradleProperties.getOrDefault(dir, emptyMap()).get(name);
                    if (referenced != null || dir.toString().isEmpty()) {
                        return referenced;
                    }
                }
            }

            private boolean declaresVariable(JavaSourceFile cu, String name) {
                return new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, AtomicBoolean found) {
                        if (name.equals(variable.getSimpleName())) {
                            found.set(true);
                        }
                        return super.visitVariable(variable, found);
                    }
                }.reduce(cu, new AtomicBoolean()).get();
            }

            @Override
            public @Nullable Statement visitStatement(Statement statement, ExecutionContext ctx) {
                if (statement instanceof J.Assignment || statement instanceof J.MethodInvocation) {
                    boolean remove = Boolean.TRUE.equals(removeRedundantOverrides);
                    ExtraProperty prop = matcher.get(getCursor()).orElse(null);
                    if (prop != null) {
                        String bomVersion = getBomProperties(ctx).get(prop.getName());
                        if (bomVersion != null) {
                            int cmp = versionComparator.compare(null, prop.getValue(), bomVersion);
                            if (remove && cmp <= 0 && !acc.reads.contains(prop.getName())) {
                                remove(statement);
                                return statement;
                            }
                            if (cmp < 0) {
                                return (Statement) prop.withValue(bomVersion).getTree();
                            }
                        }
                    } else if (remove) {
                        String name = ExtraProperty.Matcher.assignedProperty(getCursor());
                        String bomVersion = name == null || acc.reads.contains(name) ? null : getBomProperties(ctx).get(name);
                        String value = bomVersion == null ? null : referencedValue(statement);
                        if (value != null && versionComparator.compare(null, value, bomVersion) <= 0) {
                            remove(statement);
                            return statement;
                        }
                    }
                }
                return super.visitStatement(statement, ctx);
            }
        });
    }
}
