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
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.maven.UpdateScmFromGitOrigin.GitOrigin;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.maven.tree.ResolvedPom.placeholderHelper;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateProjectUrlFromGitOrigin extends ScanningRecipe<GitOriginPropertyUsage> {

    private static final XPathMatcher PROJECT_URL_MATCHER = new XPathMatcher("/project/url");

    String displayName = "Update project URL with Git origin";

    String description = "Updates the Maven project `<url>` based on the Git remote origin. " +
               "Only POMs whose parent is not in the same repository are updated. " +
               "A `<url>` that uses placeholders is kept when it already resolves to the Git origin. Otherwise the backing property is updated " +
               "when it is defined in the same POM and used nowhere else, or the new value is inlined. " +
               "Run this before `org.openrewrite.maven.UpdateScmFromGitOrigin` so `<scm>` references to `${project.url}` resolve to the updated URL.";

    @Override
    public GitOriginPropertyUsage getInitialValue(ExecutionContext ctx) {
        return new GitOriginPropertyUsage();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(GitOriginPropertyUsage acc) {
        return acc.scanner(PROJECT_URL_MATCHER);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(GitOriginPropertyUsage acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (!PROJECT_MATCHER.matches(getCursor()) || getResolutionResult().parentPomIsProjectPom()) {
                    return tag;
                }
                Xml.Document document = getCursor().firstEnclosingOrThrow(Xml.Document.class);
                GitOrigin gitOrigin = document.getMarkers()
                        .findFirst(GitProvenance.class)
                        .map(GitProvenance::getOrigin)
                        .map(GitOrigin::parseGitUrl)
                        .orElse(null);
                Optional<Xml.Tag> url = tag.getChild("url");
                if (gitOrigin == null || !url.isPresent()) {
                    return tag;
                }
                String originalUrl = url.get().getValue().orElse("");
                if (!placeholderHelper.hasPlaceholders(originalUrl)) {
                    return changeValue(tag, url.get(), gitOrigin.replaceHostAndPath(originalUrl), ctx);
                }

                String resolvedUrl = GitOriginPropertyUsage.resolve(originalUrl, getResolutionResult().getPom(), key -> null);
                if (resolvedUrl == null || resolvedUrl.equals(gitOrigin.replaceHostAndPath(resolvedUrl))) {
                    return tag;
                }
                String property = acc.safeToUpdate(originalUrl, tag, document.getSourcePath());
                if (property != null) {
                    Xml.Tag propertyTag = tag.getChild("properties").flatMap(p -> p.getChild(property)).orElseThrow(IllegalStateException::new);
                    maybeUpdateModel();
                    return changeValue(tag, propertyTag, gitOrigin.replaceHostAndPath(resolvedUrl), ctx);
                }
                return changeValue(tag, url.get(), gitOrigin.replaceHostAndPath(resolvedUrl), ctx);
            }

            private Xml.Tag changeValue(Xml.Tag project, Xml.Tag tag, String value, ExecutionContext ctx) {
                if (value.equals(tag.getValue().orElse(""))) {
                    return project;
                }
                return (Xml.Tag) new ChangeTagValueVisitor<>(tag, value)
                        .visitNonNull(project, ctx, getCursor().getParentOrThrow());
            }
        };
    }
}
