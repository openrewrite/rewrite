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

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The recorded Bun scenarios a test can replay: every directory holding a bun.lock under {@code lock/bun/} on the
 * test classpath (the in-repo fixtures and the downloaded ones merge there), and, when the environment variable
 * {@code REWRITE_LOCK_SCENARIOS} names a directory, every one under its {@code bun} sibling, or under the directory
 * itself when it holds Bun scenarios. {@code REWRITE_LOCK_HTTP_CACHE} names a directory of registry responses a
 * scenario's own {@code http/} falls back on.
 * <p>
 * The layout: {@code pkg-before}, {@code before}, {@code pkg},
 * {@code after}, {@code fresh}, a hand-made starting lock {@code lock-before} (copied to {@code before}), the project
 * config {@code npmrc} and {@code bunfig} (with {@code npmrc-before} and {@code bunfig-before} for the first phase),
 * the project files {@code tree-before/} and {@code tree/} copied over it before each phase, and {@code http/}.
 * What bun writes on an install that changes nothing over a lock, when it differs, is {@code <lock>-again}
 * ({@code again} for {@code after}) with {@code bun-<lock>-again.log}. The downloaded fixtures spell the edited
 * manifest {@code pkg-after}, a workspace's root {@code pkg-root} and its members {@code pkg-<name>},
 * {@code pkg-<name>-before} and {@code pkg-<name>-after}.
 */
final class BunScenarios {

    static final String SCENARIOS_ENV = "REWRITE_LOCK_SCENARIOS";
    static final String HTTP_CACHE_ENV = "REWRITE_LOCK_HTTP_CACHE";

    private static final List<String> LOCKS = Arrays.asList("before", "after", "fresh");
    private static final Pattern MEMBER_MANIFEST = Pattern.compile("^pkg-(.+?)(?:-(before|after))?$");
    private static final Pattern LOAD_ERROR = Pattern.compile("(?m)^error: (.*)$");
    private static final Pattern LOAD_ERROR_NAME = Pattern.compile("(?m)^(\\w+): failed to parse lockfile");
    private static final Pattern EDIT_STARTED = Pattern.compile("(?m)^edit started (\\S+)$");

    private BunScenarios() {
    }

