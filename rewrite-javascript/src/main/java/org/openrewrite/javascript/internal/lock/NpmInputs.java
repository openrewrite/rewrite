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
import org.openrewrite.javascript.internal.lock.npm.Shrinkwrap;
import org.openrewrite.javascript.internal.registry.NodeRegistries;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The npm regenerator's inputs: the host's registries as the user's config, the lock's directory's own .npmrc and the
 * paths held there, and the package.json files under each directory above the one npm runs in.
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

    static boolean writesBesideLock(String fileName) {
        return Shrinkwrap.writesBesideLock(fileName::equals);
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        return from(dir, null, host, now);
    }

    /**
     * @param registries the host's registries, npm's user config; null when the host gives none
     */
    static Inputs from(LockDirectory dir, @Nullable NodeRegistries registries, Host host, Instant now) {
        return Inputs.builder()
                .lock(dir.getLock())
                .yarnLock(dir.file("yarn.lock"))
                .npmrc(dir.file(".npmrc"))
                .userConfig(registries)
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
     * npm's walk from where it runs; it stops where npm writes a file no regeneration writes (Shrinkwrap's rules): its
     * lock as an npm-shrinkwrap.json, or the yarn.lock beside a lock the host does not hold.
     */
    static InstallRoot installRoot(LockDirectory dir) {
        InstallRoot walked = NpmLockRegenerator.installRoot(dir.getRunsIn(), dir.getManifests(), parentTrees(dir));
        String root = dir.aboveRunsIn(walked.getDepth());
        Predicate<String> holds = name -> dir.holds(RootRelativePath.join(root, name));
        boolean another = Shrinkwrap.readsShrinkwrap(holds) || Shrinkwrap.writesBesideLock(holds) &&
                                                                !holds.test("package-lock.json");
        return walked.isStopped() || !another ? walked :
                InstallRoot.stops(walked.getDepth(), InstallRoot.Stop.WRITES_ANOTHER_FILE);
    }
}
