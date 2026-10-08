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
package org.openrewrite.javascript.internal.lock.pnpm;

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
 * The recorded pnpm scenarios under {@code lock/pnpm/} on the test classpath and under {@code REWRITE_LOCK_SCENARIOS}'s
 * {@code pnpm} directory, with each lock's phase inputs; {@code REWRITE_LOCK_HTTP_CACHE} backs their {@code http/}.
 */
final class PnpmScenarios {

    static final String SCENARIOS_ENV = "REWRITE_LOCK_SCENARIOS";
    static final String HTTP_CACHE_ENV = "REWRITE_LOCK_HTTP_CACHE";

    private static final List<String> LOCKS = Arrays.asList("before", "after", "seed-lock", "again");
    private static final Pattern MEMBER_MANIFEST = Pattern.compile("^pkg-(.+?)(?:-(before|after))?$");
    private static final Pattern PHASE_TIME = Pattern.compile("(before|after|again) at (\\S+?),?(?:\\s|$)");
    private static final Pattern RECORDED_AT = Pattern.compile("^pnpm \\S+, node \\S+, (\\S+)");

    private PnpmScenarios() {
    }

    /**
     * One lock of a scenario with what pnpm had when it read or wrote it.
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
            this.before = fileName.startsWith("before") || fileName.startsWith("seed-lock");
        }

        Map<String, String> manifests() {
            return scenario.manifests(before, text);
        }

        /**
         * pnpm-workspace.yaml as the install over this lock found it: after the edit, as pnpm rewrote it.
         */
        @Nullable
        String workspaceManifest() {
            String rewritten = before ? null : scenario.file("probes/after-changed/pnpm-workspace.yaml");
            return rewritten != null ? rewritten : scenario.workspaceManifest(before);
        }

        @Nullable
        String npmrc() {
            return scenario.npmrc(before);
        }

        Map<String, String> tree() {
            return scenario.tree(before);
        }

        /**
         * The recorded phase that ran the install that changes nothing over this lock, or wrote it.
         */
        String phase() {
            if (before) {
                return "before".equals(fileName) && scenario.editChangesNothing() &&
                       scenario.file("pnpm-before-again.log") == null ? "after" : "before";
            }
            return "after".equals(fileName) && scenario.has("pnpm-again.log") ? "again" : "after";
        }

        Instant now() {
            return scenario.phaseTime(phase());
        }

        /**
         * What pnpm writes on an install that changes nothing over this lock, when that differs from the lock.
         */
        @Nullable
        String rewritten() {
            return scenario.file("after".equals(fileName) ? "again" : fileName + "-again");
        }

        /**
         * The log of pnpm's install that changes nothing over this lock, when one was recorded: the second install
         * over {@code after}, an install over another lock, or the edit phase when it changed nothing.
         */
        @Nullable
        String againLog() {
            String log = scenario.file("pnpm-" + fileName + "-again.log");
            if (log == null && "after".equals(fileName)) {
                log = scenario.file("pnpm-again.log");
            }
            if (log == null && "before".equals(fileName) && scenario.editChangesNothing()) {
                log = scenario.file("pnpm-after.log");
            }
            return log;
        }

