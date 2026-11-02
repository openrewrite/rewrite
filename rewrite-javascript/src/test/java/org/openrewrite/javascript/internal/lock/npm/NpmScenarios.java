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
import org.openrewrite.javascript.internal.lock.HostInputs;
import org.openrewrite.javascript.internal.lock.HostRegistries;
import org.openrewrite.javascript.internal.lock.LockScenario;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * A scenario's {@code releases} file names the npm releases that write its recorded outcome, and each {@code
 * release/<first>/} holds another outcome with the releases that write it.
 */
final class NpmScenarios {

    private static final Pattern LOCK = Pattern.compile("^(?:before|after|fresh)(?:-(.+))?$");
    private static final Pattern LOCK_FILE = Pattern.compile("^(before|after|fresh|again)(?:-(.+))?$");
    private static final Set<String> NOT_LOCKS = new HashSet<>(Arrays.asList("yarn", "npm", "npmrc"));
    private static final Pattern NPM_VERSION = Pattern.compile("npm (\\d+\\.\\d+\\.\\d+)");
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
            return scenario.isRecordedOutcome() ? resource("lock/npm-again/" + scenario.getName() + "/" + fileName) :
              null;
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
        private final String expectFile;

        Edit(Scenario scenario, String startFile, @Nullable String resultFile, String manifestFile) {
            this(scenario, startFile, resultFile, manifestFile, "expect");
        }

        Edit(Scenario scenario, String startFile, @Nullable String resultFile, String manifestFile,
             String expectFile) {
            this.scenario = scenario;
            this.startFile = startFile;
            this.resultFile = resultFile;
            this.manifestFile = manifestFile;
            this.expectFile = expectFile;
        }

