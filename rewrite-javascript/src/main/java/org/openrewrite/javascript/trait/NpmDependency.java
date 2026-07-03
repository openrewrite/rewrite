/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.json.JsonPathMatcher;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonKey;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * A dependency declaration inside one of package.json's dependency sections, joined with its
 * request and resolution from the {@link NodeResolutionResult} marker. Analog of
 * {@code org.openrewrite.maven.trait.MavenDependency} for the npm ecosystem.
 */
@Value
public class NpmDependency implements Trait<Json.Member> {
    Cursor cursor;
    String name;
    @Nullable String versionConstraint;
    @Nullable String resolvedVersion;

    @Nullable
    public String getVersion() {
        return resolvedVersion != null ? resolvedVersion : minimumVersion(versionConstraint);
    }

    private static @Nullable String minimumVersion(@Nullable String constraint) {
        if (constraint == null) {
            return null;
        }
        String c = constraint.trim();
        if (c.contains(":") || c.contains("/") || c.contains("|") || c.contains(" ") ||
                c.startsWith(">") || c.startsWith("<")) {
            return null;
        }
        int start = 0;
        while (start < c.length() && !Character.isDigit(c.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < c.length() && (Character.isDigit(c.charAt(end)) || c.charAt(end) == '.')) {
            end++;
        }
        return start == end ? null : c.substring(start, end);
    }

    public static class Matcher extends SimpleTraitMatcher<NpmDependency> {
        // JsonPath is anchored at the document root, so only the top-level dependency sections
        // match (never same-named keys nested elsewhere, e.g. inside `overrides`).
        private static final Map<String, JsonPathMatcher> DEPENDENCY_SECTIONS;

        static {
            Map<String, JsonPathMatcher> sections = new LinkedHashMap<>();
            for (String section : Arrays.asList("dependencies", "devDependencies", "peerDependencies",
                    "optionalDependencies", "bundledDependencies")) {
                sections.put(section, new JsonPathMatcher("$." + section));
            }
            DEPENDENCY_SECTIONS = sections;
        }

        @Override
        protected @Nullable NpmDependency test(Cursor cursor) {
            Object value = cursor.getValue();
            if (!(value instanceof Json.Member)) {
                return null;
            }
            // A dependency declaration is a member of the object value of a section member.
            Cursor sectionCursor = cursor.getParentTreeCursor().getParentTreeCursor();
            if (!(sectionCursor.getValue() instanceof Json.Member)) {
                return null;
            }
            String section = keyName(((Json.Member) sectionCursor.getValue()).getKey());
            JsonPathMatcher sectionMatcher = section == null ? null : DEPENDENCY_SECTIONS.get(section);
            if (sectionMatcher == null || !sectionMatcher.matches(sectionCursor)) {
                return null;
            }
            String name = keyName(((Json.Member) value).getKey());
            if (name == null) {
                return null;
            }
            Json.Document doc = cursor.firstEnclosing(Json.Document.class);
            NodeResolutionResult resolution = doc == null ? null :
                    doc.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
            if (resolution == null) {
                return null;
            }
            NodeResolutionResult.Dependency declared = null;
            for (NodeResolutionResult.Dependency d : sectionDependencies(resolution, section)) {
                if (name.equals(d.getName())) {
                    declared = d;
                    break;
                }
            }
            NodeResolutionResult.ResolvedDependency resolved = declared != null && declared.getResolved() != null ?
                    declared.getResolved() : resolution.getResolvedDependency(name);
            return new NpmDependency(cursor, name,
                    declared == null ? null : declared.getVersionConstraint(),
                    resolved == null ? null : resolved.getVersion());
        }

        private static List<NodeResolutionResult.Dependency> sectionDependencies(NodeResolutionResult npm, String section) {
            switch (section) {
                case "dependencies":
                    return npm.getDependencies();
                case "devDependencies":
                    return npm.getDevDependencies();
                case "peerDependencies":
                    return npm.getPeerDependencies();
                case "optionalDependencies":
                    return npm.getOptionalDependencies();
                case "bundledDependencies":
                    return npm.getBundledDependencies();
                default:
                    return emptyList();
            }
        }

        private static @Nullable String keyName(JsonKey key) {
            if (key instanceof Json.Literal) {
                Object value = ((Json.Literal) key).getValue();
                return value == null ? null : value.toString();
            }
            if (key instanceof Json.Identifier) {
                return ((Json.Identifier) key).getName();
            }
            return null;
        }
    }
}
