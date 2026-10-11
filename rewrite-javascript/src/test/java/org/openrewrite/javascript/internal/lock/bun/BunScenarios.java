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
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.HostInputs;
import org.openrewrite.javascript.internal.lock.HostRegistries;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.LockScenario;
import org.openrewrite.javascript.internal.lock.RootRelativePath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The recorded Bun scenarios ({@link LockScenario#discover}) in the recorder's layout: each lock with the manifests,
 * npmrc and bunfig of its phase.
 */
final class BunScenarios {

    private static final List<String> LOCKS = Arrays.asList("before", "after", "fresh");
    private static final Pattern MEMBER_MANIFEST = Pattern.compile("^pkg-(.+?)(?:-(before|after))?$");
    private static final Pattern LOAD_ERROR = Pattern.compile("(?m)^error: (.*)$");
    private static final Pattern LOAD_ERROR_NAME = Pattern.compile("(?m)^(\\w+): failed to parse lockfile");
    private static final Pattern EDIT_STARTED = Pattern.compile("(?m)^edit started (\\S+)$");
    private static final Pattern PLATFORM = Pattern.compile("(?m)^platform os=(\\S+) cpu=(\\S+) libc=(\\S*)$");
    private static final Pattern REQUEST = Pattern.compile("(?m)^GET ([a-z]+://[^/ ]+)");
    private static final Pattern ORIGIN = Pattern.compile("^[a-z]+://[^/]+");

    private BunScenarios() {
    }

    /**
     * One lock of a scenario with what bun had in the phase that wrote it.
     */
    static final class Lock extends LockScenario.RecordedLock<Scenario> {

        Lock(Scenario scenario, String fileName, String text, Phase phase) {
            super(scenario, fileName, text, phase);
        }

        BunLockRegenerator.Inputs.InputsBuilder inputs() {
            return scenario.inputs(phase, text);
        }

        /**
         * What bun writes on an install that changes nothing over this lock, when that differs from the lock.
         */
        @Nullable
        String rewritten() {
            return scenario.file("after".equals(fileName) ? "again" : fileName + "-again");
        }

        @Nullable
        private String againLog() {
            String log = scenario.file("bun-" + fileName + "-again.log");
            if (log == null && "after".equals(fileName)) {
                log = scenario.file("bun-again.log");
            }
            return log;
        }

        /**
         * Whether bun's install that changes nothing over this lock resolved dependencies and wrote another lock, whose
         * bytes are then the resolver's; a patched dependency makes bun run tasks without resolving anything.
         */
        boolean resolvesAgain() {
            String log = againLog();
            return log != null && log.contains("Resolved, downloaded and extracted") && log.contains("lock changed");
        }

        /**
         * Whether that install left no lock: bun deletes a lockfile with no packages.
         */
        boolean deletedAgain() {
            String log = againLog();
            return log != null && log.contains("Deleted empty lockfile");
        }

        /**
         * The error bun reported loading this lock on that install ({@code <Name>: failed to parse lockfile}), as
         * {@code Name: message}, when it ignored the lock.
         */
        @Nullable
        String loadError() {
            String log = againLog();
            if (log == null) {
                return null;
            }
            Matcher name = LOAD_ERROR_NAME.matcher(log);
            if (!name.find()) {
                return null;
            }
            Matcher message = LOAD_ERROR.matcher(log);
            return name.group(1) + ": " + (message.find() ? message.group(1) : "");
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
         * Whether the scenario records an edit: a starting lock and the edited manifest.
         */
        boolean isEdit() {
            return has("before") && (has("pkg") || has("pkg-after") || has("pkg-root"));
        }

        /**
         * When bun ran the edit, as the open-question recorder writes it ({@code edit started <instant>}), else now.
         */
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
         * The platform bun ran on, as the open-question recorder writes it ({@code platform os=<os> cpu=<cpu>
         * libc=<libc>}), else unknown.
         */
        Host host() {
            String recorded = file("recorded-with");
            Matcher platform = recorded == null ? null : PLATFORM.matcher(recorded);
            if (platform == null || !platform.find()) {
                return Host.UNKNOWN;
            }
            return Host.builder().os(platform.group(1)).cpu(platform.group(2))
                    .libc(platform.group(3).isEmpty() ? null : platform.group(3)).build();
        }

        /**
         * Whether the edit's install, rerun from an empty cache, asked the host of {@code registry}: any host when
         * {@code requests-after.log} names none.
         */
        boolean asked(String registry) {
            String requests = file("requests-after.log");
            Set<String> origins = new TreeSet<>();
            Matcher request = REQUEST.matcher(requests == null ? "" : requests);
            while (request.find()) {
                origins.add(request.group(1));
            }
            return origins.isEmpty() || origins.contains(origin(registry));
        }

        static String origin(String url) {
            Matcher origin = ORIGIN.matcher(url);
            return origin.find() ? origin.group() : url;
        }

        /**
         * Every lock bun wrote or was given: {@code before}, {@code after}, {@code fresh}, and what bun rewrote each
         * to on an install that changes nothing ({@code <lock>-again}, {@code again}).
         */
        List<Lock> locks() {
            List<Lock> locks = new ArrayList<>();
            for (String file : LOCKS) {
                Phase phase = "before".equals(file) ? Phase.BEFORE : Phase.AFTER;
                String text = file(file);
                if (text != null) {
                    locks.add(new Lock(this, file, text, phase));
                }
                String again = "after".equals(file) ? "again" : file + "-again";
                String rewritten = file(again);
                if (rewritten != null) {
                    locks.add(new Lock(this, again, rewritten, phase));
                }
            }
            return locks;
        }

        /**
         * What bun had in a phase over a lock: the manifests, npmrc and bunfig, the directory it ran in and the
         * package.json files above it, the day. Each test adds the project files it gives.
         */
        BunLockRegenerator.Inputs.InputsBuilder inputs(Phase phase, @Nullable String lock) {
            return BunLockRegenerator.Inputs.builder()
                    .lock(lock)
                    .manifests(manifests(phase, lock == null ? "" : lock))
                    .npmrc(config(phase, "npmrc", ".npmrc"))
                    .bunfig(config(phase, "bunfig", "bunfig.toml"))
                    .userNpmrc(userNpmrc(phase))
                    .runsIn(runDir(phase))
                    .parentManifests(parentManifests(phase))
                    .now(now());
        }

        /**
         * The files of the project directory in a phase: {@code tree-before/}, then {@code tree/} over it after.
         */
        Map<String, String> tree(Phase phase) {
            Map<String, String> out = new TreeMap<>(tree("tree-before"));
            if (phase == Phase.AFTER) {
                out.putAll(tree("tree"));
            }
            return out;
        }

        /**
         * Every path the project holds in a phase: its tree, its manifests and its lock.
         */
        Set<String> paths(Phase phase, @Nullable String lock) {
            Set<String> paths = new TreeSet<>(tree(phase).keySet());
            for (String location : manifests(phase, lock == null ? "" : lock).keySet()) {
                paths.add(location.isEmpty() ? "package.json" : location + "/package.json");
            }
            if (lock != null) {
                paths.add("bun.lock");
            }
            return paths;
        }

        /**
         * The directory bun ran in, relative to the lock's: {@code run-dir} for the edit, else the lock's own.
         */
        String runDir(Phase phase) {
            String runDir = phase == Phase.AFTER ? file("run-dir") : null;
            return runDir == null ? "" : RootRelativePath.normalize(runDir.trim());
        }

        /**
         * The package.json of each directory above the one bun ran the edit in, nearest first, keyed by that directory
         * as seen from there; {@code above/} holds those above the lock's, by path from where {@code lock-dir} starts.
         */
        Map<String, String> parentManifests(Phase phase) {
            Map<String, String> parents = new LinkedHashMap<>();
            if (phase == Phase.BEFORE) {
                return parents;
            }
            String before = file("before");
            Map<String, String> manifests = manifests(phase, before == null ? "" : before);
            String runDir = runDir(phase);
            List<String> within = runDir.isEmpty() ? new ArrayList<>() : Arrays.asList(runDir.split("/"));
            for (int up = 1; up <= within.size(); up++) {
                String manifest = manifests.get(String.join("/", within.subList(0, within.size() - up)));
                if (manifest != null) {
                    parents.put(String.join("/", within.subList(within.size() - up, within.size())), manifest);
                }
            }
            String lockDir = file("lock-dir");
            if (lockDir == null) {
                return parents;
            }
            Map<String, String> above = tree("above");
            List<String> segments = Arrays.asList(lockDir.trim().split("/"));
            for (int up = 1; up <= segments.size(); up++) {
                String ancestor = String.join("/", segments.subList(0, segments.size() - up));
                String manifest = above.get(ancestor.isEmpty() ? "package.json" : ancestor + "/package.json");
                if (manifest != null) {
                    String below = String.join("/", segments.subList(segments.size() - up, segments.size()));
                    parents.put(runDir.isEmpty() ? below : below + "/" + runDir, manifest);
                }
            }
            return parents;
        }

        /**
         * The files of {@code above/} in a phase, by path relative to the lock's directory
         * ({@code ../../patches/x.patch}).
         */
        Map<String, String> filesAbove(Phase phase) {
            Map<String, String> out = new TreeMap<>();
            String lockDir = file("lock-dir");
            if (lockDir != null && phase == Phase.AFTER) {
                for (Map.Entry<String, String> file : tree("above").entrySet()) {
                    out.put(RootRelativePath.relative(lockDir.trim(), file.getKey()), file.getValue());
                }
            }
            return out;
        }

        /**
         * The user's .npmrc of a phase ({@code user-npmrc}, {@code user-npmrc-before}; an empty one is none) as the
         * host renders it from the marker's user npmrc.
         */
        String userNpmrc(Phase phase) {
            String npmrc = phase == Phase.BEFORE && has("user-npmrc-before") ? file("user-npmrc-before") :
                    file("user-npmrc");
            return HostRegistries.host(npmrc == null || npmrc.isEmpty() ? null : npmrc, getDir())
                    .userNpmrc(HostInputs.USER);
        }

        @Nullable
        String config(Phase phase, String file, String treeFile) {
            String fromTree = tree(phase).get(treeFile);
            if (fromTree != null) {
                return fromTree;
            }
            if (phase == Phase.BEFORE && has(file + "-before")) {
                return file(file + "-before");
            }
            return file(file);
        }

        /**
         * The project's manifests in a phase by location: {@code ""} the root, a member's path otherwise.
         */
        Map<String, String> manifests(Phase phase, String lock) {
            Map<String, String> manifests = new LinkedHashMap<>();
            String root = phase == Phase.BEFORE ? (has("pkg-before") ? file("pkg-before") : file("pkg-root")) :
                    has("pkg") ? file("pkg") : has("pkg-after") ? file("pkg-after") : file("pkg-root");
            if (root != null) {
                manifests.put("", root);
            }
            for (Map.Entry<String, String> entry : tree(phase).entrySet()) {
                String path = entry.getKey();
                if (path.endsWith("/package.json")) {
                    manifests.put(path.substring(0, path.length() - "/package.json".length()), entry.getValue());
                } else if ("package.json".equals(path) && !manifests.containsKey("")) {
                    manifests.put("", entry.getValue());
                }
            }
            Map<String, String> memberFiles = new LinkedHashMap<>();
            for (String file : files()) {
                Matcher m = MEMBER_MANIFEST.matcher(file);
                if (!m.matches() || Arrays.asList("before", "after", "root").contains(m.group(1))) {
                    continue;
                }
                String suffix = m.group(2);
                if (suffix != null && !suffix.equals(phase == Phase.BEFORE ? "before" : "after")) {
                    continue;
                }
                if (suffix == null && memberFiles.containsKey(m.group(1))) {
                    continue;
                }
                memberFiles.put(m.group(1), file);
            }
            if (!memberFiles.isEmpty()) {
                Map<String, String> paths = memberPaths(lock);
                for (Map.Entry<String, String> member : memberFiles.entrySet()) {
                    String path = paths.get(member.getKey());
                    if (path != null) {
                        manifests.put(path, file(member.getValue()));
                    }
                }
            }
            return manifests;
        }

        private static Map<String, String> memberPaths(String lock) {
            Map<String, String> paths = new LinkedHashMap<>();
            try {
                Json.Expr workspaces = Json.parse(lock).getObject("workspaces");
                if (workspaces != null) {
                    for (Json.Property prop : workspaces.properties()) {
                        Json.Expr name = prop.value.get("name");
                        if (!prop.key.asString().isEmpty() && name != null && name.isString()) {
                            paths.put(name.asString(), prop.key.asString());
                        }
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // a lock that does not parse places no member
            }
            return paths;
        }
    }

    static List<Scenario> discover() {
        return LockScenario.discover("bun", BunScenarios::isScenario, Scenario::new);
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
