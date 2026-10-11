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
import org.openrewrite.Tree;
import org.openrewrite.javascript.internal.lock.DependencyPathSegment;
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.ManifestRules;
import org.openrewrite.javascript.internal.lock.ManifestRules.OverridePlace;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;
import org.openrewrite.marker.Markers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

/**
 * An override of a transitive dependency written where the workspace root's package manager reads it, in its own key
 * syntax: npm's and Bun's {@code overrides}, Yarn's {@code resolutions}, pnpm's by the release the project names.
 */
public final class PackageJsonOverrides {

    public static final String WORKSPACE_MANIFEST = "pnpm-workspace.yaml";

    private final ManifestRules rules;
    private final String packageName;
    private final String newVersion;
    private final @Nullable List<DependencyPathSegment> path;
    private final Json.Document root;
    private final WorkspaceSettings workspace;
    private final @Nullable PnpmRelease release;
    private final OverridePlace place;
    private @Nullable String rootText;
    private @Nullable String declined;
    private boolean declinedKnown;

    private PackageJsonOverrides(ManifestRules rules, Json.Document root, WorkspaceSettings workspace,
                                 String packageName, String newVersion, @Nullable List<DependencyPathSegment> path) {
        this.rules = rules;
        this.root = root;
        this.packageName = packageName;
        this.newVersion = newVersion;
        this.path = path;
        this.workspace = workspace;
        this.release = PnpmRelease.of(rules, rootText(), workspace.settings());
        this.place = rules.overridePlace(rootText(), workspace.settings(), release == null ? -1 : release.getMajor());
    }

    // the root as printed, once
    private String rootText() {
        if (rootText == null) {
            rootText = PackageJsonHelper.printed(root);
        }
        return rootText;
    }

    /**
     * The override of {@code packageName} along {@code path} for the workspace root {@code root}, given the
     * workspace's pnpm-workspace.yaml (null when it has none), which is read once.
     */
    public static PackageJsonOverrides of(PackageManager pm, Json.Document root, @Nullable String workspaceManifest,
                                          String packageName, String newVersion,
                                          @Nullable List<DependencyPathSegment> path) {
        ManifestRules rules = LockFileRegeneration.manifestRules(pm);
        return new PackageJsonOverrides(rules, root, new WorkspaceSettings(rules, workspaceManifest), packageName,
                newVersion, path);
    }

    /**
     * This override decided again for other revisions of the root and its pnpm-workspace.yaml; this one when it was
     * decided from them. A pnpm-workspace.yaml of the same text is not read again.
     */
    public PackageJsonOverrides from(Json.Document root, @Nullable String workspaceManifest) {
        boolean sameWorkspace = workspace.isText(workspaceManifest);
        if (root == this.root && sameWorkspace) {
            return this;
        }
        return new PackageJsonOverrides(rules, root, sameWorkspace ? workspace :
                new WorkspaceSettings(rules, workspaceManifest), packageName, newVersion, path);
    }

    /**
     * Whether {@code other} writes the override where this one does, for the same pnpm release, or declines it alike.
     */
    public boolean writesAs(PackageJsonOverrides other) {
        return place == other.place && getRelease() == other.getRelease() &&
               Objects.equals(declined(), other.declined());
    }

    @Override
    public String toString() {
        String why = declined();
        if (why != null) {
            return "declined (" + why + ")";
        }
        String where = place == OverridePlace.WORKSPACE_MANIFEST ? WORKSPACE_MANIFEST :
                place == OverridePlace.PNPM_FIELD ? "pnpm.overrides" :
                        place == OverridePlace.RESOLUTIONS ? "resolutions" : "overrides";
        return release != null ? where + " for " + release : where;
    }

    /**
     * Where the root's package manager reads the override.
     */
    public OverridePlace getPlace() {
        return place;
    }

    int getRelease() {
        return release == null ? -1 : release.getMajor();
    }

