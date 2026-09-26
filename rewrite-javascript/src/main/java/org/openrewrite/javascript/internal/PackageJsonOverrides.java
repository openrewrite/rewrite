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
package org.openrewrite.javascript.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.Tree;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Ports {@code parseDependencyPath} and {@code applyOverrideToPackageJson}
 * from {@code dependency-manager.ts}.
 */
public final class PackageJsonOverrides {

    private static final Pattern BARE_PACKAGE_NAME =
            Pattern.compile("(?:@[a-zA-Z0-9_~-][a-zA-Z0-9._~-]*/)?[a-zA-Z0-9_~-][a-zA-Z0-9._~-]*");

    private PackageJsonOverrides() {
    }

    /**
     * Parses a dependency path string into segments.
     * Accepts both {@code >} (pnpm style) and {@code /} (yarn style) as separators.
     * Scoped packages ({@code @scope/pkg}) are kept as a single segment.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code "express>accepts"} → [{name:"express"}, {name:"accepts"}]</li>
     *   <li>{@code "express@4.0.0>accepts"} → [{name:"express", version:"4.0.0"}, {name:"accepts"}]</li>
     *   <li>{@code "@scope/pkg>dep"} → [{name:"@scope/pkg"}, {name:"dep"}]</li>
     * </ul>
     */
    public static List<DependencyPathSegment> parsePath(String path) {
        List<DependencyPathSegment> segments = new ArrayList<>();

        // Split on '>' (pnpm-style separator)
        String[] gtParts = path.split(">");

        for (String gtPart : gtParts) {
            if (gtPart.contains("/")) {
                if (gtPart.startsWith("@")) {
                    // Scoped package: @scope/pkg or @scope/pkg@version or @scope/pkg/dep (yarn-style)
                    int firstSlash = gtPart.indexOf('/');
                    String afterFirstSlash = gtPart.substring(firstSlash + 1);

                    // Check if there's another '/' after the scope (yarn-style nesting)
                    int secondSlash = afterFirstSlash.indexOf('/');
                    if (secondSlash != -1) {
                        // yarn-style: @scope/pkg/dep — split further
                        String scopedPart = gtPart.substring(0, firstSlash + 1 + secondSlash);
                        segments.add(parseSegment(scopedPart));

                        // Then handle the rest as separate segments
                        String rest = afterFirstSlash.substring(secondSlash + 1);
                        for (String subPart : rest.split("/")) {
                            if (!subPart.isEmpty()) {
                                segments.add(parseSegment(subPart));
                            }
                        }
                    } else {
                        // Simple scoped package: @scope/pkg or @scope/pkg@version
                        segments.add(parseSegment(gtPart));
                    }
                } else {
                    // Non-scoped with '/': yarn-style path like "express/accepts"
                    for (String slashPart : gtPart.split("/")) {
                        if (!slashPart.isEmpty()) {
                            segments.add(parseSegment(slashPart));
                        }
                    }
                }
            } else {
                // No '/', just parse the segment directly
                segments.add(parseSegment(gtPart));
            }
        }

        return segments;
    }

    /**
     * Parses a single segment (package name, possibly with a {@code @version} suffix).
     */
    private static DependencyPathSegment parseSegment(String part) {
        if (part.startsWith("@")) {
            // Scoped package: find version separator after the slash
            int slashIndex = part.indexOf('/');
            if (slashIndex == -1) {
                return new DependencyPathSegment(part, null);
            }
            String afterSlash = part.substring(slashIndex + 1);
            int atIndex = afterSlash.lastIndexOf('@');
            if (atIndex > 0) {
                return new DependencyPathSegment(
                        part.substring(0, slashIndex + 1 + atIndex),
                        afterSlash.substring(atIndex + 1));
            }
            return new DependencyPathSegment(part, null);
        }

        // Non-scoped: name or name@version
        int atIndex = part.lastIndexOf('@');
        if (atIndex > 0) {
            return new DependencyPathSegment(part.substring(0, atIndex), part.substring(atIndex + 1));
        }
        return new DependencyPathSegment(part, null);
    }

    /**
     * Applies a transitive-dependency override to a {@code package.json} document,
     * dispatching on the package manager dialect.
     * <p>
     * For the no-path (global) case, formatting is preserved via
     * {@link PackageJsonHelper}'s mutation helpers. For deep-nested paths the
     * document is re-serialised and re-parsed (acceptable per spec).
     */
    public static Json.Document applyOverride(Json.Document doc,
                                              PackageManager pm,
                                              String packageName,
                                              String newVersion,
                                              @Nullable List<DependencyPathSegment> path) {
        boolean hasPath = path != null && !path.isEmpty();

        switch (pm) {
            case Npm:
            case Bun:
                return applyNpmOverride(doc, packageName, newVersion, path, hasPath);
            case YarnClassic:
            case YarnBerry:
                return applyYarnResolution(doc, packageName, newVersion, path, hasPath);
            case Pnpm:
                return applyPnpmOverride(doc, packageName, newVersion, path, hasPath);
            default:
                return doc;
        }
    }

