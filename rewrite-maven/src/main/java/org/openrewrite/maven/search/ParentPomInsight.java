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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.table.ParentPomsInUse;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;

import java.util.UUID;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@EqualsAndHashCode(callSuper = true)
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

    UUID searchId = randomId();

    @Override
    public String getDisplayName() {
        return "Maven parent insight";
    }

    @Override
    public String getDescription() {
        return "Find Maven parents matching a `groupId` and `artifactId`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (isParentTag()) {
                    ResolvedPom resolvedPom = getResolutionResult().getPom();
                    if (matchesGlob(resolvedPom.getValue(tag.getChildValue("groupId").orElse(null)), groupIdPattern) &&
                        matchesGlob(resolvedPom.getValue(tag.getChildValue("artifactId").orElse(null)), artifactIdPattern)) {

                        t = SearchResult.found(t);

                        String groupId = t.getChildValue("groupId").orElse(null);
                        String artifactId = t.getChildValue("artifactId").orElse(null);
                        String version = tag.getChildValue("version").orElse(null);
                        inUse.insertRow(ctx, new ParentPomsInUse.Row(
                                groupId, artifactId, version,
                                resolvedPom.getDatedSnapshotVersion()
                        ));
                    }
                }
                return t;
            }
        };
    }
}
