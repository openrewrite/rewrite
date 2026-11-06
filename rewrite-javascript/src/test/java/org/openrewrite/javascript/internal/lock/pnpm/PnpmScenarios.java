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
import org.openrewrite.javascript.internal.lock.HostInputs;
import org.openrewrite.javascript.internal.lock.HostRegistries;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.LockScenario;
import org.openrewrite.javascript.internal.lock.RootRelativePath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * The recorded pnpm scenarios ({@link LockScenario#discover}) in the recorder's layout, with each lock's phase inputs
 * and the registry documents pnpm received.
 */
final class PnpmScenarios {

    private static final List<String> LOCKS = Arrays.asList("before", "after", "seed-lock", "again");
    private static final Pattern MEMBER_MANIFEST = Pattern.compile("^pkg-(.+?)(?:-(before|after))?$");
    private static final Pattern PHASE_TIME = Pattern.compile("(before|after|again) at (\\S+?),?(?:\\s|$)");
    private static final Pattern RECORDED_AT = Pattern.compile("^pnpm \\S+, node \\S+, (\\S+)");

    private PnpmScenarios() {
    }

    /**
     * One lock of a scenario with what pnpm had when it read or wrote it.
     */
    static final class Lock extends LockScenario.RecordedLock<Scenario> {

        Lock(Scenario scenario, String fileName, String text) {
            super(scenario, fileName, text, fileName.startsWith("before") || fileName.startsWith("seed-lock") ?
                    Phase.BEFORE : Phase.AFTER);
        }

        Map<String, String> manifests() {
            return scenario.manifests(phase, text);
        }

        /**
         * pnpm-workspace.yaml as the install over this lock found it: after the edit, as pnpm rewrote it.
         */
        @Nullable
        String workspaceManifest() {
            String rewritten = phase == Phase.BEFORE ? null :
                    scenario.file("probes/after-changed/pnpm-workspace.yaml");
            return rewritten != null ? rewritten : scenario.workspaceManifest(phase);
        }

        @Nullable
        String npmrc() {
            return scenario.npmrc(phase);
        }

        Map<String, String> tree() {
            return scenario.tree(phase);
        }

        Map<String, String> otherLocks() {
            return scenario.otherLocks(phase);
        }

        /**
         * The recorded phase that ran the install that changes nothing over this lock, or wrote it.
         */
        String recordedPhase() {
            if (phase == Phase.BEFORE) {
                return "before".equals(fileName) && scenario.editChangesNothing() &&
                       scenario.file("pnpm-before-again.log") == null ? "after" : "before";
            }
            return "after".equals(fileName) && scenario.has("pnpm-again.log") ? "again" : "after";
        }

        Instant now() {
            return scenario.phaseTime(recordedPhase());
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

    static final class Scenario extends LockScenario {

        /**
         * A downloaded fixture's re-recording with pnpm 11.2.2 ({@code fixture-<name>}): the same locks and
         * manifests, with the registry documents, clock and host the fixture does not record.
         */
        @Getter
        private @Nullable Scenario reRecording;

        Scenario(String name, String label, Path dir, @Nullable Path httpCache) {
            super(name, label, dir, httpCache);
        }

        /**
         * The directory of the lock the scenario's locks are ({@code lock-dir}): a workspace member's own lock beside
         * the workspace root's manifests and files; {@code ""} the root's.
         */
        String lockDir() {
            String lockDir = file("lock-dir");
            return lockDir == null ? "" : lockDir.trim();
        }

        /**
         * The directory pnpm ran the edit in ({@code run-dir}); null for the root.
         */
        @Nullable
        String runDir() {
            String runDir = file("run-dir");
            return runDir == null ? null : runDir.trim();
        }

        /**
         * The variables the recording set for pnpm ({@code env}, one {@code KEY=VALUE} a line); null without one.
         */
        @Nullable
        Map<String, String> env() {
            String env = file("env");
            if (env == null) {
                return null;
            }
            Map<String, String> out = new LinkedHashMap<>();
            for (String line : env.split("\n")) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    out.put(line.substring(0, eq), line.substring(eq + 1));
                }
            }
            return out;
        }

        /**
         * The recording's user .npmrc ({@code user-npmrc}) as the host renders it from the marker's user npmrc.
         */
        String userNpmrc() {
            return HostRegistries.host(file("user-npmrc"), getDir()).userNpmrc(HostInputs.USER);
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
                   tree(Phase.BEFORE).equals(tree(Phase.AFTER)) &&
                   String.valueOf(workspaceManifest(Phase.BEFORE))
                           .equals(String.valueOf(workspaceManifest(Phase.AFTER))) &&
                   String.valueOf(npmrc(Phase.BEFORE)).equals(String.valueOf(npmrc(Phase.AFTER)));
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
                    seen |= other.getText().equals(candidate.getText()) && other.getPhase() == candidate.getPhase();
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
        Map<String, String> tree(Phase phase) {
            Map<String, String> out = new TreeMap<>(tree("tree-before"));
            if (phase == Phase.AFTER) {
                out.putAll(tree("tree"));
            }
            return out;
        }

        /**
         * The other projects' locks beside the {@code before} or the {@code after} lock, where the recording kept every
         * project's ({@code probes/before-locks/}), else the root's ({@code probes/before-root-lock}).
         */
        Map<String, String> otherLocks(Phase phase) {
            String probes = phase == Phase.BEFORE ? "probes/before" : "probes/after";
            Map<String, String> out = new TreeMap<>(tree(probes + "-locks"));
            if (out.isEmpty() && has(probes + "-root-lock")) {
                out.put(LockfileReader.WANTED_LOCKFILE, requireNonNull(file(probes + "-root-lock")));
            }
            out.remove(RootRelativePath.join(lockDir(), LockfileReader.WANTED_LOCKFILE));
            return out;
        }

        @Nullable
        String workspaceManifest(Phase phase) {
            String fromTree = tree(phase).get("pnpm-workspace.yaml");
            if (fromTree != null) {
                return fromTree;
            }
            if (phase == Phase.BEFORE) {
                return has("workspace-before") ? file("workspace-before") :
                        has("workspace") ? file("workspace") : file("workspace-yaml");
            }
            return has("workspace") ? file("workspace") :
                    has("workspace-before") ? file("workspace-before") : file("workspace-yaml");
        }

        @Nullable
        String npmrc(Phase phase) {
            String fromTree = tree(phase).get(".npmrc");
            if (fromTree != null) {
                return fromTree;
            }
            if (phase == Phase.BEFORE && has("npmrc-before")) {
                return file("npmrc-before");
            }
            return file("npmrc");
        }

        /**
         * The project's manifests in a phase by location: {@code ""} the root, a member's path otherwise.
         */
        Map<String, String> manifests(Phase phase, String lock) {
            Map<String, String> manifests = new LinkedHashMap<>();
            String root = phase == Phase.BEFORE ? (has("pkg-before") ? file("pkg-before") : file("pkg-root")) :
                    has("pkg") ? file("pkg") : has("pkg-after") ? file("pkg-after") : file("pkg-root");
            if (root != null) {
                manifests.put("", root);
            }
            for (Map.Entry<String, String> entry : tree(phase).entrySet()) {
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
                String filePhase = m.group(2);
                if (filePhase != null && !filePhase.equals(phase == Phase.BEFORE ? "before" : "after")) {
                    continue;
                }
                if (filePhase == null && memberFiles.containsKey(m.group(1))) {
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

        /**
         * A packument as pnpm received it: {@code http/}, else pnpm's metadata cache of a phase under {@code probes/},
         * else the shared cache; a full document from the full mirrors or a release-age upgrade's save.
         */
        @Nullable
        String packument(String name, boolean full, String phase) {
            Path own = getDir().resolve("http").resolve(full ? name + "@full" : name);
            if (Files.isRegularFile(own)) {
                return read(own);
            }
            List<String> phases = new ArrayList<>(Arrays.asList("before", "after", "again"));
            phases.remove(phase);
            phases.add(0, phase);
            for (String kind : full ? new String[]{"metadata-full", "metadata-full-filtered", "metadata"} :
                    new String[]{"metadata"}) {
                for (String p : phases) {
                    Path metadata = getDir().resolve("probes").resolve(p + "-cache").resolve("v11").resolve(kind);
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
            Path cache = getHttpCache();
            return cache != null && Files.isRegularFile(cache.resolve(full ? name + "@full" : name)) ?
                    read(cache.resolve(full ? name + "@full" : name)) : null;
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
         * The registries pnpm fetched packuments from in a phase, as its metadata cache names them; without that cache,
         * registry.npmjs.org and the hosts its {@code registry-hosts} file lists.
         */
        Set<String> registryHosts(String phase) {
            Set<String> hosts = new TreeSet<>();
            for (String kind : new String[]{"metadata", "metadata-full", "metadata-full-filtered"}) {
                Path metadata = getDir().resolve("probes").resolve(phase + "-cache").resolve("v11").resolve(kind);
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
    }

    static List<Scenario> discover() {
        List<Scenario> found = LockScenario.discover("pnpm", PnpmScenarios::isScenario, Scenario::new);
        Map<String, Scenario> byLabel = new LinkedHashMap<>();
        for (Scenario scenario : found) {
            byLabel.put(scenario.getLabel(), scenario);
        }
        for (Scenario scenario : found) {
            Scenario reRecording = byLabel.get("fixture-" + scenario.getName());
            if (scenario.recordedWith() == null && reRecording != null && reRecording.recordedWith() != null) {
                scenario.reRecording = reRecording;
            }
        }
        return found;
    }

    /**
     * What a recorded project holds, which is all a scenario gives: its files, its manifests and the lock.
     */
    static Set<String> paths(Map<String, String> files, Map<String, String> manifests, String lockDir) {
        Set<String> paths = new LinkedHashSet<>(files.keySet());
        for (String dir : manifests.keySet()) {
            paths.add(dir.isEmpty() ? "package.json" : dir + "/package.json");
        }
        paths.add(lockDir.isEmpty() ? "pnpm-lock.yaml" : lockDir + "/pnpm-lock.yaml");
        return paths;
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