        /**
         * The edit's own {@code expect} line, trimmed; null for a lock byte for byte.
         */
        @Nullable
        String expect() {
            String expect = scenario.file(expectFile);
            return expect == null ? null : expect.trim();
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

    static final class Scenario extends LockScenario {

        /**
         * The start of a release's edit over the lock the recording release wrote: the scenario's own {@code before}.
         */
        static final String RECORDED_BEFORE = "before@recorded";

        // what a scenario is given; every other file is what a release wrote for it
        private static final Set<String> INPUTS = new HashSet<>(Arrays.asList("pkg-before", "pkg", "pkg-after", "npmrc",
          "npmrc-before", "args-before", "args-after", "about", "user-npmrc", "user-npmrc-before", "before-input",
          "supersedes"));

        /**
         * Every root scenarios are discovered under: a scenario's registry responses may sit under its name in
         * another root ({@code <root>/<name>/http/}) when its own directory is read-only.
         */
        @Getter
        private final List<Path> roots;

        /**
         * The release this replay runs as; null for the scenario as it was recorded.
         */
        private final @Nullable NpmRelease replayedAs;

        /**
         * Where the outcome that release writes is kept: the scenario's own directory, or its {@code
         * release/<first>/}, which holds what the release wrote and none of the scenario's inputs.
         */
        private final Path outcome;

        Scenario(String name, String label, Path dir, @Nullable Path httpCache, List<Path> roots) {
            this(name, label, dir, httpCache, roots, null, dir);
        }

        private Scenario(String name, String label, Path dir, @Nullable Path httpCache, List<Path> roots,
                         @Nullable NpmRelease replayedAs, Path outcome) {
            super(name, label, dir, httpCache);
            this.roots = roots;
            this.replayedAs = replayedAs;
            this.outcome = outcome;
        }

        /**
         * The npm release that installs the scenario: the one this replay runs as, else the one that recorded it.
         */
        NpmRelease release() {
            if (replayedAs != null) {
                return replayedAs;
            }
            String recordedWith = recordedWith();
            Matcher m = recordedWith == null ? null : NPM_VERSION.matcher(recordedWith);
            return m != null && m.find() ? NpmRelease.of(m.group(1)) : NpmRelease.PORTED;
        }

        /**
         * Whether the outcome is the scenario's own recording rather than another release's.
         */
        boolean isRecordedOutcome() {
            return outcome.equals(getDir());
        }

        /**
         * This scenario replayed as each release its {@code releases} files name: the first and last of every range
         * of the scenario's own outcome, and of each other outcome under {@code release/}.
         */
        List<Scenario> releases() {
            List<Scenario> replays = new ArrayList<>();
            NpmRelease recorded = release();
            for (NpmRelease release : endpoints(getDir().resolve("releases"))) {
                if (!release.equals(recorded)) {
                    replays.add(replay(release, getDir()));
                }
            }
            Path others = getDir().resolve("release");
            if (Files.isDirectory(others)) {
                try (Stream<Path> list = Files.list(others)) {
                    list.filter(Files::isDirectory).sorted().forEach(dir -> {
                        for (NpmRelease release : endpoints(dir.resolve("releases"))) {
                            replays.add(replay(release, dir));
                        }
                    });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return replays;
        }

        private Scenario replay(NpmRelease release, Path outcome) {
            return new Scenario(getName(), getLabel() + "@" + release, getDir(), getHttpCache(), roots, release,
              outcome);
        }

        // "10.0.0 - 10.9.9" or "12.0.0", one range a line: the first and last release of each, or with
        // REWRITE_LOCK_EVERY_RELEASE every release npm published between them, which proves a range of releases
        private static List<NpmRelease> endpoints(Path releases) {
            List<String> all = new ArrayList<>();
            for (NpmRelease release : NpmReleases.all()) {
                all.add(release.toString());
            }
            List<NpmRelease> endpoints = new ArrayList<>();
            for (String release : LockScenario.releaseEndpoints(releases, all, NpmReleases.PROVEN)) {
                endpoints.add(NpmRelease.of(release));
            }
            return endpoints;
        }

        private Path where(String file) {
            return isRecordedOutcome() || INPUTS.contains(file) ? getDir() : outcome;
        }

        @Override
        public boolean has(String file) {
            return RECORDED_BEFORE.equals(file) ? Files.isRegularFile(getDir().resolve("before")) :
              Files.isRegularFile(where(file).resolve(file));
        }

        @Override
        public @Nullable String file(String file) {
            if (!has(file)) {
                return null;
            }
            return read(RECORDED_BEFORE.equals(file) ? getDir().resolve("before") : where(file).resolve(file));
        }

        @Override
        public List<String> files() {
            List<String> own = super.files();
            if (isRecordedOutcome()) {
                return own;
            }
            List<String> names = new ArrayList<>();
            for (String name : own) {
                if (INPUTS.contains(name)) {
                    names.add(name);
                }
            }
            try (Stream<Path> list = Files.list(outcome)) {
                list.filter(Files::isRegularFile).forEach(p -> names.add(p.getFileName().toString()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            Collections.sort(names);
            return names;
        }

        /**
         * The edits the scenario records: one per {@code before} lock (and its {@code -v2} and similar
         * variants), or from {@code before-input} when that is the only start; for another release's outcome, also
         * its edit over the scenario's own {@code before} ({@code cross}).
         */
        List<Edit> edits() {
            List<Edit> edits = new ArrayList<>();
            List<String> files = files();
            if (!isRecordedOutcome() && (has("cross") || has("expect-cross")) && has(RECORDED_BEFORE)) {
                edits.add(new Edit(this, RECORDED_BEFORE, has("cross") ? "cross" : null, pkgFile(""),
                  "expect-cross"));
            }
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

        /**
         * A recorded registry response: {@code http/<name>} (an abbreviated packument), {@code http/<name>-<version>}
         * (a manifest) or {@code http/<name>@full}, from the scenario, else from the shared cache.
         */
        @Override
        public @Nullable String http(String name) {
            Path own = getDir().resolve("http").resolve(name);
            if (Files.isRegularFile(own)) {
                return read(own);
            }
            for (Path root : roots) {
                Path sibling = root.resolve(getName()).resolve("http").resolve(name);
                if (Files.isRegularFile(sibling)) {
                    return read(sibling);
                }
            }
            Path cache = getHttpCache();
            if (cache != null && Files.isRegularFile(cache.resolve(name))) {
                return read(cache.resolve(name));
            }
            return null;
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
         * The host's side for the user config the edit ran under ({@code user-npmrc}, npm's {@code --userconfig}).
         */
        HostInputs host() {
            return HostRegistries.host(file("user-npmrc"), getDir());
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
    }

    /**
     * Every scenario on the classpath and under {@code REWRITE_LOCK_SCENARIOS}, by name, nested at any depth.
     */
    static List<Scenario> discover() {
        Map<String, Scenario> found = new TreeMap<>();
        List<Path> roots = LockScenario.classpathRoots("npm");
        LockScenario.Factory<Scenario> factory = (name, label, dir, httpCache) ->
                new Scenario(name, label, dir, httpCache, roots);
        Path httpCache = LockScenario.httpCache();
        for (Path root : roots) {
            LockScenario.collect(root, Integer.MAX_VALUE, NpmScenarios::isScenario, httpCache, factory, found);
        }
        Path corpus = LockScenario.corpusDirectory();
        if (corpus != null) {
            LockScenario.collect(corpus, Integer.MAX_VALUE, NpmScenarios::isScenario, httpCache, factory, found);
        }
        return new ArrayList<>(found.values());
    }

    /**
     * Every scenario replayed as each other release its {@code releases} files name.
     */
    static List<Scenario> discoverReleases() {
        List<Scenario> replays = new ArrayList<>();
        for (Scenario scenario : discover()) {
            replays.addAll(scenario.releases());
        }
        return replays;
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
}
