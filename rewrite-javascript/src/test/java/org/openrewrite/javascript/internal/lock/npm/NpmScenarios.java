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
import org.openrewrite.javascript.internal.lock.Host;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The recorded npm scenarios under {@code lock/npm/} on the test classpath and under the directory {@code
 * REWRITE_LOCK_SCENARIOS} names, in the recorder's layout; {@code REWRITE_LOCK_HTTP_CACHE} is the registry fallback.
 */
final class NpmScenarios {

    static final String SCENARIOS_ENV = "REWRITE_LOCK_SCENARIOS";
    static final String HTTP_CACHE_ENV = "REWRITE_LOCK_HTTP_CACHE";

    private static final Pattern LOCK = Pattern.compile("^(?:before|after|fresh)(?:-(.+))?$");
    private static final Pattern LOCK_FILE = Pattern.compile("^(before|after|fresh|again)(?:-(.+))?$");
    private static final Set<String> NOT_LOCKS = new HashSet<>(Arrays.asList("yarn", "npm", "npmrc"));
    private static final Pattern NODE_VERSION = Pattern.compile("node (v[0-9.]+)");
    private static final Pattern RECORDED_AT = Pattern.compile("after exit=-?\\d+ at ([^,\\s]+)");
    private static final Pattern PLATFORM = Pattern.compile("platform os=(\\S+) cpu=(\\S+) libc=(\\S*)");

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

        Lock(Scenario scenario, String fileName, String text, @Nullable String manifestFileName,
             @Nullable String manifest,
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
         * The project's manifests by location: the root's, when held, and every package.json of the tree directory; the
         * oldest layout's lone member manifest is placed where the lock links a package of its name.
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
         * What npm writes installing this lock's manifest over it again, when that differs: {@code again}, {@code
         * after} for an edit that changes nothing, {@code <lock>-again}, or {@code lock/npm-again/<scenario>/<lock>}.
         */
        @Nullable
        String rewritten() {
            if ("after".equals(fileName) && scenario.has("again")) {
                return scenario.file("again");
            }
            if ("before".equals(fileName) && scenario.has("after") && scenario.pkg() != null &&
                scenario.pkg().equals(scenario.pkgBefore()) && scenario.tree("tree")
                  .equals(scenario.tree("tree-before"))) {
                return scenario.after();
            }
            if (scenario.has(fileName + "-again")) {
                return scenario.file(fileName + "-again");
            }
            return resource("lock/npm-again/" + scenario.getName() + "/" + fileName);
        }

        /**
         * Whether the second install's rewrite adds a packages key the lock lacks: it placed a node, which is
         * the ideal tree builder's to reproduce rather than the data's.
         */
        boolean rewritePlacesNode() {
            String rewritten = rewritten();
            if (rewritten == null) {
                return false;
            }
            Set<String> added = packageKeys(rewritten);
            added.removeAll(packageKeys(text));
            return !added.isEmpty();
        }

        private static Set<String> packageKeys(String lock) {
            Object value = parse(lock);
            Object packages = value instanceof Map ? Js.get(value, "packages") : null;
            return packages instanceof Map ? new LinkedHashSet<>(Js.keys(packages)) : new LinkedHashSet<>();
        }

        /**
         * The error npm raised loading this lock when it installed its manifest over it again: {@code npm-again.log}'s
         * exit 1 with an error without a code (a coded rejection is the builder's, and the lock still loads).
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
            return scenario.getLabel() + "/" + fileName;
        }
    }

    /**
     * One edit of a scenario: the lock it starts from, the manifests after the edit, and the lock npm wrote
     * for it ({@code result}), absent when npm failed.
     */
    @Getter
    static final class Edit {
        private final Scenario scenario;
        private final String startFile;
        private final @Nullable String resultFile;
        private final String manifestFile;

        Edit(Scenario scenario, String startFile, @Nullable String resultFile, String manifestFile) {
            this.scenario = scenario;
            this.startFile = startFile;
            this.resultFile = resultFile;
            this.manifestFile = manifestFile;
        }

        // the locks are read on demand: holding every corpus edit's text at once does not fit a test JVM
        Lock getStart() {
            return scenario.lock(startFile, "pkg-before", "tree-before", scenario.treeYarn("tree-before"),
              scenario.beforeNpmrc());
        }

