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
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.berry.Resolution;
import org.openrewrite.javascript.internal.lock.bun.OverrideMap;
import org.openrewrite.javascript.internal.lock.pnpm.ParseOverrides;
import org.openrewrite.javascript.internal.lock.pnpm.WorkspaceManifest;
import org.openrewrite.javascript.internal.lock.yarnclassic.ResolutionMap;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * An override of a transitive dependency written where the workspace root's package manager reads it, in its own key
 * syntax: npm's and Bun's {@code overrides}, Yarn's {@code resolutions}, pnpm's by the release the project names.
 */
public final class PackageJsonOverrides {

    public static final String WORKSPACE_MANIFEST = "pnpm-workspace.yaml";

    private final PackageManager pm;
    private final String packageName;
    private final String newVersion;
    private final @Nullable List<DependencyPathSegment> path;
    private final Json.Document root;
    private final String rootText;
    private final WorkspaceSettings workspace;
    private final PnpmRelease release;
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
        this.rootText = PackageJsonHelper.printed(root);
        this.release = PnpmRelease.of(pm == PackageManager.Pnpm ? rootText : "{}", workspace.settings());
        this.place = place(pm, root, rootText, workspace, release.getMajor());
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
        return place == other.place && getRelease() == other.getRelease() &&
               Objects.equals(declined(), other.declined());
    }

    @Override
    public String toString() {
        String why = declined();
        if (why != null) {
            return "declined (" + why + ")";
        }
        String where = place == Place.WORKSPACE_MANIFEST ? WORKSPACE_MANIFEST : place == Place.PNPM_FIELD ?
                "pnpm.overrides" : place == Place.RESOLUTIONS ? "resolutions" : "overrides";
        return pm == PackageManager.Pnpm ? where + " for " + release : where;
    }

    /**
     * Where the root's package manager reads the override.
     */
    public Place getPlace() {
        return place;
    }

    int getRelease() {
        return pm == PackageManager.Pnpm ? release.getMajor() : -1;
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

    private static Place place(PackageManager pm, Json.Document root, String rootText, WorkspaceSettings workspace,
                               int release) {
        switch (pm) {
            case Bun:
                return "resolutions".equals(OverrideMap.readsFrom(rootText)) ?
                        Place.RESOLUTIONS : Place.OVERRIDES;
            case YarnClassic:
            case YarnBerry:
                return Place.RESOLUTIONS;
            case Pnpm:
                // pnpm 9 reads package.json's overrides only, 10 instead of pnpm-workspace.yaml's when it holds any
                // (override-pnpm9-reads-pnpm-field, override-pnpm10-recipe-beside-resolutions)
                if (release == 9 || release == 10 && (holdsOverrides(root) ||
                                                      !workspace.holdsOverrides() && hasPnpmField(root))) {
                    // {...resolutions, ...pnpm?.overrides}; a null pnpm field: override-pnpm10-recipe-null-pnpm-*
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
            String declined = release.declined();
            if (declined != null) {
                return "the override is not written: " + declined;
            }
            String selector = pnpmSelector(packageName, path);
            String rejected = ParseOverrides.rejects(selector);
            if (rejected != null) {
                return "pnpm rejects the override selector " + selector + ": " + rejected;
            }
        } else if (pm == PackageManager.Bun && depth > 0) {
            return "bun does not support nested overrides";
        } else if (pm == PackageManager.YarnBerry && Resolution.rejects(berryKey(packageName, path)) != null) {
            return "Yarn drops the resolution " + berryKey(packageName, path) + " (YN0057): " +
                   Resolution.rejects(berryKey(packageName, path));
        } else if (pm == PackageManager.YarnBerry && path != null && depth == 1 && // override-key-ranged-parent
                   !exactVersion(path.get(0).getVersion())) {
            return "Yarn matches a resolution's parent by its exact version only, not " + segment(path.get(0));
        } else if (pm == PackageManager.YarnClassic && path != null) { // override-key-versioned-parent
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
                    // npm reads an object's "." as its own override, the other keys below it (object-value-pinned)
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
        return applyTo(NodeCatalogs.Pnpm.parse(workspace.text, rootText)).print();
    }

    /**
     * {@code workspace}, a revision of pnpm-workspace.yaml, with the override as {@link #applyToWorkspaceManifest()}
     * writes it, by the one editor of that file.
     */
    public NodeCatalogs applyTo(NodeCatalogs.Pnpm workspace) {
        return workspace.forRoot(rootText).withOverride(pnpmKey(packageName, path), newVersion);
    }

    // the selector of the whole path, which pnpm reads with one parent only
    private static String pnpmSelector(String packageName, @Nullable List<DependencyPathSegment> path) {
        StringBuilder selector = new StringBuilder();
        for (DependencyPathSegment segment : path == null ? Collections.<DependencyPathSegment>emptyList() : path) {
            selector.append(segment(segment)).append('>');
        }
        return selector.append(packageName).toString();
    }

    // the resolution key of the whole path, which Yarn reads with one parent only
    private static String berryKey(String packageName, @Nullable List<DependencyPathSegment> path) {
        StringBuilder key = new StringBuilder();
        for (DependencyPathSegment segment : path == null ? Collections.<DependencyPathSegment>emptyList() : path) {
            key.append(segment(segment)).append('/');
        }
        return key.append(packageName).toString();
    }

    // pnpm reads one parent, a>b (override-recipe-path); Yarn classic matches from the root (parent-not-root)
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
            String glob = ResolutionMap.globPattern(key);
            for (Json m : ((Json.JsonObject) resolutions).getMembers()) {
                String existing = m instanceof Json.Member ? literalString(((Json.Member) m).getKey()) : null;
                if (existing != null && glob != null && glob.equals(ResolutionMap.globPattern(existing))) {
                    return existing;
                }
            }
        }
        return key;
    }

    /**
     * The overrides already in place that the package manager applies before a global one where both match, as it
     * reads them; null when there are none or the override is scoped.
     */
    public @Nullable String outranked() {
        if (path != null && !path.isEmpty() || declined() != null) {
            return null;
        }
        if (pm == PackageManager.YarnClassic) {
            List<String> before = ResolutionMap.triedBefore(parsedResolutions(),
                    yarnClassicKeyOf(member(root, "resolutions"), packageName));
            return before.isEmpty() ? null : "yarn applies " + String.join(", ", before) +
                                             " before a global override of " + packageName;
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

    // the root's resolutions as JSON.parse reads them
    @SuppressWarnings("unchecked")
    private Map<String, @Nullable Object> parsedResolutions() {
        Object parsed;
        try {
            parsed = EcmaScript.jsonParse(rootText);
        } catch (RuntimeException e) {
            return emptyMap();
        }
        Object resolutions = parsed instanceof Map ? ((Map<String, ?>) parsed).get("resolutions") : null;
        return resolutions instanceof Map ? (Map<String, @Nullable Object>) resolutions : emptyMap();
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
     * A revision of pnpm-workspace.yaml as pnpm reads it, once; no settings when absent or unreadable.
     */
    private static final class WorkspaceSettings {
        private final @Nullable String text;
        private final @Nullable Map<String, @Nullable Object> loaded;

        WorkspaceSettings(@Nullable String text) {
            this.text = text;
            Map<String, @Nullable Object> document = null;
            try {
                document = WorkspaceManifest.readWorkspaceManifest(text);
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
