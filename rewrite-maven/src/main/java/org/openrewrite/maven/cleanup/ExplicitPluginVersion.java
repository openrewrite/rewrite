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
package org.openrewrite.maven.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.UpgradePluginVersion;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;

public class ExplicitPluginVersion extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add explicit plugin versions";
    }

    @Override
    public String getDescription() {
        return "Add explicit plugin versions to POMs for reproducibility, as [MNG-4173](https://issues.apache.org/jira/browse/MNG-4173) removes automatic version resolution for POM plugins.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (isPluginTag() && !t.getChild("version").isPresent()) {
                    ResolvedPom resolvedPom = getResolutionResult().getPom();
                    String groupId = resolvedPom.getValue(tag.getChildValue("groupId").orElse("org.apache.maven.plugins"));
                    String artifactId = resolvedPom.getValue(tag.getChildValue("artifactId").orElse("*"));
                    // Do not override parent plugin versions
                    if (resolvedPom.getPluginManagement().stream()
                            .noneMatch(p -> groupId.equals(p.getGroupId()) && StringUtils.matchesGlob(p.getArtifactId(), artifactId))) {
                        doAfterVisit(new UpgradePluginVersion(groupId, artifactId, "latest.release", null, true, true).getVisitor());
                    }
                }
                return t;
            }
        };
    }
}
