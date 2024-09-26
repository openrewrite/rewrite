/*
 * Copyright 2024 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddCommentToXmlTag;
import org.openrewrite.xml.tree.Xml;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.openrewrite.Validated.required;

@Value
@EqualsAndHashCode(callSuper = false)
public class HasMavenAncestry extends Recipe {
    @Override
    public String getDisplayName() {
        return "Has Maven ancestry";
    }

    @Override
    public String getDescription() {
        return "Checks if a pom file has a given Maven ancestry among its parent poms. " +
                "This is useful especially as a precondition for other recipes.";
    }

    @Override
    public String getInstanceNameSuffix() {
        return version == null ? format("%s:%s", groupId, artifactId) : format("%s:%s:%s", groupId, artifactId, version);
    }

    @Option(displayName = "Group",
            description = "The groupId to find. The groupId is the first part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
            example = "org.springframework.*")
    String groupId;

    @Option(displayName = "Artifact ID",
            description = "The artifactId to find. The artifactId is the second part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
            example = "spring-boot-starter-*")
    String artifactId;

    @Option(displayName = "Version",
            description = "Match only an ancestor with the specified version. " +
                    "Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used. " +
                    "All versions are searched by default.",
            example = "1.x",
            required = false)
    @Nullable
    String version;

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate().and(
                required("groupId", groupId).or(required("artifactId", artifactId)));
        if (version != null) {
            return validated.and(Semver.validate(version, null));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Nullable
            final VersionComparator versionComparator = version != null ? Semver.validate(version, null).getValue() : null;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                checkParents(ctx);
                return document;
            }

            private void checkParents(ExecutionContext ctx) {
                try {
                    MavenResolutionResult mrr = getResolutionResult();
                    MavenPomDownloader mpd = new MavenPomDownloader(mrr.getProjectPoms(), ctx, mrr.getMavenSettings(), mrr.getActiveProfiles());

                    Parent ancestor = mrr.getPom().getRequested().getParent();
                    while (ancestor != null) {
                        if (StringUtils.matchesGlob(ancestor.getGroupId(), groupId) &&
                                StringUtils.matchesGlob(ancestor.getArtifactId(), artifactId) &&
                                (versionComparator == null || versionComparator.isValid(null, ancestor.getVersion()))) {
                            doAfterVisit(new AddCommentToXmlTag("/project/parent", "HasMavenAncestry: " + getInstanceNameSuffix()).getVisitor());
                            break;
                        }
                        ResolvedPom ancestorPom = mpd.download(ancestor.getGav(), null, null, mrr.getPom().getRepositories())
                                .resolve(emptyList(), mpd, ctx);
                        ancestor = ancestorPom.getRequested().getParent();
                    }
                } catch (MavenDownloadingException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