    // -------------------------------------------------------------------------
    // npm / Bun: "overrides" field with nested objects
    // -------------------------------------------------------------------------

    private static Json.Document applyNpmOverride(Json.Document doc,
                                                   String packageName,
                                                   String newVersion,
                                                   @Nullable List<DependencyPathSegment> path,
                                                   boolean hasPath) {
        if (!hasPath) {
            // Simple case: set overrides[packageName] = newVersion (formatting-preserved)
            return setFlatEntry(doc, "overrides", packageName, newVersion);
        }

        // Deep-nested case: build nested JSON string and reparse
        String overrideValue = buildNpmNestedOverride(packageName, newVersion, path);
        return mergeTopLevelObjectReparse(doc, "overrides", overrideValue);
    }

    /**
     * Builds a JSON object string for an npm nested override, e.g.:
     * {@code {"express":{"accepts":"^2.0.0"}}}.
     */
    private static String buildNpmNestedOverride(String packageName, String newVersion,
                                                  List<DependencyPathSegment> path) {
        // Innermost value
        StringBuilder sb = new StringBuilder();
        sb.append(jsonString(packageName)).append(": ").append(jsonString(newVersion));

        // Wrap from inside out
        for (int i = path.size() - 1; i >= 0; i--) {
            DependencyPathSegment seg = path.get(i);
            String key = seg.getVersion() != null
                    ? seg.getName() + "@" + seg.getVersion()
                    : seg.getName();
            sb.insert(0, jsonString(key) + ": {").append("}");
        }
        return "{" + sb + "}";
    }

    // -------------------------------------------------------------------------
    // Yarn Classic / Berry: "resolutions" field with flat path keys
    // -------------------------------------------------------------------------

    private static Json.Document applyYarnResolution(Json.Document doc,
                                                      String packageName,
                                                      String newVersion,
                                                      @Nullable List<DependencyPathSegment> path,
                                                      boolean hasPath) {
        String key;
        if (!hasPath) {
            key = packageName;
        } else {
            // Yarn uses / separator: "express/accepts"
            DependencyPathSegment parent = path.get(path.size() - 1);
            String parentKey = parent.getVersion() != null
                    ? parent.getName() + "@" + parent.getVersion()
                    : parent.getName();
            key = parentKey + "/" + packageName;
        }
        return setFlatEntry(doc, "resolutions", key, newVersion);
    }

    // -------------------------------------------------------------------------
    // pnpm <= 10: "pnpm.overrides" nested field with bare package keys
    // -------------------------------------------------------------------------

    private static Json.Document applyPnpmOverride(Json.Document doc,
                                                    String packageName,
                                                    String newVersion,
                                                    @Nullable List<DependencyPathSegment> path,
                                                    boolean hasPath) {
        requireBarePackageName(packageName);
        PackageJsonHelper.requireSemverRange(packageName, newVersion);
        if (hasPath) {
            throw unsupported(packageName, "pnpm override dependency paths are not supported");
        }
        return setPnpmOverridesEntry(doc, packageName, newVersion);
    }

    private static void requireBarePackageName(String name) {
        if (!BARE_PACKAGE_NAME.matcher(name).matches()) {
            throw unsupported(name, "Unsupported pnpm override selector '" + name +
                    "'; only bare package names are supported");
        }
    }

    private static EngineFailure unsupported(String name, String detail) {
        return new EngineFailure(LockFileRegeneration.Reason.UNSUPPORTED_ENTRY_TYPE, name, detail);
    }

    // -------------------------------------------------------------------------
    // Formatting-preserved helpers
    // -------------------------------------------------------------------------

    /**
     * Sets {@code topLevelKey[entryKey] = entryValue} in a flat top-level object,
     * creating the parent object if absent. Preserves formatting.
     */
    private static Json.Document setFlatEntry(Json.Document doc,
                                               String topLevelKey,
                                               String entryKey,
                                               String entryValue) {
        if (!(doc.getValue() instanceof Json.JsonObject)) return doc;
        Json.JsonObject root = (Json.JsonObject) doc.getValue();

        Json.JsonObject existingScope = findObjectMember(root, topLevelKey);
        if (existingScope == null) {
            // Create the parent object with the single entry
            return PackageJsonHelper.addDependency(doc, entryKey, entryValue, topLevelKey);
        }

        // Check if key already exists — replace or append
        for (org.openrewrite.json.tree.Json m : existingScope.getMembers()) {
            if (m instanceof Json.Member) {
                Json.Member member = (Json.Member) m;
                if (entryKey.equals(literalString(member.getKey()))) {
                    // If already set to the same value, return unchanged to stay single-cycle
                    if (entryValue.equals(literalString(member.getValue()))) {
                        return doc;
                    }
                    // Replace existing entry value
                    Json.JsonObject updated = replaceMemberValue(existingScope, entryKey, entryValue);
                    return doc.withValue(replaceMemberValue(root, topLevelKey, updated));
                }
            }
        }

        // Key not present: append
        return PackageJsonHelper.addDependency(doc, entryKey, entryValue, topLevelKey);
    }

