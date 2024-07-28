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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.table.ParentPomsInUse;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.tree.Xml;

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
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (isParentTag()) {
                    ResolvedPom resolvedPom = getResolutionResult().getPom();
                    String groupId = resolvedPom.getValue(tag.getChildValue("groupId").orElse(null));
                    String artifactId = resolvedPom.getValue(tag.getChildValue("artifactId").orElse(null));
                    if (matchesGlob(groupId, groupIdPattern) && matchesGlob(artifactId, artifactIdPattern)) {
                        String parentVersion = resolvedPom.getValue(tag.getChildValue("version").orElse(null));
                        if (version != null) {
                            if (!Semver.validate(version, null).getValue()
                                    .isValid(null, parentVersion)) {
                                return t;
                            }
                        }
                        // Found a parent pom that matches the criteria
                        String relativePath = tag.getChildValue("relativePath").orElse(null);
                        inUse.insertRow(ctx, new ParentPomsInUse.Row(
                                resolvedPom.getArtifactId(), groupId, artifactId, parentVersion, relativePath));
                        return SearchResult.found(t);
                    }
                }
                return t;
            }
        };
    }
}
