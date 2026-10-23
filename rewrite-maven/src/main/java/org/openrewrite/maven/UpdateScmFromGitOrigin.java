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
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.internal.StringUtils.isNullOrEmpty;
import static org.openrewrite.maven.tree.ResolvedPom.placeholderHelper;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateScmFromGitOrigin extends ScanningRecipe<UpdateScmFromGitOrigin.Accumulator> {

    static final String[] SCM_URL_TAGS = {"url", "connection", "developerConnection"};

    private static final XPathMatcher SCM_URL_MATCHER = new XPathMatcher("/project/scm/*");

    @Option(displayName = "Add if missing",
            description = "If set to `true`, the recipe will add a `<scm>` section if it is missing. " +
                          "If set to `false` (default), the recipe will only update existing `<scm>` sections.",
            required = false)
    @Nullable
    Boolean addIfMissing;

    String displayName = "Update SCM with Git origin";

    String description = "Updates or adds the Maven `<scm>` tag based on the Git remote origin. " +
               "By default, only existing Source Control Management (SCM) sections are updated. Set `addIfMissing` to `true` to also add missing SCM sections to root POMs (POMs without a parent element). " +
               "SCM values that use placeholders are kept when they already resolve to the Git origin. Otherwise the backing property is updated " +
               "when it is defined in the same POM and used nowhere else, or the new value is inlined.";

    public static class Accumulator {
        final Set<String> propertiesUsedOutsideScm = new HashSet<>();
        final Map<String, Set<Path>> propertiesUsedInScm = new HashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                Optional<String> value = t.getValue();
                if (value.isPresent()) {
                    boolean inScm = SCM_URL_MATCHER.matches(getCursor());
                    for (String property : placeholderHelper.getPlaceholders(value.get())) {
                        if (inScm) {
                            acc.propertiesUsedInScm.computeIfAbsent(property, k -> new HashSet<>()).add(getCursor().firstEnclosingOrThrow(Xml.Document.class).getSourcePath());
                        } else {
                            acc.propertiesUsedOutsideScm.add(property);
                        }
                    }
                }
                return t;
            }

            @Override
            public Xml.Attribute visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                acc.propertiesUsedOutsideScm.addAll(placeholderHelper.getPlaceholders(attribute.getValueAsString()));
                return super.visitAttribute(attribute, ctx);
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new MavenVisitor<ExecutionContext>() {

            @SuppressWarnings("NotNullFieldNotInitialized")
            GitOrigin gitOrigin;

            Map<String, String> propertyUpdates = new HashMap<>();

            @Nullable
            String projectUrl;

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
                        // Only add SCM to root POMs (those without a parent)
                        boolean hasParent = document.getMarkers()
                                .findFirst(MavenResolutionResult.class)
                                .map(mrr -> mrr.getParent() != null)
                                .orElse(false);
                        if (hasParent) {
                            return document;
                        }

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
                    projectUrl = tag.getChildValue("url").orElse(null);
                    propertyUpdates = propertyUpdates(tag);
                    return super.visitTag(tag, ctx);
                } else if ("properties".equals(tag.getName())) {
                    for (Map.Entry<String, String> update : propertyUpdates.entrySet()) {
                        Optional<Xml.Tag> property = tag.getChild(update.getKey());
                        if (property.isPresent()) {
                            tag = (Xml.Tag) new ChangeTagValueVisitor<>(property.get(), update.getValue())
                                    .visitNonNull(tag, ctx, getCursor().getParentTreeCursor());
                            maybeUpdateModel();
                        }
                    }
                } else if ("scm".equals(tag.getName())) {
                    // Update existing tags, preserving their URL structure
                    for (String scmTagName : SCM_URL_TAGS) {
                        tag = updateScmTag(tag, scmTagName, ctx);
                    }
                }
                return tag;
            }

            // Properties that make up a whole SCM value, are defined in this POM, and are used nowhere else
            private Map<String, String> propertyUpdates(Xml.Tag project) {
                Map<String, String> updates = new HashMap<>();
                Optional<Xml.Tag> scm = project.getChild("scm");
                Optional<Xml.Tag> properties = project.getChild("properties");
                if (!scm.isPresent() || !properties.isPresent()) {
                    return updates;
                }
                Path sourcePath = getCursor().firstEnclosingOrThrow(Xml.Document.class).getSourcePath();
                for (String scmTagName : SCM_URL_TAGS) {
                    String value = scm.get().getChildValue(scmTagName).orElse("");
                    List<String> placeholders = placeholderHelper.getPlaceholders(value);
                    if (placeholders.size() != 1 || !value.equals("${" + placeholders.get(0) + "}")) {
                        continue;
                    }
                    String property = placeholders.get(0);
                    String propertyValue = properties.get().getChildValue(property).orElse(null);
                    if (propertyValue == null || placeholderHelper.hasPlaceholders(propertyValue) ||
                        acc.propertiesUsedOutsideScm.contains(property) ||
                        !Collections.singleton(sourcePath).equals(acc.propertiesUsedInScm.get(property))) {
                        continue;
                    }
                    String updatedValue = gitOrigin.replaceHostAndPath(propertyValue);
                    if (!propertyValue.equals(updatedValue)) {
                        updates.put(property, updatedValue);
                    }
                }
                return updates;
            }

            private Xml.Tag updateScmTag(Xml.Tag tag, String tagName, ExecutionContext ctx) {
                Optional<Xml.Tag> maybeChild = tag.getChild(tagName);
                if (maybeChild.isPresent()) {
                    Xml.Tag childTag = maybeChild.get();
                    String originalUrl = childTag.getValue().orElse("");
                    String updatedUrl;
                    if (placeholderHelper.hasPlaceholders(originalUrl)) {
                        String resolvedUrl = resolvePlaceholders(originalUrl);
                        if (resolvedUrl == null) {
                            return tag;
                        }
                        String expectedUrl = gitOrigin.replaceHostAndPath(resolvedUrl);
                        updatedUrl = resolvedUrl.equals(expectedUrl) ? originalUrl : expectedUrl;
                    } else {
                        updatedUrl = gitOrigin.replaceHostAndPath(originalUrl);
                    }
                    if (!originalUrl.equals(updatedUrl)) {
                        tag = (Xml.Tag) new ChangeTagValueVisitor<>(childTag, updatedUrl)
                                .visitNonNull(tag, ctx, getCursor().getParentTreeCursor());
                    }
                }
                return tag;
            }

            private @Nullable String resolvePlaceholders(String value) {
                String resolved = placeholderHelper.replacePlaceholders(value, key -> {
                    if (propertyUpdates.containsKey(key)) {
                        return propertyUpdates.get(key);
                    }
                    if ("project.url".equals(key) || "pom.url".equals(key)) {
                        return projectUrl;
                    }
                    String placeholder = "${" + key + "}";
                    String propertyValue = getResolutionResult().getPom().getValue(placeholder);
                    return placeholder.equals(propertyValue) ? null : propertyValue;
                });
                return placeholderHelper.hasPlaceholders(resolved) ? null : resolved;
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
