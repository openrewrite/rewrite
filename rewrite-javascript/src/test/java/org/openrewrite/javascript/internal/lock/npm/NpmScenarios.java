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
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The recorded npm scenarios a test can replay: every scenario directory under {@code lock/npm/} on the
 * test classpath (the in-repo fixtures and the downloaded ones merge there), and every one under the
 * directory the environment variable {@code REWRITE_LOCK_SCENARIOS} names. {@code REWRITE_LOCK_HTTP_CACHE}
 * names a directory of registry responses a scenario without its own {@code http/} falls back on.
 */
final class NpmScenarios {

    static final String SCENARIOS_ENV = "REWRITE_LOCK_SCENARIOS";
    static final String HTTP_CACHE_ENV = "REWRITE_LOCK_HTTP_CACHE";

    private static final Set<String> MARKERS = new HashSet<>(Arrays.asList(
            "before", "after", "before-input", "pkg", "pkg-after", "pkg-before", "fresh"));
    private static final Pattern LOCK_FILE = Pattern.compile("^(before|after|fresh|again)(?:-(.+))?$");
    private static final Set<String> NOT_LOCKS = new HashSet<>(Arrays.asList("yarn", "npm", "npmrc"));
    private static final Pattern NODE_VERSION = Pattern.compile("node (v[0-9.]+)");

    private NpmScenarios() {
    }

    /**
     * One lock of a scenario with the inputs npm had when it wrote it: the manifest of the root and of
     * the workspace members ({@code tree-before/} or {@code tree/}), the yarn.lock beside it, the npmrc.
     */
    @Getter
    static final class Lock {
        private final Scenario scenario;
        private final String fileName;
        private final String text;
        private final @Nullable String manifestFileName;
        private final @Nullable String manifest;
        private final Map<String, String> tree;
        private final @Nullable String yarnLock;
        private final @Nullable String npmrc;

        Lock(Scenario scenario, String fileName, String text, @Nullable String manifestFileName, @Nullable String manifest,
             Map<String, String> tree, @Nullable String yarnLock, @Nullable String npmrc) {
            this.scenario = scenario;
            this.fileName = fileName;
            this.text = text;
            this.manifestFileName = manifestFileName;
            this.manifest = manifest;
            this.tree = tree;
            this.yarnLock = yarnLock;
            this.npmrc = npmrc;
        }

        boolean isBefore() {
            return fileName.startsWith("before");
        }

        /**
         * A hand-shaped starting lock rather than one npm wrote.
         */
        boolean isInput() {
            return "before-input".equals(fileName);
        }

        /**
         * The project's manifests by location, as {@link VirtualTreeLoader.ProjectFiles} takes them: the
         * root's (when the scenario holds it) and every package.json of the tree directory. In the oldest
         * layout a workspace scenario holds only the edited member's manifest; it is placed where the
         * lock links a package of its name.
         */
        Map<String, String> manifests() {
            Map<String, String> manifests = new LinkedHashMap<>();
            if (manifest != null) {
                String memberLocation = memberLocation(manifest);
                manifests.put(memberLocation == null ? "" : memberLocation, manifest);
            }
            for (Map.Entry<String, String> entry : tree.entrySet()) {
                String path = entry.getKey();
                if (path.equals("package.json") && !manifests.containsKey("")) {
                    manifests.put("", entry.getValue());
                } else if (path.endsWith("/package.json")) {
                    manifests.put(path.substring(0, path.length() - "/package.json".length()), entry.getValue());
                }
            }
            return manifests;
        }

        @SuppressWarnings("unchecked")
        private @Nullable String memberLocation(String manifest) {
            Object lock = parse(text);
            Object pkg = parse(manifest);
            if (!(lock instanceof Map) || !(pkg instanceof Map)) {
                return null;
            }
            Object name = ((Map<String, Object>) pkg).get("name");
            Object packages = ((Map<String, Object>) lock).get("packages");
            if (!(name instanceof String) || !(packages instanceof Map)) {
                return null;
            }
            Object root = ((Map<String, Object>) packages).get("");
            if (root instanceof Map && name.equals(((Map<String, Object>) root).get("name"))) {
                return null;
            }
            Object link = ((Map<String, Object>) packages).get("node_modules/" + name);
            if (link instanceof Map && Boolean.TRUE.equals(((Map<String, Object>) link).get("link")) &&
                ((Map<String, Object>) link).get("resolved") instanceof String) {
                return (String) ((Map<String, Object>) link).get("resolved");
            }
            return null;
        }

