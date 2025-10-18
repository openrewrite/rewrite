/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.tree;

import lombok.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.attributes.Attributed;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@With
public class Dependency implements Serializable, Attributed {
    GroupArtifactVersion gav;

    @Nullable
    String classifier;

    @Nullable
    String type;

    @With
    @Nullable
    String scope;

    @Builder.Default
    @Nullable
    List<GroupArtifact> exclusions = emptyList();

    @Nullable
    String optional;

    @Builder.Default
    @Nullable // TO-BE-REMOVED(2025-12-31) This annotation and the explicit getter below can be removed in the future
    Map<String, String> attributes = emptyMap();

    @Override
    public Map<String, String> getAttributes() {
        return attributes != null ? attributes : emptyMap();
    }

    public @Nullable String getGroupId() {
        return gav.getGroupId();
    }

    public String getArtifactId() {
        return gav.getArtifactId();
    }

    public @Nullable String getVersion() {
        return gav.getVersion();
    }

    @Override
    public String toString() {
        return gav.toString();
    }

    /**
     * Returns a Gradle-style string notation for this dependency.
     * <p>
     * Format: {@code "group:name:version:classifier@extension"}
     * <p>
     * All parts are optional except the artifact name.
     *
     * @return the dependency in Gradle string notation
     */
    public String toStringNotation() {
        StringBuilder sb = new StringBuilder();
        // Build against spec from gradle docs, all options are optional apart from name
        // configurationName "group:name:version:classifier@extension"
        if (gav.getGroupId() != null) {
            sb.append(gav.getGroupId());
        }
        sb.append(":").append(gav.getArtifactId());

        if (gav.getVersion() != null) {
            sb.append(":").append(gav.getVersion());
        } else if (classifier != null) {
            sb.append(":");
        }

        if (classifier != null) {
            sb.append(":").append(classifier);
        }

        if (type != null) {
            sb.append("@").append(type);
        }

        return sb.toString();
    }

    /**
     * Alias for {@link #getType()} for compatibility with Gradle terminology.
     * In Gradle, the file extension is referred to as "ext" while Maven uses "type".
     *
     * @return the type/extension of the dependency
     */
    public @Nullable String getExt() {
        return type;
    }

    /**
     * Convenience method to update the group ID.
     * Equivalent to {@code withGav(gav.withGroupId(groupId))}.
     *
     * @param groupId the new group ID
     * @return a new Dependency with the updated group ID
     */
    public Dependency withGroupId(@Nullable String groupId) {
        return withGav(gav.withGroupId(groupId));
    }

    /**
     * Convenience method to update the artifact ID.
     * Equivalent to {@code withGav(gav.withArtifactId(artifactId))}.
     *
     * @param artifactId the new artifact ID
     * @return a new Dependency with the updated artifact ID
     */
    public Dependency withArtifactId(String artifactId) {
        return withGav(gav.withArtifactId(artifactId));
    }

    /**
     * Convenience method to update the version.
     * Equivalent to {@code withGav(gav.withVersion(version))}.
     *
     * @param version the new version
     * @return a new Dependency with the updated version
     */
    public Dependency withVersion(@Nullable String version) {
        return withGav(gav.withVersion(version));
    }

    /**
     * Alias for {@link #withType(String)} for compatibility with Gradle terminology.
     * In Gradle, the file extension is referred to as "ext" while Maven uses "type".
     *
     * @param ext the new type/extension
     * @return a new Dependency with the updated type
     */
    public Dependency withExt(@Nullable String ext) {
        return withType(ext);
    }

    /**
     * Parses a Gradle-style dependency string notation into a Dependency object.
     * <p>
     * Format: {@code "group:artifact:version:classifier@extension"}
     * <p>
     * All parts are optional except the artifact name. The minimum valid notation is ":artifact".
     *
     * @param notation a String in the format group:artifact:version:classifier@extension
     * @return A corresponding Dependency or null if the notation could not be parsed
     */
    public static @Nullable Dependency parse(@Nullable String notation) {
        if (notation == null) {
            return null;
        }
        notation = notation.trim();
        if (notation.isEmpty()) {
            return null;
        }

        if (notation.indexOf('"') >= 0 ||
            notation.indexOf('\'') >= 0) {
            return null;
        }

        // Reject notation with spaces (which would indicate Groovy map notation or other invalid format)
        if (notation.indexOf(' ') >= 0) {
            return null;
        }

        int idx = notation.lastIndexOf('@');
        if (idx == -1) {
            return parse(notation, null);
        }

        int versionIdx = notation.lastIndexOf(':');
        if (versionIdx < idx) {
            return parse(notation.substring(0, idx), notation.substring(idx + 1));
        }

        return parse(notation, null);
    }

    private static @Nullable Dependency parse(@Nullable String notation, @Nullable String type) {
        if (notation == null) {
            return null;
        }
        String groupId = null;
        String artifactId = null;
        String version = null;
        String classifier = null;

        int count = 0;
        int idx = 0;
        int cur = 0;
        while (cur < notation.length()) {
            if (':' == notation.charAt(cur)) {
                String fragment = notation.substring(idx, cur);
                switch (count) {
                    case 0:
                        groupId = fragment;
                        break;
                    case 1:
                        artifactId = fragment;
                        break;
                    case 2:
                        version = fragment;
                        break;
                    case 3:
                        classifier = fragment;
                        break;
                }
                idx = cur + 1;
                count++;
            }
            cur++;
        }
        // Handle the last fragment
        if (idx < notation.length()) {
            String fragment = notation.substring(idx);
            switch (count) {
                case 0:
                    artifactId = fragment;
                    break;
                case 1:
                    artifactId = fragment;
                    break;
                case 2:
                    version = fragment;
                    break;
                case 3:
                    classifier = fragment;
                    break;
            }
            count++;
        }

        // Validate we have at least group and artifact
        if (count < 2 || count > 4 || artifactId == null || artifactId.isEmpty()) {
            return null;
        }

        // Validate groupId and artifactId contain only valid characters [A-Za-z0-9_.-]+
        // GroupId can be empty string (for notations like ":artifact")
        if (groupId != null && !groupId.isEmpty() && !isValidIdentifier(groupId)) {
            return null;
        }
        if (!isValidIdentifier(artifactId)) {
            return null;
        }

        // Handle empty strings in version and classifier
        if ("".equals(version)) {
            version = "";  // Preserve empty strings
        }
        if ("".equals(classifier)) {
            classifier = "";  // Preserve empty strings
        }

        return Dependency.builder()
                .gav(new GroupArtifactVersion(groupId, artifactId, version))
                .classifier(classifier)
                .type(type)
                .build();
    }

    /**
     * Validates that a string contains only valid Maven/Gradle identifier characters.
     * Valid characters are: A-Z, a-z, 0-9, underscore (_), dot (.), and hyphen (-).
     *
     * @param str the string to validate
     * @return true if the string contains only valid characters, false otherwise
     */
    private static boolean isValidIdentifier(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!((c >= 'A' && c <= 'Z') ||
                  (c >= 'a' && c <= 'z') ||
                  (c >= '0' && c <= '9') ||
                  c == '_' || c == '.' || c == '-')) {
                return false;
            }
        }
        return true;
    }
}
