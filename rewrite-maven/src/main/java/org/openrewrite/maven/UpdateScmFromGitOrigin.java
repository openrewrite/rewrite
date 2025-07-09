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
import static org.openrewrite.maven.UpdateScmFromGitOrigin.GitOrigin.replaceHostAndPath;

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
                } else if ("scm".equals(tag.getName())) {
                    String origin = Optional.ofNullable(getCursor().firstEnclosing(Xml.Document.class))
                            .map(Xml.Document::getMarkers)
                            .flatMap(markers -> markers.findFirst(GitProvenance.class))
                            .map(GitProvenance::getOrigin)
                            .orElse(null);
                    if (origin == null) {
                        return tag;
                    }

                    GitOrigin originHostAndPath;
                    try {
                        originHostAndPath = GitOrigin.parseGitUrl(origin);
                    } catch (IllegalArgumentException e) {
                        return tag;
                    }

                    updateTagValue(tag, "url", originHostAndPath);
                    updateTagValue(tag, "connection", originHostAndPath);
                    updateTagValue(tag, "developerConnection", originHostAndPath);

                    return tag;
                }
                // Only process the <scm> tag if it's a direct child of <project>
                return tag;
            }

            private void updateTagValue(Xml.Tag tag, String tagName, GitOrigin originHostAndPath) {
                tag.getChild(tagName).ifPresent(childTag -> {
                    String updated = replaceHostAndPath(childTag.getValue().orElse(""), originHostAndPath);
                    doAfterVisit(new ChangeTagValueVisitor<>(childTag, updated));
                });
            }
        });
    }

    static class GitOrigin {
        private final String host;
        private final String path;
        private static final String[] URL_PATTERNS = {
                // SSH format: git@host:path(.git)?
                "^git@([^:]+):(.+?)(?:\\.git)?$",

                // HTTP/HTTPS with optional username and port: http(s)://[username@]host[:port]/path(.git)?
                "^https?://(?:[^@]+@)?([^/:]+(?::[0-9]+)?)/(.+?)(?:\\.git)?$",

                // SSH with protocol and port: ssh://git@host[:port]/path(.git)?
                "^ssh://git@([^/:]+(?::[0-9]+)?)/(.+?)(?:\\.git)?$",

                // Generic protocol://[user@]host[:port]/path(.git)? - catches any other protocols
                "^[a-zA-Z][a-zA-Z0-9+.-]*://(?:[^@]+@)?([^/:]+(?::[0-9]+)?)/(.+?)(?:\\.git)?$"
        };

        GitOrigin(String host, String path) {
            this.host = host;
            this.path = path;
        }

        String getHost() {
            return host;
        }

        String getPath() {
            return path;
        }

        static GitOrigin parseGitUrl(String gitUrl) {
            if (isNullOrEmpty(gitUrl)) {
                throw new IllegalArgumentException("Git URL cannot be null or empty");
            }

            for (String pattern : URL_PATTERNS) {
                Matcher matcher = Pattern.compile(pattern).matcher(gitUrl);
                if (matcher.matches()) {
                    return new GitOrigin(matcher.group(1), matcher.group(2));
                }
            }
            throw new IllegalArgumentException("Unable to parse Git URL: " + gitUrl);
        }

        static String replaceHostAndPath(String originalUrl, GitOrigin hostAndPath) {
            if (isNullOrEmpty(originalUrl)) {
                throw new IllegalArgumentException("Original URL cannot be null or empty");
            }

            if (originalUrl.startsWith("scm:git:")) {
                String actualUrl = originalUrl.substring("scm:git:".length());
                return "scm:git:" + replaceHostAndPath(actualUrl, hostAndPath);
            }
            String newHost = hostAndPath.getHost();
            String newPath = hostAndPath.getPath();

            boolean hasGitSuffix = originalUrl.endsWith(".git");
            String gitSuffix = hasGitSuffix ? ".git" : "";

            if (originalUrl.startsWith("git@")) {
                return "git@" + newHost + ":" + newPath + gitSuffix;
            }

            Matcher protocolMatcher = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*://)(?:([^@/]+)@)?").matcher(originalUrl);
            if (protocolMatcher.find()) {
                String protocol = protocolMatcher.group(1);
                String user = protocolMatcher.group(2);
                String userPrefix = (user != null) ? user + "@" : "";
                return protocol + userPrefix + newHost + "/" + newPath + gitSuffix;
            }

            throw new IllegalArgumentException("Unable to parse original URL: " + originalUrl);
        }
    }
}
