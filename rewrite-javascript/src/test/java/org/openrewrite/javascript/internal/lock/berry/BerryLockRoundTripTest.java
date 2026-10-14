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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsYaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Every yarn.lock {@link BerryScenarios} finds, read with its phase's manifests and config and written back whole: the
 * lock's own bytes, or the recorded rejection, discard, migration or refresh.
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
        YarnBerryLockRegenerator regenerator = new YarnBerryLockRegenerator(lock.inputs(), BerryScenarios.NO_REGISTRY);
        YarnRelease release;
        try {
            release = regenerator.release();
        } catch (CannotKnow e) {
            release = YarnRelease.of(TARGET);
        }
        YarnBerryLockRegenerator.Loaded loaded;
        try {
            loaded = regenerator.load(release, Collections.emptyMap());
        } catch (JsYaml.YamlException e) {
            // the expect line holds the reason; js-yaml's message goes on with the position and snippet, as Yarn's log
            assertThat("pm-fails " + e).as("Yarn fails on the lock").startsWith(expect + " at line ");
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
            migratesAsYarnDoes(lock, before);
            return;
        }
        if (project.lockfileNeedsRefresh) {
            assertThat(lock.getText()).doesNotContain("\n  version: 8\n");
            String refresh = scenario.file("refresh");
            String after = scenario.file("after");
            if ("before".equals(lock.getFileName()) && refresh != null) {
                refreshesAsYarnDoes(lock, Phase.REFRESH, refresh);
            } else if ("before".equals(lock.getFileName()) && after != null && onlyTheReleaseChanges(scenario)) {
                refreshesAsYarnDoes(lock, Phase.AFTER, after);
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
     * A lock older than version 8 under a Yarn 4 install that changes nothing else is refreshed from the registry, its
     * descriptors and checksums kept, under {@code compressionLevel: mixed}: Yarn's recording is the phase's lock.
     */
    private static void refreshesAsYarnDoes(BerryScenarios.Lock lock, Phase phase, String expected) {
        BerryScenarios.Scenario scenario = lock.getScenario();
        YarnBerryLockRegenerator regenerator = new YarnBerryLockRegenerator(scenario.inputs(phase, lock.getText()),
                scenario.registry());
        assertThat(regenerator.install(lock.getText(), new LinkedHashMap<>())).isEqualTo(expected);
    }

    /**
     * A Yarn 1 lock under a Yarn 4 install that changes nothing else migrates, each registry package's checksum from
     * its tarball: Yarn's recording is the phase's lock.
     */
    private static void migratesAsYarnDoes(BerryScenarios.Lock lock, String expected) {
        BerryScenarios.Scenario scenario = lock.getScenario();
        YarnBerryLockRegenerator regenerator = new YarnBerryLockRegenerator(lock.inputs(), scenario.registry());
        assertThat(regenerator.install(lock.getText(), new LinkedHashMap<>())).isEqualTo(expected);
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
