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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.maven.search.FindScm;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.internal.StringUtils.isNullOrEmpty;

public class UpdateScmFromGitOrigin extends Recipe {
    @Override
    public String getDisplayName() {
        return "Update SCM section to match Git origin";
    }

    @Override
    public String getDescription() {
        return "Updates the Maven `<scm>` section based on the Git remote origin.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindScm(), new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if ("project".equals(tag.getName())) {
                    return super.visitTag(tag, ctx);
                }
                if ("scm".equals(tag.getName())) {
                    Optional.ofNullable(getCursor().firstEnclosing(Xml.Document.class))
                            .map(Xml.Document::getMarkers)
                            .flatMap(markers -> markers.findFirst(GitProvenance.class))
                            .map(GitProvenance::getOrigin)
                            .map(GitOrigin::parseGitUrl)
                            .ifPresent(gitOrigin -> {
                                updateTagValue(tag, "url", gitOrigin);
                                updateTagValue(tag, "connection", gitOrigin);
                                updateTagValue(tag, "developerConnection", gitOrigin);
                            });
                    return tag;
                }
                // Only process the <scm> tag if it's a direct child of <project>
                return tag;
            }

            private void updateTagValue(Xml.Tag tag, String tagName, GitOrigin gitOrigin) {
                tag.getChild(tagName).ifPresent(childTag -> {
                    String originalUrl = childTag.getValue().orElse("");
                    String updatedUrl = gitOrigin.replaceHostAndPath(originalUrl);
                    doAfterVisit(new ChangeTagValueVisitor<>(childTag, updatedUrl));
                });
            }
        });
    }

    @Value
    static class GitOrigin {
        private static final Pattern[] URL_PATTERNS = {
                // SSH format: git@host:path(.git)?
                Pattern.compile("^git@([^:]+):(.+?)(?:\\.git)?$"),

                // HTTP/HTTPS with optional username and port: http(s)://[username@]host[:port]/path(.git)?
                Pattern.compile("^https?://(?:[^@]+@)?([^/:]+(?::[0-9]+)?)/(.+?)(?:\\.git)?$"),

                // SSH with protocol and port: ssh://git@host[:port]/path(.git)?
                Pattern.compile("^ssh://git@([^/:]+(?::[0-9]+)?)/(.+?)(?:\\.git)?$"),

                // Generic protocol://[user@]host[:port]/path(.git)? - catches any other protocols
                Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://(?:[^@]+@)?([^/:]+(?::[0-9]+)?)/(.+?)(?:\\.git)?$")
        };

        String host;
        String path;

        static @Nullable GitOrigin parseGitUrl(String gitUrl) {
            if (!isNullOrEmpty(gitUrl)) {
                for (Pattern pattern : URL_PATTERNS) {
                    Matcher matcher = pattern.matcher(gitUrl);
                    if (matcher.matches()) {
                        return new GitOrigin(matcher.group(1), matcher.group(2));
                    }
                }
            }
            return null;
        }

        String replaceHostAndPath(String originalUrl) {
            if (isNullOrEmpty(originalUrl)) {
                return originalUrl;
            }

            if (originalUrl.startsWith("scm:git:")) {
                String actualUrl = originalUrl.substring("scm:git:".length());
                return "scm:git:" + replaceHostAndPath(actualUrl);
            }

            String gitSuffix = originalUrl.endsWith(".git") ? ".git" : "";
            if (originalUrl.startsWith("git@")) {
                return "git@" + host + ":" + path + gitSuffix;
            }

            Matcher protocolMatcher = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*://)(?:([^@/]+)@)?").matcher(originalUrl);
            if (protocolMatcher.find()) {
                String protocol = protocolMatcher.group(1);
                String user = protocolMatcher.group(2);
                String userPrefix = (user != null) ? user + "@" : "";
                String newUrl = protocol + userPrefix + host + "/" + path + gitSuffix;
                if (originalUrl.startsWith(newUrl)) {
                    return originalUrl; // Retain e.g. tree/${project.scm.tag}
                }
                return newUrl;
            }

            // Return the original URL if no patterns matched
            return originalUrl;
        }
    }
}
