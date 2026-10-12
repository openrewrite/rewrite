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

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A recorded scenario directory of one package manager's family under {@code lock/<family>/} on the test classpath or
 * the corpus {@code REWRITE_LOCK_SCENARIOS} names, with {@code REWRITE_LOCK_HTTP_CACHE} behind its {@code http/}.
 */
@Getter
public abstract class LockScenario {

    /**
     * The install a lock belongs to: the one from {@code pkg-before}, Yarn Berry's refresh, or the edit's install.
     */
    public enum Phase {
        BEFORE, REFRESH, AFTER
    }

    /**
     * One lock of a scenario, by its file name, with the phase that wrote or read it.
     */
    @Getter
    public abstract static class RecordedLock<S extends LockScenario> {
        protected final S scenario;
        protected final String fileName;
        protected final String text;
        protected final Phase phase;

        protected RecordedLock(S scenario, String fileName, String text, Phase phase) {
            this.scenario = scenario;
            this.fileName = fileName;
            this.text = text;
            this.phase = phase;
        }
    }

    public static final String SCENARIOS_ENV = "REWRITE_LOCK_SCENARIOS";
    public static final String HTTP_CACHE_ENV = "REWRITE_LOCK_HTTP_CACHE";

    /**
     * When set, a scenario is replayed as every release of each range its {@code releases} files name, not only the
     * first and last.
     */
    public static final String EVERY_RELEASE_ENV = "REWRITE_LOCK_EVERY_RELEASE";

    /**
     * When set to a node-semver range, the releases in it are replayed, proven or not; unset, the releases the
     * package manager's port claims to reproduce.
     */
    public static final String RELEASES_ENV = "REWRITE_LOCK_RELEASES";

    private final String name;

    /**
     * The name, plus the root it came from when another root holds a scenario of the same name.
     */
    private final String label;

    private final Path dir;
    private final @Nullable Path httpCache;

    /**
     * The in-repo scenario that re-records this downloaded fixture and names it in its {@code supersedes} file.
     */
    private @Nullable LockScenario supersededBy;

    protected LockScenario(String name, String label, Path dir, @Nullable Path httpCache) {
        this.name = name;
        this.label = label;
        this.dir = dir;
        this.httpCache = httpCache;
    }

    public interface Factory<S extends LockScenario> {
        S create(String name, String label, Path dir, @Nullable Path httpCache);
    }

    public boolean has(String file) {
        return Files.isRegularFile(dir.resolve(file));
    }

    public @Nullable String file(String file) {
        return has(file) ? read(dir.resolve(file)) : null;
    }

    /**
     * The {@code expect} line, trimmed; {@link Expect} reads it.
     */
    public @Nullable String expect() {
        String expect = file("expect");
        return expect == null ? null : expect.trim();
    }

    public @Nullable String recordedWith() {
        return file("recorded-with");
    }

