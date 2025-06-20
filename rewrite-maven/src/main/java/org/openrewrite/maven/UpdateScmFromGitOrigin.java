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
package org.openrewrite.maven;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.maven.search.FindScm;
import org.openrewrite.maven.utilities.ScmValues;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

public class UpdateScmFromGitOrigin extends Recipe {
    @Override
    public String getDisplayName() {
        return "Update SCM section to match Git origin";
    }

    @Override
    public String getDescription() {
        return "Updates the Maven <scm> section based on the Git remote origin.";
    }

    @Nullable
    private GitProvenance git;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindScm(), new MavenVisitor<ExecutionContext>() {

            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                git = document.getMarkers().findFirst(GitProvenance.class).orElse(null);
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if ("scm".equals(tag.getName()) && git != null && git.getOrigin() != null) {
                    String origin = git.getOrigin();
                    ScmValues scmValues = ScmValues.fromOrigin(origin);

                    tag.getChild("url").ifPresent(urlTag -> {
                        if (!scmValues.getUrl().equals(urlTag.getValue().orElse(null))) {
                            doAfterVisit(new ChangeTagValueVisitor<>(urlTag, scmValues.getUrl()));
                        }
                    });
                    tag.getChild("connection").ifPresent(connTag -> {
                        if (!scmValues.getConnection().equals(connTag.getValue().orElse(null))) {
                            doAfterVisit(new ChangeTagValueVisitor<>(connTag, scmValues.getConnection()));
                        }
                    });
                    tag.getChild("developerConnection").ifPresent(devConnTag -> {
                        if (!scmValues.getDeveloperConnection().equals(devConnTag.getValue().orElse(null))) {
                            doAfterVisit(new ChangeTagValueVisitor<>(devConnTag, scmValues.getDeveloperConnection()));
                        }
                    });
                }
                return super.visitTag(tag, ctx);
            }
        });
    }
}
