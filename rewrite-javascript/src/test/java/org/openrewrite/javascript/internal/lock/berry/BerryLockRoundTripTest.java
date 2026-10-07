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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every yarn.lock {@link BerryScenarios} finds, read into the project with the manifests and config of its phase,
 * each workspace's package resolved again from its manifest, and the whole file written back: the keys regrouped and
 * sorted, {@code __metadata} from the configuration, every entry rendered by the serializer. The bytes are the
 * lock's own. A lock Yarn rejects fails as the scenario recorded; one it discards, migrates or refreshes on any
 * install is the resolver's, and the test checks only that the model says so.
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
        if (Integer.valueOf(-1).equals(project.lockfileLastVersion)) {
            assertThat(loaded.getLegacyMigrationResolver().getResolutions())
                    .as("the Yarn 1 entries Yarn pins").isNotEmpty();
            return;
        }
        if (project.lockfileNeedsRefresh) {
            assertThat(lock.getText()).doesNotContain("\n  version: 8\n");
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
}
