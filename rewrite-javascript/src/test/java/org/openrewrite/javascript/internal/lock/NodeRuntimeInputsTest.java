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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.npm.NpmRelease;
import org.openrewrite.javascript.internal.lock.npm.NpmReleases;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.marker.NodeRuntime;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

/**
 * What a regeneration takes from the {@link NodeRuntime} the parser recorded: the machine where the host gives none,
 * and the npm release that installs the project.
 */
class NodeRuntimeInputsTest {

    private static final NodeRuntime LINUX = runtime(PackageManager.Npm, "11.17.0");

    private static NodeRuntime runtime(@Nullable PackageManager pm, @Nullable String version) {
        return new NodeRuntime(Tree.randomId(), "v22.12.0", "linux", "x64", "musl", pm, version);
    }

    private static ExecutionContext ctx() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        // pinned, so that no real ~/.npmrc or environment is read
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
          new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true, false)));
        return ctx;
    }

    private static Host machine(ExecutionContext ctx, @Nullable NodeRuntime runtime) {
        return HostInputs.of(ctx, null, runtime, Instant.EPOCH).getHost();
    }

    @Test
    void theMachineIsTheExecutionContextsWhereTheHostSetsIt() {
        ExecutionContext ctx = ctx();
        NodeExecutionContextView.view(ctx).setNodeVersion("v24.19.0").setPlatform("darwin", "arm64", null);

        assertThat(machine(ctx, LINUX))
          .isEqualTo(Host.builder().nodeVersion("v24.19.0").os("darwin").cpu("arm64").build());
    }

    @Test
    void elseTheOneTheParserRanOn() {
        assertThat(machine(ctx(), LINUX))
          .isEqualTo(Host.builder().nodeVersion("v22.12.0").os("linux").cpu("x64").libc("musl").build());
    }

    @Test
    void elseUnknown() {
        assertThat(machine(ctx(), null)).isEqualTo(Host.UNKNOWN);

        ExecutionContext ctx = ctx();
        NodeExecutionContextView.view(ctx).setNodeVersion("v24.19.0");
        assertThat(machine(ctx, null)).isEqualTo(Host.builder().nodeVersion("v24.19.0").build());
    }

    // a platform is os, cpu and libc of one machine: the view's when it names an os, else the marker's whole
    @Test
    void thePlatformIsNeverOneFieldFromEachMachine() {
        ExecutionContext nodeOnly = ctx();
        NodeExecutionContextView.view(nodeOnly).setNodeVersion("v24.19.0");
        assertThat(machine(nodeOnly, LINUX))
          .isEqualTo(Host.builder().nodeVersion("v24.19.0").os("linux").cpu("x64").libc("musl").build());

        // a linux the host reports no libc on has none, not the marker's
        ExecutionContext platformOnly = ctx();
        NodeExecutionContextView.view(platformOnly).setPlatform("linux", "arm64", null);
        assertThat(machine(platformOnly, LINUX))
          .isEqualTo(Host.builder().nodeVersion("v22.12.0").os("linux").cpu("arm64").build());

        // a cpu without an os is no platform the host set
        ExecutionContext cpuOnly = ctx();
        NodeExecutionContextView.view(cpuOnly).setPlatform(null, "arm64", null);
        assertThat(machine(cpuOnly, LINUX))
          .isEqualTo(Host.builder().nodeVersion("v22.12.0").os("linux").cpu("x64").libc("musl").build());
    }

    // the marker's release is the one of its package manager, which a port of another does not install as
    @Test
    void theReleaseIsTheOneOfThePackageManagerTheRequestIsFor() {
        HostInputs host = HostInputs.of(ctx(), null, runtime(PackageManager.Pnpm, "10.18.3"), Instant.EPOCH);

        assertThat(host.packageManagerVersion(PackageManager.Pnpm)).isEqualTo("10.18.3");
        assertThat(host.packageManagerVersion(PackageManager.Npm)).isNull();
        assertThat(HostInputs.of(ctx(), null, null, Instant.EPOCH).packageManagerVersion(PackageManager.Npm))
          .isNull();
    }

    private static final String MANIFEST = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\"\n}\n";
    private static final String LOCK = "{\n  \"name\": \"a\",\n  \"version\": \"1.0.0\",\n  \"lockfileVersion\": " +
                                       "3,\n  \"requires\": true,\n  \"packages\": {\n    \"\": {\n      \"name\": " +
                                       "\"a\",\n      \"version\": \"1.0.0\"\n    }\n  }\n}\n";

    private static Request npmRequest(@Nullable NodeRuntime runtime) {
        return Request.builder()
          .packageJson(MANIFEST)
          .lock(LOCK)
          .marker(nodeResolutionResult(PackageManager.Npm))
          .runtime(runtime)
          .packageJsonPath(Paths.get("package.json"))
          .manifest(Paths.get("package.json"), MANIFEST)
          .build();
    }

    private static List<NpmRelease> proven() {
        return NpmReleases.all().stream().filter(NpmReleases::isProven).collect(toList());
    }

    private static NpmRelease installedAs(Request request) {
        HostInputs host = HostInputs.of(ctx(), request.getMarker(), request.getRuntime(), Instant.EPOCH);
        return NpmInputs.from(LockDirectory.of(request), host).getRelease();
    }

    @Test
    void anNpmRequestIsInstalledAsTheProvenReleaseOfItsMarker() {
        for (NpmRelease proven : proven()) {
            assertThat(installedAs(npmRequest(runtime(PackageManager.Npm, proven.toString())))).isEqualTo(proven);
        }
        assertThat(NativeLockEngine.regenerate(NpmInputs.PORT, npmRequest(LINUX), ctx()).getLockFileContent())
          .isEqualTo(LOCK);
    }

    @Test
    void withoutTheMarkerOrAReleaseOfNpmItIsInstalledAs11_17_0() {
        assertThat(installedAs(npmRequest(null))).isEqualTo(NpmRelease.PORTED);
        assertThat(installedAs(npmRequest(runtime(PackageManager.Npm, null)))).isEqualTo(NpmRelease.PORTED);
        assertThat(installedAs(npmRequest(runtime(PackageManager.Pnpm, "10.18.3")))).isEqualTo(NpmRelease.PORTED);
    }

    // a prerelease, npm 6, a release newer than the list
    @ParameterizedTest
    @ValueSource(strings = {"11.17.0-pre.1", "6.14.18", "99.0.0"})
    void anUnknownReleaseIsCannotKnow(String version) {
        assertCannotKnow(version);
    }

    @Test
    void aReleaseTheEngineDoesNotReproduceIsCannotKnow() {
        NpmRelease first = proven().get(0);
        List<NpmRelease> before = NpmReleases.all().subList(0, NpmReleases.all().indexOf(first));
        assumeThat(before).isNotEmpty();

        assertCannotKnow(before.get(before.size() - 1).toString());
    }

    // a failure the port returns, not an exception out of it
    private static void assertCannotKnow(String version) {
        Result result = NativeLockEngine.regenerate(NpmInputs.PORT, npmRequest(runtime(PackageManager.Npm, version)),
          ctx());

        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getErrorMessage()).startsWith("RESOLUTION_REQUIRED: the engine cannot know what npm " +
          version + " writes, since the engine reproduces npm ");
    }
}
