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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.npm.Host;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
 * The recorded Yarn Berry scenarios under {@code lock/yarn-berry/} on the test classpath (the committed set and
 * the downloaded fixtures) and, when {@code REWRITE_LOCK_SCENARIOS} names a directory, under its
 * {@code yarn-berry} sibling or itself.
 */
final class BerryScenarios {

    static final String SCENARIOS_ENV = "REWRITE_LOCK_SCENARIOS";
    static final String HTTP_CACHE_ENV = "REWRITE_LOCK_HTTP_CACHE";
    static final String FAMILY = "yarn-berry";

    /**
     * The locks the after phase wrote or would write, and those the before phase did.
     */
    private static final List<String> AFTER_LOCKS = Arrays.asList("after", "fresh", "again");
    private static final List<String> BEFORE_LOCKS = Arrays.asList("before", "before-again");
    private static final Pattern PLATFORM = Pattern.compile("\\(node (\\w+)-(\\w+)\\)");
    private static final Pattern RECORDED_ON = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s*$", Pattern.MULTILINE);

    /**
     * The environment every recorder here runs Yarn under.
     */
    private static final Map<String, String> RECORDER_ENV = new LinkedHashMap<>();

    static {
        RECORDER_ENV.put("YARN_ENABLE_TELEMETRY", "0");
        RECORDER_ENV.put("YARN_ENABLE_GLOBAL_CACHE", "false");
        RECORDER_ENV.put("YARN_ENABLE_IMMUTABLE_INSTALLS", "false");
        RECORDER_ENV.put("YARN_ENABLE_PROGRESS_BARS", "false");
        RECORDER_ENV.put("YARN_ENABLE_COLORS", "false");
    }

    private BerryScenarios() {
    }

    /**
     * One lock of a scenario with what Yarn had in the phase that wrote or read it.
     */
    @Getter
    static final class Lock {
        private final Scenario scenario;
        private final String fileName;
        private final String text;

        /**
         * Whether the lock belongs to the before phase (or is that phase's starting lock).
         */
        private final boolean before;

        Lock(Scenario scenario, String fileName, String text, boolean before) {
            this.scenario = scenario;
            this.fileName = fileName;
            this.text = text;
            this.before = before;
        }

        BerryLockRegenerator.Inputs inputs() {
            return scenario.inputs(before, text);
        }

