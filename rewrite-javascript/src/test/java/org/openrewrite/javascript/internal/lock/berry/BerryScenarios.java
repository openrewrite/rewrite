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
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.LockScenario;
import org.openrewrite.javascript.internal.lock.RecordedRegistry;
import org.openrewrite.javascript.internal.lock.RegistrySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import java.util.stream.Stream;

/**
 * The recorded Yarn Berry scenarios ({@link LockScenario#discover}) in the recorder's layout: each lock with the
 * manifests, .yarnrc.yml, patches, environment, machine and day of its phase.
 */
final class BerryScenarios {

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
        public @Nullable String packument(String registry, String name, boolean full, @Nullable String authorization) {
            return null;
        }

        @Override
        public byte @Nullable [] tarball(String url, String name, String version, @Nullable String authorization) {
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
    static final class Lock extends LockScenario.RecordedLock<Scenario> {

        Lock(Scenario scenario, String fileName, String text, Phase phase) {
            super(scenario, fileName, text, phase);
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

    static final class Scenario extends LockScenario {
        Scenario(String name, String label, Path dir, @Nullable Path httpCache) {
            super(name, label, dir, httpCache);
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
            try (Stream<Path> files = Files.list(getDir())) {
                files.map(f -> f.getFileName().toString())
                        .filter(f -> f.startsWith("before-") && !f.equals("before-given") &&
                                     !f.equals("before-commands") && !f.equals("before-again"))
                        .sorted()
                        .forEach(f -> locks.add(new Lock(this, f, read(getDir().resolve(f)), Phase.BEFORE)));
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
         * environment, the machine and the day, and after the edit the home's {@code user-yarnrc-yml}.
         */
        YarnBerryLockRegenerator.Inputs inputs(Phase phase, @Nullable String lock) {
            Map<String, String> configFiles = new LinkedHashMap<>();
            String yarnrc = yarnrc(phase);
            if (yarnrc != null) {
                configFiles.put(Configuration.DEFAULT_RC_FILENAME, yarnrc);
            }
            Map<String, String> env = new LinkedHashMap<>();
            if (has("recorded-with")) {
                env.putAll(RECORDER_ENV);
                String registry = recordingRegistry();
                if (registry != null) {
                    env.put("YARN_NPM_REGISTRY_SERVER", registry);
                }
            }
            // the edit runs where run-dir names
            String runsIn = phase == Phase.AFTER && has("run-dir") ? String.valueOf(file("run-dir")).trim() : "";
            Map<String, String> patchFiles = new LinkedHashMap<>();
            Map<String, String> rcFiles = new LinkedHashMap<>();
            for (Map.Entry<String, String> file : tree(phase).entrySet()) {
                String path = file.getKey();
                if (path.endsWith(".patch")) {
                    patchFiles.put(path, file.getValue());
                } else if (path.endsWith("/" + Configuration.DEFAULT_RC_FILENAME)) {
                    rcFiles.put(path, file.getValue());
                }
            }
            configFiles.putAll(Configuration.findRcFiles(runsIn, rcFiles));
            Map<String, String> home = new LinkedHashMap<>();
            String userYarnrc = file("user-yarnrc-yml");
            if (phase == Phase.AFTER && userYarnrc != null) {
                home.put(Configuration.DEFAULT_RC_FILENAME, userYarnrc);
            }
            return YarnBerryLockRegenerator.Inputs.builder()
                    .lock(lock)
                    .manifests(manifests(phase))
                    .configFiles(configFiles)
                    .homeFiles(home)
                    .runsIn(runsIn)
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
                paths.add(Configuration.DEFAULT_RC_FILENAME);
            }
            return paths;
        }

        /**
         * The registry as the scenario recorded it: {@code http/<name>} whatever the registry's url, the version
         * documents standing in for the full packument's entries, and {@code http/<name>-<version>.tgz}.
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
            String tree = tree(phase).get(Configuration.DEFAULT_RC_FILENAME);
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
                try (Stream<Path> files = Files.list(getDir())) {
                    files.map(f -> f.getFileName().toString())
                            .filter(f -> f.startsWith("pkg-") && !f.equals("pkg-before") && !f.equals("pkg-after") &&
                                         !f.equals("pkg-root") &&
                                         !f.endsWith(phase == Phase.AFTER ? "-before" : "-after"))
                            .sorted()
                            .forEach(f -> manifests.put("packages/" + f.substring(4).replaceAll("-(before|after)$", ""),
                                    read(getDir().resolve(f))));
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
                Path file = getHttpCache() == null ? null : getHttpCache().resolve(name);
                return file != null && Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * {@link Scenario#registry()} ({@code http-<mirror>/} for a mirror's urls), with every route it was asked for and
     * does not have.
     */
    static final class ScenarioRegistry extends RecordedRegistry<Scenario> implements RegistrySource {
        private static final ObjectMapper JSON = new ObjectMapper();

        ScenarioRegistry(Scenario scenario) {
            super(scenario);
        }

        @Override
        public @Nullable String packument(String registry, String name, boolean full, @Nullable String authorization) {
            String mirror = mirror(registry, "npmjs", "yarnpkg");
            byte[] body = http(mirror, name);
            if (body == null) {
                notRecorded(mirror == null ? name : mirror + "/" + name);
                return null;
            }
            String text = withRecordedVersions(mirror, name, new String(body, StandardCharsets.UTF_8), false);
            if (mirror != null) {
                return text;
            }
            byte[] cached = scenario.getHttpCache() == null ? null : scenario.cached(name);
            if (text == null && cached != null) {
                text = withRecordedVersions(null, name, new String(cached, StandardCharsets.UTF_8), false);
            }
            if (text == null) {
                notRecorded(name + "-<version> for the versions recorded empty");
                text = withRecordedVersions(null, name, new String(body, StandardCharsets.UTF_8), true);
            }
            String recorded = scenario.recordingRegistry();
            return recorded == null ? text : text.replace(NPM_REGISTRY, recorded + "/");
        }

        @Override
        public byte @Nullable [] tarball(String url, String name, String version, @Nullable String authorization) {
            String file = name + "-" + version + ".tgz";
            byte[] tgz = http(mirror(url, "npmjs", "yarnpkg"), file);
            if (tgz == null) {
                notRecorded(file);
            }
            return tgz;
        }

        private byte @Nullable [] http(@Nullable String mirror, String file) {
            if (mirror == null) {
                return scenario.httpBytes(file);
            }
            try {
                Path path = scenario.getDir().resolve(mirror).resolve(file);
                return Files.isRegularFile(path) ? Files.readAllBytes(path) : null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        // the packument with each recorded version document in its entry; null when an entry is left empty unless
        // that is acceptable
        private @Nullable String withRecordedVersions(@Nullable String mirror, String name, String body,
                                                      boolean incomplete) {
            Object packument = Js.jsonParse(body);
            Object versions = Js.get(packument, "versions");
            if (!(versions instanceof Map)) {
                return body;
            }
            boolean filled = false;
            int unrecorded = 0;
            @SuppressWarnings("unchecked") Map<String, Object> byVersion = (Map<String, Object>) versions;
            for (Map.Entry<String, Object> entry : byVersion.entrySet()) {
                byte[] manifest = http(mirror, name + "-" + entry.getKey());
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
    }

    /**
     * The configuration as an install reads it for a release: a copy restricted to the release's settings.
     */
    static Configuration restricted(Configuration options, YarnRelease release) {
        Configuration configuration = options.copy();
        configuration.restrictTo(release);
        return configuration;
    }

    static List<Scenario> discover() {
        return LockScenario.discover(FAMILY, BerryScenarios::isScenario, Scenario::new);
    }

    private static boolean isScenario(Path dir) {
        return Files.isRegularFile(dir.resolve("before")) || Files.isRegularFile(dir.resolve("after")) ||
               Files.isRegularFile(dir.resolve("before-given"));
    }
}
