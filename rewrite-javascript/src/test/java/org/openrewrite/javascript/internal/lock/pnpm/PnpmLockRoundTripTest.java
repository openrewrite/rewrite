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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An install that changes nothing, for every pnpm-lock.yaml {@link PnpmScenarios} finds, with the manifests,
 * pnpm-workspace.yaml and project files of the phase that read or wrote it: the configuration read, the lock loaded
 * into the model, the lock-level settings checked against the configuration, every project checked against its
 * importer, and, when pnpm takes that path, the whole file written from the model. The bytes are the lock's own or
 * what pnpm itself wrote on that install ({@link PnpmScenarios.Lock#againLock()}).
 * <p>
 * Where pnpm's log of that install shows it resolving (or merging a conflicted lock), the model must say pnpm
 * resolves, and the lock is still written back from the model unchanged: the resolution is the resolver's. A lock pnpm
 * ignores ({@code Ignoring broken lockfile}) fails to load with pnpm's reason. Without a recorded install, a lock
 * pnpm wrote is one it would leave as it is (every recorded second install did). A lock with no manifests (some
 * downloaded fixtures) is loaded and written back.
 */
class PnpmLockRoundTripTest {

    static Stream<Arguments> locks() {
        List<Arguments> out = new ArrayList<>();
        for (PnpmScenarios.Scenario scenario : PnpmScenarios.discover()) {
            for (PnpmScenarios.Lock lock : scenario.locks()) {
                out.add(Arguments.of(lock.toString(), lock));
            }
        }
        return out.stream();
    }

    static PnpmLockRegenerator.Inputs inputs(PnpmScenarios.Lock lock) {
        Map<String, String> files = new LinkedHashMap<>(lock.tree());
        if (lock.workspaceManifest() != null) {
            files.put("pnpm-workspace.yaml", lock.workspaceManifest());
        }
        if (lock.npmrc() != null) {
            files.put(".npmrc", lock.npmrc());
        }
        return PnpmLockRegenerator.Inputs.builder()
          .lock(lock.getText())
          .manifests(lock.manifests())
          .files(files)
          .now(lock.now())
          .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("locks")
    void writesBackWhatPnpmWrites(String name, PnpmScenarios.Lock lock) {
        String log = lock.againLog();
        if (lock.manifests().isEmpty()) {
            assertModelRoundTrip(lock);
            return;
        }
        PnpmLockRegenerator.Install install;
        try {
            install = new PnpmLockRegenerator(inputs(lock)).install();
        } catch (LockfileReader.IgnoredLockfile e) {
            String readingLog = lock.isBefore() ? lock.getScenario().file("pnpm-after.log") : log;
            if (readingLog == null) {
                // a downloaded fixture without logs: lock-v54-no-edit and lock-v6-* record pnpm ignoring such a lock
                assertThat(e.getMessage()).isEqualTo("Lockfile pnpm-lock.yaml not compatible with current pnpm");
                return;
            }
            assertThat(ignoredReason(readingLog)).as("pnpm ignores the lock").isEqualTo(e.getMessage().split("\n")[0]);
            return;
        } catch (CannotKnow e) {
            assertThat(lock.getScenario().expect()).as(e.getMessage()).startsWith("cannot-know");
            return;
        }
        if (log != null && verifierRejects(log)) {
            assertThat(install.getWritten()).as("the lock as it stands, which pnpm's verification then rejects")
              .isEqualTo(lock.getText());
            return;
        }
        boolean resolves = log != null &&
                           (log.contains("Progress: resolved") || log.contains("Merge conflict detected"));
        if (resolves) {
            assertThat(install.getResolution()).as("pnpm resolves on this install").isNotNull();
            if (!install.isLockfileHadConflicts()) {
                assertModelRoundTrip(lock);
            }
            return;
        }
        if (install.getResolution() != null && nothingToResolve(install)) {
            // pnpm resolves nothing and prints no progress
            assertModelRoundTrip(lock);
            return;
        }
        assertThat(install.getResolution()).as("pnpm writes the lock without resolving").isNull();
        String expected = log != null ? lock.againLock() : lock.getText();
        assertThat(install.getWritten()).isEqualTo(expected);
    }

    private static void assertModelRoundTrip(PnpmScenarios.Lock lock) {
        LockfileReader.ReadResult read;
        try {
            read = LockfileReader.read(lock.getText());
        } catch (LockfileReader.IgnoredLockfile e) {
            String log = lock.getScenario().file("pnpm-after.log");
            if (log != null) {
                assertThat(log).as("pnpm ignores the lock").contains("Ignoring broken lockfile");
            } else {
                // a downloaded fixture without logs: lock-v54-no-edit and lock-v6-* record pnpm ignoring such a lock
                assertThat(e.getMessage()).isEqualTo("Lockfile pnpm-lock.yaml not compatible with current pnpm");
            }
            return;
        }
        assertThat(read.getLockfile()).isNotNull();
        assertThat(LockfileWriter.writeWantedLockfile(read.getLockfile(), lock.getText())).isEqualTo(lock.getText());
    }

    private static boolean verifierRejects(String log) {
        return log.contains("ERR_PNPM_MINIMUM_RELEASE_AGE_VIOLATION") || log.contains("ERR_PNPM_MISSING_TIME");
    }

    // the reason of pnpm's "Ignoring broken lockfile at <dir>: <reason>", the lock named as the port names it
    private static @Nullable String ignoredReason(String log) {
        for (String line : log.split("\n")) {
            Matcher m = IGNORED.matcher(line);
            if (m.find()) {
                return m.group(2).replace(m.group(1) + "/" + LockfileReader.WANTED_LOCKFILE,
                        LockfileReader.WANTED_LOCKFILE);
            }
        }
        return null;
    }

    private static final Pattern IGNORED = Pattern.compile("Ignoring broken lockfile at (\\S+): (.*)$");

    // nothing for pnpm to resolve: no package in the lock and no dependency in any project
    private static boolean nothingToResolve(PnpmLockRegenerator.Install install) {
        if (install.getWantedLockfile().getPackages() != null && !install.getWantedLockfile().getPackages().isEmpty()) {
            return false;
        }
        for (Verification.Project project : install.getProjects()) {
            for (String field : new String[]{"dependencies", "devDependencies", "optionalDependencies",
              "peerDependencies"}) {
                if (!Verification.field(project.getManifest(), field).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
