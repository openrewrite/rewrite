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
import org.openrewrite.javascript.internal.lock.pnpm.PnpmOptions;
import org.openrewrite.javascript.internal.lock.pnpm.WorkspaceManifest;
import org.openrewrite.javascript.internal.lock.pnpm.WorkspaceManifestWriter;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.semver.NodeSemver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * An override of a transitive dependency written where the workspace root's package manager reads it, in its own key
 * syntax: npm's and Bun's {@code overrides}, Yarn's {@code resolutions}, pnpm's by the release the project names.
 */
public final class PackageJsonOverrides {

    public static final String WORKSPACE_MANIFEST = "pnpm-workspace.yaml";
    private static final Pattern YARN_PATH_SEGMENT = Pattern.compile("(@[^/]+/)?([^/]+)");

    private final PackageManager pm;
    private final String packageName;
    private final String newVersion;
    private final @Nullable List<DependencyPathSegment> path;
    private final Json.Document root;
    private final WorkspaceSettings workspace;
    private final int release;
    private final Place place;
    private @Nullable String declined;
    private boolean declinedKnown;

    private PackageJsonOverrides(PackageManager pm, Json.Document root, WorkspaceSettings workspace,
                                 String packageName, String newVersion, @Nullable List<DependencyPathSegment> path) {
        this.pm = pm;
        this.root = root;
        this.packageName = packageName;
        this.newVersion = newVersion;
        this.path = path;
        this.workspace = workspace;
        this.release = pm == PackageManager.Pnpm ? pnpmRelease(root, workspace) : -1;
        this.place = place(pm, root, workspace, release);
    }

    /**
     * The override of {@code packageName} along {@code path} for the workspace root {@code root}, given the
     * workspace's pnpm-workspace.yaml (null when it has none), which is read once.
     */
    public static PackageJsonOverrides of(PackageManager pm, Json.Document root, @Nullable String workspaceManifest,
                                          String packageName, String newVersion,
                                          @Nullable List<DependencyPathSegment> path) {
        return new PackageJsonOverrides(pm, root, new WorkspaceSettings(workspaceManifest), packageName, newVersion,
                path);
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
        return new PackageJsonOverrides(pm, root, sameWorkspace ? workspace : new WorkspaceSettings(workspaceManifest),
                packageName, newVersion, path);
    }

    /**
     * Whether {@code other} writes the override where this one does, for the same pnpm release, or declines it alike.
     */
    public boolean writesAs(PackageJsonOverrides other) {
        return place == other.place && release == other.release && Objects.equals(declined(), other.declined());
    }

    @Override
    public String toString() {
        String why = declined();
        if (why != null) {
            return "declined (" + why + ")";
        }
        String where = place == Place.WORKSPACE_MANIFEST ? WORKSPACE_MANIFEST : place == Place.PNPM_FIELD ?
                "pnpm.overrides" : place == Place.RESOLUTIONS ? "resolutions" : "overrides";
        return pm == PackageManager.Pnpm ? where + " for pnpm " + release : where;
    }

    /**
     * Where the root's package manager reads the override.
     */
    public Place getPlace() {
        return place;
    }

    int getRelease() {
        return release;
    }

    /**
     * Where a package manager reads overrides.
     */
    public enum Place {
        OVERRIDES, RESOLUTIONS, PNPM_FIELD, WORKSPACE_MANIFEST
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

    private static Place place(PackageManager pm, Json.Document root, WorkspaceSettings workspace, int release) {
        switch (pm) {
            case Bun:
                return member(root, "overrides") == null && member(root, "resolutions") != null ?
                        Place.RESOLUTIONS : Place.OVERRIDES;
            case YarnClassic:
            case YarnBerry:
                return Place.RESOLUTIONS;
            case Pnpm:
                // pnpm 10 reads package.json's overrides, when it holds any, instead of pnpm-workspace.yaml's
                if (release == 9 || release == 10 && (holdsOverrides(root) ||
                                                      !workspace.holdsOverrides() && hasPnpmField(root))) {
                    // pnpm reads {...resolutions, ...pnpm?.overrides}, and a null pnpm field holds no object to edit
                    return !hasPnpmField(root) && member(root, "pnpm") != null &&
                           holdsMembers(member(root, "resolutions")) ? Place.RESOLUTIONS : Place.PNPM_FIELD;
                }
                return Place.WORKSPACE_MANIFEST;
            case Npm:
            default:
                return Place.OVERRIDES;
        }
    }