    /**
     * Parses a dependency path ({@code express>accepts}, {@code express@4.0.0/accepts}) into segments at {@code >} or
     * {@code /}; a scoped name ({@code @scope/pkg}) is one segment.
     */
    public static List<DependencyPathSegment> parsePath(String path) {
        List<DependencyPathSegment> segments = new ArrayList<>();
        for (String gtPart : path.split(">")) {
            if (!gtPart.contains("/")) {
                segments.add(parseSegment(gtPart));
            } else if (gtPart.startsWith("@")) {
                int firstSlash = gtPart.indexOf('/');
                String afterFirstSlash = gtPart.substring(firstSlash + 1);
                int secondSlash = afterFirstSlash.indexOf('/');
                if (secondSlash == -1) {
                    segments.add(parseSegment(gtPart));
                } else {
                    segments.add(parseSegment(gtPart.substring(0, firstSlash + 1 + secondSlash)));
                    for (String subPart : afterFirstSlash.substring(secondSlash + 1).split("/")) {
                        if (!subPart.isEmpty()) {
                            segments.add(parseSegment(subPart));
                        }
                    }
                }
            } else {
                for (String slashPart : gtPart.split("/")) {
                    if (!slashPart.isEmpty()) {
                        segments.add(parseSegment(slashPart));
                    }
                }
            }
        }
        return segments;
    }

    private static DependencyPathSegment parseSegment(String part) {
        if (part.startsWith("@")) {
            int slashIndex = part.indexOf('/');
            if (slashIndex == -1) {
                return new DependencyPathSegment(part, null);
            }
            String afterSlash = part.substring(slashIndex + 1);
            int atIndex = afterSlash.lastIndexOf('@');
            if (atIndex > 0) {
                return new DependencyPathSegment(part.substring(0, slashIndex + 1 + atIndex),
                        afterSlash.substring(atIndex + 1));
            }
            return new DependencyPathSegment(part, null);
        }
        int atIndex = part.lastIndexOf('@');
        if (atIndex > 0) {
            return new DependencyPathSegment(part.substring(0, atIndex), part.substring(atIndex + 1));
        }
        return new DependencyPathSegment(part, null);
    }

    /**
     * Why the root's package manager would not read the override where it is written, or why pnpm-workspace.yaml is
     * not edited in place; null when neither.
     */
    public @Nullable String declined() {
        if (!declinedKnown) {
            declined = decline();
            if (declined == null && place == OverridePlace.WORKSPACE_MANIFEST) {
                try {
                    applyToWorkspaceManifest();
                } catch (IllegalArgumentException e) {
                    declined = String.valueOf(e.getMessage());
                }
            }
            declinedKnown = true;
        }
        return declined;
    }

    private @Nullable String decline() {
        if (release != null && release.declined() != null) {
            return "the override is not written: " + release.declined();
        }
        String rejected = rules.overrideDeclined(packageName, segments());
        if (rejected != null) {
            return rejected;
        }
        String field = place == OverridePlace.OVERRIDES ? "overrides" :
                place == OverridePlace.RESOLUTIONS ? "resolutions" : place == OverridePlace.PNPM_FIELD ? "pnpm" : null;
        Json value = field == null ? null : member(root, field);
        if (value != null && !(value instanceof Json.JsonObject)) {
            return "the root's " + field + " is not an object";
        }
        if (place == OverridePlace.PNPM_FIELD && value != null) {
            Json overrides = member((Json.JsonObject) value, "overrides");
            if (overrides != null && !(overrides instanceof Json.JsonObject)) {
                return "the root's pnpm.overrides is not an object";
            }
        }
        return null;
    }

