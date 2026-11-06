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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.HostInputs;
import org.openrewrite.javascript.internal.lock.HostRegistries;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.LockScenario.Replay;
import org.openrewrite.javascript.internal.lock.LockScenario;
import org.openrewrite.javascript.internal.lock.RegistrySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The recorded Yarn classic scenarios ({@link LockScenario#discover}) in the recorder's layout: each lock with the
 * manifests, config files and paths of its phase.
 */
final class YarnClassicScenarios {

    static final String FAMILY = "yarn-classic";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FIXTURE_TARBALLS = "lock/" + FAMILY + "/fixture-tarballs/";

    private static final List<String> LOCKS = Arrays.asList("before", "after", "again");
    private static final List<String> CONFIG_FILES = Arrays.asList(".yarnrc", ".yarnrc.yml", ".npmrc");
    private static final Pattern NODE_VERSION = Pattern.compile("node (v[0-9.]+)");
    private static final Pattern PLATFORM = Pattern.compile("(?m)^platform os=(\\S*) cpu=(\\S*)");
    private static final Pattern LAYOUT = Pattern.compile("(?m)^layout repo/(\\S+)");
    private static final Pattern GIVEN = Pattern.compile("(?m)^before exit=given");
    private static final Pattern RECORDED_RELEASE = Pattern.compile("^yarn (\\d+\\.\\d+\\.\\d+),");
    private static final String RECORDED_ON_OS = "darwin";
    private static final String RECORDED_ON_CPU = "arm64";

    private YarnClassicScenarios() {
    }

    /**
     * One lock of a scenario with what yarn had when it wrote or read it: the manifests and the config of the
     * phase.
     */
    static final class Lock extends LockScenario.RecordedLock<Scenario> {

        Lock(Scenario scenario, String fileName, String text) {
            super(scenario, fileName, text, fileName.startsWith("before") ? Phase.BEFORE : Phase.AFTER);
        }

        Map<String, String> manifests() {
            return scenario.manifests(phase);
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
            return scenario.inputs(phase, text);
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
         * A tarball the scenario recorded; a downloaded fixture, which predates tarballs, finds the registry's in
         * {@code lock/yarn-classic/fixture-tarballs/}.
         */
        byte @Nullable [] tarball(String file) {
            byte[] tgz = httpBytes(file);
            if (tgz != null || recordedWith() != null) {
                return tgz;
            }
            try (InputStream in = YarnClassicScenarios.class.getClassLoader()
                    .getResourceAsStream(FIXTURE_TARBALLS + file)) {
                if (in == null) {
                    return null;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                for (int n; (n = in.read(buffer)) > 0; ) {
                    out.write(buffer, 0, n);
                }
                return out.toByteArray();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        byte @Nullable [] bytes(String file) {
            try {
                Path path = getDir().resolve(file);
                return Files.isRegularFile(path) ? Files.readAllBytes(path) : null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * Whether the recorder's registry answered 404 for this url's path ({@code registry-config}'s
         * {@code notFound}).
         */
        boolean notFound(String url) {
            String config = file("registry-config");
            if (config == null) {
                return false;
            }
            try {
                JsonNode notFound = JSON.readTree(config).get("notFound");
                String path = URI.create(url).getRawPath();
                if (notFound != null) {
                    for (JsonNode p : notFound) {
                        if (p.asText().equals(path)) {
                            return true;
                        }
                    }
                }
                return false;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * Whether the scenario records an edit: a lock to start from and the edited manifest.
         */
        boolean isEdit() {
            return has("before") && (has("pkg") || has("pkg-after"));
        }

        /**
         * Whether the recording was given the lock its edit starts from rather than writing it from {@code pkg-before}.
         */
        boolean isGiven() {
            String recorded = recordedWith();
            return has("before-given") || recorded != null && GIVEN.matcher(recorded).find();
        }

        /**
         * The release that recorded the edit: {@code recorded-with}'s, and 1.22.22 for the downloaded fixtures, which
         * it reproduces.
         */
        String recordedRelease() {
            String recorded = recordedWith();
            Matcher m = recorded == null ? null : RECORDED_RELEASE.matcher(recorded);
            return m != null && m.find() ? m.group(1) : YarnClassicRelease.PORTED.toString();
        }

        /**
         * This scenario replayed as each release its {@code releases} files name ({@link #releaseReplays}).
         */
        List<Replay> releases() {
            List<String> all = new ArrayList<>();
            for (YarnClassicRelease release : YarnClassicReleases.all()) {
                all.add(release.toString());
            }
            return releaseReplays(all, YarnClassicReleases.PROVEN, recordedRelease());
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

        /**
         * The machine yarn ran on: the {@code platform} line of {@code recorded-with}, else the machine every
         * recording here was made on.
         */
        Host host() {
            String os = RECORDED_ON_OS;
            String cpu = RECORDED_ON_CPU;
            String recorded = file("recorded-with");
            if (recorded != null) {
                Matcher m = PLATFORM.matcher(recorded);
                if (m.find()) {
                    os = m.group(1);
                    cpu = m.group(2);
                }
            }
            return Host.builder().nodeVersion(nodeVersion()).os(os).cpu(cpu).build();
        }


        /**
         * Every lock yarn wrote or was given, and what it wrote over each with no change or under {@code --force}.
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
            for (String lock : Arrays.asList("before", "after", "before-given")) {
                for (String written : Arrays.asList(lock + "-again", lock + "-forced")) {
                    String text = file(written);
                    if (text != null) {
                        locks.add(new Lock(this, written, text));
                    }
                }
            }
            return locks;
        }

        /**
         * What yarn had in a phase: the lock, manifests and tree, the config files ({@code parent/} too), its paths,
         * the directory {@code cwd} names, and after the edit the home's {@code user-npmrc} and {@code user-yarnrc}.
         */
        YarnClassicLockRegenerator.Inputs inputs(Phase phase, @Nullable String lock) {
            Map<String, String> configFiles = new LinkedHashMap<>();
            putIfPresent(configFiles, ".yarnrc", file("yarnrc"));
            putIfPresent(configFiles, ".npmrc", file("npmrc"));
            Map<String, Map<String, String>> trees = new LinkedHashMap<>();
            trees.put("", tree(phase));
            trees.put("../", tree("parent"));
            trees.put("../../", tree("grandparent"));
            Set<String> paths = new LinkedHashSet<>(configFiles.keySet());
            paths.add("package.json");
            if (lock != null) {
                paths.add("yarn.lock");
            }
            for (Map.Entry<String, Map<String, String>> tree : trees.entrySet()) {
                for (String name : CONFIG_FILES) {
                    putIfPresent(configFiles, tree.getKey() + name, tree.getValue().get(name));
                }
                if (!tree.getKey().isEmpty()) {
                    putIfPresent(configFiles, tree.getKey() + "package.json", tree.getValue().get("package.json"));
                }
                for (String path : tree.getValue().keySet()) {
                    paths.add(tree.getKey() + path);
                }
            }
            String cwd = file("cwd");
            for (String dir = cwd == null ? "" : cwd.trim(); !dir.isEmpty();
                 dir = dir.substring(0, Math.max(dir.lastIndexOf('/'), 0))) {
                for (String name : CONFIG_FILES) {
                    putIfPresent(configFiles, dir + "/" + name, trees.get("").get(dir + "/" + name));
                }
            }
            Map<String, String> home = new LinkedHashMap<>();
            if (phase == Phase.AFTER) {
                if (file("user-npmrc") != null) {
                    home.put(".npmrc", HostRegistries.host(file("user-npmrc"), getDir())
                            .userNpmrc(HostInputs.GLOBAL_AND_USER));
                }
                putIfPresent(home, ".yarnrc", file("user-yarnrc"));
            }
            return YarnClassicLockRegenerator.Inputs.builder()
                    .lock(lock)
                    .manifests(manifests(phase))
                    .configFiles(configFiles)
                    .homeFiles(home)
                    .paths(has("recorded-with") ? paths : null)
                    .directory(directory())
                    .cwd(cwd == null ? "" : cwd.trim())
                    .env(new LinkedHashMap<>())
                    .host(host())
                    .build();
        }

        /**
         * The project's directory in the repository the recorder laid out ({@code mid/app} under {@code parent/} and
         * {@code grandparent/}); a recording made elsewhere does not say.
         */
        @Nullable
        String directory() {
            String recorded = file("recorded-with");
            if (recorded == null) {
                return null;
            }
            Matcher m = LAYOUT.matcher(recorded);
            return m.find() ? m.group(1) : "";
        }

        private void putIfPresent(Map<String, String> files, String path, @Nullable String text) {
            if (text != null) {
                files.put(path, text);
            }
        }

        /**
         * The project's manifests in a phase by location: {@code ""} the root, a directory's path otherwise.
         */
        Map<String, String> manifests(Phase phase) {
            Map<String, String> manifests = new LinkedHashMap<>();
            String root = phase == Phase.BEFORE ? file("pkg-before") : has("pkg") ? file("pkg") :
                    file("pkg-after");
            if (root != null) {
                manifests.put("", root);
            }
            for (Map.Entry<String, String> entry : tree(phase).entrySet()) {
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
        Map<String, String> tree(Phase phase) {
            Map<String, String> out = new TreeMap<>(tree("tree-before"));
            if (phase == Phase.AFTER) {
                out.putAll(tree("tree"));
            }
            return out;
        }
    }

    static List<Scenario> discover() {
        return LockScenario.discover(FAMILY, YarnClassicScenarios::isScenario, Scenario::new);
    }

    interface Packuments {
        @Nullable
        String packument(String registry, String name);
    }

    /**
     * A registry of packuments that holds no tarball.
     */
    static RegistrySource packuments(Packuments packuments) {
        return new RegistrySource() {
            @Override
            public @Nullable String packument(String registry, String name, boolean full,
                                              @Nullable String authorization) {
                return packuments.packument(registry, name);
            }

            @Override
            public byte @Nullable [] tarball(String url, String name, String version, @Nullable String authorization) {
                return null;
            }
        };
    }

    static final RegistrySource NO_REGISTRY = packuments((registry, name) -> null);

    /**
     * The config of an install run with these config files under this environment, read as an install reads it.
     */
    static Config config(Map<String, String> configFiles, @Nullable Map<String, String> env) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .configFiles(configFiles).env(env).build();
        return new Config(inputs, YarnClassicLockRegenerator.start(inputs));
    }

    static Config defaults() {
        return config(Collections.emptyMap(), Collections.emptyMap());
    }

    static PackageResolver resolver(Config options, Lockfile lockfile) {
        return new PackageResolver(options, lockfile, new ResolutionMap(options), NO_REGISTRY,
                new ProjectFiles(Collections.emptyMap(), null), Host.UNKNOWN);
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