        /**
         * Whether the scenario holds the manifests this lock was written for: a downloaded fixture's second start
         * ({@code before-<suffix>}) comes with none of its own.
         */
        boolean manifestsRecorded() {
            if (!fileName.startsWith("before-") || "before-given".equals(fileName) || "before-again".equals(fileName) ||
                scenario.has("recorded-with")) {
                return true;
            }
            String suffix = fileName.substring("before-".length());
            try (Stream<Path> files = Files.list(scenario.getDir())) {
                return files.anyMatch(f -> f.getFileName().toString().startsWith("pkg") &&
                                           f.getFileName().toString().endsWith("-" + suffix));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
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
         * Every lock Yarn wrote or was given: the phase-one seed, the crafted start, before and after, a fresh
         * install's, and what a second install rewrote.
         */
        List<Lock> locks() {
            List<Lock> locks = new ArrayList<>();
            String seed = file("lock-seed");
            if (seed != null) {
                locks.add(new Lock(this, "lock-seed", seed, true));
            }
            String given = file("before-given");
            if (given != null && !given.equals(file("before"))) {
                locks.add(new Lock(this, "before-given", given, true));
            }
            try (Stream<Path> files = Files.list(dir)) {
                files.map(f -> f.getFileName().toString())
                        .filter(f -> f.startsWith("before-") && !f.equals("before-given") &&
                                     !f.equals("before-commands") && !f.equals("before-again"))
                        .sorted()
                        .forEach(f -> locks.add(new Lock(this, f, read(dir.resolve(f)), true)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            for (String lock : BEFORE_LOCKS) {
                String text = file(lock);
                if (text != null) {
                    locks.add(new Lock(this, lock, text, true));
                }
            }
            for (String lock : AFTER_LOCKS) {
                String text = file(lock);
                if (text != null) {
                    locks.add(new Lock(this, lock, text, false));
                }
            }
            return locks;
        }

        /**
         * What Yarn had in a phase: the phase's manifests and {@code .yarnrc.yml}, the recorder's environment, the
         * machine and the day.
         */
        BerryLockRegenerator.Inputs inputs(boolean before, @Nullable String lock) {
            Map<String, String> configFiles = new LinkedHashMap<>();
            String yarnrc = yarnrc(before);
            if (yarnrc != null) {
                configFiles.put(BerryOptions.DEFAULT_RC_FILENAME, yarnrc);
            }
            return BerryLockRegenerator.Inputs.builder()
                    .lock(lock)
                    .manifests(manifests(before))
                    .configFiles(configFiles)
                    .env(has("recorded-with") ? RECORDER_ENV : new LinkedHashMap<>())
                    .host(host())
                    .now(now())
                    .build();
        }

        /**
         * The {@code .yarnrc.yml} Yarn ended the phase with: what it wrote, else what the phase was given, else
         * the tree's; the downloaded fixtures name theirs {@code workspace-yarnrc}.
         */
        @Nullable
        String yarnrc(boolean before) {
            List<String> order = before ? Arrays.asList("yarnrc-before.written", "yarnrc") :
                    Arrays.asList("yarnrc.written", "yarnrc-after", "yarnrc-before.written", "yarnrc");
            for (String f : order) {
                if (has(f)) {
                    return file(f);
                }
            }
            String tree = tree(before).get(BerryOptions.DEFAULT_RC_FILENAME);
            return tree != null ? tree : file("workspace-yarnrc");
        }

        /**
         * The project's manifests in a phase by directory: {@code ""} the root, then every package.json of the
         * phase's tree; a downloaded fixture's {@code pkg-<member>} sits at {@code packages/<member>}.
         */
        Map<String, String> manifests(boolean before) {
            Map<String, String> manifests = new LinkedHashMap<>();
            String root = before ? first("pkg-before", "pkg-root") : first("pkg", "pkg-after", "pkg-root");
            if (root != null) {
                manifests.put("", root);
            }
            for (Map.Entry<String, String> entry : tree(before).entrySet()) {
                String path = entry.getKey();
                if (path.endsWith("/package.json")) {
                    manifests.put(path.substring(0, path.length() - "/package.json".length()), entry.getValue());
                }
            }
            if (!has("recorded-with")) {
                try (Stream<Path> files = Files.list(dir)) {
                    files.map(f -> f.getFileName().toString())
                            .filter(f -> f.startsWith("pkg-") && !f.equals("pkg-before") && !f.equals("pkg-after") &&
                                         !f.equals("pkg-root") && !f.endsWith(before ? "-after" : "-before"))
                            .sorted()
                            .forEach(f -> manifests.put("packages/" + f.substring(4).replaceAll("-(before|after)$", ""),
                                    read(dir.resolve(f))));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return manifests;
        }

        private @Nullable String first(String... files) {
            for (String f : files) {
                if (has(f)) {
                    return file(f);
                }
            }
            return null;
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

        Host host() {
            String recorded = file("recorded-with");
            if (recorded != null) {
                Matcher m = PLATFORM.matcher(recorded);
                if (m.find()) {
                    return Host.builder().os(m.group(1)).cpu(m.group(2)).build();
                }
            }
            return Host.builder().os("darwin").cpu("arm64").build();
        }

        Instant now() {
            String recorded = file("recorded-with");
            if (recorded != null) {
                Matcher m = RECORDED_ON.matcher(recorded.split("\n")[0]);
                if (m.find()) {
                    return LocalDate.parse(m.group(1)).atStartOfDay().toInstant(ZoneOffset.UTC);
                }
            }
            return Instant.now();
        }

        /**
         * A recorded registry response from the scenario's {@code http/}, else the shared cache.
         */
        @Nullable
        byte[] http(String name) {
            Path own = dir.resolve("http").resolve(name);
            try {
                if (Files.isRegularFile(own)) {
                    return Files.readAllBytes(own);
                }
                if (httpCache != null && Files.isRegularFile(httpCache.resolve(name))) {
                    return Files.readAllBytes(httpCache.resolve(name));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
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
            Enumeration<URL> roots = BerryScenarios.class.getClassLoader().getResources("lock/" + FAMILY);
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
        return Files.isRegularFile(dir.resolve("before")) || Files.isRegularFile(dir.resolve("after")) ||
               Files.isRegularFile(dir.resolve("before-given"));
    }

    static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
