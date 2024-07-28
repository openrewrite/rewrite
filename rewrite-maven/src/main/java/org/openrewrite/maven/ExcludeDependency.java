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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExcludeDependency extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Scope",
            description = "Match dependencies with the specified scope. If you specify `compile`, this will NOT match dependencies in `runtime`. " +
                          "The purpose of this is to be able to exclude dependencies that should be in a higher scope, e.g. a compile dependency that should be a test dependency.",
            valid = {"compile", "test", "runtime", "provided"},
            example = "compile",
            required = false)
    @Nullable
    String scope;

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("scope", "scope is a valid Maven scope", scope, s -> {
            try {
                if (s != null) {
                    //noinspection ResultOfMethodCallIgnored
                    Scope.fromName(s);
                }
                return true;
            } catch (Throwable t) {
                return false;
            }
        }));
    }

    @Override
    public String getDisplayName() {
        return "Exclude Maven dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", groupId, artifactId);
    }

    @Override
    public String getDescription() {
        return "Exclude specified dependency from any dependency that transitively includes it.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ExcludeDependencyVisitor();
    }

    private class ExcludeDependencyVisitor extends MavenVisitor<ExecutionContext> {
        @Nullable
        private final Scope scope = ExcludeDependency.this.scope == null ? null : Scope.fromName(ExcludeDependency.this.scope);

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isDependencyTag()) {
                ResolvedDependency dependency = findDependency(tag, scope);
                if (dependency != null &&
                    !(matchesGlob(dependency.getGroupId(), groupId) && matchesGlob(dependency.getArtifactId(), artifactId)) &&
                    dependency.findDependency(groupId, artifactId) != null) {
                    Optional<Xml.Tag> maybeExclusions = tag.getChild("exclusions");
                    if (maybeExclusions.isPresent()) {
                        Xml.Tag exclusions = maybeExclusions.get();

                        List<Xml.Tag> individualExclusions = exclusions.getChildren("exclusion");
                        if (individualExclusions.stream().noneMatch(exclusion ->
                                groupId.equals(exclusion.getChildValue("groupId").orElse(null)) &&
                                artifactId.equals(exclusion.getChildValue("artifactId").orElse(null)))) {
                            Xml.Tag newExclusions = (Xml.Tag) new AddToTagVisitor<>(exclusions,
                                    Xml.Tag.build("" +
                                                  "<exclusion>\n" +
                                                  "<groupId>" + groupId + "</groupId>\n" +
                                                  "<artifactId>" + artifactId + "</artifactId>\n" +
                                                  "</exclusion>"))
                                    .visitNonNull(exclusions, ctx, getCursor());
                            tag = tag.withContent(ListUtils.map((List<Content>) tag.getContent(), t -> t == exclusions ? newExclusions : t));
                        }
                    } else {
                        tag = (Xml.Tag) new AddToTagVisitor<>(tag,
                                Xml.Tag.build("" +
                                              "<exclusions>\n" +
                                              "<exclusion>\n" +
                                              "<groupId>" + groupId + "</groupId>\n" +
                                              "<artifactId>" + artifactId + "</artifactId>\n" +
                                              "</exclusion>\n" +
                                              "</exclusions>"))
                                .visitNonNull(tag, ctx, getCursor().getParentOrThrow());
                    }
                    maybeUpdateModel();
                }
                return tag;
            }

            return super.visitTag(tag, ctx);
        }
    }
}