        @Nullable
        Lock getResult() {
            return resultFile == null ? null : inputs();
        }

        /**
         * The lock npm wrote, or, when it failed, the starting lock standing in for it, with the manifests,
         * tree, yarn.lock and npmrc of the edit.
         */
        Lock getInputs() {
            return inputs();
        }

        private Lock inputs() {
            String yarn = scenario.has("before-yarn") ? scenario.file("before-yarn") : scenario.treeYarn("tree");
            Lock lock = scenario.lock(resultFile != null ? resultFile : startFile, manifestFile, "tree", yarn,
              scenario.npmrc());
            return resultFile != null ? lock : new Lock(scenario, "after", lock.getText(), lock.getManifestFileName(),
              lock.getManifest(), lock.getTree(), yarn, scenario.npmrc());
        }

        /**
         * The project's manifests after the edit, by location.
         */
        Map<String, String> manifests() {
            return inputs().manifests();
        }

        @Override
        public String toString() {
            return scenario.getLabel() + "/" + startFile;
        }
    }

    @Getter
    static final class Scenario {
        private final String name;

        /**
         * The name, plus the root it came from when another root holds a scenario of the same name.
         */
        private final String label;
        private final Path dir;
        private final @Nullable Path httpCache;

        /**
         * Every root scenarios are discovered under: a scenario's registry responses may sit under its name in
         * another root ({@code <root>/<name>/http/}) when its own directory is read-only.
         */
        private final List<Path> roots;

        Scenario(String name, String label, Path dir, @Nullable Path httpCache, List<Path> roots) {
            this.name = name;
            this.label = label;
            this.dir = dir;
            this.httpCache = httpCache;
            this.roots = roots;
        }

        /**
         * The edits the scenario records: one per {@code before} lock (and its {@code -v2} and similar
         * variants), or from {@code before-input} when that is the only start.
         */
        List<Edit> edits() {
            List<Edit> edits = new ArrayList<>();
            List<String> files = files();
            for (String start : files) {
                Matcher m = LOCK_FILE.matcher(start);
                if (!m.matches() || !"before".equals(m.group(1)) || "before-again".equals(start) ||
                    m.group(2) != null && NOT_LOCKS.contains(m.group(2))) {
                    continue;
                }
                boolean input = "before-input".equals(start);
                if (input && has("before")) {
                    continue;
                }
                String suffix = input ? "" : start.substring("before".length());
                String result = files.contains("after" + suffix) ? "after" + suffix : null;
                String manifestFile = pkgFile(suffix);
                if (result == null && !has(manifestFile)) {
                    // a lock with neither an edited manifest nor a recorded result is not an edit
                    continue;
                }
                edits.add(new Edit(this, start, result, manifestFile));
            }
            return edits;
        }

        /**
         * The extra command-line arguments of the edit, such as {@code --os=linux}, as npmrc lines.
         */
        String argsAfter() {
            String args = file("args-after");
            if (args == null) {
                return "";
            }
            StringBuilder lines = new StringBuilder();
            String[] tokens = args.trim().split("\\s+");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (!token.startsWith("--")) {
                    continue;
                }
                String key = token.substring(2);
                int eq = key.indexOf('=');
                if (eq >= 0) {
                    lines.append(key, 0, eq).append('=').append(key.substring(eq + 1)).append('\n');
                } else if (key.startsWith("no-")) {
                    lines.append(key.substring(3)).append("=false\n");
                } else if (i + 1 < tokens.length && !tokens[i + 1].startsWith("--")) {
                    lines.append(key).append('=').append(tokens[++i]).append('\n');
                } else {
                    lines.append(key).append("=true\n");
                }
            }
            return lines.toString();
        }

