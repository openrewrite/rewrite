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

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.Tree;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.*;

/**
 * Shared utilities for npm dependency recipes operating on package.json files
 * and their associated lock files.
 */
@UtilityClass
public class PackageJsonHelper {

    private static final Set<String> LOCK_FILE_NAMES = new LinkedHashSet<>(Arrays.asList(
            "package-lock.json", "pnpm-lock.yaml", "yarn.lock", "bun.lock"));

    /** True when the basename matches one of the recognised npm-ecosystem lock files. */
    public static boolean isLockFile(String basename) {
        return LOCK_FILE_NAMES.contains(basename);
    }

    /** Map a lock file path to the sibling {@code package.json}. */
    public static Path correspondingPackageJsonPath(Path lockFilePath) {
        return lockFilePath.resolveSibling("package.json");
    }

    // --- ctx side-channel for cross-recipe chaining ----------------------

    private static final String LIVE_PACKAGE_JSON_TREES =
            "org.openrewrite.javascript.livePackageJsonTrees";

    @SuppressWarnings("unchecked")
    public static @Nullable SourceFile getLiveTree(ExecutionContext ctx, Path packageJsonPath) {
        Map<Path, SourceFile> map = (Map<Path, SourceFile>) ctx.getMessage(LIVE_PACKAGE_JSON_TREES);
        return map == null ? null : map.get(packageJsonPath);
    }

    public static void putLiveTree(ExecutionContext ctx, Path packageJsonPath, SourceFile tree) {
        Map<Path, SourceFile> map = ctx.computeMessageIfAbsent(LIVE_PACKAGE_JSON_TREES, k -> new HashMap<>());
        map.put(packageJsonPath, tree);
    }

    // --- .npmrc serialization -------------------------------------------

    /**
     * Serialize the marker's npmrc configs into a {@code Map<filename, content>}
     * suitable to seed a temp directory before running the package manager.
     * Returns {@code null} if there are no configs.
     */
    public static @Nullable Map<String, String> serializeConfigFiles(NodeResolutionResult marker) {
        List<Npmrc> configs = marker.getNpmrcConfigs();
        if (configs == null || configs.isEmpty()) {
            return null;
        }
        // Merge configs in scope priority (Global → User → Project; later overrides earlier).
        Map<String, String> merged = new LinkedHashMap<>();
        List<NpmrcScope> order = Arrays.asList(NpmrcScope.Global, NpmrcScope.User, NpmrcScope.Project);
        for (NpmrcScope scope : order) {
            for (Npmrc cfg : configs) {
                if (cfg.getScope() == scope && cfg.getProperties() != null) {
                    merged.putAll(cfg.getProperties());
                }
            }
        }
        if (merged.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : merged.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        Map<String, String> out = new LinkedHashMap<>();
        out.put(".npmrc", sb.toString());
        return out;
    }

    // --- Reparse helpers (preserve identity + markers) ------------------

    public static Json.Document reparseJson(Json.Document original, String newContent) {
        JsonParser parser = new JsonParser();
        Parser.Input input = Parser.Input.fromString(original.getSourcePath(), newContent);
        Optional<SourceFile> parsed = parser.parseInputs(Collections.singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)).findFirst();
        if (parsed.isPresent() && parsed.get() instanceof Json.Document) {
            Json.Document doc = (Json.Document) parsed.get();
            return doc.withId(original.getId()).withMarkers(original.getMarkers());
        }
        return original;
    }