        /**
         * What that install wrote: the rewrite, the lock itself, or {@code after} when the edit phase changed
         * nothing.
         */
        @Nullable
        String againLock() {
            String rewritten = rewritten();
            if (rewritten != null) {
                return rewritten;
            }
            if ("before".equals(fileName) && scenario.editChangesNothing() &&
                scenario.file("pnpm-before-again.log") == null) {
                return scenario.file("after");
            }
            return againLog() != null ? text : null;
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

        /**
         * A downloaded fixture's re-recording with pnpm 11.2.2 ({@code fixture-<name>}): the same locks and
         * manifests, with the registry documents, clock and host the fixture does not record.
         */
        private @Nullable Scenario reRecording;

        /**
         * The in-repo scenario that re-records this downloaded fixture and names it in its {@code supersedes} file.
         */
        private @Nullable Scenario supersededBy;

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

        @Nullable
        String recordedWith() {
            return file("recorded-with");
        }

        /**
         * The second phase's command-line flags ({@code --frozen-lockfile}), which a lock-only install does not take.
         */
        @Nullable
        String args() {
            String args = file("args");
            return args == null ? null : args.trim();
        }

        /**
         * Whether the second phase is a lock-only install over the same project files as the first: its log is
         * then pnpm's install that changes nothing over {@code before}.
         */
        boolean editChangesNothing() {
            return has("before") && has("pkg") && args() == null && !has("full-before") &&
                   String.valueOf(file("pkg")).equals(file("pkg-before")) &&
                   tree(true).equals(tree(false)) &&
                   String.valueOf(workspaceManifest(true)).equals(String.valueOf(workspaceManifest(false))) &&
                   String.valueOf(npmrc(true)).equals(String.valueOf(npmrc(false)));
        }

        /**
         * When pnpm ran a phase ({@code phase times: before at ..., after at ...}), else when the scenario was
         * recorded.
         */
        Instant phaseTime(String phase) {
            String recorded = recordedWith();
            if (recorded != null) {
                Matcher m = PHASE_TIME.matcher(recorded);
                while (m.find()) {
                    if (phase.equals(m.group(1))) {
                        return Instant.parse(m.group(2));
                    }
                }
                Matcher at = RECORDED_AT.matcher(recorded);
                if (at.find()) {
                    return Instant.parse(at.group(1));
                }
            }
            return Instant.parse("2026-10-07T00:00:00Z");
        }

        /**
         * Every lock pnpm wrote or was given, and what it rewrote each to on an install that changes nothing.
         */
        List<Lock> locks() {
            List<Lock> locks = new ArrayList<>();
            for (String file : files()) {
                boolean lock = LOCKS.contains(file) || file.endsWith("-again") && LOCKS.contains(
                        file.substring(0, file.length() - "-again".length()));
                if (!lock) {
                    continue;
                }
                Lock candidate = new Lock(this, file, file(file));
                boolean seen = false;
                for (Lock other : locks) {
                    // the recorder copies seed-lock to before; a rewrite can come back to a lock already held
                    seen |= other.getText().equals(candidate.getText()) && other.isBefore() == candidate.isBefore();
                }
                if (!seen) {
                    locks.add(candidate);
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
        String workspaceManifest(boolean before) {
            String fromTree = tree(before).get("pnpm-workspace.yaml");
            if (fromTree != null) {
                return fromTree;
            }
            if (before) {
                return has("workspace-before") ? file("workspace-before") :
                        has("workspace") ? file("workspace") : file("workspace-yaml");
            }
            return has("workspace") ? file("workspace") :
                    has("workspace-before") ? file("workspace-before") : file("workspace-yaml");
        }

        @Nullable
        String npmrc(boolean before) {
            String fromTree = tree(before).get(".npmrc");
            if (fromTree != null) {
                return fromTree;
            }
            if (before && has("npmrc-before")) {
                return file("npmrc-before");
            }
            return file("npmrc");
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
                for (String importer : importerIds(lock)) {
                    String member = memberFiles.get(importer.substring(importer.lastIndexOf('/') + 1));
                    if (member != null && !".".equals(importer)) {
                        manifests.put(importer, file(member));
                    }
                }
            }
            return manifests;
        }

        @SuppressWarnings("unchecked")
        private static List<String> importerIds(String lock) {
            try {
                Object file = JsYaml.load(lock);
                Object importers = file instanceof Map ? ((Map<String, Object>) file).get("importers") : null;
                if (importers instanceof Map) {
                    return new ArrayList<>(((Map<String, Object>) importers).keySet());
                }
            } catch (RuntimeException ignored) {
                // a lock that does not load places no member
            }
            return Collections.emptyList();
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
         * A recorded registry response: {@code http/<name>} (an abbreviated packument) or {@code http/<name>@full},
         * from the scenario, else from the shared cache.
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

        /**
         * A packument as pnpm received it: {@code http/}, else pnpm's own metadata cache of the phase (then of the
         * other phases) under {@code probes/<phase>-cache/}, else the shared cache. A full document comes from the full
         * mirrors, or from the abbreviated one where pnpm saved it after a release-age upgrade.
         */
        @Nullable
        String packument(String name, boolean full, String phase) {
            Path own = dir.resolve("http").resolve(full ? name + "@full" : name);
            if (Files.isRegularFile(own)) {
                return read(own);
            }
            List<String> phases = new ArrayList<>(Arrays.asList("before", "after", "again"));
            phases.remove(phase);
            phases.add(0, phase);
            for (String kind : full ? new String[]{"metadata-full", "metadata-full-filtered", "metadata"} :
                    new String[]{"metadata"}) {
                for (String p : phases) {
                    Path metadata = dir.resolve("probes").resolve(p + "-cache").resolve("v11").resolve(kind);
                    if (!Files.isDirectory(metadata)) {
                        continue;
                    }
                    try (Stream<Path> registries = Files.list(metadata)) {
                        for (Path registry : (Iterable<Path>) registries::iterator) {
                            Path mirror = registry.resolve(name + ".jsonl");
                            if (Files.isRegularFile(mirror)) {
                                String jsonl = read(mirror);
                                String document = jsonl.substring(jsonl.indexOf('\n') + 1);
                                // pnpm keeps a full document in the abbreviated mirror only after a release-age upgrade
                                if (!"metadata".equals(kind) || !full || isFull(document)) {
                                    return document;
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
            return httpCache != null && Files.isRegularFile(httpCache.resolve(full ? name + "@full" : name)) ?
                    read(httpCache.resolve(full ? name + "@full" : name)) : null;
        }

        // the registry leaves the publish times and the CouchDB id and revision out of an abbreviated packument
        private static boolean isFull(String document) {
            Object meta = Manifests.parseValue(document, "packument");
            if (!(meta instanceof Map)) {
                return false;
            }
            Map<?, ?> packument = (Map<?, ?>) meta;
            return packument.containsKey("time") || packument.containsKey("_id") || packument.containsKey("_rev");
        }

        /**
         * The registries pnpm fetched packuments from in a phase, as its metadata cache names them (the host, a port
         * after a {@code +}); for a recording without that cache, registry.npmjs.org and the hosts its
         * {@code registry-hosts} file lists.
         */
        Set<String> registryHosts(String phase) {
            Set<String> hosts = new TreeSet<>();
            for (String kind : new String[]{"metadata", "metadata-full", "metadata-full-filtered"}) {
                Path metadata = dir.resolve("probes").resolve(phase + "-cache").resolve("v11").resolve(kind);
                if (!Files.isDirectory(metadata)) {
                    continue;
                }
                try (Stream<Path> registries = Files.list(metadata)) {
                    registries.filter(Files::isDirectory).forEach(p -> hosts.add(p.getFileName().toString()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            if (hosts.isEmpty()) {
                hosts.add("registry.npmjs.org");
                String listed = file("registry-hosts");
                if (listed != null) {
                    hosts.addAll(Arrays.asList(listed.trim().split("\\s+")));
                }
            }
            return hosts;
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
            Enumeration<URL> roots = PnpmScenarios.class.getClassLoader().getResources("lock/pnpm");
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
        for (Scenario scenario : found.values()) {
            Scenario reRecording = found.get("fixture-" + scenario.getName());
            if (scenario.recordedWith() == null && reRecording != null && reRecording.recordedWith() != null) {
                scenario.reRecording = reRecording;
            }
            String supersedes = scenario.file("supersedes");
            if (supersedes != null) {
                for (String name : supersedes.trim().split("\\s+")) {
                    Scenario superseded = found.get(name);
                    if (superseded != null && superseded.recordedWith() == null) {
                        superseded.supersededBy = scenario;
                    }
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    private static @Nullable Path corpusRoot() {
        String corpus = System.getenv(SCENARIOS_ENV);
        if (corpus == null || corpus.isEmpty() || !Files.isDirectory(Paths.get(corpus))) {
            return null;
        }
        Path dir = Paths.get(corpus);
        if ("pnpm".equals(String.valueOf(dir.getFileName()))) {
            return dir;
        }
        Path sibling = dir.resolveSibling("pnpm");
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
