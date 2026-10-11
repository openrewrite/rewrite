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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * An install that changes nothing over every pnpm-lock.yaml {@link PnpmScenarios} finds: the lock written back
 * byte-exact, or the resolution, rejection or ignored lock pnpm's log of that install records.
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
        files.putAll(lock.otherLocks());
        PnpmLockRegenerator.Inputs.InputsBuilder inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(lock.getText())
          .lockDir(lock.getScenario().lockDir())
          .manifests(lock.manifests())
          .files(files)
          .paths(PnpmScenarios.paths(files, lock.manifests(), lock.getScenario().lockDir()))
          .userNpmrc(lock.getScenario().userNpmrc())
          .now(lock.now())
          .host(PnpmScenarioTest.host(lock.getScenario().recordedWith()));
        if (lock.getScenario().recordedWith() == null) {
            // a downloaded fixture records no clock and no publish times, so its lock cannot be checked for maturity
            inputs.env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"));
        }
        return inputs.build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("locks")
    void writesBackWhatPnpmWrites(String name, PnpmScenarios.Lock lock) {
        EnvLockfile envLockfile = EnvLockfile.readEnvLockfile(lock.getText());
        if (envLockfile != null) {
            assertThat(EnvLockfile.writeEnvLockfile(lock.getText(), envLockfile)).as("the env document")
              .isEqualTo(lock.getText());
        }
        String log = lock.againLog();
        if (lock.manifests().isEmpty()) {
            assertModelRoundTrip(lock);
            return;
        }
        Install.Result install;
        try {
            install = new PnpmLockRegenerator(inputs(lock),
              new ScenarioRegistry(lock.getScenario(), lock.recordedPhase())).install();
        } catch (LockfileReader.IgnoredLockfile e) {
            String readingLog = lock.getPhase() == Phase.BEFORE ? lock.getScenario().file("pnpm-after.log") :
              log;
            if (readingLog == null) {
                // a downloaded fixture without logs: pnpm 11.2.2 ignores a v5.4 or v6 lock (recorded)
                assertThat(e.getMessage()).isEqualTo("Lockfile pnpm-lock.yaml not compatible with current pnpm");
                return;
            }
            assertThat(ignoredReason(readingLog)).as("pnpm ignores the lock").isEqualTo(e.getMessage().split("\n")[0]);
            return;
        } catch (CannotKnow e) {
            String expect = lock.getScenario().expect();
            assertThat(expect).as(e.getMessage()).startsWith("cannot-know");
            assertThat(e.getSubject()).as("the subject of " + expect).contains(subject(expect));
            return;
        } catch (PnpmError e) {
            assertThat(log).as(e.getCode() + " " + e.getMessage()).isNotNull();
            String[] lines = e.getMessage().split("\n");
            assertThat(log).as("pnpm rejects the lock").contains("[" + e.getCode() + "] " + lines[0]);
            if (lines.length > 1) {
                // the cutoff is the clock's, which the recording keeps to the second
                int cutoff = lines[1].indexOf(" cutoff (");
                assertThat(log).as("the first entry pnpm rejects")
                  .contains(cutoff < 0 ? lines[1] : lines[1].substring(0, cutoff + " cutoff (".length()));
            }
            return;
        } catch (ScenarioRegistry.RouteNotRecorded e) {
            fail(e.getMessage());
            return;
        }
        assertThat(log == null || !verifierRejects(log)).as("pnpm rejects the lock").isTrue();
        boolean resolves = log != null &&
                           (log.contains("Progress: resolved") || log.contains("Merge conflict detected"));
        if (resolves) {
            assertThat(install.getResolution()).as("pnpm resolves on this install").isNotNull();
            if (!install.getCtx().isLockfileHadConflicts()) {
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

    // the last word of an expect line's subject: the file, spec or tarball it names
    private static String subject(String expect) {
        String subject = expect.substring("cannot-know ".length());
        int paren = subject.indexOf(" (");
        if (paren >= 0) {
            subject = subject.substring(0, paren);
        }
        return subject.substring(subject.lastIndexOf(' ') + 1);
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
                // a downloaded fixture without logs: pnpm 11.2.2 ignores a v5.4 or v6 lock (recorded)
                assertThat(e.getMessage()).isEqualTo("Lockfile pnpm-lock.yaml not compatible with current pnpm");
            }
            return;
        }
        assertThat(read.getLockfile()).isNotNull();
        assertThat(LockfileWriter.writeWantedLockfile(read.getLockfile(), lock.getText())).isEqualTo(lock.getText());
    }

    private static boolean verifierRejects(String log) {
        return log.contains("lockfile entries failed verification");
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
    private static boolean nothingToResolve(Install.Result install) {
        Map<String, PackageSnapshot> packages = install.getCtx().getWantedLockfile().getPackages();
        if (packages != null && !packages.isEmpty()) {
            return false;
        }
        for (Verification.Project project : install.getCtx().getProjects()) {
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
