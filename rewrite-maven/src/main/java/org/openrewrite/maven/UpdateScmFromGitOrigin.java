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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.internal.StringUtils.isNullOrEmpty;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateScmFromGitOrigin extends Recipe {

    @Option(displayName = "Add if missing",
            description = "If set to `true`, the recipe will add a `<scm>` section if it is missing. " +
                          "If set to `false` (default), the recipe will only update existing `<scm>` sections.",
            required = false)
    @Nullable
    Boolean addIfMissing;

    @Override
    public String getDisplayName() {
        return "Update SCM with Git origin";
    }

    @Override
    public String getDescription() {
        return "Updates or adds the Maven `<scm>` tag based on the Git remote origin. " +
               "By default, only existing Source Control Management (SCM) sections are updated. Set `addIfMissing` to `true` to also add missing SCM sections.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {

            @SuppressWarnings("NotNullFieldNotInitialized")
            GitOrigin gitOrigin;

            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Optional<GitOrigin> maybeOrigin = document.getMarkers().findFirst(GitProvenance.class)
                        .map(GitProvenance::getOrigin)
                        .map(GitOrigin::parseGitUrl);
                if(!maybeOrigin.isPresent()) {
                    return document;
                }
                gitOrigin = maybeOrigin.get();

                if(!document.getRoot().getChild("scm").isPresent()) {
                    if (Boolean.TRUE.equals(addIfMissing)) {
                        // Build the SCM tag with all required elements
                        String httpUrl = "https://" + gitOrigin.getHost() + "/" + gitOrigin.getPath();
                        String gitPath = gitOrigin.getPath().endsWith(".git") ?
                                gitOrigin.getPath().substring(0, gitOrigin.getPath().length() - 4) : gitOrigin.getPath();
                        String scmContent = "<scm>\n" +
                                "  <url>" + httpUrl + "</url>\n" +
                                "  <connection>scm:git:" + httpUrl + (httpUrl.endsWith(".git") ? "" : ".git") + "</connection>\n" +
                                "  <developerConnection>scm:git:git@" + gitOrigin.getHost() + ":" + gitPath + ".git</developerConnection>\n" +
                                "</scm>";
                        document = (Xml.Document) new AddToTagVisitor<>(document.getRoot(), Xml.Tag.build(scmContent),
                                new MavenTagInsertionComparator(document.getRoot().getChildren()))
                                .visitNonNull(document, ctx, Objects.requireNonNull(getCursor().getParent()));

                    }
                    return document;
                }

                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if ("project".equals(tag.getName())) {
                    return super.visitTag(tag, ctx);
                } else if ("scm".equals(tag.getName())) {
                    // Update existing tags, preserving their URL structure
                    tag = updateScmTag(tag, "url", ctx);
                    tag = updateScmTag(tag, "connection", ctx);
                    tag = updateScmTag(tag, "developerConnection", ctx);
                }
                return tag;
            }

            private Xml.Tag updateScmTag(Xml.Tag tag, String tagName, ExecutionContext ctx) {
                Optional<Xml.Tag> maybeChild = tag.getChild(tagName);
                if (maybeChild.isPresent()) {
                    Xml.Tag childTag = maybeChild.get();
                    String originalUrl = childTag.getValue().orElse("");
                    String updatedUrl = gitOrigin.replaceHostAndPath(originalUrl);
                    if (!originalUrl.equals(updatedUrl)) {
                        tag = (Xml.Tag) new ChangeTagValueVisitor<>(childTag, updatedUrl)
                                .visitNonNull(tag, ctx, getCursor().getParentTreeCursor());
                    }
                }
                return tag;
            }
        };
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

            // Handle git@ format (SSH)
            if (originalUrl.startsWith("git@")) {
                // First check if the URL already has the correct host and path
                String expectedPrefix = "git@" + host + ":" + path;
                if (originalUrl.startsWith(expectedPrefix)) {
                    return originalUrl; // Already correct, don't modify
                }

                // Otherwise, replace the host and path
                Matcher gitMatcher = Pattern.compile("^git@[^:]+:(.+?)$").matcher(originalUrl);
                if (gitMatcher.matches()) {
                    String originalFullPath = gitMatcher.group(1);
                    // Check if original path ends with .git
                    boolean hasGitExtension = originalFullPath.endsWith(".git");

                    // Build new URL
                    String newUrl = "git@" + host + ":" + path;
                    if (hasGitExtension && !path.endsWith(".git")) {
                        newUrl += ".git";
                    }
                    return newUrl;
                }
            }

            // Handle protocol-based URLs (http, https, ssh, etc.)
            Matcher protocolMatcher = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*://)(?:([^@/]+)@)?([^/]+)(/[^?#]*)?(.*)?$").matcher(originalUrl);
            if (protocolMatcher.find()) {
                String protocol = protocolMatcher.group(1);
                String user = protocolMatcher.group(2);
                String originalHost = protocolMatcher.group(3);
                String originalPath = protocolMatcher.group(4);
                String suffix = protocolMatcher.group(5) != null ? protocolMatcher.group(5) : "";

                String userPrefix = (user != null) ? user + "@" : "";

                // Determine if we need to add .git extension
                boolean needsGitExtension = originalPath != null && originalPath.endsWith(".git") && !path.endsWith(".git");
                String newPath = "/" + path + (needsGitExtension ? ".git" : "");

                // Check if the base URL already matches (to preserve suffixes)
                String newBaseUrl = protocol + userPrefix + host + newPath;
                if (originalUrl.startsWith(newBaseUrl)) {
                    return originalUrl; // Already correct, preserve any suffix
                }

                return newBaseUrl + suffix;
            }

            // Return the original URL if no patterns matched
            return originalUrl;
        }
    }
}