    public static Yaml.Documents reparseYaml(Yaml.Documents original, String newContent) {
        YamlParser parser = new YamlParser();
        Parser.Input input = Parser.Input.fromString(original.getSourcePath(), newContent);
        Optional<SourceFile> parsed = parser.parseInputs(Collections.singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)).findFirst();
        if (parsed.isPresent() && parsed.get() instanceof Yaml.Documents) {
            Yaml.Documents docs = (Yaml.Documents) parsed.get();
            return docs.withId(original.getId()).withMarkers(original.getMarkers());
        }
        return original;
    }

    public static PlainText reparsePlainText(PlainText original, String newContent) {
        return original.withText(newContent);
    }

    /**
     * Reparse a regenerated lock file's content, dispatching by the runtime type
     * of the original SourceFile.
     */
    public static SourceFile reparseLock(SourceFile original, String newContent) {
        if (original instanceof Json.Document) {
            return reparseJson((Json.Document) original, newContent);
        }
        if (original instanceof Yaml.Documents) {
            return reparseYaml((Yaml.Documents) original, newContent);
        }
        if (original instanceof PlainText) {
            return reparsePlainText((PlainText) original, newContent);
        }
        return original;
    }

    // --- Marker refresh ---------------------------------------------------

    /**
     * Re-derive the {@code dependencies}/{@code devDependencies}/etc. lists on
     * the marker by walking the modified document. {@code resolvedDependencies}
     * and other fields carry over unchanged from the existing marker.
     * Returns the source file unchanged when no marker is present.
     */
    public static SourceFile refreshMarker(SourceFile packageJson) {
        if (!(packageJson instanceof Json.Document)) {
            return packageJson;
        }
        NodeResolutionResult existing = packageJson.getMarkers()
                .findFirst(NodeResolutionResult.class).orElse(null);
        if (existing == null) {
            return packageJson;
        }
        Json.Document doc = (Json.Document) packageJson;
        if (!(doc.getValue() instanceof Json.JsonObject)) {
            return packageJson;
        }
        Json.JsonObject root = (Json.JsonObject) doc.getValue();

        List<Dependency> deps = readScope(root, "dependencies", existing.getDependencies());
        List<Dependency> devDeps = readScope(root, "devDependencies", existing.getDevDependencies());
        List<Dependency> peerDeps = readScope(root, "peerDependencies", existing.getPeerDependencies());
        List<Dependency> optDeps = readScope(root, "optionalDependencies", existing.getOptionalDependencies());
        List<Dependency> bundledDeps = readScope(root, "bundledDependencies", existing.getBundledDependencies());

        NodeResolutionResult updated = existing
                .withDependencies(deps)
                .withDevDependencies(devDeps)
                .withPeerDependencies(peerDeps)
                .withOptionalDependencies(optDeps)
                .withBundledDependencies(bundledDeps);
        return doc.withMarkers(doc.getMarkers().setByType(updated));
    }

    private static List<Dependency> readScope(Json.JsonObject root, String scopeName,
                                              List<Dependency> previous) {
        Json.JsonObject scope = findObjectMember(root, scopeName);
        if (scope == null) {
            return Collections.emptyList();
        }
        Map<String, Dependency> previousByName = new LinkedHashMap<>();
        if (previous != null) {
            for (Dependency d : previous) {
                previousByName.put(d.getName(), d);
            }
        }
        List<Dependency> out = new ArrayList<>();
        for (Json m : scope.getMembers()) {
            if (!(m instanceof Json.Member)) continue;
            Json.Member member = (Json.Member) m;
            String key = literalString(member.getKey());
            String value = literalString(member.getValue());
            if (key == null || value == null) continue;
            Dependency prior = previousByName.get(key);
            // Preserve resolved-dep linkage when version constraint is unchanged.
            if (prior != null && value.equals(prior.getVersionConstraint())) {
                out.add(prior);
            } else {
                out.add(new Dependency(key, value, null));
            }
        }
        return out;
    }

    private static Json.@Nullable JsonObject findObjectMember(Json.JsonObject obj, String name) {
        for (Json m : obj.getMembers()) {
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

    private static @Nullable String literalString(@Nullable Object node) {
        if (node instanceof Json.Literal) {
            Object value = ((Json.Literal) node).getValue();
            return value == null ? null : value.toString();
        }
        return null;
    }

    // --- JSON mutation helpers -------------------------------------------

    /**
     * Add {@code name: version} to the given scope object inside {@code doc}.
     * If the scope does not exist it is created. If {@code name} is already
     * present the document is returned unchanged.
     */
    public static Json.Document addDependency(Json.Document doc, String name, String version, String scope) {
        if (!(doc.getValue() instanceof Json.JsonObject)) return doc;
        Json.JsonObject root = (Json.JsonObject) doc.getValue();

        Json.JsonObject existingScope = findObjectMember(root, scope);
        if (existingScope == null) {
            // Detect indent unit from the first root member's prefix (e.g. "\n  " → "  ").
            String outerIndent = detectIndentUnit(root);
            String innerIndent = outerIndent + outerIndent;

            // The new dep member inside the scope gets prefix="\n" + innerIndent.
            // Its after carries the closing-brace whitespace for the scope object: "\n" + outerIndent.
            Json.Member depMember = makeMember(name, makeStringLiteral(version),
                    Space.build("\n" + innerIndent, Collections.emptyList()));
            JsonRightPadded<Json> depRP = JsonRightPadded.build((Json) depMember)
                    .withAfter(Space.build("\n" + outerIndent, Collections.emptyList()));

            Json.JsonObject scopeObj = new Json.JsonObject(
                    Tree.randomId(), Space.SINGLE_SPACE,
                    org.openrewrite.marker.Markers.EMPTY,
                    Collections.singletonList(depRP));

            // The new scope member at root level: prefix will be set by appendMember.
            Json.Member newScopeMember = makeMember(scope, scopeObj, Space.EMPTY);

            return doc.withValue(appendMember(root, newScopeMember));
        }

        // Member already present? Skip (Add semantics).
        for (Json m : existingScope.getMembers()) {
            if (m instanceof Json.Member && name.equals(literalString(((Json.Member) m).getKey()))) {
                return doc;
            }
        }

        Json.Member newDep = makeMember(name, makeStringLiteral(version), Space.EMPTY);
        Json.JsonObject updatedScope = appendMember(existingScope, newDep);
        return doc.withValue(replaceMember(root, scope, updatedScope));
    }

    /**
     * Return the indent unit detected from the first member of {@code obj}.
     * Falls back to two spaces if no members exist or prefix has no newline.
     */
    private static String detectIndentUnit(Json.JsonObject obj) {
        List<JsonRightPadded<Json>> members = obj.getPadding().getMembers();
        if (!members.isEmpty()) {
            String ws = members.get(0).getElement().getPrefix().getWhitespace();
            if (ws != null) {
                int nl = ws.lastIndexOf('\n');
                if (nl >= 0) {
                    return ws.substring(nl + 1);
                }
            }
        }
        return "  ";
    }

    /**
     * Append {@code newMember} to {@code obj}, transferring the trailing after-space
     * from the previous last member to the new last member so that the closing brace
     * keeps its indentation.
     */
    private static Json.JsonObject appendMember(Json.JsonObject obj, Json.Member newMember) {
        List<JsonRightPadded<Json>> members = new ArrayList<>(obj.getPadding().getMembers());
        if (members.isEmpty()) {
            // Empty scope: use the object's own first-member indent (fallback).
            members.add(JsonRightPadded.build((Json) newMember));
        } else {
            int prevLastIdx = members.size() - 1;
            JsonRightPadded<Json> prevLast = members.get(prevLastIdx);

            // The new member's prefix should match the existing members' indent.
            Space copiedPrefix = prevLast.getElement().getPrefix();
            Space trailingAfter = prevLast.getAfter();

            Json.Member prefixedNewMember = newMember.withPrefix(copiedPrefix);
            JsonRightPadded<Json> newRP = JsonRightPadded.build((Json) prefixedNewMember)
                    .withAfter(trailingAfter);

            // Previous last member loses its trailing newline (new member owns closing-brace whitespace).
            members.set(prevLastIdx, prevLast.withAfter(Space.EMPTY));
            members.add(newRP);
        }
        return obj.getPadding().withMembers(members);
    }

    private static Json.JsonObject replaceMember(Json.JsonObject root, String key, JsonValue newValue) {
        List<JsonRightPadded<Json>> members = new ArrayList<>(root.getPadding().getMembers());
        for (int i = 0; i < members.size(); i++) {
            Json elem = members.get(i).getElement();
            if (elem instanceof Json.Member &&
                    key.equals(literalString(((Json.Member) elem).getKey()))) {
                Json.Member oldMember = (Json.Member) elem;
                Json.Member updated = oldMember.withValue(newValue);
                members.set(i, members.get(i).withElement(updated));
                break;
            }
        }
        return root.getPadding().withMembers(members);
    }

    private static Json.Literal makeStringLiteral(String value) {
        return new Json.Literal(
                Tree.randomId(), Space.EMPTY,
                org.openrewrite.marker.Markers.EMPTY,
                "\"" + value + "\"", value);
    }

    /**
     * Create a member with the given prefix on the member itself.
     * If the value is a literal with no prefix whitespace, a single space is
     * added so that the printed output reads {@code "key": "value"}.
     */
    private static Json.Member makeMember(String key, JsonValue value, Space prefix) {
        Json.Literal keyLit = makeStringLiteral(key);
        // Ensure there's a space between ':' and the value (standard JSON formatting).
        JsonValue spacedValue = value;
        if (value instanceof Json.Literal && ((Json.Literal) value).getPrefix() == Space.EMPTY) {
            spacedValue = ((Json.Literal) value).withPrefix(Space.SINGLE_SPACE);
        }
        return new Json.Member(
                Tree.randomId(), prefix,
                org.openrewrite.marker.Markers.EMPTY,
                JsonRightPadded.build(keyLit),
                spacedValue);
    }
}
