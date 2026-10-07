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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.npm.UriComponent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Every yarn.lock {@link BerryScenarios} finds, read into the project with the manifests and config of its phase,
 * each workspace's package resolved again from its manifest, and the whole file written back: the keys regrouped and
 * sorted, {@code __metadata} from the configuration, every entry rendered by the serializer. The bytes are the
 * lock's own. A lock Yarn rejects fails as the scenario recorded, one it discards is malformed, and one it migrates
 * (Yarn 1) or refreshes (older than version 8) on any install comes out as Yarn's recording of such an install.
 */
class BerryLockRoundTripTest {

    /**
     * The release the round trip renders with when the project names none: the engine's target.
     */
    private static final String TARGET = "4.10.3";

    static Stream<Arguments> locks() {
        List<Arguments> out = new ArrayList<>();
        for (BerryScenarios.Scenario scenario : BerryScenarios.discover()) {
            for (BerryScenarios.Lock lock : scenario.locks()) {
                out.add(Arguments.of(lock.toString(), lock));
            }
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("locks")
    void writesTheLockYarnWrites(String name, BerryScenarios.Lock lock) {
        String expect = lock.getScenario().expect();
        BerryLockRegenerator regenerator = new BerryLockRegenerator(lock.inputs());
        YarnRelease release;
        try {
            release = regenerator.release();
        } catch (CannotKnow e) {
            release = YarnRelease.of(TARGET);
        }
        BerryLockRegenerator.Loaded loaded;
        try {
            loaded = regenerator.load(release);
        } catch (JsYaml.YamlException e) {
            assertThat(expect).as("Yarn rejects the lock").isEqualTo("pm-rejects " + e);
            return;
        }
        Project project = loaded.getProject();

        if (project.lockfileLastVersion == null) {
            assertThat(expect).as("Yarn discards a lock without __metadata").startsWith("malformed");
            return;
        }
        BerryScenarios.Scenario scenario = lock.getScenario();
        if (Integer.valueOf(-1).equals(project.lockfileLastVersion)) {
            String before = scenario.file("before");
            assertThat("lock-seed".equals(lock.getFileName()) ? before : null)
                    .as("Yarn's migration of this Yarn 1 lock").isNotNull();
            migratesAsYarnDoes(lock, loaded, before);
            return;
        }
        if (project.lockfileNeedsRefresh) {
            assertThat(lock.getText()).doesNotContain("\n  version: 8\n");
            String refresh = scenario.file("refresh");
            String after = scenario.file("after");
            if ("before".equals(lock.getFileName()) && refresh != null) {
                refreshesAsYarnDoes(lock, BerryScenarios.Phase.REFRESH, refresh);
            } else if ("before".equals(lock.getFileName()) && after != null && onlyTheReleaseChanges(scenario)) {
                refreshesAsYarnDoes(lock, BerryScenarios.Phase.AFTER, after);
            } else {
                fail("no recording of a Yarn 4 install that changes nothing over this lock");
            }
            return;
        }

        if (lock.manifestsRecorded()) {
            for (Workspace workspace : project.workspaces) {
                Locator anchored = workspace.getAnchoredLocator();
                if (project.originalPackages.containsKey(anchored.locatorHash)) {
                    project.originalPackages.put(anchored.locatorHash, WorkspaceResolver.resolve(anchored, project));
                }
            }
            for (Package pkg : project.originalPackages.values()) {
                if (pkg.reference.startsWith(WorkspaceResolver.PROTOCOL)) {
                    assertThat(project.tryWorkspaceByLocator(pkg)).as("the lock's workspace " + pkg).isNotNull();
                }
            }
        }
        String written = regenerator.write(project);
        assertThat(written == null ? lock.getText() : written).isEqualTo(lock.getText());
    }

    /**
     * A lock older than version 8 under a Yarn 4 install that changes nothing else: every locked package is
     * resolved again from the registry through the resolver chain (the lockfile resolver no longer answers for
     * locators), the stored descriptors and checksums kept, the migration's {@code compressionLevel: mixed} in
     * force; Yarn's recording of that install is the phase's lock.
     */
    private static void refreshesAsYarnDoes(BerryScenarios.Lock lock, BerryScenarios.Phase phase, String expected) {
        BerryScenarios.Scenario scenario = lock.getScenario();
        BerryLockRegenerator regenerator = new BerryLockRegenerator(scenario.inputs(phase, lock.getText()));
        Project project = regenerator.load(regenerator.release()).getProject();
        Resolver chain = chain();
        Resolver resolver = new MultiResolver(new LockfileResolver(chain), chain);
        Resolver.ResolveOptions opts = new Resolver.ResolveOptions(project, resolver, registry(scenario));
        for (Package pkg : new ArrayList<>(project.originalPackages.values())) {
            project.originalPackages.put(pkg.locatorHash, resolver.resolve(pkg, opts));
        }
        assertThat(regenerator.write(project)).isEqualTo(expected);
    }

    /**
     * A Yarn 1 lock under a Yarn 4 install that changes nothing else: every descriptor from the workspaces down
     * resolved through the chain {@code resolveEverything} builds (lockfile, legacy pins, resolvers), each package
     * prepared and its dependencies followed, then each registry package's checksum from its tarball, as the fetch
     * step stores it; Yarn's recording of that install is the phase's lock.
     */
    private static void migratesAsYarnDoes(BerryScenarios.Lock lock, BerryLockRegenerator.Loaded loaded,
                                           String expected) {
        BerryScenarios.Scenario scenario = lock.getScenario();
        Project project = loaded.getProject();
        Resolver chain = chain();
        Resolver resolver = new MultiResolver(new LockfileResolver(chain), loaded.getLegacyMigrationResolver(),
                chain);
        Resolver.ResolveOptions opts = new Resolver.ResolveOptions(project, resolver, registry(scenario));
        Map<String, Descriptor> allDescriptors = new LinkedHashMap<>();
        Map<String, String> allResolutions = new LinkedHashMap<>();
        Map<String, Package> originalPackages = new LinkedHashMap<>();
        Deque<Descriptor> queue = new ArrayDeque<>();
        for (Workspace workspace : project.workspaces) {
            queue.add(workspace.getAnchoredDescriptor());
        }
        while (!queue.isEmpty()) {
            Descriptor descriptor = queue.poll();
            if (allDescriptors.put(descriptor.descriptorHash, descriptor) != null) {
                continue;
            }
            Locator locator = resolver.getCandidates(descriptor, Collections.emptyMap(), opts).get(0);
            allResolutions.put(descriptor.descriptorHash, locator.locatorHash);
            if (!originalPackages.containsKey(locator.locatorHash)) {
                Package originalPkg = resolver.resolve(locator, opts);
                originalPackages.put(originalPkg.locatorHash, originalPkg);
                queue.addAll(project.preparePackage(originalPkg, resolver, opts).dependencies.values());
            }
        }
        project.storedDescriptors = allDescriptors;
        project.storedResolutions = allResolutions;
        project.originalPackages.clear();
        project.originalPackages.putAll(originalPackages);
        for (Package pkg : originalPackages.values()) {
            if (pkg.reference.startsWith("npm:")) {
                byte[] tgz = scenario.http(StructUtils.stringifyIdent(pkg) + "-" +
                                           pkg.reference.substring("npm:".length()) + ".tgz");
                assertThat(tgz).as("the tarball of " + StructUtils.stringifyLocator(pkg)).isNotNull();
                project.storedChecksums.put(pkg.locatorHash, Cache.checksum(tgz, pkg,
                        project.configuration.getOptions()));
            }
        }
        assertThat(new BerryLockRegenerator(lock.inputs()).write(project)).isEqualTo(expected);
    }

    private static Resolver chain() {
        return new MultiResolver(new WorkspaceResolver(), new NpmRemapResolver(), new NpmSemverResolver(),
                new NpmTagResolver());
    }

    private static NpmHttpUtils.Registry registry(BerryScenarios.Scenario scenario) {
        return (url, identUrl) -> {
            byte[] packument = scenario.http(UriComponent.decodeURIComponent(identUrl.substring(1)));
            return packument == null ? null : Js.jsonParse(new String(packument, StandardCharsets.UTF_8));
        };
    }

    // the edit is only the packageManager field: the after phase is a second Yarn over the same project
    private static boolean onlyTheReleaseChanges(BerryScenarios.Scenario scenario) {
        String before = scenario.file("pkg-before");
        String pkg = scenario.file("pkg");
        if (before == null || pkg == null || scenario.has("tree") || scenario.has("yarnrc-after")) {
            return false;
        }
        String field = "\"packageManager\": \"[^\"]*\"";
        return before.replaceAll(field, "").equals(pkg.replaceAll(field, ""));
    }
}