    /**
     * {@code doc}, a revision of the root's package.json, with the override at {@link #getPlace()}; unchanged when
     * that is pnpm-workspace.yaml or the package manager already reads {@code newVersion} there.
     */
    public Json.Document apply(Json.Document doc) {
        switch (place) {
            case OVERRIDES:
                if (path == null || path.isEmpty()) {
                    // npm reads an object's "." as its own override, the other keys below it
                    // (override-recipe-object-value-pinned)
                    Json overrides = member(doc, "overrides");
                    return overrides instanceof Json.JsonObject &&
                           member((Json.JsonObject) overrides, packageName) instanceof Json.JsonObject ?
                            PackageJsonHelper.setNestedEntry(doc, "overrides", packageName, ".", newVersion) :
                            setFlatEntry(doc, "overrides", packageName, newVersion);
                }
                if (newVersion.equals(nestedOverrideValue(doc, path, packageName))) {
                    return doc;
                }
                return setNestedOverride(doc, path, packageName, newVersion);
            case RESOLUTIONS:
                return setFlatEntry(doc, "resolutions", rules.overrideKey(packageName, segments(),
                        keys(member(doc, "resolutions"))), newVersion);
            case PNPM_FIELD:
                String key = rules.overrideKey(packageName, segments(), emptyList());
                Json pnpm = member(doc, "pnpm");
                Json overrides = pnpm instanceof Json.JsonObject ? member((Json.JsonObject) pnpm, "overrides") : null;
                Json resolutions = member(doc, "resolutions");
                if ((!(overrides instanceof Json.JsonObject) || member((Json.JsonObject) overrides, key) == null) &&
                    resolutions instanceof Json.JsonObject &&
                    newVersion.equals(literalString(member((Json.JsonObject) resolutions, key)))) {
                    return doc;
                }
                return PackageJsonHelper.setNestedEntry(doc, "pnpm", "overrides", key, newVersion);
            case WORKSPACE_MANIFEST:
            default:
                return doc;
        }
    }

    /**
     * The pnpm-workspace.yaml this override was decided from (none when null) with the override as pnpm writes it;
     * an {@link IllegalArgumentException} names why it is not written (an anchor, a block scalar).
     */
    public String applyToWorkspaceManifest() {
        return applyTo(NodeCatalogs.Pnpm.parse(workspace.text, rules, requireNonNull(release))).print();
    }

    /**
     * {@code workspace}, the revision of pnpm-workspace.yaml this override was decided from, with the override as
     * {@link #applyToWorkspaceManifest()} writes it, by the one editor of that file.
     */
    public NodeCatalogs applyTo(NodeCatalogs.Pnpm workspace) {
        return workspace.forRelease(requireNonNull(release)).withOverride(rules.overrideKey(packageName, segments(),
                emptyList()), newVersion);
    }

    private List<DependencyPathSegment> segments() {
        return path == null ? emptyList() : path;
    }

