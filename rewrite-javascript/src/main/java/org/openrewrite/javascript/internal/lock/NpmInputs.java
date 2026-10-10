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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator;
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator.Inputs;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The npm regenerator's inputs: the lock's directory's own .npmrc and the paths held there, and the package.json
 * files under each directory above the one npm runs in.
 */
final class NpmInputs {

    private NpmInputs() {
    }

    static boolean reads(String fileName) {
        return readsOnlyBesideLock(fileName);
    }

    static boolean readsOnlyBesideLock(String fileName) {
        return ".npmrc".equals(fileName);
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        return Inputs.builder()
                .lock(dir.getLock())
                .npmrc(dir.file(".npmrc"))
                .paths(dir.pathsUnder(""))
                .host(host)
                .now(now)
                .directoryName(dir.name())
                .manifests(dir.getManifests())
                .runsIn(dir.getRunsIn())
                .parentTrees(parentTrees(dir))
                .build();
    }

    private static Map<String, Map<String, @Nullable String>> parentTrees(LockDirectory dir) {
        Map<String, Map<String, @Nullable String>> trees = new LinkedHashMap<>();
        Set<String> withManifest = dir.manifestsAbove(dir.getRunsIn()).keySet();
        String runsIn = dir.fromRoot(dir.getRunsIn());
        String[] segments = runsIn.isEmpty() ? new String[0] : runsIn.split("/");
        for (int depth = 1; depth <= segments.length; depth++) {
            String parent = dir.aboveRunsIn(depth);
            String seenFrom = String.join("/", Arrays.asList(segments).subList(segments.length - depth,
                    segments.length));
            if (!withManifest.contains(seenFrom) && !dir.holds(RootRelativePath.join(parent, "package.json"))) {
                continue;
            }
            Map<String, @Nullable String> tree = new LinkedHashMap<>();
            Set<String> held = dir.pathsUnder(parent);
            if (held != null) {
                for (String path : held) {
                    String location = manifestLocation(path);
                    if (location != null) {
                        tree.put(location, null);
                    }
                }
            }
            for (Map.Entry<String, String> file : dir.filesUnder(parent).entrySet()) {
                String location = manifestLocation(file.getKey());
                if (location != null) {
                    tree.put(location, file.getValue());
                }
            }
            tree.putAll(dir.manifestsUnder(parent));
            trees.put(seenFrom, tree);
        }
        return trees;
    }

    private static @Nullable String manifestLocation(String path) {
        if ("package.json".equals(path)) {
            return "";
        }
        return path.endsWith("/package.json") ? path.substring(0, path.length() - "/package.json".length()) : null;
    }

    /**
     * npm's walk from where it runs; where it installs it writes an npm-shrinkwrap.json in place of its lock and
     * rewrites a yarn.lock (shrinkwrap.js), which no regeneration writes, so a walk to either the host holds stops.
     */
    static InstallRoot installRoot(LockDirectory dir) {
        InstallRoot walked = NpmLockRegenerator.installRoot(from(dir, Host.UNKNOWN, Instant.EPOCH));
        String root = dir.aboveRunsIn(walked.getDepth());
        return walked.isStopped() || !dir.holds(RootRelativePath.join(root, "npm-shrinkwrap.json")) &&
                                     !dir.holds(RootRelativePath.join(root, "yarn.lock")) ? walked :
                InstallRoot.stops(walked.getDepth(), InstallRoot.Stop.WRITES_ANOTHER_FILE);
    }
}
