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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.maven.UpdateScmFromGitOrigin.GitOrigin;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateProjectUrlFromGitOrigin extends Recipe {

    String displayName = "Update project URL with Git origin";

    String description = "Updates the Maven project `<url>` based on the Git remote origin. " +
               "Only POMs whose parent is not in the same repository are updated, as modules typically inherit or extend the parent's URL.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {

            @SuppressWarnings("NotNullFieldNotInitialized")
            GitOrigin gitOrigin;

            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Optional<String> maybeOriginUrl = document.getMarkers().findFirst(GitProvenance.class)
                        .map(GitProvenance::getOrigin);
                Optional<GitOrigin> maybeOrigin = maybeOriginUrl.map(GitOrigin::parseGitUrl);
                if (!maybeOrigin.isPresent() || getResolutionResult().parentPomIsProjectPom()) {
                    return document;
                }
                gitOrigin = maybeOrigin.get();
                // An SSH/git protocol port does not apply to the web URL
                if (!maybeOriginUrl.get().matches("^https?://.*")) {
                    gitOrigin = new GitOrigin(gitOrigin.getHost().replaceFirst(":[0-9]+$", ""), gitOrigin.getPath());
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (!"project".equals(tag.getName())) {
                    return tag;
                }
                Optional<Xml.Tag> maybeUrl = tag.getChild("url");
                if (maybeUrl.isPresent()) {
                    String originalUrl = maybeUrl.get().getValue().orElse("");
                    String updatedUrl = gitOrigin.replaceHostAndPath(originalUrl);
                    if (!originalUrl.equals(updatedUrl)) {
                        return new ChangeTagValueVisitor<>(maybeUrl.get(), updatedUrl)
                                .visitNonNull(tag, ctx, getCursor().getParentTreeCursor());
                    }
                }
                return tag;
            }
        };
    }
}
