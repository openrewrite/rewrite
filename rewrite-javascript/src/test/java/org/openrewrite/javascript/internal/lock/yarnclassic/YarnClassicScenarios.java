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
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The recorded Yarn classic scenarios a test can replay: every directory holding a lock under
 * {@code lock/yarn-classic/} on the test classpath (the in-repo fixtures and the downloaded ones merge there), and,
 * when the environment variable {@code REWRITE_LOCK_SCENARIOS} names a directory, every one under its
 * {@code yarn-classic} sibling, or under the directory itself when it holds Yarn classic scenarios.
 * {@code REWRITE_LOCK_HTTP_CACHE} names a directory of registry responses a scenario's own {@code http/} falls back
 * on.
 * <p>
 * The layout is the one {@code record-yarn-classic.sh} writes: {@code pkg-before}, {@code before}, {@code pkg},
 * {@code after}, a hand-made starting lock {@code before-given} (copied to {@code before}), the project config
 * {@code yarnrc} and {@code npmrc}, the project files {@code tree-before/} and {@code tree/} copied over it before
 * each phase, and {@code http/}. An install that changes nothing over a lock is logged as
 * {@code yarnclassic-again.log} (over {@code after}) or {@code yarnclassic-<lock>-again.log}; when it changed the lock
 * yarn's rewrite is {@code again} or {@code <lock>-again}, and {@code <lock>-forced} is the lock yarn writes for it
 * under {@code --force}. The downloaded fixtures spell the edited manifest {@code pkg-after}.
 */
final class YarnClassicScenarios {

    static final String SCENARIOS_ENV = "REWRITE_LOCK_SCENARIOS";
    static final String HTTP_CACHE_ENV = "REWRITE_LOCK_HTTP_CACHE";
    static final String FAMILY = "yarn-classic";

    private static final List<String> LOCKS = Arrays.asList("before", "after", "again");
    private static final Pattern NODE_VERSION = Pattern.compile("node (v[0-9.]+)");
    private static final Pattern EDIT_STARTED = Pattern.compile("(?m)^edit started (\\S+)$");

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
            return YarnClassicLockRegenerator.Inputs.builder()
                    .lock(text)
                    .manifests(manifests())
                    .yarnrc(scenario.file("yarnrc"))
                    .npmrc(scenario.file("npmrc"))
                    .env(new LinkedHashMap<>())
                    .now(scenario.now())
                    .build();
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
         * Every lock yarn wrote or was given: {@code before}, {@code after}, and yarn's rewrite of {@code after} on an
         * install that changes nothing ({@code again}).
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
            return locks;
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

        /**
         * A recorded registry response: {@code http/<name>} (a packument) or {@code http/<name>-<version>} (a
         * version manifest), from the scenario, else from the shared cache.
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
            Enumeration<URL> roots = YarnClassicScenarios.class.getClassLoader().getResources("lock/" + FAMILY);
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
        if (FAMILY.equals(String.valueOf(dir.getFileName()))) {
            return dir;
        }
        Path sibling = dir.resolveSibling(FAMILY);
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

    static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