    private static boolean holdsOverrides(Json.Document root) {
        Json pnpm = member(root, "pnpm");
        return holdsMembers(member(root, "resolutions")) ||
               pnpm instanceof Json.JsonObject && holdsMembers(member((Json.JsonObject) pnpm, "overrides"));
    }

    private static boolean holdsMembers(@Nullable Json value) {
        if (value instanceof Json.JsonObject) {
            for (Json m : ((Json.JsonObject) value).getMembers()) {
                if (m instanceof Json.Member) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasPnpmField(Json.Document root) {
        Json pnpm = member(root, "pnpm");
        return pnpm != null && !(pnpm instanceof Json.Literal && ((Json.Literal) pnpm).getValue() == null);
    }

    /**
     * Why the root's package manager would not read the override where it is written, or why pnpm-workspace.yaml is
     * not edited in place; null when neither.
     */
    public @Nullable String declined() {
        if (!declinedKnown) {
            declined = decline();
            if (declined == null && place == Place.WORKSPACE_MANIFEST) {
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
        int depth = path == null ? 0 : path.size();
        if (pm == PackageManager.Pnpm) {
            if (release < 9 || release > 11) {
                return "where pnpm " + (release < 0 ? "of the release the project names" : release + ".x") +
                       " reads overrides is not known";
            }
            if (depth > 1) {
                return "pnpm reads an override selector of one parent only (ERR_PNPM_INVALID_SELECTOR)";
            }
        } else if (pm == PackageManager.Bun && depth > 0) {
            return "bun does not support nested overrides";
        } else if (pm == PackageManager.YarnBerry && depth > 1) {
            return "Yarn reads a resolution of one parent only (YN0057)";
        } else if (pm == PackageManager.YarnBerry && path != null && depth == 1 &&
                   !exactVersion(path.get(0).getVersion())) {
            return "Yarn matches a resolution's parent by its exact version only, not " + segment(path.get(0));
        } else if (pm == PackageManager.YarnClassic && path != null) {
            for (DependencyPathSegment segment : path) {
                if (segment.getVersion() != null) {
                    return "yarn matches a resolution's path by package names only, not " + segment.getName() +
                           "@" + segment.getVersion();
                }
            }
        }
        String field = place == Place.OVERRIDES ? "overrides" : place == Place.RESOLUTIONS ? "resolutions" :
                place == Place.PNPM_FIELD ? "pnpm" : null;
        Json value = field == null ? null : member(root, field);
        if (value != null && !(value instanceof Json.JsonObject)) {
            return "the root's " + field + " is not an object";
        }
        if (place == Place.PNPM_FIELD && value != null) {
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
                    // npm reads an object's "." as the package's own override and the other keys below it
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
                String resolution = pm == PackageManager.Pnpm ? pnpmKey(packageName, path) :
                        resolutionKey(pm, packageName, path);
                if (pm == PackageManager.YarnClassic) {
                    resolution = yarnClassicKeyOf(member(doc, "resolutions"), resolution);
                }
                return setFlatEntry(doc, "resolutions", resolution, newVersion);
            case PNPM_FIELD:
                String key = pnpmKey(packageName, path);
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
     * The pnpm-workspace.yaml this override was decided from (none when null) with the override where pnpm writes a
     * new key or entry; an {@link IllegalArgumentException} for a layout not edited in place (flow style, anchors).
     */
    public String applyToWorkspaceManifest() {
        return workspace.manifest(release).withOverride(pnpmKey(packageName, path), newVersion);
    }

    private static int pnpmRelease(Json.Document root, WorkspaceSettings workspace) {
        return PnpmOptions.releaseMajor(PackageJsonHelper.printed(root), workspace.settings());
    }

    private static String pnpmKey(String packageName, @Nullable List<DependencyPathSegment> path) {
        return path == null || path.isEmpty() ? packageName : segment(path.get(path.size() - 1)) + ">" + packageName;
    }

    private static String resolutionKey(PackageManager pm, String packageName,
                                        @Nullable List<DependencyPathSegment> path) {
        if (path == null || path.isEmpty()) {
            return packageName;
        }
        if (pm == PackageManager.YarnBerry) {
            return segment(path.get(path.size() - 1)) + "/" + packageName;
        }
        StringBuilder key = new StringBuilder("**/");
        for (DependencyPathSegment segment : path) {
            key.append(segment.getName()).append('/');
        }
        return key.append(packageName).toString();
    }

    // the first key yarn reads as the same glob, since it applies the first resolution whose glob matches
    private static String yarnClassicKeyOf(@Nullable Json resolutions, String key) {
        if (resolutions instanceof Json.JsonObject) {
            for (Json m : ((Json.JsonObject) resolutions).getMembers()) {
                String existing = m instanceof Json.Member ? literalString(((Json.Member) m).getKey()) : null;
                if (existing != null && yarnClassicGlob(existing).equals(yarnClassicGlob(key))) {
                    return existing;
                }
            }
        }
        return key;
    }

    // yarn 1.22.22's resolution-map.js parsePatternInfo: a bare name is **/name
    private static String yarnClassicGlob(String key) {
        Matcher segments = YARN_PATH_SEGMENT.matcher(key);
        int count = 0;
        while (segments.find()) {
            count++;
        }
        return count == 1 ? "**/" + key : key;
    }

    /**
     * The overrides already in place that the package manager applies before a global one where both match, as it
     * reads them; null when there are none or the override is scoped.
     */
    public @Nullable String outranked() {
        if (path != null && !path.isEmpty() || declined() != null) {
            return null;
        }
        if (pm != PackageManager.Pnpm && pm != PackageManager.YarnBerry) {
            return null;
        }
        List<String> keys = new ArrayList<>();
        for (String key : overrideKeys()) {
            int at = key.lastIndexOf('>');
            if (pm == PackageManager.Pnpm ? at > 0 && packageName.equals(nameOf(key.substring(at + 1))) :
                    key.endsWith("/" + packageName) && isParent(key.substring(0, key.length() -
                                                                              packageName.length() - 1))) {
                keys.add(key);
            }
        }
        return keys.isEmpty() ? null : (pm == PackageManager.Pnpm ? "pnpm" : "Yarn") + " applies " +
                                      String.join(", ", keys) + " before a global override of " + packageName;
    }

    private List<String> overrideKeys() {
        List<String> keys = new ArrayList<>();
        if (place == Place.WORKSPACE_MANIFEST) {
            Object overrides = workspace.setting("overrides");
            if (overrides instanceof Map) {
                for (Object key : ((Map<?, ?>) overrides).keySet()) {
                    keys.add(String.valueOf(key));
                }
            }
            return keys;
        }
        Json pnpm = member(root, "pnpm");
        for (Json map : Arrays.asList(member(root, "resolutions"), place == Place.PNPM_FIELD &&
                pnpm instanceof Json.JsonObject ? member((Json.JsonObject) pnpm, "overrides") : null)) {
            if (map instanceof Json.JsonObject) {
                for (Json m : ((Json.JsonObject) map).getMembers()) {
                    String key = m instanceof Json.Member ? literalString(((Json.Member) m).getKey()) : null;
                    if (key != null) {
                        keys.add(key);
                    }
                }
            }
        }
        return keys;
    }

    // a Berry resolution's parent descriptor, not the scope of the package it resolves
    private static boolean isParent(String prefix) {
        return !prefix.isEmpty() && !(prefix.startsWith("@") && prefix.indexOf('/') < 0);
    }

    // a selector's package name, without the range after its name's @
    private static String nameOf(String selector) {
        int at = selector.indexOf('@', selector.startsWith("@") ? 1 : 0);
        return at < 0 ? selector : selector.substring(0, at);
    }

    private static String segment(DependencyPathSegment segment) {
        return segment.getVersion() == null ? segment.getName() : segment.getName() + "@" + segment.getVersion();
    }

    // Berry compares a resolution's parent with the resolved locator, which an npm range never equals
    private static boolean exactVersion(@Nullable String version) {
        String bare = version != null && version.startsWith("npm:") ? version.substring("npm:".length()) : version;
        return bare == null || bare.equals(NodeSemver.valid(bare, NodeSemver.Options.DEFAULT));
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
     * A revision of pnpm-workspace.yaml as a YAML document and as lines, each read once; no settings when absent or
     * unreadable.
     */
    private static final class WorkspaceSettings {
        private final @Nullable String text;
        private final @Nullable Map<String, @Nullable Object> loaded;
        private final @Nullable RuntimeException unreadable;
        private @Nullable WorkspaceLines manifest;
        private @Nullable IllegalArgumentException refused;
        private int manifestRelease;

        WorkspaceSettings(@Nullable String text) {
            this.text = text;
            Map<String, @Nullable Object> document = null;
            RuntimeException error = null;
            try {
                document = WorkspaceManifest.readWorkspaceManifest(text);
            } catch (RuntimeException e) {
                error = e;
            }
            this.loaded = document;
            this.unreadable = error;
        }

        @Nullable
        Map<String, @Nullable Object> settings() {
            return loaded;
        }

        boolean isText(@Nullable String other) {
            return text == null ? other == null : text.equals(other);
        }

        WorkspaceLines manifest(int release) {
            if (manifest == null && refused == null || manifestRelease != release) {
                manifestRelease = release;
                manifest = null;
                refused = null;
                try {
                    manifest = new WorkspaceLines(this, release);
                } catch (IllegalArgumentException e) {
                    refused = e;
                }
            }
            if (refused != null) {
                throw refused;
            }
            return requireNonNull(manifest);
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

    /**
     * pnpm-workspace.yaml as lines, with its top-level entries and those of its {@code overrides}, edited in place
     * where pnpm's workspace manifest writer puts a new key or entry.
     */
    private static final class WorkspaceLines {
        private final @Nullable String text;
        private final WorkspaceManifestWriter writer;
        private final List<String> lines = new ArrayList<>();
        private final List<Entry> entries;

        WorkspaceLines(WorkspaceSettings settings, int release) {
            this.text = settings.text;
            this.writer = new WorkspaceManifestWriter(release);
            if (text == null) {
                entries = emptyList();
                return;
            }
            if (settings.unreadable != null) {
                throw new IllegalArgumentException("pnpm-workspace.yaml is not YAML pnpm reads: " +
                                                   settings.unreadable.getMessage(), settings.unreadable);
            }
            if (text.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("pnpm-workspace.yaml has CR line ends");
            }
            lines.addAll(Arrays.asList(text.split("\n", -1)));
            if (lines.get(lines.size() - 1).isEmpty()) {
                lines.remove(lines.size() - 1);
            }
            entries = scan(0, lines.size(), true);
            if (!keys(entries).equals(keysOf(settings.loaded))) {
                throw new IllegalArgumentException("pnpm-workspace.yaml is not a block mapping of plain keys");
            }
            Entry overrides = entry(entries, "overrides");
            Object loadedOverrides = overrides == null ? null : settings.setting("overrides");
            if (overrides != null && (overrides.inline != null ||
                                      !keys(children(overrides)).equals(keysOf(loadedOverrides)))) {
                throw new IllegalArgumentException("pnpm-workspace.yaml's overrides is not a block mapping");
            }
        }

        String withOverride(String key, String value) {
            String entry = WorkspaceManifestWriter.scalar(key) + ": " + WorkspaceManifestWriter.scalar(value);
            List<String> out = new ArrayList<>(lines);
            Entry overrides = entry(entries, "overrides");
            if (entries.isEmpty()) {
                if (!out.isEmpty() && !out.get(out.size() - 1).trim().isEmpty()) {
                    out.add("");
                }
                out.add("overrides:");
                out.add("  " + entry);
            } else if (overrides == null) {
                out.addAll(topLevelIndex(), topLevelBlock(entry));
            } else {
                List<Entry> children = children(overrides);
                Entry existing = entry(children, key);
                if (existing != null) {
                    if (existing.inline == null || existing.end > existing.keyLine + 1) {
                        throw new IllegalArgumentException("pnpm-workspace.yaml's override of " + key +
                                                           " is not a scalar");
                    }
                    if (value.equals(unquote(existing.inline))) {
                        return requireNonNull(text);
                    }
                    out.set(existing.keyLine, lines.get(existing.keyLine).substring(0, existing.valueStart) +
                                              restyle(existing.inline, value) + existing.comment);
                } else if (children.isEmpty()) {
                    out.add(overrides.keyLine + 1, "  " + entry);
                } else {
                    String indent = lines.get(children.get(0).keyLine).substring(0, children.get(0).indent);
                    int at = writer.insertionIndex(keys(children), key);
                    out.add(at < children.size() ? children.get(at).prefixStart :
                            children.get(children.size() - 1).end, indent + entry);
                }
            }
            return String.join("\n", out) + "\n";
        }

        private int topLevelIndex() {
            int at = writer.insertionIndex(keys(entries), "overrides");
            return at < entries.size() ? entries.get(at).prefixStart : entries.get(entries.size() - 1).end;
        }

        private List<String> topLevelBlock(String entry) {
            int at = writer.insertionIndex(keys(entries), "overrides");
            List<Boolean> blankBefore = new ArrayList<>();
            for (Entry e : entries) {
                blankBefore.add(e.spaceBefore);
            }
            List<String> block = new ArrayList<>();
            if (writer.blankBefore(blankBefore, at)) {
                block.add("");
            }
            block.add("overrides:");
            block.add("  " + entry);
            int trailer = entries.get(entries.size() - 1).end;
            if (at == 0 && writer.blankLineStyle(blankBefore) && !entries.get(0).spaceBefore ||
                at == entries.size() && trailer < lines.size() && !lines.get(trailer).trim().isEmpty()) {
                block.add("");
            }
            return block;
        }

        // the entries of lines [from, to) whose keys sit at the column of the first key there
        private List<Entry> scan(int from, int to, boolean topLevel) {
            List<Entry> found = new ArrayList<>();
            int column = -1;
            int lastContent = from - 1;
            Entry current = null;
            for (int i = from; i < to; i++) {
                String line = lines.get(i);
                String trimmed = line.trim();
                int indent = 0;
                while (indent < line.length() && (line.charAt(indent) == ' ' || line.charAt(indent) == '\t')) {
                    indent++;
                }
                if (trimmed.isEmpty() || trimmed.startsWith("#") && (column < 0 || indent <= column)) {
                    continue;
                }
                if (topLevel && (line.startsWith("---") || line.startsWith("...") || line.startsWith("%"))) {
                    throw new IllegalArgumentException("pnpm-workspace.yaml has document markers");
                }
                if (column < 0) {
                    column = indent;
                }
                boolean sequenceItem = "-".equals(trimmed) || trimmed.startsWith("- ");
                if (indent == column && !sequenceItem) {
                    if (current != null) {
                        current.end = lastContent + 1;
                    }
                    current = Entry.parse(line, i, indent, lastContent + 1, blankBetween(lastContent + 1, i));
                    found.add(current);
                } else if (indent < column || current == null) {
                    throw new IllegalArgumentException("pnpm-workspace.yaml is not a block mapping");
                }
                lastContent = i;
            }
            if (current != null) {
                current.end = lastContent + 1;
            }
            return found;
        }

        private List<Entry> children(Entry parent) {
            return parent.inline != null ? emptyList() : scan(parent.keyLine + 1, parent.end, false);
        }

        private boolean blankBetween(int from, int to) {
            for (int i = from; i < to; i++) {
                if (lines.get(i).trim().isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        private static @Nullable Entry entry(List<Entry> entries, String key) {
            for (Entry e : entries) {
                if (e.key.equals(key)) {
                    return e;
                }
            }
            return null;
        }

        private static List<String> keys(List<Entry> entries) {
            List<String> keys = new ArrayList<>();
            for (Entry e : entries) {
                keys.add(e.key);
            }
            return keys;
        }

        private static List<?> keysOf(@Nullable Object loaded) {
            if (loaded == null) {
                return emptyList();
            }
            if (!(loaded instanceof Map)) {
                throw new IllegalArgumentException("pnpm-workspace.yaml is not a mapping");
            }
            return new ArrayList<>(((Map<?, ?>) loaded).keySet());
        }

        // a scalar keeps its quotes when its value changes
        private static String restyle(String old, String value) {
            if (old.startsWith("'")) {
                return "'" + value.replace("'", "''") + "'";
            }
            return old.startsWith("\"") ? WorkspaceManifestWriter.doubleQuoted(value) :
                    WorkspaceManifestWriter.scalar(value);
        }

        private static String unquote(String scalar) {
            if (scalar.length() >= 2 && scalar.startsWith("'") && scalar.endsWith("'")) {
                return scalar.substring(1, scalar.length() - 1).replace("''", "'");
            }
            if (scalar.length() >= 2 && scalar.startsWith("\"") && scalar.endsWith("\"")) {
                return scalar.substring(1, scalar.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
            }
            return scalar;
        }

        private static final class Entry {
            final String key;
            final int keyLine;
            final int indent;
            final int prefixStart;
            final boolean spaceBefore;
            final int valueStart;
            final @Nullable String inline;
            final String comment;
            int end;

            Entry(String key, int keyLine, int indent, int prefixStart, boolean spaceBefore, int valueStart,
                  @Nullable String inline, String comment) {
                this.key = key;
                this.keyLine = keyLine;
                this.indent = indent;
                this.prefixStart = prefixStart;
                this.spaceBefore = spaceBefore;
                this.valueStart = valueStart;
                this.inline = inline;
                this.comment = comment;
            }

            static Entry parse(String line, int keyLine, int indent, int prefixStart, boolean spaceBefore) {
                char first = line.charAt(indent);
                int colon;
                String key;
                if (first == '\'' || first == '"') {
                    int close = indent + 1;
                    while (close < line.length() && line.charAt(close) != first) {
                        close += first == '"' && line.charAt(close) == '\\' ||
                                 first == '\'' && line.startsWith("''", close) ? 2 : 1;
                    }
                    key = unquote(line.substring(indent, Math.min(close + 1, line.length())));
                    colon = close + 1;
                    while (colon < line.length() && line.charAt(colon) == ' ') {
                        colon++;
                    }
                    if (colon >= line.length() || line.charAt(colon) != ':') {
                        throw new IllegalArgumentException("pnpm-workspace.yaml has a key it cannot read: " + line);
                    }
                } else {
                    colon = line.indexOf(':', indent);
                    while (colon >= 0 && colon + 1 < line.length() && line.charAt(colon + 1) != ' ' &&
                           line.charAt(colon + 1) != '\t') {
                        colon = line.indexOf(':', colon + 1);
                    }
                    if (colon < 0 || first == '?' || first == '{' || first == '[') {
                        throw new IllegalArgumentException("pnpm-workspace.yaml has a key it cannot read: " + line);
                    }
                    key = line.substring(indent, colon).trim();
                }
                int valueStart = colon + 1;
                while (valueStart < line.length() && line.charAt(valueStart) == ' ') {
                    valueStart++;
                }
                String rest = line.substring(valueStart);
                int hash = rest.startsWith("'") || rest.startsWith("\"") ? -1 : rest.indexOf(" #");
                String value = (hash < 0 ? rest : rest.substring(0, hash)).trim();
                if (!value.isEmpty() && "&*|>!{[".indexOf(value.charAt(0)) >= 0) {
                    throw new IllegalArgumentException("pnpm-workspace.yaml has a value it does not edit: " + line);
                }
                return new Entry(key, keyLine, indent, prefixStart, spaceBefore, valueStart,
                        value.isEmpty() ? null : value, hash < 0 ? "" : rest.substring(hash));
            }
        }
    }
}
