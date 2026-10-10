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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.LockScenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The recorded Yarn classic scenarios ({@link LockScenario#discover}) in the recorder's layout: each lock with the
 * manifests, config files and paths of its phase.
 */
final class YarnClassicScenarios {

    static final String FAMILY = "yarn-classic";

    private static final List<String> LOCKS = Arrays.asList("before", "after", "again");
    private static final List<String> CONFIG_FILES = Arrays.asList(".yarnrc", ".yarnrc.yml", ".npmrc");
    private static final Pattern NODE_VERSION = Pattern.compile("node (v[0-9.]+)");
    private static final Pattern EDIT_STARTED = Pattern.compile("(?m)^edit started (\\S+)$");
    private static final Pattern PLATFORM = Pattern.compile("(?m)^platform os=(\\S*) cpu=(\\S*)");
    private static final Pattern LAYOUT = Pattern.compile("(?m)^layout repo/(\\S+)");
    private static final String RECORDED_ON_OS = "darwin";
    private static final String RECORDED_ON_CPU = "arm64";

    private YarnClassicScenarios() {
    }

    /**
     * One lock of a scenario with what yarn had when it wrote or read it: the manifests and the config of the
     * phase.
     */
    @Getter
    static final class Lock {
        private final Scenario scenario;
        private final String fileName;
        private final String text;
        private final boolean before;

        Lock(Scenario scenario, String fileName, String text) {
            this.scenario = scenario;
            this.fileName = fileName;
            this.text = text;
            this.before = fileName.startsWith("before");
        }

        Map<String, String> manifests() {
            return scenario.manifests(before);
        }

        /**
         * What {@code yarn install --force} writes over this lock, when that differs from it.
         */
        @Nullable
        String forced() {
            return scenario.file(fileName + "-forced");
        }

        /**
         * The log of an install that changes nothing over this lock, when one was recorded.
         */
        @Nullable
        String againLog() {
            return "after".equals(fileName) ? scenario.file("yarnclassic-again.log") :
                    scenario.file("yarnclassic-" + fileName + "-again.log");
        }

        /**
         * What that install wrote, when it changed the lock.
         */
        @Nullable
        String again() {
            return "after".equals(fileName) ? scenario.file("again") : scenario.file(fileName + "-again");
        }

        YarnClassicLockRegenerator.Inputs inputs() {
            return scenario.inputs(before, text);
        }

        @Override
        public String toString() {
            return scenario.getLabel() + "/" + fileName;
        }
    }

    static final class Scenario extends LockScenario {
        Scenario(String name, String label, Path dir, @Nullable Path httpCache) {
            super(name, label, dir, httpCache);
        }

        /**
         * Whether the scenario records an edit: a lock to start from and the edited manifest.
         */
        boolean isEdit() {
            return has("before") && (has("pkg") || has("pkg-after"));
        }

        /**
         * The Node version yarn ran under, as {@code process.version} prints it.
         */
        @Nullable
        String nodeVersion() {
            String recorded = file("recorded-with");
            if (recorded != null) {
                Matcher m = NODE_VERSION.matcher(recorded);
                if (m.find()) {
                    return m.group(1);
                }
            }
            return null;
        }

        /**
         * The machine yarn ran on: the {@code platform} line of {@code recorded-with}, else the machine every
         * recording here was made on.
         */
        Host host() {
            String os = RECORDED_ON_OS;
            String cpu = RECORDED_ON_CPU;
            String recorded = file("recorded-with");
            if (recorded != null) {
                Matcher m = PLATFORM.matcher(recorded);
                if (m.find()) {
                    os = m.group(1);
                    cpu = m.group(2);
                }
            }
            return Host.builder().nodeVersion(nodeVersion()).os(os).cpu(cpu).build();
        }

        Instant now() {
            String recorded = file("recorded-with");
            if (recorded != null) {
                Matcher started = EDIT_STARTED.matcher(recorded);
                if (started.find()) {
                    return Instant.parse(started.group(1));
                }
            }
            return Instant.now();
        }

        /**
         * Every lock yarn wrote or was given, and what it wrote over each with no change or under {@code --force}.
         */
        List<Lock> locks() {
            List<Lock> locks = new ArrayList<>();
            for (String file : LOCKS) {
                String text = file(file);
                if (text != null) {
                    locks.add(new Lock(this, file, text));
                }
            }
            String given = file("before-given");
            if (given != null && !given.equals(file("before"))) {
                locks.add(new Lock(this, "before-given", given));
            }
            for (String lock : Arrays.asList("before", "after", "before-given")) {
                for (String written : Arrays.asList(lock + "-again", lock + "-forced")) {
                    String text = file(written);
                    if (text != null) {
                        locks.add(new Lock(this, written, text));
                    }
                }
            }
            return locks;
        }

        /**
         * What yarn had in a phase: the lock it started from, the phase's manifests and tree, the recorder's config
         * files ({@code parent/}, {@code grandparent/}), its paths, and the directory the {@code cwd} file names.
         */
        YarnClassicLockRegenerator.Inputs inputs(boolean before, @Nullable String lock) {
            Map<String, String> configFiles = new LinkedHashMap<>();
            putIfPresent(configFiles, ".yarnrc", file("yarnrc"));
            putIfPresent(configFiles, ".npmrc", file("npmrc"));
            Map<String, Map<String, String>> trees = new LinkedHashMap<>();
            trees.put("", tree(before));
            trees.put("../", tree("parent"));
            trees.put("../../", tree("grandparent"));
            Set<String> paths = new LinkedHashSet<>(configFiles.keySet());
            paths.add("package.json");
            if (lock != null) {
                paths.add("yarn.lock");
            }
            for (Map.Entry<String, Map<String, String>> tree : trees.entrySet()) {
                for (String name : CONFIG_FILES) {
                    putIfPresent(configFiles, tree.getKey() + name, tree.getValue().get(name));
                }
                if (!tree.getKey().isEmpty()) {
                    putIfPresent(configFiles, tree.getKey() + "package.json", tree.getValue().get("package.json"));
                }
                for (String path : tree.getValue().keySet()) {
                    paths.add(tree.getKey() + path);
                }
            }
            String cwd = file("cwd");
            for (String dir = cwd == null ? "" : cwd.trim(); !dir.isEmpty();
                 dir = dir.substring(0, Math.max(dir.lastIndexOf('/'), 0))) {
                for (String name : CONFIG_FILES) {
                    putIfPresent(configFiles, dir + "/" + name, trees.get("").get(dir + "/" + name));
                }
            }
            return YarnClassicLockRegenerator.Inputs.builder()
                    .lock(lock)
                    .manifests(manifests(before))
                    .configFiles(configFiles)
                    .paths(has("recorded-with") ? paths : null)
                    .directory(directory())
                    .cwd(cwd == null ? "" : cwd.trim())
                    .env(new LinkedHashMap<>())
                    .host(host())
                    .now(now())
                    .build();
        }

        /**
         * The project's directory in the repository the recorder laid out ({@code mid/app} under {@code parent/} and
         * {@code grandparent/}); a recording made elsewhere does not say.
         */
        @Nullable
        String directory() {
            String recorded = file("recorded-with");
            if (recorded == null) {
                return null;
            }
            Matcher m = LAYOUT.matcher(recorded);
            return m.find() ? m.group(1) : "";
        }

        private void putIfPresent(Map<String, String> files, String path, @Nullable String text) {
            if (text != null) {
                files.put(path, text);
            }
        }

        /**
         * The project's manifests in a phase by location: {@code ""} the root, a directory's path otherwise.
         */
        Map<String, String> manifests(boolean before) {
            Map<String, String> manifests = new LinkedHashMap<>();
            String root = before ? file("pkg-before") : has("pkg") ? file("pkg") : file("pkg-after");
            if (root != null) {
                manifests.put("", root);
            }
            for (Map.Entry<String, String> entry : tree(before).entrySet()) {
                String path = entry.getKey();
                if (path.endsWith("/package.json")) {
                    manifests.put(path.substring(0, path.length() - "/package.json".length()), entry.getValue());
                }
            }
            return manifests;
        }

        /**
         * The files of the project directory in a phase: {@code tree-before/}, then {@code tree/} over it after.
         */
        Map<String, String> tree(boolean before) {
            Map<String, String> out = new TreeMap<>(tree("tree-before"));
            if (!before) {
                out.putAll(tree("tree"));
            }
            return out;
        }
    }

    static List<Scenario> discover() {
        return LockScenario.discover(FAMILY, YarnClassicScenarios::isScenario, Scenario::new);
    }

    static String read(Path path) {
        return LockScenario.read(path);
    }

    private static boolean isScenario(Path dir) {
        for (String lock : LOCKS) {
            if (Files.isRegularFile(dir.resolve(lock))) {
                return true;
            }
        }
        return false;
    }
}