    // a JSON object's keys in the order of its members
    private static List<String> keys(@Nullable Json object) {
        List<String> keys = new ArrayList<>();
        if (object instanceof Json.JsonObject) {
            for (Json m : ((Json.JsonObject) object).getMembers()) {
                String key = m instanceof Json.Member ? literalString(((Json.Member) m).getKey()) : null;
                if (key != null) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    /**
     * The overrides already in place that the package manager applies before a global one where both match, as it
     * reads them; null when there are none or the override is scoped.
     */
    public @Nullable String outranked() {
        if (!segments().isEmpty() || declined() != null) {
            return null;
        }
        return rules.outranked(packageName, overrideKeys(), parsedResolutions());
    }

    // the root's resolutions as JSON.parse reads them
    @SuppressWarnings("unchecked")
    private Map<String, @Nullable Object> parsedResolutions() {
        Object parsed;
        try {
            parsed = EcmaScript.jsonParse(rootText());
        } catch (RuntimeException e) {
            return emptyMap();
        }
        Object resolutions = parsed instanceof Map ? ((Map<String, ?>) parsed).get("resolutions") : null;
        return resolutions instanceof Map ? (Map<String, @Nullable Object>) resolutions : emptyMap();
    }

    private List<String> overrideKeys() {
        List<String> keys = new ArrayList<>();
        if (place == OverridePlace.WORKSPACE_MANIFEST) {
            Object overrides = workspace.setting("overrides");
            if (overrides instanceof Map) {
                for (Object key : ((Map<?, ?>) overrides).keySet()) {
                    keys.add(String.valueOf(key));
                }
            }
            return keys;
        }
        Json pnpm = member(root, "pnpm");
        for (Json map : Arrays.asList(member(root, "resolutions"), place == OverridePlace.PNPM_FIELD &&
                pnpm instanceof Json.JsonObject ? member((Json.JsonObject) pnpm, "overrides") : null)) {
            keys.addAll(keys(map));
        }
        return keys;
    }

    private static String segment(DependencyPathSegment segment) {
        return segment.getVersion() == null ? segment.getName() : segment.getName() + "@" + segment.getVersion();
    }

    private static Json.Document setNestedOverride(Json.Document doc, List<DependencyPathSegment> path,
                                                   String packageName, String newVersion) {
        if (!(doc.getValue() instanceof Json.JsonObject)) {
            return doc;
        }
        String indent = PackageJsonHelper.indentUnit(doc);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(PackageJsonHelper.printed(doc));
            ObjectNode overrides = rootNode.path("overrides").isObject() ?
                    (ObjectNode) rootNode.get("overrides") : mapper.createObjectNode();
            ObjectNode at = overrides;
            for (DependencyPathSegment seg : path) {
                String key = segment(seg);
                JsonNode existing = at.get(key);
                if (existing != null && existing.isObject()) {
                    at = (ObjectNode) existing;
                } else {
                    ObjectNode nested = at.putObject(key);
                    if (existing != null && existing.isTextual()) {
                        nested.put(".", existing.asText());
                    }
                    at = nested;
                }
            }
            if (at.path(packageName).isObject()) {
                ((ObjectNode) at.get(packageName)).put(".", newVersion);
            } else {
                at.put(packageName, newVersion);
            }

            return PackageJsonHelper.setMember(doc, "overrides", parseFragment(doc, mapper, overrides, indent));
        } catch (IOException e) {
            throw new IllegalStateException("could not write the override of " + packageName, e);
        }
    }

    private static JsonValue parseFragment(Json.Document doc, ObjectMapper mapper, ObjectNode overrides,
                                           String indent) throws IOException {
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
        if (!(holder.getValue() instanceof Json.JsonObject)) {
            throw new IllegalStateException("the overrides object did not reparse as one");
        }
        return ((Json.JsonObject) holder.getValue()).withPrefix(Space.build(" ", emptyList()));
    }

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
            at = findObjectMember(at, segment(seg));
        }
        if (at == null) {
            return null;
        }
        Json.JsonObject pinned = findObjectMember(at, packageName);
        String key = pinned == null ? packageName : ".";
        for (Json m : (pinned == null ? at : pinned).getMembers()) {
            if (m instanceof Json.Member && key.equals(literalString(((Json.Member) m).getKey()))) {
                return literalString(((Json.Member) m).getValue());
            }
        }
        return null;
    }

    private static Json.Document setFlatEntry(Json.Document doc, String topLevelKey, String entryKey,
                                              String entryValue) {
        if (!(doc.getValue() instanceof Json.JsonObject)) {
            return doc;
        }
        Json.JsonObject root = (Json.JsonObject) doc.getValue();
        Json.JsonObject existingScope = findObjectMember(root, topLevelKey);
        if (existingScope != null) {
            for (Json m : existingScope.getMembers()) {
                if (m instanceof Json.Member && entryKey.equals(literalString(((Json.Member) m).getKey()))) {
                    if (entryValue.equals(literalString(((Json.Member) m).getValue()))) {
                        return doc;
                    }
                    Json.JsonObject updated = replaceMemberValue(existingScope, entryKey,
                            makeStringLiteral(entryValue));
                    return doc.withValue(replaceMemberValue(root, topLevelKey, updated));
                }
            }
        }
        return PackageJsonHelper.addDependency(doc, entryKey, entryValue, topLevelKey);
    }

