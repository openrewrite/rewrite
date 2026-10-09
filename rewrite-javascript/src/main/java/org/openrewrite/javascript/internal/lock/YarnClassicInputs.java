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
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Yarn classic regenerator's inputs: the config files of the lock's directory, of each directory above it and of
 * those from where yarn runs up to it, and the package.json of those directories.
 */
final class YarnClassicInputs {

    private static final List<String> CONFIG_FILES = Arrays.asList(".yarnrc", ".yarnrc.yml", ".npmrc");

    private YarnClassicInputs() {
    }

    static boolean reads(String fileName) {
        return CONFIG_FILES.contains(fileName);
    }

    static Inputs from(LockDirectory dir, Host host, Instant now) {
        Map<String, String> configFiles = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : dir.getFiles().entrySet()) {
            String path = file.getKey();
            int slash = path.lastIndexOf('/');
            String name = path.substring(slash + 1);
            String parent = slash < 0 ? "" : path.substring(0, slash);
            boolean above = parent.matches("\\.\\.(/\\.\\.)*");
            if (CONFIG_FILES.contains(name) && (parent.isEmpty() || above || isOnTheWayTo(parent, dir.getRunsIn())) ||
                "package.json".equals(name) && (above || isOnTheWayTo(parent, dir.getRunsIn()))) {
                configFiles.put(path, file.getValue());
            }
        }
        return Inputs.builder()
                .lock(dir.getLock())
                .manifests(dir.getManifests())
                .configFiles(configFiles)
                .paths(paths(dir))
                .directory(dir.getPath())
                .cwd(dir.getRunsIn())
                .host(host)
                .now(now)
                .build();
    }

    // the paths under the lock's directory and those directly in a directory above it
    private static @Nullable Set<String> paths(LockDirectory dir) {
        if (dir.getPaths() == null) {
            return null;
        }
        Set<String> paths = new LinkedHashSet<>();
        for (String path : dir.getPaths()) {
            int slash = path.lastIndexOf('/');
            if (!path.startsWith("../") || path.substring(0, slash).matches("\\.\\.(/\\.\\.)*")) {
                paths.add(path);
            }
        }
        return paths;
    }

    private static boolean isOnTheWayTo(String dir, String runsIn) {
        return !dir.startsWith("..") && (runsIn.equals(dir) || runsIn.startsWith(dir + "/"));
    }
}
