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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
        assertThat(log).contains("exit=0");
        Loaded loaded = load(lock);
        if (loaded == null) {
            return;
        }
        String again = lock.again();
        String expect = lock.getScenario().expect();
        assumeFalse(again != null && expect != null &&
                    (expect.startsWith("cannot-know") || expect.startsWith("second-install-rejects")),
                "yarn writes each package's own package.json, which differs from the entry the resolver reused");
        assumeFalse(loaded.resolvesAgain(), "the resolver's: an install over this lock resolves some pattern anew");
        String saved = loaded.save();
        assertThat(saved == null ? lock.getText() : saved).isEqualTo(again != null ? again : lock.getText());
        if (again == null) {
            assertThat(saved == null || saved.equals(lock.getText())).as("yarn leaves the file as it is").isTrue();
        }
    }

    /**
     * The lock loaded, or {@code null} when yarn rejects or sets it aside, as the scenario recorded.
     */
    private static @Nullable Loaded load(YarnClassicScenarios.Lock lock) {
        YarnClassicLockRegenerator.Inputs inputs = lock.inputs();
        YarnClassicLockRegenerator regenerator = new YarnClassicLockRegenerator(inputs);
        YarnClassicOptions options = YarnClassicOptions.load(inputs.getConfigFiles(), inputs.getEnv());
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
        return new Loaded(lock, regenerator, options, lockfile);
    }

    private static String logs(YarnClassicScenarios.Scenario scenario) {
        StringBuilder out = new StringBuilder();
        try (Stream<Path> files = Files.list(scenario.getDir())) {
            files.filter(f -> f.getFileName().toString().endsWith(".log"))
                    .forEach(f -> out.append(YarnClassicScenarios.read(f)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString();
    }

    private static final class Loaded {
        final YarnClassicScenarios.Lock lock;
        final YarnClassicLockRegenerator regenerator;
        final YarnClassicOptions options;
        final Lockfile lockfile;
        final PackageResolver resolver;
        final List<String> warnings = new ArrayList<>();

        Loaded(YarnClassicScenarios.Lock lock, YarnClassicLockRegenerator regenerator, YarnClassicOptions options,
               Lockfile lockfile) {
            this.lock = lock;
            this.regenerator = regenerator;
            this.options = options;
            this.lockfile = lockfile;
            this.resolver = reuseEveryEntry(lockfile, options, warnings);
        }

        String write() {
            return YarnClassicLockRegenerator.writeFilePreservingEol(lock.getText(), LockfileStringifier.stringify(
                    lockfile.getLockfile(resolver.patterns), false, options.isEnableLockfileVersions(),
                    lock.getScenario().nodeVersion()), lock.getScenario().host().getOs());
        }

        @Nullable
        String save() {
            YarnClassicOptions options = regenerator.init(warnings);
            YarnClassicOptions.Flags flags = options.getFlags();
            if (!flags.isLockfile() || flags.isPureLockfile() || flags.isFrozenLockfile()) {
                return null;
            }
            YarnClassicLockRegenerator.InstallCwdRequest request = regenerator.fetchRequestFromCwd(lockfile, options,
                    new ResolutionMap(options), warnings);
            return YarnClassicLockRegenerator.saveLockfileAndIntegrity(resolver, request.getPatterns(),
                    request.getWorkspaceLayout(), options, lockfile, lock.getText(), lock.getScenario().host());
        }

        /**
         * Whether an install over this lock resolves some pattern anew: a root dependency's has no entry, or an
         * entry's version no longer satisfies its pattern's range (a name the resolutions cover aside).
         */
        boolean resolvesAgain() {
            YarnClassicOptions options = regenerator.init(warnings);
            ResolutionMap resolutionMap = new ResolutionMap(options);
            YarnClassicLockRegenerator.InstallCwdRequest request = regenerator.fetchRequestFromCwd(lockfile, options,
                    resolutionMap, warnings);
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
    static PackageResolver reuseEveryEntry(Lockfile lockfile, YarnClassicOptions options, List<String> warnings) {
        PackageResolver resolver = new PackageResolver(options, lockfile);
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
            Map<String, @Nullable Object> entry = lockfile.getLocked(ref.patterns.get(0));
            Manifest pkg = new Manifest();
            pkg.put("name", entry.get("name"));
            pkg.put("version", entry.get("version"));
            pkg.put("dependencies", entry.get("dependencies"));
            pkg.put("optionalDependencies", entry.get("optionalDependencies"));
            return pkg;
        }, warnings));
        return resolver;
    }

    // an entry without `resolved`: the link or directory a link: or file: pattern names
    private static Manifest directoryManifest(String pattern, Map<String, @Nullable Object> entry,
                                              YarnClassicOptions options) {
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
