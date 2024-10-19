/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.ParentPomsInUse;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import static java.util.Collections.emptyList;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@EqualsAndHashCode(callSuper = false)
@Value
public class ParentPomInsight extends Recipe {
    transient ParentPomsInUse inUse = new ParentPomsInUse(this);

    @Option(displayName = "Group pattern",
            description = "Group glob pattern used to match dependencies.",
            example = "org.springframework.boot")
    String groupIdPattern;

    @Option(displayName = "Artifact pattern",
            description = "Artifact glob pattern used to match dependencies.",
            example = "spring-boot-starter-*")
    String artifactIdPattern;

    @Option(displayName = "Version",
            description = "Match only dependencies with the specified version. " +
                          "Node-style [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors) may be used." +
                          "All versions are searched by default.",
            example = "1.x",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Recursive",
            description = "Whether to search recursively through the parents. True by default.",
            required = false)
    @Nullable
    Boolean recursive;

    @Override
    public String getDisplayName() {
        return "Maven parent insight";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("for `%s:%s`", groupIdPattern, artifactIdPattern);
    }

    @Override
    public String getDescription() {
        return "Find Maven parents matching a `groupId` and `artifactId`.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate();
        if (version != null) {
            v = v.and(Semver.validate(version, null));
        }
        return v;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Nullable
            final VersionComparator versionComparator = version == null ? null : Semver.validate(version, null).getValue();

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (isParentTag()) {
                    MavenResolutionResult mrr = getResolutionResult();
                    ResolvedPom resolvedPom = mrr.getPom();
                    MavenPomDownloader mpd = new MavenPomDownloader(mrr.getProjectPoms(), ctx, mrr.getMavenSettings(), mrr.getActiveProfiles());

                    Parent ancestor = mrr.getPom().getRequested().getParent();
                    String relativePath = tag.getChildValue("relativePath").orElse(null);
                    while (ancestor != null) {
                        String groupId = ancestor.getGroupId();
                        String artifactId = ancestor.getArtifactId();
                        if (matchesGlob(groupId, groupIdPattern) && matchesGlob(artifactId, artifactIdPattern)) {
                            String parentVersion = ancestor.getVersion();
                            if (versionComparator == null || versionComparator.isValid(null, parentVersion)) {
                                // Found a parent pom that matches the criteria
                                inUse.insertRow(ctx, new ParentPomsInUse.Row(
                                        resolvedPom.getArtifactId(), groupId, artifactId, parentVersion, relativePath));
                                return SearchResult.found(t);
                            }
                        }
                        if (Boolean.FALSE.equals(recursive)) {
                            return t;
                        }
                        try {
                            ResolvedPom ancestorPom = mpd.download(ancestor.getGav(), null, null, mrr.getPom().getRepositories())
                                    .resolve(emptyList(), mpd, ctx);
                            ancestor = ancestorPom.getRequested().getParent();
                            relativePath = null;
                        } catch (MavenDownloadingException e) {
                            return e.warn(t);
                        }
                    }
                }
                return t;
            }
        };
    }
}