    /**
     * One lock of a scenario with what bun had when it wrote it: the manifests of the root and the workspace
     * members, the project's npmrc and bunfig.
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
            return scenario.manifests(before, text);
        }

        @Nullable
        String npmrc() {
            return scenario.config(before, "npmrc", ".npmrc");
        }

        @Nullable
        String bunfig() {
            return scenario.config(before, "bunfig", "bunfig.toml");
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
         * Whether bun's install that changes nothing over this lock resolved dependencies (from scratch, or some it
         * found changed) and wrote another lock: its bytes are the resolver's. A patched dependency makes bun run
         * tasks, and report them, without resolving anything.
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

    @Getter
    static final class Scenario {
        private final String name;
        private final String label;
        private final Path dir;
        private final @Nullable Path httpCache;

        Scenario(String name, String label, Path dir, @Nullable Path httpCache) {
            this.name = name;
            this.label = label;
            this.dir = dir;
            this.httpCache = httpCache;
        }

        boolean has(String file) {
            return Files.isRegularFile(dir.resolve(file));
        }

        @Nullable
        String file(String file) {
            return has(file) ? read(dir.resolve(file)) : null;
        }

        @Nullable
        String expect() {
            String expect = file("expect");
            return expect == null ? null : expect.trim();
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

        @Nullable
        String recordedWith() {
            return file("recorded-with");
        }

        /**
         * Every lock bun wrote or was given: {@code before}, {@code after}, {@code fresh}, and what bun rewrote each
         * to on an install that changes nothing ({@code <lock>-again}, {@code again}).
         */
        List<Lock> locks() {
            List<Lock> locks = new ArrayList<>();
            for (String file : LOCKS) {
                String text = file(file);
                if (text != null) {
                    locks.add(new Lock(this, file, text));
                }
                String again = "after".equals(file) ? "again" : file + "-again";
                String rewritten = file(again);
                if (rewritten != null) {
                    locks.add(new Lock(this, again, rewritten));
                }
            }
            return locks;
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

        /**
         * Every path the project holds in a phase: its tree, its manifests and its lock.
         */
        Set<String> paths(boolean before, @Nullable String lock) {
            Set<String> paths = new TreeSet<>(tree(before).keySet());
            for (String location : manifests(before, lock == null ? "" : lock).keySet()) {
                paths.add(location.isEmpty() ? "package.json" : location + "/package.json");
            }
            if (lock != null) {
                paths.add("bun.lock");
            }
            return paths;
        }

        private Map<String, String> tree(String treeDir) {
            Path root = dir.resolve(treeDir);
            Map<String, String> out = new TreeMap<>();
            if (!Files.isDirectory(root)) {
                return out;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile)
                        .forEach(p -> out.put(root.relativize(p).toString().replace('\\', '/'), read(p)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return out;
        }

        @Nullable
        String config(boolean before, String file, String treeFile) {
            String fromTree = tree(before).get(treeFile);
            if (fromTree != null) {
                return fromTree;
            }
            if (before && has(file + "-before")) {
                return file(file + "-before");
            }
            return file(file);
        }

        /**
         * The project's manifests in a phase by location: {@code ""} the root, a member's path otherwise.
         */
        Map<String, String> manifests(boolean before, String lock) {
            Map<String, String> manifests = new LinkedHashMap<>();
            String root = before ? (has("pkg-before") ? file("pkg-before") : file("pkg-root")) :
                    has("pkg") ? file("pkg") : has("pkg-after") ? file("pkg-after") : file("pkg-root");
            if (root != null) {
                manifests.put("", root);
            }
            for (Map.Entry<String, String> entry : tree(before).entrySet()) {
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
                String phase = m.group(2);
                if (phase != null && !phase.equals(before ? "before" : "after")) {
                    continue;
                }
                if (phase == null && memberFiles.containsKey(m.group(1))) {
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

        List<String> files() {
            try (Stream<Path> list = Files.list(dir)) {
                List<String> names = new ArrayList<>();
                list.filter(Files::isRegularFile).forEach(p -> names.add(p.getFileName().toString()));
                Collections.sort(names);
                return names;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * A recorded registry response: {@code http/<name>} (an abbreviated packument) or {@code http/<name>-<version>}
         * (a manifest), from the scenario, else from the shared cache.
         */
        @Nullable
        String http(String name) {
            Path own = dir.resolve("http").resolve(name);
            if (Files.isRegularFile(own)) {
                return read(own);
            }
            if (httpCache != null && Files.isRegularFile(httpCache.resolve(name))) {
                return read(httpCache.resolve(name));
            }
            return null;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static List<Scenario> discover() {
        Map<String, Scenario> found = new TreeMap<>();
        List<Path> classpathRoots = new ArrayList<>();
        try {
            Enumeration<URL> roots = BunScenarios.class.getClassLoader().getResources("lock/bun");
            while (roots.hasMoreElements()) {
                URL url = roots.nextElement();
                if ("file".equals(url.getProtocol())) {
                    classpathRoots.add(Paths.get(url.toURI()));
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        String cache = System.getenv(HTTP_CACHE_ENV);
        Path httpCache = cache != null && !cache.isEmpty() && Files.isDirectory(Paths.get(cache)) ?
                Paths.get(cache) : null;
        for (Path root : classpathRoots) {
            collect(root, httpCache, found);
        }
        Path corpus = corpusRoot();
        if (corpus != null) {
            collect(corpus, httpCache, found);
        }
        return new ArrayList<>(found.values());
    }

    private static @Nullable Path corpusRoot() {
        String corpus = System.getenv(SCENARIOS_ENV);
        if (corpus == null || corpus.isEmpty() || !Files.isDirectory(Paths.get(corpus))) {
            return null;
        }
        Path dir = Paths.get(corpus);
        if ("bun".equals(String.valueOf(dir.getFileName()))) {
            return dir;
        }
        Path sibling = dir.resolveSibling("bun");
        return Files.isDirectory(sibling) ? sibling : null;
    }

    private static void collect(Path root, @Nullable Path httpCache, Map<String, Scenario> found) {
        try (Stream<Path> walk = Files.walk(root, 1)) {
            walk.filter(Files::isDirectory).forEach(dir -> {
                if (dir.equals(root) || !isScenario(dir)) {
                    return;
                }
                String name = root.relativize(dir).toString().replace('\\', '/');
                String label = found.containsKey(name) ? name + " (" + root + ")" : name;
                found.put(label, new Scenario(name, label, dir, httpCache));
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isScenario(Path dir) {
        for (String lock : LOCKS) {
            if (Files.isRegularFile(dir.resolve(lock))) {
                return true;
            }
        }
        return false;
    }

    private static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