    /**
     * The names of the regular files directly in the scenario directory, sorted.
     */
    public List<String> files() {
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
     * The files of a tree directory of the scenario ({@code tree-before}, {@code tree}) by path relative to it.
     */
    public Map<String, String> tree(String treeDir) {
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
     * A recorded registry response from the scenario's {@code http/}, else from the shared cache.
     */
    public @Nullable String http(String name) {
        byte[] body = httpBytes(name);
        return body == null ? null : new String(body, StandardCharsets.UTF_8);
    }

    public byte @Nullable [] httpBytes(String name) {
        try {
            Path own = dir.resolve("http").resolve(name);
            if (Files.isRegularFile(own)) {
                return Files.readAllBytes(own);
            }
            if (httpCache != null && Files.isRegularFile(httpCache.resolve(name))) {
                return Files.readAllBytes(httpCache.resolve(name));
            }
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Every scenario of a family directly under its classpath roots and its corpus root, by label, with each
     * {@code supersedes} file's downloaded fixtures linked to the re-recording that names them.
     */
    public static <S extends LockScenario> List<S> discover(String family, Predicate<Path> isScenario,
                                                            Factory<S> factory) {
        Map<String, S> found = new TreeMap<>();
        Path httpCache = httpCache();
        for (Path root : classpathRoots(family)) {
            collect(root, 1, isScenario, httpCache, factory, found);
        }
        Path corpus = corpusRoot(family);
        if (corpus != null) {
            collect(corpus, 1, isScenario, httpCache, factory, found);
        }
        for (S scenario : found.values()) {
            String supersedes = scenario.file("supersedes");
            if (supersedes != null) {
                for (String name : supersedes.trim().split("\\s+")) {
                    S superseded = found.get(name);
                    if (superseded != null && superseded.recordedWith() == null) {
                        ((LockScenario) superseded).supersededBy = scenario;
                    }
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    public static List<Path> classpathRoots(String family) {
        List<Path> roots = new ArrayList<>();
        try {
            Enumeration<URL> urls = LockScenario.class.getClassLoader().getResources("lock/" + family);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if ("file".equals(url.getProtocol())) {
                    roots.add(Paths.get(url.toURI()));
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return roots;
    }

    public static @Nullable Path httpCache() {
        return directory(System.getenv(HTTP_CACHE_ENV));
    }

    /**
     * The directory {@code REWRITE_LOCK_SCENARIOS} names, whatever its name.
     */
    public static @Nullable Path corpusDirectory() {
        return directory(System.getenv(SCENARIOS_ENV));
    }

    /**
     * A family's corpus: the directory {@code REWRITE_LOCK_SCENARIOS} names when it is the family's, else its sibling
     * of the family's name.
     */
    public static @Nullable Path corpusRoot(String family) {
        Path dir = corpusDirectory();
        if (dir == null || family.equals(String.valueOf(dir.getFileName()))) {
            return dir;
        }
        Path sibling = dir.resolveSibling(family);
        return Files.isDirectory(sibling) ? sibling : null;
    }

    /**
     * The scenarios under {@code root} down to {@code maxDepth}: each directory that is one and, deeper than its
     * children, lies in none.
     */
    public static <S extends LockScenario> void collect(Path root, int maxDepth, Predicate<Path> isScenario,
                                                        @Nullable Path httpCache, Factory<S> factory,
                                                        Map<String, S> found) {
        try (Stream<Path> walk = Files.walk(root, maxDepth)) {
            walk.filter(Files::isDirectory).forEach(dir -> {
                if (dir.equals(root) || !isScenario.test(dir) || maxDepth > 1 && inScenario(dir, root, isScenario)) {
                    return;
                }
                String name = root.relativize(dir).toString().replace('\\', '/');
                String label = found.containsKey(name) ? name + " (" + root + ")" : name;
                found.put(label, factory.create(name, label, dir, httpCache));
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean inScenario(Path dir, Path root, Predicate<Path> isScenario) {
        for (Path p = dir.getParent(); p != null && !p.equals(root); p = p.getParent()) {
            if (isScenario.test(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The releases a {@code releases} file names, one range a line ({@code 10.0.0 - 10.9.9} or {@code 12.0.0}): the
     * first and last release of each, or with {@link #EVERY_RELEASE_ENV} every release of {@code all} (ascending)
     * between them, which proves a range of releases. Only the releases of {@code proven}, the node-semver range the
     * engine claims, are replayed, unless {@link #RELEASES_ENV} names another range, proven or not.
     */
    public static List<String> releaseEndpoints(Path releases, List<String> all, String proven) {
        boolean every = System.getenv(EVERY_RELEASE_ENV) != null && !System.getenv(EVERY_RELEASE_ENV).isEmpty();
        List<String> endpoints = new ArrayList<>();
        if (Files.isRegularFile(releases)) {
            for (String line : read(releases).split("\n")) {
                String[] range = line.trim().split("\\s+-\\s+");
                if (range[0].isEmpty()) {
                    continue;
                }
                String first = range[0];
                String last = range[range.length - 1];
                for (String release : every ? all : Arrays.asList(first, last)) {
                    if (compareReleases(release, first) >= 0 && compareReleases(release, last) <= 0 &&
                        replays(release, proven) && !endpoints.contains(release)) {
                        endpoints.add(release);
                    }
                }
            }
        }
        return endpoints;
    }

    /**
     * A release a scenario is replayed as, and where the outcome it writes is kept: the scenario's own directory, or
     * its {@code release/<first>/}.
     */
    @Value
    public static class Replay {
        String release;
        Path outcome;
    }

    /**
     * This scenario replayed as each release its {@code releases} files name ({@link #releaseEndpoints}): of the
     * scenario's own outcome every one but {@code recorded}, the release that recorded it, and of each other outcome
     * under {@code release/} every one.
     */
    public List<Replay> releaseReplays(List<String> all, String proven, @Nullable String recorded) {
        List<Replay> replays = new ArrayList<>();
        for (String release : releaseEndpoints(dir.resolve("releases"), all, proven)) {
            if (!release.equals(recorded)) {
                replays.add(new Replay(release, dir));
            }
        }
        Path others = dir.resolve("release");
        if (Files.isDirectory(others)) {
            try (Stream<Path> list = Files.list(others)) {
                list.filter(Files::isDirectory).sorted().forEach(outcome -> {
                    for (String release : releaseEndpoints(outcome.resolve("releases"), all, proven)) {
                        replays.add(new Replay(release, outcome));
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return replays;
    }

    /**
     * A file of an outcome's directory, null where it holds none.
     */
    public static @Nullable String read(Path outcome, String file) {
        Path path = outcome.resolve(file);
        return Files.isRegularFile(path) ? read(path) : null;
    }

    /**
     * Two releases ({@code 4.10.3}) by major, minor and patch.
     */
    public static int compareReleases(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            int c = Integer.compare(Integer.parseInt(x[i]), Integer.parseInt(y[i]));
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    // the releases the engine claims, or the node-semver range REWRITE_LOCK_RELEASES names
    private static boolean replays(String release, String proven) {
        String range = System.getenv(RELEASES_ENV);
        return NodeSemver.satisfies(release, range == null || range.isEmpty() ? proven : range,
                NodeSemver.Options.DEFAULT);
    }

    private static @Nullable Path directory(@Nullable String path) {
        return path != null && !path.isEmpty() && Files.isDirectory(Paths.get(path)) ? Paths.get(path) : null;
    }

    public static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