    /**
     * Sets {@code pnpm.overrides[key] = value}, creating {@code pnpm} and/or
     * {@code pnpm.overrides} objects as needed. Preserves formatting.
     */
    private static Json.Document setPnpmOverridesEntry(Json.Document doc, String key, String value) {
        if (!(doc.getValue() instanceof Json.JsonObject)) return doc;
        Json.JsonObject root = (Json.JsonObject) doc.getValue();

        for (Json member : root.getMembers()) {
            if (member instanceof Json.Member && "packageManager".equals(literalString(((Json.Member) member).getKey()))) {
                String manager = literalString(((Json.Member) member).getValue());
                if (manager != null && manager.matches("pnpm@(?:1[1-9]|[2-9][0-9]|[1-9][0-9]{2,})\\..*")) {
                    throw unsupported(key, "pnpm 11 and newer require overrides in pnpm-workspace.yaml; " +
                            "package.json pnpm.overrides is ignored");
                }
            }
        }
        Json.JsonObject pnpm = objectMember(root, "pnpm", key);
        Json.JsonObject overrides = objectMember(pnpm, "overrides", key);
        for (Json entry : overrides.getMembers()) {
            if (entry instanceof Json.Member) {
                Json.Member member = (Json.Member) entry;
                String selector = literalString(member.getKey());
                if (selector == null) throw unsupported(key, "Unsupported pnpm override selector");
                requireBarePackageName(selector);
                PackageJsonHelper.requireSemverRange(selector, literalString(member.getValue()));
            }
        }
        String indent = PackageJsonHelper.detectIndentUnit(root);
        Json.JsonObject updated = setEntry(overrides, key, makeStringLiteral(value), indent, 3);
        if (updated == overrides) return doc;
        pnpm = setEntry(pnpm, "overrides", updated, indent, 2);
        return doc.withValue(setEntry(root, "pnpm", pnpm, indent, 1));
    }

    private static Json.JsonObject objectMember(Json.JsonObject parent, String key, String packageName) {
        for (Json entry : parent.getMembers()) {
            if (entry instanceof Json.Member && key.equals(literalString(((Json.Member) entry).getKey()))) {
                JsonValue value = ((Json.Member) entry).getValue();
                if (value instanceof Json.JsonObject) return (Json.JsonObject) value;
                throw unsupported(packageName, "Expected pnpm " + key + " to be an object");
            }
        }
        return new Json.JsonObject(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, emptyList());
    }

    private static Json.JsonObject setEntry(Json.JsonObject obj, String key, JsonValue value,
                                            String indent, int depth) {
        for (Json entry : obj.getMembers()) {
            if (entry instanceof Json.Member && key.equals(literalString(((Json.Member) entry).getKey()))) {
                JsonValue oldValue = ((Json.Member) entry).getValue();
                if (value == oldValue || value instanceof Json.Literal &&
                        Objects.equals(literalString(value), literalString(oldValue))) return obj;
                return replaceMemberValue(obj, key, value);
            }
        }
        Json.Member member = PackageJsonHelper.makeMember(key, value, Space.EMPTY);
        if (obj.getMembers().stream().allMatch(m -> m instanceof Json.Empty)) {
            String memberIndent = String.join("", java.util.Collections.nCopies(depth, indent));
            String closingIndent = String.join("", java.util.Collections.nCopies(depth - 1, indent));
            Space closing = Space.build("\n" + closingIndent, emptyList());
            if (!obj.getPadding().getMembers().isEmpty()) {
                Space existingClosing = obj.getPadding().getMembers().get(0).getAfter();
                if (!existingClosing.getComments().isEmpty()) closing = existingClosing;
            }
            return obj.getPadding().withMembers(singletonList(JsonRightPadded.build((Json) member
                            .withPrefix(Space.build("\n" + memberIndent, emptyList())))
                    .withAfter(closing)));
        }
        List<JsonRightPadded<Json>> members = new ArrayList<>(obj.getPadding().getMembers());
        int last = members.size() - 1;
        boolean trailingComma = members.get(last).getElement() instanceof Json.Empty;
        int previous = trailingComma ? last - 1 : last;
        JsonRightPadded<Json> sibling = members.get(previous);
        Space prefix = sibling.getElement().getPrefix();
        if (prefix.isEmpty() && sibling.getElement() instanceof Json.Member) {
            prefix = ((Json.Member) sibling.getElement()).getKey().getPrefix();
        }
        // Copy indentation, never duplicate a sibling's comments.
        member = member.withPrefix(Space.build(prefix.getWhitespace(), emptyList()));
        JsonRightPadded<Json> added = JsonRightPadded.build((Json) member);
        if (trailingComma) {
            members.add(last, added);
        } else {
            members.set(last, sibling.withAfter(Space.EMPTY));
            members.add(added.withAfter(sibling.getAfter()));
        }
        return obj.getPadding().withMembers(members);
    }

