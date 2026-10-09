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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Host;

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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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

    /**
     * A phase of a recording: the before install, the after install with the edit, and a Yarn 4 install that
     * changes nothing over a {@code before} an older Yarn wrote.
     */
    enum Phase {
        BEFORE, REFRESH, AFTER
    }
    private static final Pattern PLATFORM = Pattern.compile("\\(node (\\w+)-(\\w+)\\)");
    private static final Pattern RECORDED_ON = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s*$", Pattern.MULTILINE);
    private static final Pattern RECORDING_REGISTRY = Pattern.compile("^registry: yarn-berry-registry\\.js on " +
                                                                      "(127\\.0\\.0\\.1:\\d+) ", Pattern.MULTILINE);
    private static final String NPM_REGISTRY = "https://registry.npmjs.org/";

    /**
     * A registry that has nothing.
     */
    static final RegistrySource NO_REGISTRY = new RegistrySource() {
        @Override
        public @Nullable String packument(String registry, String name) {
            return null;
        }

        @Override
        public byte @Nullable [] tarball(String url, String name, String version) {
            return null;
        }
    };

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
         * The phase that wrote the lock, or that started from it.
         */
        private final Phase phase;

        Lock(Scenario scenario, String fileName, String text, Phase phase) {
            this.scenario = scenario;
            this.fileName = fileName;
            this.text = text;
            this.phase = phase;
        }

        YarnBerryLockRegenerator.Inputs inputs() {
            return scenario.inputs(phase, text);
        }

        boolean isBefore() {
            return phase == Phase.BEFORE;
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
         * Every lock Yarn wrote or was given: the phase-one seed, the crafted start, before and after, a refresh,
         * a fresh install's, and what a second install rewrote.
         */
        List<Lock> locks() {
            List<Lock> locks = new ArrayList<>();
            String seed = file("lock-seed");
            if (seed != null) {
                locks.add(new Lock(this, "lock-seed", seed, Phase.BEFORE));
            }
            String given = file("before-given");
            if (given != null && !given.equals(file("before"))) {
                locks.add(new Lock(this, "before-given", given, Phase.BEFORE));
            }
            try (Stream<Path> files = Files.list(dir)) {
                files.map(f -> f.getFileName().toString())
                        .filter(f -> f.startsWith("before-") && !f.equals("before-given") &&
                                     !f.equals("before-commands") && !f.equals("before-again"))
                        .sorted()
                        .forEach(f -> locks.add(new Lock(this, f, read(dir.resolve(f)), Phase.BEFORE)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            for (String lock : BEFORE_LOCKS) {
                String text = file(lock);
                if (text != null) {
                    locks.add(new Lock(this, lock, text, Phase.BEFORE));
                }
            }
            String refresh = file("refresh");
            if (refresh != null) {
                locks.add(new Lock(this, "refresh", refresh, Phase.REFRESH));
            }
            for (String lock : AFTER_LOCKS) {
                String text = file(lock);
                if (text != null) {
                    locks.add(new Lock(this, lock, text, Phase.AFTER));
                }
            }
            return locks;
        }

        /**
         * What Yarn had in a phase: the phase's manifests, {@code .yarnrc.yml} and patch files, the recorder's
         * environment, the machine and the day.
         */
        YarnBerryLockRegenerator.Inputs inputs(Phase phase, @Nullable String lock) {
            Map<String, String> configFiles = new LinkedHashMap<>();
            String yarnrc = yarnrc(phase);
            if (yarnrc != null) {
                configFiles.put(BerryOptions.DEFAULT_RC_FILENAME, yarnrc);
            }
            Map<String, String> env = new LinkedHashMap<>();
            if (has("recorded-with")) {
                env.putAll(RECORDER_ENV);
                String registry = recordingRegistry();
                if (registry != null) {
                    env.put("YARN_NPM_REGISTRY_SERVER", registry);
                }
            }
            Map<String, String> patchFiles = new LinkedHashMap<>();
            for (Map.Entry<String, String> file : tree(phase).entrySet()) {
                if (file.getKey().endsWith(".patch")) {
                    patchFiles.put(file.getKey(), file.getValue());
                }
            }
            return YarnBerryLockRegenerator.Inputs.builder()
                    .lock(lock)
                    .manifests(manifests(phase))
                    .configFiles(configFiles)
                    .files(patchFiles)
                    .paths(paths(phase, yarnrc != null))
                    .env(env)
                    .host(host())
                    .now(now())
                    .build();
        }

        /**
         * The registry Yarn was pointed at through the recorder's home {@code .yarnrc.yml}, when it named its port.
         */
        @Nullable
        String recordingRegistry() {
            String recorded = file("recorded-with");
            Matcher m = recorded == null ? null : RECORDING_REGISTRY.matcher(recorded);
            return m != null && m.find() ? "http://" + m.group(1) : null;
        }

        /**
         * The files of the project directory the recorder laid out for a phase; none for a recording made elsewhere.
         */
        @Nullable
        Set<String> paths(Phase phase, boolean yarnrc) {
            if (!has("recorded-with")) {
                return null;
            }
            Set<String> paths = new TreeSet<>(tree(phase).keySet());
            paths.add("package.json");
            paths.add("yarn.lock");
            if (yarnrc) {
                paths.add(BerryOptions.DEFAULT_RC_FILENAME);
            }
            return paths;
        }

        /**
         * The registry as the scenario recorded it: {@code http/<name>} for a packument whatever the registry's
         * url, {@code http/<name>-<version>.tgz} for a tarball. A packument is recorded abbreviated (the downloaded
         * fixtures' with their versions empty) beside the version documents Yarn read as {@code http/<name>-<version>},
         * which stand in for their entries of the full packument Yarn asks for; an empty entry without one takes the
         * shared cache's packument instead, else is a route not recorded. Through a recording registry, the
         * documents carry its url as Yarn saw them.
         */
        ScenarioRegistry registry() {
            return new ScenarioRegistry(this);
        }

        /**
         * The {@code .yarnrc.yml} Yarn ended the phase with: what it wrote, else what the phase was given, else
         * the tree's; the downloaded fixtures name theirs {@code workspace-yarnrc}.
         */
        @Nullable
        String yarnrc(Phase phase) {
            List<String> order = phase == Phase.BEFORE ? Arrays.asList("yarnrc-before.written", "yarnrc") :
                    phase == Phase.REFRESH ?
                            Arrays.asList("yarnrc-refresh.written", "yarnrc-before.written", "yarnrc") :
                            Arrays.asList("yarnrc.written", "yarnrc-after", "yarnrc-before.written", "yarnrc");
            for (String f : order) {
                if (has(f)) {
                    return file(f);
                }
            }
            String tree = tree(phase).get(BerryOptions.DEFAULT_RC_FILENAME);
            return tree != null ? tree : file("workspace-yarnrc");
        }

        /**
         * The project's manifests in a phase by directory: {@code ""} the root, then every package.json of the
         * phase's tree; a downloaded fixture's {@code pkg-<member>} sits at {@code packages/<member>}.
         */
        Map<String, String> manifests(Phase phase) {
            Map<String, String> manifests = new LinkedHashMap<>();
            String root = phase == Phase.BEFORE ? first("pkg-before", "pkg-root") :
                    phase == Phase.REFRESH ? first("pkg-refresh") : first("pkg", "pkg-after", "pkg-root");
            if (root != null) {
                manifests.put("", root);
            }
            for (Map.Entry<String, String> entry : tree(phase).entrySet()) {
                String path = entry.getKey();
                if (path.endsWith("/package.json")) {
                    manifests.put(path.substring(0, path.length() - "/package.json".length()), entry.getValue());
                }
            }
            if (!has("recorded-with")) {
                try (Stream<Path> files = Files.list(dir)) {
                    files.map(f -> f.getFileName().toString())
                            .filter(f -> f.startsWith("pkg-") && !f.equals("pkg-before") && !f.equals("pkg-after") &&
                                         !f.equals("pkg-root") &&
                                         !f.endsWith(phase == Phase.AFTER ? "-before" : "-after"))
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
        Map<String, String> tree(Phase phase) {
            Map<String, String> out = new TreeMap<>(tree("tree-before"));
            if (phase == Phase.AFTER) {
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
         * A registry response of the shared cache.
         */
        byte @Nullable [] cached(String name) {
            try {
                Path file = httpCache == null ? null : httpCache.resolve(name);
                return file != null && Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
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

    /**
     * {@link Scenario#registry()}, with every route it was asked for and does not have.
     */
    static final class ScenarioRegistry implements RegistrySource {
        private static final ObjectMapper JSON = new ObjectMapper();

        private final Scenario scenario;
        final List<String> missing = new ArrayList<>();

        ScenarioRegistry(Scenario scenario) {
            this.scenario = scenario;
        }

        @Override
        public @Nullable String packument(String registry, String name) {
            byte[] body = scenario.http(name);
            if (body == null) {
                missing.add(name);
                return null;
            }
            String text = withRecordedVersions(name, new String(body, StandardCharsets.UTF_8), false);
            byte[] cached = scenario.getHttpCache() == null ? null : scenario.cached(name);
            if (text == null && cached != null) {
                text = withRecordedVersions(name, new String(cached, StandardCharsets.UTF_8), false);
            }
            if (text == null) {
                missing.add(name + "-<version> for the versions recorded empty");
                text = withRecordedVersions(name, new String(body, StandardCharsets.UTF_8), true);
            }
            String recording = scenario.recordingRegistry();
            return recording == null ? text : text.replace(NPM_REGISTRY, recording + "/");
        }

        @Override
        public byte @Nullable [] tarball(String url, String name, String version) {
            String file = name + "-" + version + ".tgz";
            byte[] tgz = scenario.http(file);
            if (tgz == null) {
                missing.add(file);
            }
            return tgz;
        }

        // the packument with each recorded version document in its entry; null when an entry is left empty unless
        // that is acceptable
        private @Nullable String withRecordedVersions(String name, String body, boolean incomplete) {
            Object packument = Js.jsonParse(body);
            Object versions = Js.get(packument, "versions");
            if (!(versions instanceof Map)) {
                return body;
            }
            boolean filled = false;
            int unrecorded = 0;
            @SuppressWarnings("unchecked") Map<String, Object> byVersion = (Map<String, Object>) versions;
            for (Map.Entry<String, Object> entry : byVersion.entrySet()) {
                byte[] manifest = scenario.http(name + "-" + entry.getKey());
                if (manifest != null) {
                    entry.setValue(Js.jsonParse(new String(manifest, StandardCharsets.UTF_8)));
                    filled = true;
                } else if (entry.getValue() instanceof Map && ((Map<?, ?>) entry.getValue()).isEmpty()) {
                    unrecorded++;
                }
            }
            if (unrecorded > 0 && !incomplete) {
                return null;
            }
            if (!filled) {
                return body;
            }
            try {
                return JSON.writeValueAsString(packument);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }

        String describe() {
            return missing.isEmpty() ? "" : " [routes not recorded: " + String.join(", ", missing) + "]";
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
