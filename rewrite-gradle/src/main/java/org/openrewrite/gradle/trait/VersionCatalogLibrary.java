/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.trait.Trait;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.openrewrite.gradle.trait.GradleTraitMatcher.asChainedInvocation;
import static org.openrewrite.gradle.trait.GradleTraitMatcher.literalArgument;

/**
 * Represents a single {@code library(...)} declaration inside a Gradle version catalog, in any
 * of its forms: the three-argument {@code library(alias, group, artifact)} form, optionally
 * terminated by {@code .version(...)}, {@code .versionRef(...)}, or {@code .withoutVersion()},
 * or the single coordinate-string {@code library(alias, "group:artifact:version")} form (which
 * is always terminal -- Gradle's API returns {@code void} for it, so it can't be chained).
 */
@Value
public class VersionCatalogLibrary implements Trait<J.MethodInvocation> {
    Cursor cursor;

    public @Nullable String getAlias() {
        return literalArgument(libraryCall(), 0);
    }

    public @Nullable GroupArtifact getGroupArtifact() {
        J.MethodInvocation library = libraryCall();
        if (library.getArguments().size() == 3) {
            String groupId = literalArgument(library, 1);
            String artifactId = literalArgument(library, 2);
            return groupId == null || artifactId == null ? null : new GroupArtifact(groupId, artifactId);
        }
        if (library.getArguments().size() == 2) {
            String groupArtifactVersion = literalArgument(library, 1);
            String[] parts = groupArtifactVersion == null ? null : groupArtifactVersion.split(":");
            return parts != null && parts.length == 3 ? new GroupArtifact(parts[0], parts[1]) : null;
        }
        return null;
    }

    /**
     * @return the alias of the shared {@code version(...)} declaration this library resolves its
     * version through, or {@code null} if it's declared inline or not at all.
     */
    public @Nullable String getVersionRefAlias() {
        J.MethodInvocation outer = getTree();
        return "versionRef".equals(outer.getSimpleName()) && outer.getArguments().size() == 1 ?
                literalArgument(outer, 0) : null;
    }

    /**
     * @return the inline version literal, whether chained as {@code .version(...)} or embedded in
     * a single coordinate string, or {@code null} if the version comes via
     * {@code versionRef(...)} or isn't declared at all.
     */
    public @Nullable String getInlineVersion() {
        J.MethodInvocation outer = getTree();
        if ("version".equals(outer.getSimpleName()) && outer.getArguments().size() == 1 && outer.getSelect() instanceof J.MethodInvocation) {
            return literalArgument(outer, 0);
        }
        if ("library".equals(outer.getSimpleName()) && outer.getArguments().size() == 2) {
            String groupArtifactVersion = literalArgument(outer, 1);
            String[] parts = groupArtifactVersion == null ? null : groupArtifactVersion.split(":");
            return parts != null && parts.length == 3 ? parts[2] : null;
        }
        return null;
    }

    /**
     * @return a copy with its inline version literal rewritten to {@code newVersion}, or this
     * library unchanged if it has no inline version to rewrite.
     */
    public VersionCatalogLibrary withVersion(String newVersion) {
        J.MethodInvocation outer = getTree();
        if ("version".equals(outer.getSimpleName()) && outer.getArguments().size() == 1 && outer.getSelect() instanceof J.MethodInvocation) {
            return withArgumentLiteral(outer, 0, newVersion);
        }
        if ("library".equals(outer.getSimpleName()) && outer.getArguments().size() == 2) {
            String groupArtifactVersion = literalArgument(outer, 1);
            String[] parts = groupArtifactVersion == null ? null : groupArtifactVersion.split(":");
            if (parts != null && parts.length == 3) {
                return withArgumentLiteral(outer, 1, parts[0] + ":" + parts[1] + ":" + newVersion);
            }
        }
        return this;
    }

    private VersionCatalogLibrary withArgumentLiteral(J.MethodInvocation outer, int argIndex, String newValue) {
        Expression argument = outer.getArguments().get(argIndex);
        if (argument instanceof J.Literal) {
            J.Literal oldLiteral = (J.Literal) argument;
            String quote = oldLiteral.getValueSource() == null ? "'" : oldLiteral.getValueSource().substring(0, 1);
            J.Literal newLiteral = oldLiteral.withValue(newValue).withValueSource(quote + newValue + quote);
            List<Expression> newArguments = new ArrayList<>(outer.getArguments());
            newArguments.set(argIndex, newLiteral);
            return new VersionCatalogLibrary(new Cursor(cursor.getParent(), outer.withArguments(newArguments)));
        }
        return this;
    }

