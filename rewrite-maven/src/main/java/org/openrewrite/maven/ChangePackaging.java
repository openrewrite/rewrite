/*
 * Copyright 2021 the original author or authors.
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
 *
 */
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.internal.StringUtils.matchesGlob;
import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePackaging extends Recipe {
    private static final XPathMatcher PROJECT_MATCHER = new XPathMatcher("/project");

    @Option(displayName = "Group",
            description = "The groupId of the project whose packaging should be changed. Accepts glob patterns.",
            example = "org.openrewrite.*")
    String groupId;

    @Option(displayName = "Group",
            description = "The artifactId of the project whose packaging should be changed. Accepts glob patterns.",
            example = "rewrite-*")
    String artifactId;

    @Option(displayName = "Packaging",
            description = "The type of packaging to set. If `null` specified the packaging tag will be removed",
            example = "jar")
    @Nullable
    String packaging;

    @Option(displayName = "Old Packaging",
            description = "The old packaging type. If provided, will only change if the current packaging matches",
            required = false,
            example = "jar")
    @Nullable
    String oldPackaging;

    @Override
    public String getDisplayName() {
        return "Set Maven project packaging";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("for `%s:%s` to `%s`", groupId, artifactId, packaging);
    }

    public String getDescription() {
        return "Sets the packaging type of Maven projects. Either adds the packaging tag if it is missing or changes its context if present.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                ResolvedPom pom = getResolutionResult().getPom();
                if (!matchesGlob(pom.getGroupId(), groupId) || !matchesGlob(pom.getArtifactId(), artifactId)) {
                    return document;
                }
                Xml.Document xml = super.visitDocument(document, ctx);
                if (xml != document) {
                    return xml.withMarkers(xml.getMarkers().withMarkers(ListUtils.map(xml.getMarkers().getMarkers(), m -> {
                        if (m instanceof MavenResolutionResult) {
                            return getResolutionResult().withPom(pom.withRequested(pom.getRequested().withPackaging(packaging)));
                        }
                        return m;
                    })));
                }
                return xml;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (PROJECT_MATCHER.matches(getCursor())) {
                    Optional<Xml.Tag> maybePackaging = t.getChild("packaging");
                    if (!maybePackaging.isPresent() || oldPackaging == null || oldPackaging.equals(maybePackaging.get().getValue().orElse(null))) {
                        if (packaging == null || "jar".equals(packaging)) {
                            t = filterTagChildren(t, it -> !"packaging".equals(it.getName()));
                        } else {
                            t = addOrUpdateChild(t, Xml.Tag.build("\n<packaging>" + packaging + "</packaging>"), getCursor().getParentOrThrow());
                        }
                    }
                }
                return t;
            }
        };
    }
}