    private static @Nullable Json member(Json.Document doc, String name) {
        return doc.getValue() instanceof Json.JsonObject ? member((Json.JsonObject) doc.getValue(), name) : null;
    }

    private static @Nullable Json member(Json.JsonObject obj, String name) {
        for (Json m : obj.getMembers()) {
            if (m instanceof Json.Member && name.equals(literalString(((Json.Member) m).getKey()))) {
                return ((Json.Member) m).getValue();
            }
        }
        return null;
    }

    private static @Nullable String literal(Json obj, String name) {
        return obj instanceof Json.JsonObject ? literalString(member((Json.JsonObject) obj, name)) : null;
    }

    // a JSON string's value; null for any other value, which pnpm does not read as a string
    private static @Nullable String string(@Nullable Json value) {
        Object literal = value instanceof Json.Literal ? ((Json.Literal) value).getValue() : null;
        return literal instanceof String ? (String) literal : null;
    }

    private static boolean truthy(@Nullable Json value) {
        if (!(value instanceof Json.Literal)) {
            return value != null;
        }
        Object literal = ((Json.Literal) value).getValue();
        return literal != null && !Boolean.FALSE.equals(literal) && !"".equals(literal) &&
               !(literal instanceof Number && (((Number) literal).doubleValue() == 0 ||
                                               Double.isNaN(((Number) literal).doubleValue())));
    }

    private static @Nullable String literalString(@Nullable Object node) {
        if (node instanceof Json.Literal) {
            Object val = ((Json.Literal) node).getValue();
            return val == null ? null : val.toString();
        }
        return null;
    }

    private static Json.@Nullable JsonObject findObjectMember(Json.JsonObject obj, String name) {
        Json value = member(obj, name);
        return value instanceof Json.JsonObject ? (Json.JsonObject) value : null;
    }

    private static Json.JsonObject replaceMemberValue(Json.JsonObject obj, String key, JsonValue newValue) {
        List<JsonRightPadded<Json>> members = new ArrayList<>(obj.getPadding().getMembers());
        for (int i = 0; i < members.size(); i++) {
            Json elem = members.get(i).getElement();
            if (elem instanceof Json.Member && key.equals(literalString(((Json.Member) elem).getKey()))) {
                Json.Member member = (Json.Member) elem;
                JsonValue spaced = newValue instanceof Json.Literal ?
                        ((Json.Literal) newValue).withPrefix(member.getValue().getPrefix()) : newValue;
                members.set(i, members.get(i).withElement(member.withValue(spaced)));
                break;
            }
        }
        return obj.getPadding().withMembers(members);
    }

    private static Json.Literal makeStringLiteral(String value) {
        return new Json.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "\"" + value + "\"", value);
    }

    /**
     * A revision of pnpm-workspace.yaml as pnpm reads it, once; no settings when absent or unreadable.
     */
    private static final class WorkspaceSettings {
        private final @Nullable String text;
        private final @Nullable Map<String, @Nullable Object> loaded;

        WorkspaceSettings(ManifestRules rules, @Nullable String text) {
            this.text = text;
            Map<String, @Nullable Object> document = null;
            try {
                document = rules.workspaceSettings(text);
            } catch (RuntimeException ignored) {
                // the editor names why pnpm rejects it
            }
            this.loaded = document;
        }

        @Nullable
        Map<String, @Nullable Object> settings() {
            return loaded;
        }

        boolean isText(@Nullable String other) {
            return text == null ? other == null : text.equals(other);
        }

        @Nullable
        Object setting(String key) {
            return loaded == null ? null : loaded.get(key);
        }

        boolean holdsOverrides() {
            Object overrides = setting("overrides");
            return overrides instanceof Map && !((Map<?, ?>) overrides).isEmpty();
        }
    }
}
