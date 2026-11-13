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
import org.openrewrite.xml.tree.Xml;

import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateProjectUrlFromGitOrigin extends Recipe {

    private static final Pattern PORT = Pattern.compile(":[0-9]+$");

    String displayName = "Update project URL with Git origin";

    String description = "Updates the Maven project `<url>` based on the Git remote origin. " +
               "Only POMs whose parent is not in the same repository are updated, as modules typically inherit or extend the parent's URL.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Tag project = document.getRoot();
                String originalUrl = project.getChildValue("url").orElse(null);
                String originUrl = document.getMarkers().findFirst(GitProvenance.class)
                        .map(GitProvenance::getOrigin)
                        .orElse(null);
                if (originalUrl == null || originUrl == null || getResolutionResult().parentPomIsProjectPom()) {
                    return document;
                }
                GitOrigin gitOrigin = GitOrigin.parseGitUrl(originUrl);
                if (gitOrigin == null) {
                    return document;
                }
                if (!originUrl.startsWith("http://") && !originUrl.startsWith("https://")) {
                    // An SSH port does not apply to the web URL
                    gitOrigin = new GitOrigin(PORT.matcher(gitOrigin.getHost()).replaceFirst(""), gitOrigin.getPath());
                }
                String updatedUrl = gitOrigin.replaceHostAndPath(originalUrl);
                return originalUrl.equals(updatedUrl) ? document : document.withRoot(project.withChildValue("url", updatedUrl));
            }
        };
    }
}
