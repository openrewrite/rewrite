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
import org.openrewrite.marker.Markup;
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
import java.util.regex.Pattern;

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

    // --- Overlay helpers -------------------------------------------------

    /**
     * Re-derive {@code resolvedDependencies} on the marker by parsing the given
     * lock-file content. Supports npm and Bun. For other PMs, returns the input
     * unchanged. If parsing fails, throws — the caller (typically
     * {@link #editAndRegenerate}) is expected to catch and surface the failure
     * via {@link org.openrewrite.marker.Markup#warn}.
     */
    public static SourceFile overlayResolvedDeps(SourceFile pkg,
                                                 String lockContent,
                                                 NodeResolutionResult.PackageManager pm) {
        NodeResolutionResult marker = pkg.getMarkers()
                .findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null) {
            return pkg;
        }
        if (pm != NodeResolutionResult.PackageManager.Npm
                && pm != NodeResolutionResult.PackageManager.Bun) {
            return pkg;
        }
        String npmV3 = pm == NodeResolutionResult.PackageManager.Bun
                ? BunLockAdapter.toNpmV3(lockContent)
                : lockContent;
        LockFileParser.ParseResult parsed = LockFileParser.parse(npmV3);

        // Relink declared deps.
        List<NodeResolutionResult.Dependency> deps =
                relink(marker.getDependencies(), parsed.getTopLevel());
        List<NodeResolutionResult.Dependency> devDeps =
                relink(marker.getDevDependencies(), parsed.getTopLevel());
        List<NodeResolutionResult.Dependency> peerDeps =
                relink(marker.getPeerDependencies(), parsed.getTopLevel());
        List<NodeResolutionResult.Dependency> optionalDeps =
                relink(marker.getOptionalDependencies(), parsed.getTopLevel());
        List<NodeResolutionResult.Dependency> bundledDeps =
                relink(marker.getBundledDependencies(), parsed.getTopLevel());

        // Relink transitive deps inside each ResolvedDependency.
        List<NodeResolutionResult.ResolvedDependency> all = new ArrayList<>();
        for (NodeResolutionResult.ResolvedDependency r : parsed.getAll()) {
            all.add(r
                    .withDependencies(relink(r.getDependencies(), parsed.getTopLevel()))
                    .withDevDependencies(relink(r.getDevDependencies(), parsed.getTopLevel()))
                    .withPeerDependencies(relink(r.getPeerDependencies(), parsed.getTopLevel()))
                    .withOptionalDependencies(relink(r.getOptionalDependencies(), parsed.getTopLevel())));
        }

        NodeResolutionResult updated = marker
                .withDependencies(deps)
                .withDevDependencies(devDeps)
                .withPeerDependencies(peerDeps)
                .withOptionalDependencies(optionalDeps)
                .withBundledDependencies(bundledDeps)
                .withResolvedDependencies(all);
        return pkg.withMarkers(pkg.getMarkers().setByType(updated));
    }

    private static @Nullable List<NodeResolutionResult.Dependency> relink(
            @Nullable List<NodeResolutionResult.Dependency> deps,
            Map<String, NodeResolutionResult.ResolvedDependency> topLevel) {
        if (deps == null) {
            return null;
        }
        List<NodeResolutionResult.Dependency> out = new ArrayList<>(deps.size());
        for (NodeResolutionResult.Dependency d : deps) {
            out.add(d.withResolved(topLevel.get(d.getName())));
        }
        return out;
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

        // Check if the existing scope is effectively empty (contains only Json.Empty placeholder).
        // The TypeScript JSON parser represents {} as a single Json.Empty member.
        boolean scopeEffectivelyEmpty = existingScope.getMembers().stream()
                .allMatch(m -> m instanceof Json.Empty);

        Json.JsonObject updatedScope;
        if (scopeEffectivelyEmpty) {
            // Replace the Json.Empty placeholder with a properly indented new member.
            String outerIndent = detectIndentUnit(root);
            String innerIndent = outerIndent + outerIndent;
            Json.Member depMember = makeMember(name, makeStringLiteral(version),
                    Space.build("\n" + innerIndent, Collections.emptyList()));
            JsonRightPadded<Json> depRP = JsonRightPadded.build((Json) depMember)
                    .withAfter(Space.build("\n" + outerIndent, Collections.emptyList()));
            updatedScope = existingScope.getPadding().withMembers(Collections.singletonList(depRP));
        } else {
            Json.Member newDep = makeMember(name, makeStringLiteral(version), Space.EMPTY);
            updatedScope = appendMember(existingScope, newDep);
        }
        return doc.withValue(replaceMember(root, scope, updatedScope));
    }

    /**
     * Remove the named dependency from each given scope in {@code doc}.
     * If a scope ends up empty after removal, the scope member itself is dropped.
     * Returns the document unchanged if no matching member was found in any scope.
     */
    public static Json.Document removeDependency(Json.Document doc, String name, Set<String> scopes) {
        if (!(doc.getValue() instanceof Json.JsonObject)) return doc;
        Json.JsonObject root = (Json.JsonObject) doc.getValue();

        List<JsonRightPadded<Json>> rootMembers = new ArrayList<>(root.getPadding().getMembers());
        boolean changed = false;

        for (int i = 0; i < rootMembers.size(); i++) {
            Json elem = rootMembers.get(i).getElement();
            if (!(elem instanceof Json.Member)) continue;
            Json.Member m = (Json.Member) elem;
            String key = literalString(m.getKey());
            if (key == null || !scopes.contains(key)) continue;
            if (!(m.getValue() instanceof Json.JsonObject)) continue;
            Json.JsonObject scopeObj = (Json.JsonObject) m.getValue();

            List<JsonRightPadded<Json>> kept = new ArrayList<>();
            for (JsonRightPadded<Json> rp : scopeObj.getPadding().getMembers()) {
                Json child = rp.getElement();
                if (child instanceof Json.Member &&
                        name.equals(literalString(((Json.Member) child).getKey()))) {
                    continue;
                }
                kept.add(rp);
            }
            if (kept.size() == scopeObj.getPadding().getMembers().size()) {
                continue;  // member not present in this scope
            }
            changed = true;
            if (kept.isEmpty()) {
                rootMembers.remove(i);
                i--;
                continue;
            }
            // Move the trailing whitespace from the original last member to the new last member
            JsonRightPadded<Json> originalLast =
                    scopeObj.getPadding().getMembers().get(scopeObj.getPadding().getMembers().size() - 1);
            kept.set(kept.size() - 1, kept.get(kept.size() - 1).withAfter(originalLast.getAfter()));
            Json.JsonObject newScope = scopeObj.getPadding().withMembers(kept);
            rootMembers.set(i, rootMembers.get(i).withElement(m.withValue(newScope)));
        }
        if (!changed) return doc;

        // If we removed a top-level member, also fix the trailing whitespace of the new last root member.
        if (rootMembers.size() < root.getPadding().getMembers().size() && !rootMembers.isEmpty()) {
            JsonRightPadded<Json> originalLast =
                    root.getPadding().getMembers().get(root.getPadding().getMembers().size() - 1);
            int last = rootMembers.size() - 1;
            rootMembers.set(last, rootMembers.get(last).withAfter(originalLast.getAfter()));
        }
        return doc.withValue(root.getPadding().withMembers(rootMembers));
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
            // RPC-parsed JSON (from the TypeScript PackageJsonParser) stores whitespace on the
            // key literal's prefix rather than on the member itself — fall back to the key prefix
            // when the member prefix is empty.
            Space memberPrefix = prevLast.getElement().getPrefix();
            if (memberPrefix.isEmpty() && prevLast.getElement() instanceof Json.Member) {
                Json.Member prevMember = (Json.Member) prevLast.getElement();
                Space keyPrefix = prevMember.getPadding().getKey().getElement().getPrefix();
                if (!keyPrefix.isEmpty()) {
                    memberPrefix = keyPrefix;
                }
            }
            Space copiedPrefix = memberPrefix;
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

    public static Json.Document upgradeVersion(Json.Document doc, List<MatchedDependency> matched, String newVersion) {
        if (!(doc.getValue() instanceof Json.JsonObject) || matched.isEmpty()) {
            return doc;
        }
        Json.JsonObject root = (Json.JsonObject) doc.getValue();
        Map<String, Set<String>> scopeToNames = new LinkedHashMap<>();
        for (MatchedDependency md : matched) {
            scopeToNames.computeIfAbsent(md.getDependencyScope(), k -> new LinkedHashSet<>())
                    .add(md.getPackageName());
        }

        List<JsonRightPadded<Json>> rootMembers = new ArrayList<>(root.getPadding().getMembers());
        boolean changed = false;

        for (int i = 0; i < rootMembers.size(); i++) {
            Json elem = rootMembers.get(i).getElement();
            if (!(elem instanceof Json.Member)) continue;
            Json.Member m = (Json.Member) elem;
            String scopeKey = literalString(m.getKey());
            Set<String> targetNames = scopeKey == null ? null : scopeToNames.get(scopeKey);
            if (targetNames == null || !(m.getValue() instanceof Json.JsonObject)) continue;
            Json.JsonObject scope = (Json.JsonObject) m.getValue();

            List<JsonRightPadded<Json>> children = new ArrayList<>(scope.getPadding().getMembers());
            boolean scopeChanged = false;
            for (int j = 0; j < children.size(); j++) {
                Json child = children.get(j).getElement();
                if (!(child instanceof Json.Member)) continue;
                Json.Member depMember = (Json.Member) child;
                String name = literalString(depMember.getKey());
                if (name == null || !targetNames.contains(name)) continue;
                if (!(depMember.getValue() instanceof Json.Literal)) continue;
                Json.Literal newLit = makeStringLiteral(newVersion);
                Json.Literal oldLit = (Json.Literal) depMember.getValue();
                newLit = newLit.withPrefix(oldLit.getPrefix());
                children.set(j, children.get(j).withElement(depMember.withValue(newLit)));
                scopeChanged = true;
            }
            if (scopeChanged) {
                rootMembers.set(i, rootMembers.get(i)
                        .withElement(m.withValue(scope.getPadding().withMembers(children))));
                changed = true;
            }
        }
        if (!changed) return doc;
        return doc.withValue(root.getPadding().withMembers(rootMembers));
    }

    public static Json.Document changeDependency(Json.Document doc,
                                                 String oldName, String newName,
                                                 @Nullable String newVersion,
                                                 @Nullable String scope) {
        if (!(doc.getValue() instanceof Json.JsonObject)) return doc;
        Json.JsonObject root = (Json.JsonObject) doc.getValue();

        List<JsonRightPadded<Json>> rootMembers = new ArrayList<>(root.getPadding().getMembers());
        boolean changed = false;

        for (int i = 0; i < rootMembers.size(); i++) {
            Json elem = rootMembers.get(i).getElement();
            if (!(elem instanceof Json.Member)) continue;
            Json.Member scopeMember = (Json.Member) elem;
            String scopeKey = literalString(scopeMember.getKey());
            if (scopeKey == null) continue;
            if (scope != null && !scope.equals(scopeKey)) continue;
            if (!isDeclaredScope(scopeKey)) continue;
            if (!(scopeMember.getValue() instanceof Json.JsonObject)) continue;
            Json.JsonObject scopeObj = (Json.JsonObject) scopeMember.getValue();

            List<JsonRightPadded<Json>> children = new ArrayList<>(scopeObj.getPadding().getMembers());
            boolean scopeChanged = false;
            for (int j = 0; j < children.size(); j++) {
                Json child = children.get(j).getElement();
                if (!(child instanceof Json.Member)) continue;
                Json.Member depMember = (Json.Member) child;
                if (!oldName.equals(literalString(depMember.getKey()))) continue;

                Json.Literal oldKeyLit = (Json.Literal) depMember.getKey();
                Json.Literal newKeyLit = makeStringLiteral(newName).withPrefix(oldKeyLit.getPrefix());

                JsonValue newValue = depMember.getValue();
                if (newVersion != null && depMember.getValue() instanceof Json.Literal) {
                    Json.Literal oldValLit = (Json.Literal) depMember.getValue();
                    newValue = makeStringLiteral(newVersion).withPrefix(oldValLit.getPrefix());
                }
                Json.Member updated = depMember.withKey(newKeyLit).withValue(newValue);
                children.set(j, children.get(j).withElement(updated));
                scopeChanged = true;
            }
            if (scopeChanged) {
                rootMembers.set(i, rootMembers.get(i)
                        .withElement(scopeMember.withValue(scopeObj.getPadding().withMembers(children))));
                changed = true;
            }
        }
        return changed ? doc.withValue(root.getPadding().withMembers(rootMembers)) : doc;
    }

    /**
     * Apply a transitive-dependency override to {@code doc} for the given package manager.
     * Delegates to {@link PackageJsonOverrides#applyOverride}.
     */
    public static Json.Document upgradeTransitive(Json.Document doc,
                                                   NodeResolutionResult.PackageManager pm,
                                                   String name, String newVersion,
                                                   @Nullable List<DependencyPathSegment> path) {
        return PackageJsonOverrides.applyOverride(doc, pm, name, newVersion, path);
    }

    /**
     * Compile a glob pattern (using {@code *} and {@code ?} wildcards) into a {@link Pattern}.
     * Special regex characters in the glob are escaped before compiling.
     */
    public static Pattern compileGlobPattern(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '+':
                case '^':
                case '$':
                case '|':
                case '\\':
                    regex.append("\\").append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
    }

    private static boolean isDeclaredScope(String name) {
        return "dependencies".equals(name)
                || "devDependencies".equals(name)
                || "peerDependencies".equals(name)
                || "optionalDependencies".equals(name)
                || "bundledDependencies".equals(name);
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
        if (value instanceof Json.Literal && ((Json.Literal) value).getPrefix().getWhitespace().isEmpty()) {
            spacedValue = ((Json.Literal) value).withPrefix(Space.SINGLE_SPACE);
        }
        return new Json.Member(
                Tree.randomId(), prefix,
                org.openrewrite.marker.Markers.EMPTY,
                JsonRightPadded.build(keyLit),
                spacedValue);
    }

    // --- Edit-and-regenerate orchestration ---------------------------------

    @lombok.Value
    public static class EditAndRegenerateResult {
        @Nullable SourceFile modifiedPackageJson;
        LockFileRegeneration.@Nullable Result regenResult;

        public boolean isChanged() { return modifiedPackageJson != null; }

        public static EditAndRegenerateResult unchanged() {
            return new EditAndRegenerateResult(null, null);
        }

        public static EditAndRegenerateResult changed(SourceFile modified,
                                                      LockFileRegeneration.@Nullable Result regen) {
            return new EditAndRegenerateResult(modified, regen);
        }
    }

    /**
     * Apply a recipe-specific edit to a package.json, refresh its declared-deps
     * marker, and (when the marker carries a {@link NodeResolutionResult#getPackageManager()
     * package manager} and a lock was captured at scan time) regenerate the lock
     * file content via {@link LockFileRegeneration}.
     */
    public static EditAndRegenerateResult editAndRegenerate(
            SourceFile packageJson,
            java.util.function.Function<Json.Document, Json.Document> editFn,
            @Nullable String capturedLockContent,
            @Nullable Map<String, String> configFiles) {
        if (!(packageJson instanceof Json.Document)) {
            return EditAndRegenerateResult.unchanged();
        }
        Json.Document before = (Json.Document) packageJson;
        Json.Document after = editFn.apply(before);
        if (after == before) {
            return EditAndRegenerateResult.unchanged();
        }
        SourceFile refreshed = refreshMarker(after);
        LockFileRegeneration.Result regen = capturedLockContent == null ? null
                : regenerateLockContent(refreshed, capturedLockContent, configFiles);
        SourceFile finalSource = refreshed;
        if (regen != null && regen.isSuccess()) {
            NodeResolutionResult marker = refreshed.getMarkers()
                    .findFirst(NodeResolutionResult.class).orElse(null);
            if (marker != null
                    && (marker.getPackageManager() == NodeResolutionResult.PackageManager.Npm
                        || marker.getPackageManager() == NodeResolutionResult.PackageManager.Bun)) {
                try {
                    finalSource = overlayResolvedDeps(refreshed,
                            regen.getLockFileContent(), marker.getPackageManager());
                } catch (RuntimeException e) {
                    finalSource = Markup.warn(refreshed,
                            new RuntimeException("lock parse failed: " + e.getMessage()));
                }
            }
        }
        return EditAndRegenerateResult.changed(finalSource, regen);
    }

    public static LockFileRegeneration.@Nullable Result regenerateLockContent(
            SourceFile packageJson,
            @Nullable String capturedLockContent,
            @Nullable Map<String, String> configFiles) {
        NodeResolutionResult marker = packageJson.getMarkers()
                .findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null || marker.getPackageManager() == null) {
            return null;
        }
        LockFileRegeneration regen = LockFileRegeneration.forPackageManager(marker.getPackageManager());
        if (regen == null) {
            return null;
        }
        return regen.regenerate(packageJson.printAll(), capturedLockContent, configFiles);
    }
}
