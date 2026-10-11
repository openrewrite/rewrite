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

import lombok.Value;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

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

    /**
     * A workspace root's member {@code package.json} paths, from its
     * {@code workspacePackagePaths}; empty when not a workspace root.
     */
    public static List<Path> workspaceMemberPaths(SourceFile rootPackageJson) {
        NodeResolutionResult marker = rootPackageJson.getMarkers()
                .findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null || marker.getWorkspacePackagePaths() == null) {
            return emptyList();
        }
        Path base = rootPackageJson.getSourcePath().getParent();
        List<Path> members = new ArrayList<>();
        for (String rel : marker.getWorkspacePackagePaths()) {
            members.add((base == null ? Paths.get(rel) : base.resolve(rel)).normalize());
        }
        return members;
    }

    /**
     * A recipe's edits of the manifests and the package-manager files beside them, such as pnpm-workspace.yaml.
     */
    @FunctionalInterface
    public interface FileEditor {

        /**
         * {@code file}, as the recipes before this one in the cycle left it, with this recipe's edits.
         */
        SourceFile edit(ExecutionContext ctx, SourceFile file);
    }

    private static final String FILE_EDITORS = "org.openrewrite.javascript.packageManagerFileEditors";

    private static final class FileEditors {
        @Nullable
        Object cycle;

        final List<FileEditor> editors = new ArrayList<>();
    }

    /**
     * Add {@code editor} to this cycle's editors of manifests and package-manager files, after those added before it.
     */
    public static void addFileEditor(ExecutionContext ctx, FileEditor editor) {
        List<FileEditor> editors = fileEditors(ctx);
        if (!editors.contains(editor)) {
            editors.add(editor);
        }
    }

    /**
     * {@code scanned} as the editors added this cycle before {@code editor} leave it, in the order they were added.
     */
    public static SourceFile editedBefore(ExecutionContext ctx, FileEditor editor, SourceFile scanned) {
        SourceFile file = scanned;
        for (FileEditor earlier : new ArrayList<>(fileEditors(ctx))) {
            if (earlier == editor) {
                break;
            }
            file = earlier.edit(ctx, file);
        }
        return file;
    }

    private static List<FileEditor> fileEditors(ExecutionContext ctx) {
        FileEditors editors = ctx.computeMessageIfAbsent(FILE_EDITORS, k -> new FileEditors());
        Object cycle = ctx.getMessage(ExecutionContext.CURRENT_CYCLE);
        if (editors.cycle != cycle) {
            editors.cycle = cycle;
            editors.editors.clear();
        }
        return editors.editors;
    }

    /**
     * The file's text without its markers, which an earlier recipe's warning would otherwise print into.
     */
    public static String printed(SourceFile file) {
        return file.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
    }

    public static Json.Document reparseJson(Json.Document original, String newContent) {
        JsonParser parser = new JsonParser();
        Parser.Input input = Parser.Input.fromString(original.getSourcePath(), newContent);
        Optional<SourceFile> parsed = parser.parseInputs(singletonList(input), null,
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
        Optional<SourceFile> parsed = parser.parseInputs(singletonList(input), null,
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

    /** Reparse regenerated lock content or an edited package-manager file, by the original's runtime type. */
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

    /**
     * Re-derive the declared-dependency lists on the marker from the modified document;
     * {@code resolvedDependencies} and other fields carry over unchanged.
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
            return emptyList();
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

    /**
     * Re-derive {@code resolvedDependencies} on the marker from {@code lockContent}; throws on a parse failure, which
     * {@link #editAndRegenerate} surfaces as a warning.
     */
    public static SourceFile overlayResolvedDeps(SourceFile pkg,
                                                 String lockContent,
                                                 NodeResolutionResult.PackageManager pm) {
        NodeResolutionResult marker = pkg.getMarkers()
                .findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null) {
            return pkg;
        }
        String npmV3;
        switch (pm) {
            case Npm:
                npmV3 = lockContent;
                break;
            case Bun:
                npmV3 = BunLockAdapter.toNpmV3(lockContent);
                break;
            case YarnClassic:
                npmV3 = YarnClassicLockAdapter.toNpmV3(lockContent);
                break;
            case YarnBerry:
                npmV3 = YarnBerryLockAdapter.toNpmV3(lockContent);
                break;
            case Pnpm:
                npmV3 = PnpmLockAdapter.toNpmV3(lockContent);
                break;
            default:
                return pkg; // unsupported PM
        }
        LockFileParser.ParseResult parsed = LockFileParser.parse(npmV3);

        // Relink declared deps into the parsed graph; the graph itself is
        // already fully linked (transitive deps included) by LockFileParser.
        NodeResolutionResult updated = marker
                .withDependencies(relink(marker.getDependencies(), parsed))
                .withDevDependencies(relink(marker.getDevDependencies(), parsed))
                .withPeerDependencies(relink(marker.getPeerDependencies(), parsed))
                .withOptionalDependencies(relink(marker.getOptionalDependencies(), parsed))
                .withBundledDependencies(relink(marker.getBundledDependencies(), parsed))
                .withResolvedDependencies(parsed.getAll());
        return pkg.withMarkers(pkg.getMarkers().setByType(updated));
    }

    private static boolean isOverlaySupported(NodeResolutionResult.PackageManager pm) {
        switch (pm) {
            case Npm:
            case Bun:
            case YarnClassic:
            case YarnBerry:
            case Pnpm:
                return true;
            default:
                return false;
        }
    }

    private static @Nullable List<NodeResolutionResult.Dependency> relink(
            @Nullable List<NodeResolutionResult.Dependency> deps,
            LockFileParser.ParseResult parsed) {
        if (deps == null) {
            return null;
        }
        List<NodeResolutionResult.Dependency> out = new ArrayList<>(deps.size());
        for (NodeResolutionResult.Dependency d : deps) {
            out.add(d.withResolved(parsed.resolve(d.getName(), d.getVersionConstraint())));
        }
        return out;
    }

    /**
     * Add {@code name: version} to {@code scope} in {@code doc}, creating the scope if absent;
     * a no-op when {@code name} is already present.
     */
    public static Json.Document addDependency(Json.Document doc, String name, String version, String scope) {
        if (!(doc.getValue() instanceof Json.JsonObject)) return doc;
        Json.JsonObject root = (Json.JsonObject) doc.getValue();

        Json.JsonObject existingScope = findObjectMember(root, scope);
        if (existingScope == null) {
            String outerIndent = detectIndentUnit(root);
            String innerIndent = outerIndent + outerIndent;

            // New member owns its leading indent; its after carries the scope's closing-brace whitespace.
            Json.Member depMember = makeMember(name, makeStringLiteral(version),
                    Space.build("\n" + innerIndent, emptyList()));
            JsonRightPadded<Json> depRP = JsonRightPadded.build((Json) depMember)
                    .withAfter(Space.build("\n" + outerIndent, emptyList()));

            Json.JsonObject scopeObj = new Json.JsonObject(
                    Tree.randomId(), Space.SINGLE_SPACE,
                    org.openrewrite.marker.Markers.EMPTY,
                    singletonList(depRP));

            // The new scope member at root level: prefix will be set by appendMember.
            Json.Member newScopeMember = makeMember(scope, scopeObj, Space.EMPTY);

            return doc.withValue(appendMember(root, newScopeMember));
        }

        for (Json m : existingScope.getMembers()) {
            if (m instanceof Json.Member && name.equals(literalString(((Json.Member) m).getKey()))) {
                return doc;
            }
        }

        // The TypeScript JSON parser represents {} as a single Json.Empty member.
        boolean scopeEffectivelyEmpty = existingScope.getMembers().stream()
                .allMatch(m -> m instanceof Json.Empty);

        Json.JsonObject updatedScope;
        if (scopeEffectivelyEmpty) {
            String outerIndent = detectIndentUnit(root);
            String innerIndent = outerIndent + outerIndent;
            Json.Member depMember = makeMember(name, makeStringLiteral(version),
                    Space.build("\n" + innerIndent, emptyList()));
            JsonRightPadded<Json> depRP = JsonRightPadded.build((Json) depMember)
                    .withAfter(Space.build("\n" + outerIndent, emptyList()));
            updatedScope = existingScope.getPadding().withMembers(singletonList(depRP));
        } else {
            Json.Member newDep = makeMember(name, makeStringLiteral(version), Space.EMPTY);
            updatedScope = appendMember(existingScope, newDep);
        }
        return doc.withValue(replaceMember(root, scope, updatedScope));
    }

    /**
     * {@code doc} with the members of its object {@code scope} in {@code order} of their keys, as a package manager
     * writes a dependency field; each takes the layout of the member that stood in its place.
     */
    public static Json.Document sortMembers(Json.Document doc, String scope, Comparator<String> order) {
        if (!(doc.getValue() instanceof Json.JsonObject)) return doc;
        Json.JsonObject root = (Json.JsonObject) doc.getValue();
        Json.JsonObject object = findObjectMember(root, scope);
        if (object == null) return doc;
        List<JsonRightPadded<Json>> members = object.getPadding().getMembers();
        List<Json.Member> sorted = new ArrayList<>();
        for (JsonRightPadded<Json> member : members) {
            if (!(member.getElement() instanceof Json.Member)) return doc;
            sorted.add((Json.Member) member.getElement());
        }
        sorted.sort(Comparator.comparing(member -> String.valueOf(literalString(member.getKey())), order));
        List<JsonRightPadded<Json>> out = new ArrayList<>();
        boolean changed = false;
        for (int i = 0; i < members.size(); i++) {
            Json.Member at = (Json.Member) members.get(i).getElement();
            changed |= sorted.get(i) != at;
            out.add(members.get(i).withElement(sorted.get(i).withPrefix(at.getPrefix())));
        }
        return changed ? doc.withValue(replaceMember(root, scope, object.getPadding().withMembers(out))) : doc;
    }

    /**
     * Set {@code outerKey.innerKey[entryKey] = entryValue}, creating either object as needed; a no-op when the entry
     * already holds {@code entryValue}.
     */
    public static Json.Document setNestedEntry(Json.Document doc, String outerKey, String innerKey,
                                               String entryKey, String entryValue) {
        if (!(doc.getValue() instanceof Json.JsonObject)) return doc;
        Json.JsonObject root = (Json.JsonObject) doc.getValue();
        String indent = detectIndentUnit(root);

        Json.JsonObject outer = findObjectMember(root, outerKey);
        if (outer == null) {
            Json.JsonObject inner = newObjectHolding(makeMember(entryKey, makeStringLiteral(entryValue),
                    Space.build("\n" + indent + indent + indent, emptyList())), indent + indent);
            Json.JsonObject outerObj = newObjectHolding(makeMember(innerKey, inner,
                    Space.build("\n" + indent + indent, emptyList())), indent);
            return doc.withValue(appendMember(root, makeMember(outerKey, outerObj, Space.EMPTY)));
        }

        Json.JsonObject inner = findObjectMember(outer, innerKey);
        if (inner == null) {
            Json.JsonObject innerObj = newObjectHolding(makeMember(entryKey, makeStringLiteral(entryValue),
                    Space.build("\n" + indent + indent + indent, emptyList())), indent + indent);
            // {} parses as a single Json.Empty member, which an append would print as {,}
            Json.JsonObject updatedOuter = outer.getMembers().stream().allMatch(m -> m instanceof Json.Empty) ?
                    newObjectHolding(makeMember(innerKey, innerObj, Space.build("\n" + indent + indent, emptyList())),
                            indent).withPrefix(outer.getPrefix()) :
                    appendMember(outer, makeMember(innerKey, innerObj, Space.EMPTY));
            return doc.withValue(replaceMember(root, outerKey, updatedOuter));
        }

        for (Json m : inner.getMembers()) {
            if (!(m instanceof Json.Member)) continue;
            Json.Member member = (Json.Member) m;
            if (!entryKey.equals(literalString(member.getKey()))) continue;
            if (entryValue.equals(literalString(member.getValue()))) {
                return doc;
            }
            Json.Literal newLit = makeStringLiteral(entryValue).withPrefix(member.getValue().getPrefix());
            return doc.withValue(replaceMember(root, outerKey,
                    replaceMember(outer, innerKey, replaceMember(inner, entryKey, newLit))));
        }

        Json.JsonObject updatedInner;
        if (inner.getMembers().stream().allMatch(m -> m instanceof Json.Empty)) {
            updatedInner = newObjectHolding(makeMember(entryKey, makeStringLiteral(entryValue),
                    Space.build("\n" + indent + indent + indent, emptyList())), indent + indent)
                    .withPrefix(inner.getPrefix());
        } else {
            updatedInner = appendMember(inner, makeMember(entryKey, makeStringLiteral(entryValue), Space.EMPTY));
        }
        return doc.withValue(replaceMember(root, outerKey, replaceMember(outer, innerKey, updatedInner)));
    }

    private static Json.JsonObject newObjectHolding(Json.Member member, String closingIndent) {
        return new Json.JsonObject(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                singletonList(JsonRightPadded.build((Json) member)
                        .withAfter(Space.build("\n" + closingIndent, emptyList()))));
    }

    /**
     * Remove {@code name} from each of {@code scopes} in {@code doc}, dropping a scope that ends up
     * empty; a no-op when no matching member is found.
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
                continue;
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
     * {@code doc} without each of its top-level {@code keys} whose value is an empty object.
     */
    public static Json.Document withoutEmptyObjects(Json.Document doc, Collection<String> keys) {
        if (!(doc.getValue() instanceof Json.JsonObject)) return doc;
        Json.JsonObject root = (Json.JsonObject) doc.getValue();
        List<JsonRightPadded<Json>> members = root.getPadding().getMembers();
        List<JsonRightPadded<Json>> kept = new ArrayList<>();
        for (JsonRightPadded<Json> rp : members) {
            Json elem = rp.getElement();
            if (elem instanceof Json.Member && keys.contains(literalString(((Json.Member) elem).getKey())) &&
                ((Json.Member) elem).getValue() instanceof Json.JsonObject && ((Json.JsonObject) ((Json.Member) elem)
                    .getValue()).getMembers().stream().allMatch(m -> m instanceof Json.Empty)) {
                continue;
            }
            kept.add(rp);
        }
        if (kept.size() == members.size() || kept.isEmpty()) return doc;
        kept.set(kept.size() - 1, kept.get(kept.size() - 1).withAfter(members.get(members.size() - 1).getAfter()));
        return doc.withValue(root.getPadding().withMembers(kept));
    }

    /** The indent unit of {@code doc}'s top-level members, or two spaces when none can be detected. */
    public static String indentUnit(Json.Document doc) {
        return doc.getValue() instanceof Json.JsonObject ? detectIndentUnit((Json.JsonObject) doc.getValue()) : "  ";
    }

    /** {@code doc} with the top-level member {@code key} holding {@code value}, appended when absent. */
    public static Json.Document setMember(Json.Document doc, String key, JsonValue value) {
        if (!(doc.getValue() instanceof Json.JsonObject)) return doc;
        Json.JsonObject root = (Json.JsonObject) doc.getValue();
        for (Json m : root.getMembers()) {
            if (m instanceof Json.Member && key.equals(literalString(((Json.Member) m).getKey()))) {
                return doc.withValue(replaceMember(root, key, value));
            }
        }
        return doc.withValue(appendMember(root, makeMember(key, value, Space.EMPTY)));
    }

    /** The indent unit from {@code obj}'s first member, or two spaces when none can be detected. */
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

    /** Append {@code newMember} to {@code obj}, moving the trailing space so the closing brace keeps its indent. */
    private static Json.JsonObject appendMember(Json.JsonObject obj, Json.Member newMember) {
        List<JsonRightPadded<Json>> members = new ArrayList<>(obj.getPadding().getMembers());
        if (members.isEmpty()) {
            // Empty scope: use the object's own first-member indent (fallback).
            members.add(JsonRightPadded.build((Json) newMember));
        } else {
            int prevLastIdx = members.size() - 1;
            JsonRightPadded<Json> prevLast = members.get(prevLastIdx);

            // RPC-parsed JSON (the TypeScript PackageJsonParser) stores whitespace on the key literal's
            // prefix, not the member — fall back to the key prefix when the member's is empty.
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
     * Whether {@code doc}'s root holds {@code key} with a value that is not an object.
     */
    public static boolean isNonObjectMember(Json.Document doc, String key) {
        if (!(doc.getValue() instanceof Json.JsonObject)) {
            return false;
        }
        for (Json m : ((Json.JsonObject) doc.getValue()).getMembers()) {
            if (m instanceof Json.Member && key.equals(literalString(((Json.Member) m).getKey()))) {
                return !(((Json.Member) m).getValue() instanceof Json.JsonObject);
            }
        }
        return false;
    }

    /**
     * The dependency maps a package.json declares, by scope then name, read from the document: the values as written.
     */
    public static Map<String, Map<String, String>> declaredDependencies(SourceFile packageJson) {
        Map<String, Map<String, String>> declared = new LinkedHashMap<>();
        if (!(packageJson instanceof Json.Document) || !(((Json.Document) packageJson).getValue() instanceof
                Json.JsonObject)) {
            return declared;
        }
        Json.JsonObject root = (Json.JsonObject) ((Json.Document) packageJson).getValue();
        for (String scope : Arrays.asList("dependencies", "devDependencies", "peerDependencies",
                "optionalDependencies")) {
            Json.JsonObject members = findObjectMember(root, scope);
            if (members == null) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (Json m : members.getMembers()) {
                if (m instanceof Json.Member) {
                    String name = literalString(((Json.Member) m).getKey());
                    Object value = ((Json.Member) m).getValue() instanceof Json.Literal ?
                            ((Json.Literal) ((Json.Member) m).getValue()).getValue() : null;
                    if (name != null && value instanceof String) {
                        values.put(name, (String) value);
                    }
                }
            }
            declared.put(scope, values);
        }
        return declared;
    }

    /**
     * The {@code name} a package.json declares, read from the document; null when it holds no string there.
     */
    public static @Nullable String declaredName(SourceFile packageJson) {
        if (!(packageJson instanceof Json.Document) || !(((Json.Document) packageJson).getValue() instanceof
                Json.JsonObject)) {
            return null;
        }
        for (Json m : ((Json.JsonObject) ((Json.Document) packageJson).getValue()).getMembers()) {
            if (m instanceof Json.Member && "name".equals(literalString(((Json.Member) m).getKey()))) {
                return literalString(((Json.Member) m).getValue());
            }
        }
        return null;
    }

    /**
     * {@code doc} with the string member {@code key} of the object {@code path} leads to set to {@code value}, in place
     * or appended; unchanged when the path leads to no object holding members.
     */
    public static Json.Document withStringMember(Json.Document doc, List<String> path, String key, String value) {
        if (!(doc.getValue() instanceof Json.JsonObject)) {
            return doc;
        }
        Json.JsonObject updated = withStringMember((Json.JsonObject) doc.getValue(), path, 0, key, value);
        return updated == null ? doc : doc.withValue(updated);
    }

    private static Json.@Nullable JsonObject withStringMember(Json.JsonObject obj, List<String> path, int depth,
                                                              String key, String value) {
        if (depth < path.size()) {
            Json.JsonObject child = findObjectMember(obj, path.get(depth));
            Json.JsonObject updated = child == null ? null : withStringMember(child, path, depth + 1, key, value);
            return updated == null ? null : replaceMember(obj, path.get(depth), updated);
        }
        for (Json m : obj.getMembers()) {
            if (m instanceof Json.Member && key.equals(literalString(((Json.Member) m).getKey()))) {
                JsonValue old = ((Json.Member) m).getValue();
                return replaceMember(obj, key, makeStringLiteral(value).withPrefix(old.getPrefix()));
            }
        }
        if (obj.getMembers().stream().allMatch(m -> m instanceof Json.Empty)) {
            return null;
        }
        return appendMember(obj, makeMember(key, makeStringLiteral(value), Space.EMPTY));
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

    /** A member with {@code prefix}; a bare literal value gets a leading space so it prints {@code "key": "value"}. */
    private static Json.Member makeMember(String key, JsonValue value, Space prefix) {
        Json.Literal keyLit = makeStringLiteral(key);
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

    @Value
    public static class EditAndRegenerateResult {
        /**
         * The package.json as the edit leaves it; null when the edit changes only {@link #editedFiles}.
         */
        @Nullable SourceFile modifiedPackageJson;

        LockFileRegeneration.@Nullable Result regenResult;

        /**
         * The package-manager files the edit changes or creates, by path, which travel to the visitor as the lock does.
         */
        Map<Path, String> editedFiles;

        /**
         * Whether the edit changes the package.json or one of {@link #editedFiles}, and so the lock was regenerated.
         */
        public boolean isChanged() { return modifiedPackageJson != null || !editedFiles.isEmpty(); }

        /**
         * {@code visited} as the edit leaves it, parsed as it was, when it is one of {@link #editedFiles}; else null.
         */
        public @Nullable SourceFile editedFile(SourceFile visited) {
            String content = editedFiles.get(visited.getSourcePath());
            return content == null ? null : reparseLock(visited, content);
        }

        public static EditAndRegenerateResult unchanged() {
            return new EditAndRegenerateResult(null, null, Collections.emptyMap());
        }
    }

    /**
     * Apply a recipe's edit to a package.json and the package-manager files it edits ({@code editedFiles}, by path),
     * refresh its marker and regenerate the captured lock over {@code projectFiles}, the rest of the project.
     */
    public static EditAndRegenerateResult editAndRegenerate(
            SourceFile packageJson,
            Function<Json.Document, Json.Document> editFn,
            Map<Path, String> editedFiles,
            @Nullable String capturedLockContent,
            NodeDependencyScan.ProjectFiles projectFiles,
            ExecutionContext ctx) {
        if (!(packageJson instanceof Json.Document)) {
            return EditAndRegenerateResult.unchanged();
        }
        Json.Document before = (Json.Document) packageJson;
        Json.Document after = editFn.apply(before);
        Map<Path, String> changed = changedFiles(editedFiles, projectFiles);
        if (after == before && changed.isEmpty()) {
            return EditAndRegenerateResult.unchanged();
        }
        SourceFile refreshed = refreshMarker(after);
        LockFileRegeneration.Result regen = capturedLockContent == null ? null
                : regenerateLockContent(refreshed, capturedLockContent, projectFiles, changed, ctx);
        return new EditAndRegenerateResult(after == before ? null : withResolvedDeps(refreshed, regen), regen, changed);
    }

    private static Map<Path, String> changedFiles(Map<Path, String> editedFiles,
                                                  NodeDependencyScan.ProjectFiles projectFiles) {
        Map<Path, String> changed = new LinkedHashMap<>();
        for (Map.Entry<Path, String> file : editedFiles.entrySet()) {
            String captured = projectFiles.getPackageManagerFiles().get(file.getKey());
            if (captured == null) {
                captured = projectFiles.getManifests().get(file.getKey());
            }
            if (!file.getValue().equals(captured)) {
                changed.put(file.getKey(), file.getValue());
            }
        }
        return changed;
    }

    private static SourceFile withResolvedDeps(SourceFile refreshed, LockFileRegeneration.@Nullable Result regen) {
        if (regen == null || !regen.isSuccess()) {
            return refreshed;
        }
        NodeResolutionResult marker = refreshed.getMarkers()
                .findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null || !isOverlaySupported(marker.getPackageManager())) {
            return refreshed;
        }
        try {
            return overlayResolvedDeps(refreshed, regen.getLockFileContent(), marker.getPackageManager());
        } catch (RuntimeException e) {
            return Markup.markup(refreshed,
                    new Markup.Warn(Tree.randomId(), "lock parse failed: " + e.getMessage(), null));
        }
    }

    public static LockFileRegeneration.@Nullable Result regenerateLockContent(
            SourceFile packageJson,
            @Nullable String capturedLockContent,
            NodeDependencyScan.ProjectFiles projectFiles,
            Map<Path, String> editedFiles,
            ExecutionContext ctx) {
        NodeResolutionResult marker = packageJson.getMarkers()
                .findFirst(NodeResolutionResult.class).orElse(null);
        if (marker == null || marker.getPackageManager() == null) {
            return null;
        }
        LockFileRegeneration regen = LockFileRegeneration.forPackageManager(marker.getPackageManager());
        if (regen == null) {
            return null;
        }
        String content = printed(packageJson);
        return regen.regenerate(LockFileRegeneration.Request.builder()
                .packageJson(content)
                .lock(capturedLockContent)
                .marker(marker)
                .packageJsonPath(packageJson.getSourcePath())
                .manifests(projectFiles.getManifests())
                .packageManagerFiles(projectFiles.getPackageManagerFiles())
                .editedFiles(editedFiles)
                .projectPaths(projectFiles.getPaths())
                .build(), ctx);
    }
}
