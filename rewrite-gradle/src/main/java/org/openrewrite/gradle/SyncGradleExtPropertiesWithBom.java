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
import org.openrewrite.gradle.internal.SpringBomProperty;
import org.openrewrite.gradle.trait.ExtraProperty;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
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
import java.util.UUID;
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
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            final ExtraProperty.Matcher matcher = new ExtraProperty.Matcher().matchVariableDeclarations(false);
            final LatestRelease versionComparator = new LatestRelease(null);
            final Set<UUID> redundant = new HashSet<>();
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
                                redundant.add(statement.getId());
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
                            redundant.add(statement.getId());
                            return statement;
                        }
                    }
                }
                return super.visitStatement(statement, ctx);
            }

            @Override
            public @Nullable J postVisit(J tree, ExecutionContext ctx) {
                if (tree instanceof J.Block) {
                    J.Block block = (J.Block) tree;
                    List<Statement> statements = block.getStatements();
                    // A Kotlin script wraps its statements in a block, and what follows the last one is the end of the file
                    boolean script = getCursor().getParentTreeCursor().getValue() instanceof JavaSourceFile;
                    return block.withStatements(withoutRedundant(statements, script))
                            .withEnd(script ? block.getEnd() : afterRedundant(statements, statements.size(), block.getEnd(), false));
                }
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    // The statements as they were, since those of a Kotlin script have lost the redundant ones by now
                    List<Statement> statements = SpringBomProperty.topLevelStatements(getCursor().getValue());
                    if (cu instanceof G.CompilationUnit) {
                        cu = ((G.CompilationUnit) cu).withStatements(withoutRedundant(((G.CompilationUnit) cu).getStatements(), true));
                    }
                    if (!statements.isEmpty() && isRedundant(statements.get(0))) {
                        cu = SpringBomProperty.withTopLevelStatements(cu, ListUtils.mapFirst(SpringBomProperty.topLevelStatements(cu),
                                first -> first.withPrefix(first.getPrefix().withWhitespace(""))));
                    }
                    return cu.withEof(afterRedundant(statements, statements.size(), cu.getEof(), true));
                }
                return tree;
            }

            private List<Statement> withoutRedundant(List<Statement> statements, boolean script) {
                return ListUtils.map(statements, (i, statement) -> isRedundant(statement) ? null :
                        statement.withPrefix(afterRedundant(statements, i, statement.getPrefix(), script)));
            }

            /**
             * The space of what follows the statement before {@code index}. After a run of redundant overrides it
             * loses the comment on the line of the last one, and takes what the first one's prefix holds that is
             * not about the override: the comment on the line before it, and the comments a blank line sets apart.
             */
            private Space afterRedundant(List<Statement> statements, int index, Space space, boolean script) {
                int first = index;
                while (first > 0 && isRedundant(statements.get(first - 1))) {
                    first--;
                }
                if (first == index) {
                    return space;
                }
                Space after = withoutTrailingComment(space);
                Space before = statements.get(first).getPrefix();
                List<Comment> comments = before.getComments();
                // Nothing precedes the first statement of a script, and what starts the file is its header
                boolean header = script && first == 0;
                int kept = header ? 0 : comments.size() - withoutTrailingComment(before).getComments().size();
                for (int i = comments.size(); i > kept; i--) {
                    if (blankLines(comments.get(i - 1).getSuffix()) > 0) {
                        kept = i;
                    }
                }
                if (header && kept == 0) {
                    kept = comments.size();
                }
                if (kept == 0) {
                    return after;
                }
                String suffix = comments.get(kept - 1).getSuffix();
                String indent = after.getWhitespace().substring(after.getWhitespace().lastIndexOf('\n') + 1);
                String separator = blankLines(suffix) > blankLines(after.getWhitespace()) ?
                        suffix.substring(0, suffix.lastIndexOf('\n') + 1) + indent : after.getWhitespace();
                return Space.build(before.getWhitespace(), ListUtils.concatAll(
                        ListUtils.mapLast(comments.subList(0, kept), comment -> comment.withSuffix(separator)), after.getComments()));
            }

            private int blankLines(String whitespace) {
                return Math.max(0, StringUtils.countOccurrences(whitespace, "\n") - 1);
            }

            private boolean isRedundant(Statement statement) {
                // The last statement of a closure is its implicit return
                Tree override = statement instanceof J.Return && ((J.Return) statement).getExpression() != null ?
                        ((J.Return) statement).getExpression() : statement;
                return redundant.contains(override.getId());
            }

            // A comment on the line of a removed override is held by whatever follows the override
            private Space withoutTrailingComment(Space space) {
                while (!space.getComments().isEmpty() && !space.getWhitespace().contains("\n")) {
                    List<Comment> comments = space.getComments();
                    space = space.withWhitespace(comments.get(0).getSuffix()).withComments(comments.subList(1, comments.size()));
                }
                return space;
            }
        });
    }
}