        /**
         * What npm writes when it installs this lock's manifest over it again, when that differs from the
         * lock: the corpus records {@code again} beside {@code after} (with {@code npm-again.log}), a scenario
         * whose edit changes nothing records it as {@code after}, a committed scenario as {@code <lock>-again},
         * and a read-only fixture has it under {@code lock/npm-again/<scenario>/<lock>}.
         */
        @Nullable
        String rewritten() {
            if ("after".equals(fileName) && scenario.has("again")) {
                return scenario.file("again");
            }
            if ("before".equals(fileName) && scenario.has("after") && scenario.pkg() != null &&
                scenario.pkg().equals(scenario.pkgBefore()) && scenario.tree("tree").equals(scenario.tree("tree-before"))) {
                return scenario.after();
            }
            if (scenario.has(fileName + "-again")) {
                return scenario.file(fileName + "-again");
            }
            return resource("lock/npm-again/" + scenario.getName() + "/" + fileName);
        }

        /**
         * The error npm raised while loading this lock as data when it installed the lock's manifest over
         * it again: {@code npm-again.log} records exit 1 with an error that carries no code (a rejection
         * with a code, such as {@code ERESOLVE}, is the builder's and the lock still loads).
         */
        @Nullable
        String loadError() {
            String log = "after".equals(fileName) ? scenario.file("npm-again.log") : null;
            if (log == null || !Arrays.asList(log.split("\n")).contains("exit=1")) {
                return null;
            }
            String first = log.split("\n")[0];
            return first.startsWith("npm error ") && !first.startsWith("npm error code ") ?
              first.substring("npm error ".length()) : null;
        }

        @Override
        public String toString() {
            return scenario.getName() + "/" + fileName;
        }
    }

    @Getter
    static final class Scenario {
        private final String name;
        private final Path dir;
        private final @Nullable Path httpCache;

