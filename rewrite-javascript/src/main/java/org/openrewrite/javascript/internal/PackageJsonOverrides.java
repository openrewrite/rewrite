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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;
import org.openrewrite.marker.Markers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Ports {@code parseDependencyPath} and {@code applyOverrideToPackageJson}
 * from {@code dependency-manager.ts}.
 */
public final class PackageJsonOverrides {

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

        // The reparse below rebuilds the whole document, so without this the entry is rewritten on every cycle
        // and the recipe never stabilises. Same guard setFlatEntry applies to the un-nested case.
        if (newVersion.equals(nestedOverrideValue(doc, path, packageName))) {
            return doc;
        }

        return setNestedOverride(doc, path, packageName, newVersion);
    }

    /**
     * Set {@code overrides -> path... -> packageName}. Only the {@code overrides} value is rebuilt and spliced
     * back in, so every other member keeps its original whitespace, which a whole-document reparse discarded.
     * <p>
     * Members already inside {@code overrides} are re-rendered rather than preserved: an existing
     * {@code "overrides": { "a": "1.0.0" }} comes back expanded over several lines. Only this nested path
     * does that; the un-nested one appends through {@code setFlatEntry} and keeps the block as it was.
     * Building the missing members directly with {@link PackageJsonHelper#makeMember} rather than re-rendering
     * the value would close the gap.
     */
    private static Json.Document setNestedOverride(Json.Document doc, List<DependencyPathSegment> path,
                                                   String packageName, String newVersion) {
        if (!(doc.getValue() instanceof Json.JsonObject)) {
            return doc;
        }
        Json.JsonObject root = (Json.JsonObject) doc.getValue();
        String indent = PackageJsonHelper.detectIndentUnit(root);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(doc.printAll());
            ObjectNode overrides = rootNode.path("overrides").isObject() ?
                    (ObjectNode) rootNode.get("overrides") : mapper.createObjectNode();

            ObjectNode at = overrides;
            for (DependencyPathSegment seg : path) {
                String key = seg.getVersion() != null ? seg.getName() + "@" + seg.getVersion() : seg.getName();
                at = at.path(key).isObject() ? (ObjectNode) at.get(key) : at.putObject(key);
            }
            at.put(packageName, newVersion);

            JsonValue value = parseFragment(doc, mapper, overrides, indent);
            if (value == null) {
                return doc;
            }
            if (findObjectMember(root, "overrides") != null) {
                return doc.withValue(replaceMemberValue(root, "overrides", value));
            }
            List<JsonRightPadded<Json>> members = new ArrayList<>(root.getPadding().getMembers());
            // The last member's `after` holds the whitespace before the object's closing brace, so it moves to
            // the new last member rather than being dropped.
            Space closing = Space.EMPTY;
            if (!members.isEmpty()) {
                int last = members.size() - 1;
                closing = members.get(last).getAfter();
                members.set(last, members.get(last).withAfter(Space.EMPTY));
            }
            members.add(new JsonRightPadded<>(
                    PackageJsonHelper.makeMember("overrides", value, Space.build("\n" + indent, emptyList())),
                    closing, Markers.EMPTY));
            return doc.withValue(root.getPadding().withMembers(members));
        } catch (Exception e) {
            return doc;
        }
    }

    /**
     * Render {@code overrides} with the document's own indent unit and parse it on its own, so only this value
     * is rebuilt. It sits one level in, so every line after the first carries an extra unit.
     */
    private static @Nullable JsonValue parseFragment(Json.Document doc, ObjectMapper mapper,
                                                     ObjectNode overrides, String indent) throws Exception {
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter() {
            @Override
            public DefaultPrettyPrinter createInstance() {
                return this;
            }

            @Override
            public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
                g.writeRaw(": ");
            }
        };
        printer.indentObjectsWith(new DefaultIndenter(indent, "\n"));
        String printed = mapper.writer(printer).writeValueAsString(overrides);

        StringBuilder sb = new StringBuilder();
        String[] lines = printed.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            sb.append(i == 0 ? "" : "\n" + indent).append(lines[i]);
        }
        Json.Document holder = PackageJsonHelper.reparseJson(doc, sb.toString());
        return holder.getValue() instanceof Json.JsonObject ?
                ((Json.JsonObject) holder.getValue()).withPrefix(Space.build(" ", emptyList())) : null;
    }

    /** The value {@code overrides} already holds at {@code path -> packageName}, or {@code null}. */
    private static @Nullable String nestedOverrideValue(Json.Document doc, List<DependencyPathSegment> path,
                                                        String packageName) {
        if (!(doc.getValue() instanceof Json.JsonObject)) {
            return null;
        }
        Json.JsonObject at = findObjectMember((Json.JsonObject) doc.getValue(), "overrides");
        for (DependencyPathSegment seg : path) {
            if (at == null) {
                return null;
            }
            at = findObjectMember(at, seg.getVersion() != null ?
                    seg.getName() + "@" + seg.getVersion() : seg.getName());
        }
        if (at == null) {
            return null;
        }
        for (Json m : at.getMembers()) {
            if (m instanceof Json.Member && packageName.equals(literalString(((Json.Member) m).getKey()))) {
                return literalString(((Json.Member) m).getValue());
            }
        }
        return null;
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
    // pnpm: "pnpm.overrides" nested field with flat > path keys
    // -------------------------------------------------------------------------

    private static Json.Document applyPnpmOverride(Json.Document doc,
                                                    String packageName,
                                                    String newVersion,
                                                    @Nullable List<DependencyPathSegment> path,
                                                    boolean hasPath) {
        String key;
        if (!hasPath) {
            key = packageName;
        } else {
            StringBuilder sb = new StringBuilder();
            for (DependencyPathSegment seg : path) {
                if (sb.length() > 0) sb.append(">");
                sb.append(seg.getVersion() != null ? seg.getName() + "@" + seg.getVersion() : seg.getName());
            }
            sb.append(">").append(packageName);
            key = sb.toString();
        }
        return setPnpmOverridesEntry(doc, key, newVersion);
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
     * Sets {@code pnpm.overrides[key] = value}, creating {@code pnpm} and/or {@code pnpm.overrides}
     * as needed. Preserves formatting, and returns the document unchanged when the entry already holds
     * that value, which is what keeps the calling recipe single-cycle.
     */
    private static Json.Document setPnpmOverridesEntry(Json.Document doc, String key, String value) {
        return PackageJsonHelper.setNestedEntry(doc, "pnpm", "overrides", key, value);
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
                Space.EMPTY,
                org.openrewrite.marker.Markers.EMPTY,
                "\"" + value + "\"", value);
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