    // -------------------------------------------------------------------------
    // Reparse-based helpers (used for deep-nested cases)
    // -------------------------------------------------------------------------

    /**
     * Merges {@code newObjectJson} into the existing {@code topLevelKey} object
     * (or creates it) by re-serializing the whole document.
     */
    private static Json.Document mergeTopLevelObjectReparse(Json.Document doc,
                                                             String topLevelKey,
                                                             String newObjectJson) {
        String serialized = doc.printAll();
        // Use Jackson-style logic: parse as map, merge, re-serialize.
        // Simpler: build string from scratch using the existing document as source.
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> root =
                    mapper.readValue(serialized, java.util.Map.class);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> newFragment =
                    mapper.readValue(newObjectJson, java.util.Map.class);

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> existing =
                    (java.util.Map<String, Object>) root.getOrDefault(topLevelKey, new java.util.LinkedHashMap<>());
            java.util.Map<String, Object> merged = mergeDeep(existing, newFragment);
            root.put(topLevelKey, merged);

            String newJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            return PackageJsonHelper.reparseJson(doc, newJson);
        } catch (Exception e) {
            return doc;
        }
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<String, Object> mergeDeep(java.util.Map<String, Object> base,
                                                             java.util.Map<String, Object> overlay) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>(base);
        for (java.util.Map.Entry<String, Object> e : overlay.entrySet()) {
            Object existing = result.get(e.getKey());
            if (existing instanceof java.util.Map && e.getValue() instanceof java.util.Map) {
                result.put(e.getKey(), mergeDeep(
                        (java.util.Map<String, Object>) existing,
                        (java.util.Map<String, Object>) e.getValue()));
            } else {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Tiny JSON helpers (avoid importing full JsonHelper as these are static utils)
    // -------------------------------------------------------------------------

    private static @Nullable String literalString(@Nullable Object node) {
        if (node instanceof Json.Literal) {
            Object val = ((Json.Literal) node).getValue();
            return val == null ? null : val.toString();
        }
        return null;
    }

    private static Json.@Nullable JsonObject findObjectMember(Json.JsonObject obj, String name) {
        for (org.openrewrite.json.tree.Json m : obj.getMembers()) {
            if (m instanceof Json.Member) {
                Json.Member member = (Json.Member) m;
                if (name.equals(literalString(member.getKey()))
                        && member.getValue() instanceof Json.JsonObject) {
                    return (Json.JsonObject) member.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Returns a copy of {@code obj} with the string value of member {@code key}
     * replaced by a new literal for {@code newValue}.
     */
    private static Json.JsonObject replaceMemberValue(Json.JsonObject obj, String key, String newValue) {
        org.openrewrite.json.tree.JsonValue newLit = makeStringLiteral(newValue);
        return replaceMemberValue(obj, key, newLit);
    }

    private static Json.JsonObject replaceMemberValue(Json.JsonObject obj, String key, org.openrewrite.json.tree.JsonValue newValue) {
        java.util.List<org.openrewrite.json.tree.JsonRightPadded<org.openrewrite.json.tree.Json>> members =
                new java.util.ArrayList<>(obj.getPadding().getMembers());
        for (int i = 0; i < members.size(); i++) {
            org.openrewrite.json.tree.Json elem = members.get(i).getElement();
            if (elem instanceof Json.Member) {
                Json.Member member = (Json.Member) elem;
                if (key.equals(literalString(member.getKey()))) {
                    org.openrewrite.json.tree.JsonValue spaced = newValue;
                    if (spaced instanceof Json.Literal) {
                        spaced = ((Json.Literal) spaced).withPrefix(member.getValue().getPrefix());
                    }
                    members.set(i, members.get(i).withElement(member.withValue(spaced)));
                    break;
                }
            }
        }
        return obj.getPadding().withMembers(members);
    }

    private static Json.Literal makeStringLiteral(String value) {
        return new Json.Literal(
                org.openrewrite.Tree.randomId(),
                org.openrewrite.json.tree.Space.EMPTY,
                org.openrewrite.marker.Markers.EMPTY,
                "\"" + value + "\"", value);
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