        Scenario(String name, Path dir, @Nullable Path httpCache) {
            this.name = name;
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

        /**
         * The names of the regular files directly in the scenario directory, sorted.
         */
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
         * The files of a tree directory ({@code tree-before} or {@code tree}) by path relative to it.
         */
        Map<String, String> tree(String name) {
            Path root = dir.resolve(name);
            Map<String, String> out = new TreeMap<>();
            if (!Files.isDirectory(root)) {
                return out;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile).forEach(p -> out.put(root.relativize(p).toString().replace('\\', '/'), read(p)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return out;
        }

        /**
         * A recorded registry response: {@code http/<name>} (an abbreviated packument), {@code http/<name>-<version>}
         * (a manifest) or {@code http/<name>@full}, from the scenario, else from the shared cache.
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

        @Nullable
        String expect() {
            String expect = file("expect");
            return expect == null ? null : expect.trim();
        }

        @Nullable
        String recordedWith() {
            return file("recorded-with");
        }

        /**
         * The Node version npm ran under, from {@code recorded-with}, such as {@code v24.19.0}.
         */
        @Nullable
        String nodeVersion() {
            String recordedWith = recordedWith();
            if (recordedWith == null) {
                return null;
            }
            Matcher m = NODE_VERSION.matcher(recordedWith);
            return m.find() ? m.group(1) : null;
        }

        @Nullable
        String npmrc() {
            return file("npmrc");
        }

        /**
         * The manifest after the edit: {@code pkg}, or {@code pkg-after} in the older layout.
         */
        @Nullable
        String pkg() {
            return has("pkg") ? file("pkg") : file("pkg-after");
        }

        @Nullable
        String pkgBefore() {
            return file("pkg-before");
        }

        @Nullable
        String before() {
            return file("before");
        }

        @Nullable
        String after() {
            return file("after");
        }

        /**
         * Every lock the scenario holds ({@code before}, {@code after}, {@code fresh}, {@code again},
         * {@code before-input} and their {@code -v2}, {@code -dev}, {@code -reshape} variants), each with
         * the manifest, tree, yarn.lock and npmrc of its phase.
         */
        List<Lock> locks() {
            List<Lock> locks = new ArrayList<>();
            for (String file : files()) {
                Matcher m = LOCK_FILE.matcher(file);
                if (!m.matches() || (m.group(2) != null && NOT_LOCKS.contains(m.group(2)) && !"input".equals(m.group(2)))) {
                    continue;
                }
                if ("before-input".equals(file)) {
                    locks.add(lock(file, "pkg-before", "tree-before", treeYarn("tree-before"), beforeNpmrc()));
                    continue;
                }
                String suffix = m.group(2) == null ? "" : "-" + m.group(2);
                if ("before".equals(m.group(1))) {
                    String manifestFile = has("pkg-before" + suffix) ? "pkg-before" + suffix : "pkg-before";
                    locks.add(lock(file, manifestFile, "tree-before", treeYarn("tree-before"), beforeNpmrc()));
                } else {
                    String manifestFile = has("pkg-after" + suffix) ? "pkg-after" + suffix : has("pkg-after") ? "pkg-after" : "pkg";
                    String yarn = has("before-yarn") ? file("before-yarn") : treeYarn("tree");
                    locks.add(lock(file, manifestFile, "tree", yarn, npmrc()));
                }
            }
            return locks;
        }

        private @Nullable String beforeNpmrc() {
            return has("npmrc-before") ? file("npmrc-before") : has("before-npmrc") ? file("before-npmrc") : npmrc();
        }

        private @Nullable String treeYarn(String treeDir) {
            String yarn = tree("tree-before").get("yarn.lock");
            return "tree".equals(treeDir) && tree("tree").containsKey("yarn.lock") ? tree("tree").get("yarn.lock") : yarn;
        }

        private Lock lock(String file, String manifestFile, String treeDir, @Nullable String yarnLock, @Nullable String npmrc) {
            Map<String, String> tree = new TreeMap<>(tree("tree-before"));
            if ("tree".equals(treeDir)) {
                tree.putAll(tree("tree"));
            }
            return new Lock(this, file, file(file), has(manifestFile) ? manifestFile : null, file(manifestFile), tree, yarnLock, npmrc);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static @Nullable Object parse(String json) {
        try {
            return JsonParseEvenBetterErrors.parse(json).getValue();
        } catch (JsonParseEvenBetterErrors.JsonParseException e) {
            return null;
        }
    }

    private static @Nullable String resource(String path) {
        try (InputStream in = NpmScenarios.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Every scenario on the classpath and under {@code REWRITE_LOCK_SCENARIOS}, by name.
     */
    static List<Scenario> discover() {
        Map<String, Scenario> found = new TreeMap<>();
        try {
            Enumeration<URL> roots = NpmScenarios.class.getClassLoader().getResources("lock/npm");
            while (roots.hasMoreElements()) {
                URL url = roots.nextElement();
                if ("file".equals(url.getProtocol())) {
                    collect(Paths.get(url.toURI()), null, found);
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        String corpus = System.getenv(SCENARIOS_ENV);
        if (corpus != null && !corpus.isEmpty() && Files.isDirectory(Paths.get(corpus))) {
            String cache = System.getenv(HTTP_CACHE_ENV);
            Path httpCache = cache != null && !cache.isEmpty() && Files.isDirectory(Paths.get(cache)) ? Paths.get(cache) : null;
            collect(Paths.get(corpus), httpCache, found);
        }
        return new ArrayList<>(found.values());
    }

    private static void collect(Path root, @Nullable Path httpCache, Map<String, Scenario> found) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isDirectory).forEach(dir -> {
                if (dir.equals(root) || !isScenario(dir) || inScenario(dir, root)) {
                    return;
                }
                String name = root.relativize(dir).toString().replace('\\', '/');
                found.put(name, new Scenario(name, dir, httpCache));
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isScenario(Path dir) {
        for (String marker : MARKERS) {
            if (Files.isRegularFile(dir.resolve(marker))) {
                return true;
            }
        }
        return false;
    }

    private static boolean inScenario(Path dir, Path root) {
        for (Path p = dir.getParent(); p != null && !p.equals(root); p = p.getParent()) {
            if (isScenario(p)) {
                return true;
            }
        }
        return false;
    }
}
