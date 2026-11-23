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
package org.openrewrite.javascript.internal.lock.npm;

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/patched-dependencies.js (npm 12.0.0): the root's {@code patchedDependencies} resolved
 * against the ideal tree. The record of a patched node holds the integrity of its patch file, whose contents the
 * engine is not given.
 */
final class PatchedDependencies {

    /**
     * {@code EPATCHAMBIGUOUS}, {@code EPATCHUNSAFE}, {@code EPATCHNOTFOUND}, {@code EPATCHWORKSPACE}, {@code
     * EPATCHNONREGISTRY} or {@code EPATCHUNUSED}.
     */
    @Getter
    static final class PatchError extends IllegalStateException implements NpmError {
        private final String code;

        PatchError(String message, String code) {
            super(message);
            this.code = code;
        }
    }

    /**
     * A key of {@code patchedDependencies}, a name with an optional version or range after it, and the patch it names.
     */
    @Value
    static class Selector {
        String name;

        /**
         * Null for a key that is a name only.
         */
        @Nullable
        String spec;

        String key;

        @Nullable
        Object patchPath;
    }

    private PatchedDependencies() {
    }

    static Selector parseSelector(String key, @Nullable Object patchPath) {
        int at = key.indexOf('@', 1);
        return at == -1 ? new Selector(key, null, key, patchPath) :
                new Selector(key.substring(0, at), key.substring(at + 1), key, patchPath);
    }

    // the most specific of several ranges that all match a version: the one that is a subset of every other
    private static Selector pickRange(List<Selector> ranges, @Nullable String name, Object version,
                                      NodeSemver.Options loose) {
        Selector best = ranges.get(0);
        for (Selector r : ranges.subList(1, ranges.size())) {
            String spec = String.valueOf(r.getSpec());
            String bestSpec = String.valueOf(best.getSpec());
            if (NodeSemver.subset(spec, bestSpec, loose)) {
                best = r;
            } else if (!NodeSemver.subset(bestSpec, spec, loose)) {
                throw new PatchError("Ambiguous patch selectors for " + name + "@" + Js.string(version) + ": \"" +
                                     name + "@" + bestSpec + "\" and \"" + name + "@" + spec +
                                     "\" overlap but neither is a subset. Add an exact \"" + name + "@" +
                                     Js.string(version) + "\" entry to disambiguate.", "EPATCHAMBIGUOUS");
            }
        }
        return best;
    }

    /**
     * The selector that wins a node: an exact version, else the narrowest range, else the name alone.
     * @throws PatchError {@code EPATCHAMBIGUOUS} for ranges of which none is the narrowest
     */
    static @Nullable Selector matchSelector(List<Selector> selectors, Node node) {
        String name = node.getName();
        Object version = node.getVersion();
        List<Selector> matches = new ArrayList<>();
        for (Selector s : selectors) {
            if (s.getName().equals(name)) {
                matches.add(s);
            }
        }
        if (matches.isEmpty()) {
            return null;
        }
        // the node-semver of the npm release that builds the tree, as its valid() and its { loose: true }
        NodeSemver.Options strict = NodeSemver.Options.DEFAULT.release(node.getRelease().semver());
        NodeSemver.Options loose = NodeSemver.Options.LOOSE.release(node.getRelease().semver());

        for (Selector s : matches) {
            if (truthy(s.getSpec()) && NodeSemver.valid(s.getSpec(), strict) != null &&
                NodeSemver.compare(s.getSpec(), version, loose) == 0) {
                return s;
            }
        }

        List<Selector> ranges = new ArrayList<>();
        for (Selector s : matches) {
            if (truthy(s.getSpec()) && NodeSemver.valid(s.getSpec(), strict) == null &&
                NodeSemver.satisfies(version, s.getSpec(), loose)) {
                ranges.add(s);
            }
        }
        if (!ranges.isEmpty()) {
            return pickRange(ranges, name, version, loose);
        }

        for (Selector s : matches) {
            if (s.getSpec() == null) {
                return s;
            }
        }
        return null;
    }