        /**
         * When the edit was recorded ({@code after exit=0 at <instant>} in {@code recorded-with}), for the
         * clock {@code min-release-age} counts from.
         */
        @Nullable
        Instant recordedAt() {
            String recordedWith = recordedWith();
            if (recordedWith == null) {
                return null;
            }
            Matcher m = RECORDED_AT.matcher(recordedWith);
            return m.find() ? Instant.parse(m.group(1)) : null;
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
                walk.filter(Files::isRegularFile).forEach(p -> out.put(root.relativize(p).toString().replace('\\', '/'),
                                                                       read(p)));
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
            for (Path root : roots) {
                Path sibling = root.resolve(this.name).resolve("http").resolve(name);
                if (Files.isRegularFile(sibling)) {
                    return read(sibling);
                }
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

        /**
         * The machine the scenario was recorded on, when {@code recorded-with} says ({@code platform os=darwin
         * cpu=arm64 libc=}).
         */
        @Nullable
        Host platform() {
            String recordedWith = recordedWith();
            Matcher m = recordedWith == null ? null : PLATFORM.matcher(recordedWith);
            if (m == null || !m.find()) {
                return null;
            }
            return Host.builder().os(m.group(1)).cpu(m.group(2)).libc(m.group(3).isEmpty() ? null : m.group(3)).build();
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

        /** Every lock the scenario holds, each with the manifest, tree, yarn.lock and npmrc of its phase. */
        List<Lock> locks() {
            List<Lock> locks = new ArrayList<>();
            for (String file : files()) {
                Matcher m = LOCK_FILE.matcher(file);
                if (!m.matches() || (m.group(2) != null && NOT_LOCKS.contains(m.group(2)) &&
                                     !"input".equals(m.group(2)))) {
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
                    String yarn = has("before-yarn") ? file("before-yarn") : treeYarn("tree");
                    locks.add(lock(file, pkgFile(suffix), "tree", yarn, npmrc()));
                }
            }
            return locks;
        }

        // the manifest after the edit: its variant's own, else pkg, else the read-only fixtures' pkg-after
        private String pkgFile(String suffix) {
            if (!suffix.isEmpty() && has("pkg" + suffix)) {
                return "pkg" + suffix;
            }
            return has("pkg") ? "pkg" : "pkg-after";
        }

        private @Nullable String beforeNpmrc() {
            return has("npmrc-before") ? file("npmrc-before") : npmrc();
        }

        private @Nullable String treeYarn(String treeDir) {
            String yarn = tree("tree-before").get("yarn.lock");
            return "tree".equals(treeDir) && tree("tree").containsKey("yarn.lock") ? tree("tree").get("yarn.lock") :
              yarn;
        }

        private Lock lock(String file, String manifestFile, String treeDir, @Nullable String yarnLock,
                          @Nullable String npmrc) {
            Map<String, String> tree = new TreeMap<>(tree("tree-before"));
            if ("tree".equals(treeDir)) {
                tree.putAll(tree("tree"));
            }
            return new Lock(this, file, file(file), has(manifestFile) ? manifestFile : null, file(manifestFile), tree,
                            yarnLock, npmrc);
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
        List<Path> classpathRoots = new ArrayList<>();
        try {
            Enumeration<URL> roots = NpmScenarios.class.getClassLoader().getResources("lock/npm");
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
            collect(root, httpCache, classpathRoots, found);
        }
        String corpus = System.getenv(SCENARIOS_ENV);
        if (corpus != null && !corpus.isEmpty() && Files.isDirectory(Paths.get(corpus))) {
            collect(Paths.get(corpus), httpCache, classpathRoots, found);
        }
        return new ArrayList<>(found.values());
    }

    private static void collect(Path root, @Nullable Path httpCache, List<Path> roots, Map<String, Scenario> found) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isDirectory).forEach(dir -> {
                if (dir.equals(root) || !isScenario(dir) || inScenario(dir, root)) {
                    return;
                }
                String name = root.relativize(dir).toString().replace('\\', '/');
                String label = found.containsKey(name) ? name + " (" + root + ")" : name;
                found.put(label, new Scenario(name, label, dir, httpCache, roots));
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // a directory holding a lock: before, after, fresh, a variant of one, or a hand-shaped before-input
    private static boolean isScenario(Path dir) {
        try (Stream<Path> list = Files.list(dir)) {
            return list.anyMatch(p -> {
                Matcher m = LOCK.matcher(p.getFileName().toString());
                return Files.isRegularFile(p) && m.matches() && (m.group(1) == null || !NOT_LOCKS.contains(m.group(1)));
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
