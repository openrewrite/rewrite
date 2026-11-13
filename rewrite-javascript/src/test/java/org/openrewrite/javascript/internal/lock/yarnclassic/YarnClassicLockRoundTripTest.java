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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.LockScenario;
import org.openrewrite.javascript.internal.lock.Rejected;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Every yarn.lock {@link YarnClassicScenarios} finds, loaded, every entry reused and written back: the lock's bytes,
 * what yarn {@code --force} wrote ({@code <lock>-forced}), or what a no-change install saved.
 */
class YarnClassicLockRoundTripTest {

    static Stream<Arguments> locks() {
        List<Arguments> out = new ArrayList<>();
        for (YarnClassicScenarios.Scenario scenario : YarnClassicScenarios.discover()) {
            for (YarnClassicScenarios.Lock lock : scenario.locks()) {
                out.add(Arguments.of(lock.toString(), lock));
            }
        }
        return out.stream();
    }

    static Stream<Arguments> installsThatChangeNothing() {
        return locks().filter(args -> ((YarnClassicScenarios.Lock) args.get()[1]).againLog() != null);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("locks")
    void writesTheLockYarnWrites(String name, YarnClassicScenarios.Lock lock) {
        Loaded loaded = load(lock);
        if (loaded == null) {
            return;
        }
        String forced = lock.forced();
        assumeFalse(forced != null && loaded.resolvesAgain(),
                "the resolver's: yarn --force resolves some pattern of this lock anew");
        assertThat(loaded.write()).isEqualTo(forced != null ? forced : lock.getText());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("installsThatChangeNothing")
    void savesTheLockAsYarnDoes(String name, YarnClassicScenarios.Lock lock) {
        String log = lock.againLog();
        String expected = lock.getScenario().expect();
        assumeFalse(!log.contains("exit=0") && expected != null && expected.startsWith("cannot-know"),
                "yarn's second install read its cache, which the engine cannot know");
        assertThat(log).contains("exit=0");
        if (installsElsewhere(lock)) {
            return;
        }
        Loaded loaded = load(lock);
        if (loaded == null) {
            return;
        }
        String again = lock.again();
        String expect = lock.getScenario().expect();
        assumeFalse(again != null && expect != null && (expect.startsWith("cannot-know") ||
                                                         expect.startsWith("second-install-rejects") ||
                                                         expect.startsWith("not-reproduced")),
                "yarn writes each package's own package.json, which differs from the entry the resolver reused");
        assumeFalse(loaded.resolvesAgain(), "the resolver's: an install over this lock resolves some pattern anew");
        String saved = loaded.save();
        assertThat(saved == null ? lock.getText() : saved).isEqualTo(again != null ? again : lock.getText());
        if (again == null) {
            assertThat(saved == null || saved.equals(lock.getText())).as("yarn leaves the file as it is").isTrue();
        }
    }

    // yarn installs at a workspace root above the project, or where it runs when no workspaces list that, and leaves
    // this lock as it is
    private static boolean installsElsewhere(YarnClassicScenarios.Lock lock) {
        YarnClassicLockRegenerator.Inputs inputs = lock.inputs();
        Map<String, @Nullable Object> flags = Rc.getRcArgs(Rc.getRcConfigForCwd(inputs.configFilesFromCwd(),
                inputs.getHomeFiles()));
        boolean looseSemver = !Js.truthy(flags.get("strictSemver"));
        int workspaceRoot = Config.findWorkspaceRoot(inputs, looseSemver);
        if (workspaceRoot == 0 || workspaceRoot < 0 && inputs.getCwd().isEmpty()) {
            return false;
        }
        assertThat(lock.getScenario().expect()).startsWith("cannot-know");
        return true;
    }

    /**
     * The lock loaded, or {@code null} when yarn rejects or sets it aside, as the scenario recorded.
     */
    private static @Nullable Loaded load(YarnClassicScenarios.Lock lock) {
        YarnClassicLockRegenerator.Inputs inputs = lock.inputs();
        Config options = null;
        try {
            options = new Config(inputs, YarnClassicLockRegenerator.start(inputs));
        } catch (CannotKnow | JsError | MessageError | Rejected e) {
            if (e instanceof CannotKnow) {
                assertThat(lock.getScenario().expect()).startsWith("cannot-know");
                assumeFalse(true, "the engine cannot know what yarn does with this configuration: " +
                                  e.getMessage());
            }
            String log = installOver(lock);
            assertThat(lock.getScenario().expect()).matches("(pm-fails|pm-rejects) .*");
            assertThat(log == null || log.contains("exit=1")).as("yarn failed the install over this lock").isTrue();
            assumeFalse(true, "yarn 1.22.22 fails on this configuration: " + e.getMessage());
        }
        Lockfile lockfile;
        try {
            lockfile = Lockfile.fromDirectory(inputs.getLock());
        } catch (MessageError e) {
            assertThat(logs(lock.getScenario())).as("yarn rejects the lock").contains("error " + e.getMessage());
            return null;
        }
        if (lockfile.getLockfilev2() != null) {
            assertThat(lock.getScenario().expect()).as("yarn sets a Yarn Berry lock aside").startsWith("malformed");
            return null;
        }
        if (LockfileParser.CONFLICT.equals(lockfile.getParseResultType())) {
            assertThat(logs(lock.getScenario())).as("yarn regenerates a lock whose conflict does not merge")
                    .contains("could not be successfully merged, regenerating yarn.lock from scratch");
            assertThat(lock.getScenario().expect()).startsWith("malformed");
            return null;
        }
        return new Loaded(lock, inputs, requireNonNull(options), lockfile);
    }

    // the second install recorded over this lock: its own, or the scenario's when that ran over this lock
    private static @Nullable String installOver(YarnClassicScenarios.Lock lock) {
        String own = lock.againLog();
        if (own != null) {
            return own;
        }
        String log = lock.getScenario().file("yarnclassic-again.log");
        return log != null && log.contains("over=" + lock.getFileName() + "\n") ? log : null;
    }

    private static String logs(YarnClassicScenarios.Scenario scenario) {
        StringBuilder out = new StringBuilder();
        try (Stream<Path> files = Files.list(scenario.getDir())) {
            files.filter(f -> f.getFileName().toString().endsWith(".log"))
                    .forEach(f -> out.append(LockScenario.read(f)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString();
    }

    private static final class Loaded {
        final YarnClassicScenarios.Lock lock;
        final YarnClassicLockRegenerator.Inputs inputs;
        final Config options;
        final Lockfile lockfile;
        final PackageResolver resolver;

        Loaded(YarnClassicScenarios.Lock lock, YarnClassicLockRegenerator.Inputs inputs, Config options,
               Lockfile lockfile) {
            this.lock = lock;
            this.inputs = inputs;
            this.options = options;
            this.lockfile = lockfile;
            this.resolver = reuseEveryEntry(lockfile, options);
        }

        String write() {
            return Fs.writeFilePreservingEol(lock.getText(), LockfileStringifier.stringify(
                    lockfile.getLockfile(resolver.patterns), false, options.isEnableLockfileVersions(),
                    lock.getScenario().nodeVersion()), lock.getScenario().host().getOs());
        }

        @Nullable
        String save() {
            Map<String, @Nullable Object> flags = YarnClassicLockRegenerator.start(inputs);
            Config config = new Config(inputs, flags);
            Install.InstallCwdRequest request = new Install(inputs, config, flags, YarnClassicScenarios.NO_REGISTRY)
                    .fetchRequestFromCwd(lockfile, new ResolutionMap(config));
            return Install.saveLockfileAndIntegrity(resolver, request.getPatterns(), request.getWorkspaceLayout(),
                    config, new Install.Flags(config, flags), lockfile, lock.getText(), lock.getScenario().host());
        }

        /**
         * Whether an install over this lock resolves some pattern anew: a root dependency's has no entry, or an
         * entry's version no longer satisfies its pattern's range (a name the resolutions cover aside).
         */
        boolean resolvesAgain() {
            Map<String, @Nullable Object> flags = YarnClassicLockRegenerator.start(inputs);
            Config config = new Config(inputs, flags);
            ResolutionMap resolutionMap = new ResolutionMap(config);
            Install.InstallCwdRequest request = new Install(inputs, config, flags, YarnClassicScenarios.NO_REGISTRY)
                    .fetchRequestFromCwd(lockfile, resolutionMap);
            for (String pattern : request.getPatterns()) {
                if ((request.getWorkspaceLayout() == null ||
                     request.getWorkspaceLayout().getManifestByPattern(pattern) == null) &&
                    lockfile.getLocked(pattern) == null) {
                    return true;
                }
            }
            if (lockfile.getCache() instanceof Map) {
                for (Object key : ((Map<?, ?>) lockfile.getCache()).keySet()) {
                    Map<String, @Nullable Object> entry = lockfile.getLocked((String) key);
                    NormalizedPattern p = NormalizedPattern.normalizePattern((String) key);
                    if (entry != null && !resolutionMap.resolutionsByPackage.containsKey(p.getName()) &&
                        resolver.isLockfileEntryOutdated(entry.get("version"), p.getRange(),
                            p.isHasVersion())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * The resolver's state after an install that reuses every entry of the lock, the fetch's manifests included.
     */
    @SuppressWarnings("unchecked")
    static PackageResolver reuseEveryEntry(Lockfile lockfile, Config options) {
        PackageResolver resolver = YarnClassicScenarios.resolver(options, lockfile);
        if (!(lockfile.getCache() instanceof Map)) {
            return resolver;
        }
        List<String> keys = new ArrayList<>(((Map<String, Object>) lockfile.getCache()).keySet());
        keys.sort(Misc::sortAlpha);
        for (String pattern : keys) {
            Map<String, @Nullable Object> entry = lockfile.getLocked(pattern);
            if (entry == null) {
                continue;
            }
            PackageRequest request = new PackageRequest(DependencyRequestPattern.builder()
                    .pattern(pattern).registry(NpmResolver.NPM_REGISTRY_ID).build(), resolver);
            Manifest info = request.getLocked("tarball");
            if (info == null) {
                info = directoryManifest(pattern, entry, options);
            }
            PackageReference ref = new PackageReference(request, info, info.remote);
            ref.addPattern(pattern, info);
            info.reference = ref;
        }
        resolver.updateManifests(PackageFetcher.fetch(resolver.getManifests(), ref -> {
            if ("link".equals(ref.remote.type)) {
                return PackageFetcher.linkMock();
            }
            Map<String, @Nullable Object> entry = lockfile.getLocked(ref.patterns.get(0));
            Manifest pkg = new Manifest();
            pkg.put("name", entry.get("name"));
            pkg.put("version", entry.get("version"));
            pkg.put("dependencies", entry.get("dependencies"));
            pkg.put("optionalDependencies", entry.get("optionalDependencies"));
            return pkg;
        }, new HashSet<>()));
        return resolver;
    }

    // an entry without `resolved`: the link or directory a link: or file: pattern names
    private static Manifest directoryManifest(String pattern, Map<String, @Nullable Object> entry,
                                              Config options) {
        String range = NormalizedPattern.normalizePattern(pattern).getRange();
        boolean link = range.startsWith("link:") || (range.startsWith("file:") && options.isLinkFileDependencies());
        Manifest info = new Manifest();
        info.put("name", entry.get("name"));
        info.put("version", entry.get("version"));
        info.put("_uid", entry.get("version"));
        PackageRemote remote = new PackageRemote();
        remote.type = link ? "link" : "copy";
        remote.registry = NpmResolver.NPM_REGISTRY_ID;
        remote.reference = range.substring(range.indexOf(':') + 1);
        remote.hash = link ? null : pattern;
        info.remote = remote;
        return info;
    }
}