    /**
     * Whether resolving has anything to do in the tree: a patch the root or a workspace declares, or a patch record a
     * node holds from its lock.
     */
    static boolean concerns(Node tree) {
        Object declared = tree.getPackage().get("patchedDependencies");
        if (truthy(declared) && !Js.entries(declared).isEmpty()) {
            return true;
        }
        for (Node node : tree.getInventory().values()) {
            if (truthy(node.getPatched()) ||
                node.isWorkspace() && truthy(node.getPackage().get("patchedDependencies"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param path the project directory, which a patch path resolves against and may not leave
     * @param held the files the project holds, relative to {@code path}, when the host says
     * @throws PatchError where npm rejects the patch set
     * @throws CannotKnow for a patch file the project holds, whose integrity the node's record is, once npm has nothing
     *                    left to refuse; and for one it may hold, where the host does not say
     */
    static void resolvePatchedDependencies(Node tree, String path, @Nullable Set<String> held) {
        Object declared = tree.getPackage().get("patchedDependencies");
        List<Selector> selectors = new ArrayList<>();
        for (Map.Entry<String, @Nullable Object> entry : Js.entries(truthy(declared) ? declared :
                Collections.emptyMap())) {
            selectors.add(parseSelector(entry.getKey(), entry.getValue()));
        }

        // npm reads a patch file that is there and goes on to the next node, so what it refuses after is its answer
        // whatever the file holds; the integrity it records of the file is what the engine is not given
        CannotKnow unread = null;
        Set<String> usedKeys = new LinkedHashSet<>();
        for (Node node : tree.getInventory().values()) {
            // patchedDependencies is honoured only in the root manifest
            if (node.isWorkspace()) {
                Object pkgPatches = node.getPackage().get("patchedDependencies");
                if (truthy(pkgPatches) && !Js.keys(pkgPatches).isEmpty()) {
                    throw new PatchError("patchedDependencies is only supported in the root package.json, but was " +
                                         "found in workspace \"" + node.getName() + "\". Move the entry to the root.",
                            "EPATCHWORKSPACE");
                }
                continue;
            }
            if (node.isProjectRoot()) {
                continue;
            }

            Selector selector = matchSelector(selectors, node);
            if (selector == null) {
                node.setPatched(null);
                continue;
            }

            // a consumer edge that is no registry spec leaves no registry tarball to patch; an npm: alias stays one
            for (Edge e : node.getEdgesIn()) {
                if (truthy(e.getSpec()) &&
                    !Spec.npa(e.getSpec(), null, node.getRelease().npmPackageArg()).isRegistry()) {
                    throw new PatchError("Cannot patch non-registry dependency " + node.getName() + "@" +
                                         Js.string(node.getVersion()) + " (selector \"" + selector.getKey() +
                                         "\"). Only registry dependencies can be patched.", "EPATCHNONREGISTRY");
                }
            }

            // node.patched = { path, integrity }: the integrity is the patch file's, so no node is given a record
            RuntimeException refusal = readPatch(selector.getPatchPath(), path, held);
            if (held == null || !(refusal instanceof CannotKnow)) {
                throw refusal;
            }
            if (unread == null) {
                unread = (CannotKnow) refusal;
            }
            usedKeys.add(selector.getKey());
        }

        // allow-unused-patches is read from the command line only, where the install has none
        List<String> unused = new ArrayList<>();
        for (Selector s : selectors) {
            if (!usedKeys.contains(s.getKey())) {
                unused.add("  " + s.getKey() + " -> " + Js.string(s.getPatchPath()));
            }
        }
        if (!unused.isEmpty()) {
            throw new PatchError("The following patches were registered but matched no installed package:\n" +
                                 String.join("\n", unused) + "\nUse --allow-unused-patches to install anyway.",
                    "EPATCHUNUSED");
        }
        if (unread != null) {
            throw unread;
        }
    }

    // readPatch up to the file's contents: what npm rejects of the path, else what the engine cannot know
    private static RuntimeException readPatch(@Nullable Object patchPath, String path, @Nullable Set<String> held) {
        if (!(patchPath instanceof String)) {
            // path.resolve's TypeError, under the code Node gives it and npm prints
            return new NpmRejection("ERR_INVALID_ARG_TYPE", "The \"paths[1]\" argument must be of type string. " +
                                                            "Received " + Js.received(patchPath), null);
        }
        String abs = PosixPath.resolve(path, (String) patchPath);
        String rel = PosixPath.relative(path, abs);
        if (rel.isEmpty() || rel.startsWith("..") || PosixPath.isAbsolute(rel)) {
            return new PatchError("patch path escapes the project: " + patchPath, "EPATCHUNSAFE");
        }
        if (held == null) {
            // a declared patch is likelier there than not: where the host does not say, npm's answer is not known
            return new CannotKnow("patch file " + rel);
        }
        return held.contains(rel) ? CannotKnow.unread(rel) :
                new PatchError("patch file not found: " + patchPath, "EPATCHNOTFOUND");
    }
}
