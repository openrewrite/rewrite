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

import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.maven.search.FindScm;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

public class UpdateScmFromGitOrigin extends Recipe {
    @Override
    public String getDisplayName() {
        return "Update SCM section to match Git origin";
    }

    @Override
    public String getDescription() {
        return "Updates the Maven <scm> section based on the Git remote origin.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindScm(), new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if ("project".equals(tag.getName())) {
                    return super.visitTag(tag, ctx);
                } else if ("scm".equals(tag.getName())) {
                    ScmValues scm = Optional.ofNullable(getCursor().firstEnclosing(Xml.Document.class))
                            .map(Xml.Document::getMarkers)
                            .flatMap(markers -> markers.findFirst(GitProvenance.class))
                            .map(GitProvenance::getOrigin)
                            .map(ScmValues::fromOrigin)
                            .orElse(null);
                    if (scm == null) {
                        return tag;
                    }

                    tag.getChild("url").ifPresent(t -> doAfterVisit(new ChangeTagValueVisitor<>(t, scm.getUrl())));
                    tag.getChild("connection").ifPresent(t -> doAfterVisit(new ChangeTagValueVisitor<>(t, scm.getConnection())));
                    tag.getChild("developerConnection").ifPresent(t -> doAfterVisit(new ChangeTagValueVisitor<>(t, scm.getDeveloperConnection())));
                }
                // Only process the <scm> tag if it's a direct child of <project>
                return tag;
            }
        });
    }

    @Value
    private static class ScmValues {

        String url;
        String connection;
        String developerConnection;

        static ScmValues fromOrigin(String origin) {
            String cleanOrigin = origin.replaceAll("\\.git$", "");

            String url;
            String connection;
            String developerConnection;

            if (origin.startsWith("git@")) {
                // SSH origin
                String hostAndPath = cleanOrigin.substring("git@".length()).replaceFirst(":", "/");
                url = "https://" + hostAndPath;
                connection = "scm:git:https://" + hostAndPath + ".git";
                developerConnection = "scm:git:" + origin;
            } else if (origin.startsWith("http://") || origin.startsWith("https://")) {
                // HTTPS origin
                url = cleanOrigin;
                connection = "scm:git:" + origin;
                String sshPath = cleanOrigin
                        .replaceFirst("^https?://", "") // github.com/user/repo
                        .replaceFirst("/", ":");        // github.com:user/repo
                developerConnection = "scm:git:git@" + sshPath + ".git";
            } else {
                url = cleanOrigin;
                connection = "scm:git:" + origin;
                developerConnection = "scm:git:" + origin;
            }

            return new ScmValues(url, connection, developerConnection);
        }
    }
}
