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

import static org.openrewrite.maven.tree.ResolvedPom.placeholderHelper;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateProjectUrlFromGitOrigin extends Recipe {

    String displayName = "Update project URL with Git origin";

    String description = "Updates the Maven project `<url>` based on the Git remote origin, when it points at the same repository. " +
               "The URL is considered to point at the repository when the `<scm>` section references `${project.url}`, " +
               "or when a `<scm>` URL shares its host. Only POMs whose parent is not in the same repository are updated. " +
               "Run this before `org.openrewrite.maven.UpdateScmFromGitOrigin` so `<scm>` references to `${project.url}` resolve to the updated URL.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (!PROJECT_MATCHER.matches(getCursor()) || getResolutionResult().parentPomIsProjectPom()) {
                    return tag;
                }
                GitOrigin gitOrigin = getCursor().firstEnclosingOrThrow(Xml.Document.class).getMarkers()
                        .findFirst(GitProvenance.class)
                        .map(GitProvenance::getOrigin)
                        .map(GitOrigin::parseGitUrl)
                        .orElse(null);
                Optional<Xml.Tag> url = tag.getChild("url");
                if (gitOrigin == null || !url.isPresent() || !pointsAtRepository(tag)) {
                    return tag;
                }
                String originalUrl = url.get().getValue().orElse("");
                String updatedUrl = gitOrigin.replaceHostAndPath(originalUrl);
                if (originalUrl.equals(updatedUrl)) {
                    return tag;
                }
                return (Xml.Tag) new ChangeTagValueVisitor<>(url.get(), updatedUrl)
                        .visitNonNull(tag, ctx, getCursor().getParentOrThrow());
            }

            private boolean pointsAtRepository(Xml.Tag project) {
                String projectUrl = project.getChildValue("url").orElse("");
                GitOrigin projectUrlOrigin = GitOrigin.parseGitUrl(projectUrl);
                Optional<Xml.Tag> scm = project.getChild("scm");
                if (projectUrlOrigin == null || placeholderHelper.hasPlaceholders(projectUrl) || !scm.isPresent()) {
                    return false;
                }
                for (String scmTagName : UpdateScmFromGitOrigin.SCM_URL_TAGS) {
                    String scmUrl = scm.get().getChildValue(scmTagName).orElse("");
                    if (scmUrl.contains("${project.url}")) {
                        return true;
                    }
                    GitOrigin scmOrigin = GitOrigin.parseGitUrl(scmUrl.startsWith("scm:git:") ? scmUrl.substring("scm:git:".length()) : scmUrl);
                    if (scmOrigin != null && scmOrigin.getHost().equals(projectUrlOrigin.getHost())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