    /**
     * @return a copy with its chained {@code .versionRef(...)} call replaced by
     * {@code .version(newVersion)}, or this library unchanged if it isn't on a
     * {@code versionRef(...)} chain.
     */
    public VersionCatalogLibrary detachToVersion(String newVersion) {
        J.MethodInvocation outer = getTree();
        if ("versionRef".equals(outer.getSimpleName()) && outer.getArguments().size() == 1) {
            return withRenamedChainedCall(outer, "version", newVersion);
        }
        return this;
    }

    /**
     * @return a copy with its chained {@code .version(...)} call replaced by
     * {@code .versionRef(alias)}, or this library unchanged if it has no chained
     * {@code .version(...)} call to rewrite.
     */
    public VersionCatalogLibrary reattachToVersionRef(String alias) {
        J.MethodInvocation outer = getTree();
        if ("version".equals(outer.getSimpleName()) && outer.getArguments().size() == 1 && outer.getSelect() instanceof J.MethodInvocation) {
            return withRenamedChainedCall(outer, "versionRef", alias);
        }
        return this;
    }

    private VersionCatalogLibrary withRenamedChainedCall(J.MethodInvocation outer, String methodName, String newArgumentValue) {
        Expression argument = outer.getArguments().get(0);
        if (!(argument instanceof J.Literal)) {
            return this;
        }
        J.Literal oldLiteral = (J.Literal) argument;
        String quote = oldLiteral.getValueSource() == null ? "'" : oldLiteral.getValueSource().substring(0, 1);
        J.Literal newLiteral = oldLiteral.withValue(newArgumentValue).withValueSource(quote + newArgumentValue + quote);
        J.MethodInvocation newOuter = outer.withName(outer.getName().withSimpleName(methodName))
                .withArguments(Collections.singletonList(newLiteral));
        return new VersionCatalogLibrary(new Cursor(cursor.getParent(), newOuter));
    }

    private J.MethodInvocation libraryCall() {
        J.MethodInvocation outer = getTree();
        J.MethodInvocation chained = asChainedInvocation(outer);
        return chained != null && "library".equals(chained.getSimpleName()) ? chained : outer;
    }

    public static class Matcher extends GradleTraitMatcher<VersionCatalogLibrary> {
        @Override
        protected @Nullable VersionCatalogLibrary test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof J.MethodInvocation && isTopLevelStatement(cursor) && withinBlock(cursor, "versionCatalogs")) {

                J.MethodInvocation outer = (J.MethodInvocation) value;
                String versionRefAlias = null;
                String inlineVersion = null;
                boolean withoutVersion = false;

                if ("versionRef".equals(outer.getSimpleName()) && outer.getArguments().size() == 1) {
                    versionRefAlias = literalArgument(outer, 0);
                    outer = asChainedInvocation(outer);
                } else if ("version".equals(outer.getSimpleName()) && outer.getArguments().size() == 1) {
                    inlineVersion = literalArgument(outer, 0);
                    outer = asChainedInvocation(outer);
                } else if ("withoutVersion".equals(outer.getSimpleName()) && outer.getArguments().isEmpty()) {
                    withoutVersion = true;
                    outer = asChainedInvocation(outer);
                }

                if (outer != null && "library".equals(outer.getSimpleName()) && literalArgument(outer, 0) != null) {
                    if (outer.getArguments().size() == 3) {
                        if (literalArgument(outer, 1) != null && literalArgument(outer, 2) != null) {
                            return new VersionCatalogLibrary(cursor);
                        }
                    } else if (outer.getArguments().size() == 2 && versionRefAlias == null && inlineVersion == null && !withoutVersion) {
                        String groupArtifactVersion = literalArgument(outer, 1);
                        String[] parts = groupArtifactVersion == null ? null : groupArtifactVersion.split(":");
                        if (parts != null && parts.length == 3) {
                            return new VersionCatalogLibrary(cursor);
                        }
                    }
                }

            }
            return null;
        }
    }
}
