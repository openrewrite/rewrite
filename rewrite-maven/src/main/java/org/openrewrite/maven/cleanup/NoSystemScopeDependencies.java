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
package org.openrewrite.maven.cleanup;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

public class NoSystemScopeDependencies extends Recipe {

    @Getter
    final String displayName = "Dependencies should not have `system` scope";

    @Getter
    final String description = "Replaces `<scope>system</scope>` with the default compile scope and removes " +
            "`<systemPath>` for dependencies that are available in configured repositories.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if ((isDependencyTag() || isManagedDependencyTag()) &&
                        "system".equals(tag.getChildValue("scope").orElse(null))) {

                    String groupId = tag.getChildValue("groupId").orElse(null);
                    String artifactId = tag.getChildValue("artifactId").orElse(null);
                    String version = tag.getChildValue("version").orElse(null);
                    if (groupId == null || artifactId == null || version == null) {
                        return super.visitTag(tag, ctx);
                    }

                    try {
                        MavenMetadata metadata = downloadMetadata(groupId, artifactId, ctx);
                        if (metadata.getVersioning() == null ||
                                !metadata.getVersioning().getVersions().contains(version)) {
                            return super.visitTag(tag, ctx);
                        }
                    } catch (MavenDownloadingException e) {
                        return super.visitTag(tag, ctx);
                    }

                    tag.getChild("scope").ifPresent(
                            scope -> doAfterVisit(new RemoveContentVisitor<>(scope, false, true)));
                    tag.getChild("systemPath").ifPresent(
                            systemPath -> doAfterVisit(new RemoveContentVisitor<>(systemPath, false, true)));
                    maybeUpdateModel();
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
