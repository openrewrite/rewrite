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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.Host;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;

/**
 * npm-install-checks 8.0.0 test/check-engine.js and test/check-platform.js over a known {@link Host}; the libc
 * detection cases mock the OS, whose libc is an input here.
 */
class NpmInstallChecksTest {

    private static final Host DARWIN = Host.builder().os("darwin").cpu("arm64").build();

    private static Map<String, Object> e(@Nullable String npm, @Nullable String node) {
        return e(npm, node, "pkg@1.2.3");
    }

    private static Map<String, Object> e(@Nullable String npm, @Nullable String node, String id) {
        Map<String, Object> engines = new LinkedHashMap<>();
        if (npm != null) {
            engines.put("npm", npm);
        }
        if (node != null) {
            engines.put("node", node);
        }
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("engines", engines);
        target.put("_id", id);
        return target;
    }

    private static void checkEngine(Map<String, Object> target, @Nullable String npmVer, @Nullable String nodeVer,
                                    boolean force) {
        NpmInstallChecks.checkEngine(target, npmVer, nodeVer, force, NpmRelease.PORTED);
    }

    @Test
    void noEngine() {
        assertThatCode(() -> checkEngine(new LinkedHashMap<>(), "1.3.2", "0.2.1", false))
          .doesNotThrowAnyException();
    }

    @Test
    void emptyEngineObject() {
        assertThatCode(() -> checkEngine(e(null, null), "1.1.2", "0.2.1", false))
          .doesNotThrowAnyException();
    }

    @Test
    void nodeVersionTooOld() {
        assertThatThrownBy(() -> checkEngine(e(null, "0.10.24"), "1.1.2", "0.10.18", false))
          .isInstanceOf(NpmInstallChecks.UnsupportedEngine.class)
          .matches(t -> "0.10.24".equals(Js.get(((NpmInstallChecks.UnsupportedEngine) t).getRequired(), "node")))
          .matches(t -> "EBADENGINE".equals(((NpmError) t).getCode()));
    }

    @Test
    void npmVersionTooOld() {
        assertThatThrownBy(() -> checkEngine(e("^1.4.6", null), "1.3.2", "0.2.1", false))
          .matches(t -> "^1.4.6".equals(Js.get(((NpmInstallChecks.UnsupportedEngine) t).getRequired(), "npm")))
          .matches(t -> "EBADENGINE".equals(((NpmError) t).getCode()));
    }

    @Test
    void forceNodeVersionTooOld() {
        assertThatCode(() -> checkEngine(e(null, "0.1.0", "test@1.0.0"), "1.3.2", "0.2.1", true))
          .doesNotThrowAnyException();
    }

    @Test
    void cannotForceWhenNpmVersionTooOld() {
        assertThatThrownBy(() -> checkEngine(e("^1.4.6", null, "test@1.0.0"), "1.3.2", "0.2.1", false))
          .matches(t -> "EBADENGINE".equals(((NpmError) t).getCode()));
    }

    @Test
    void npmPrerelease() {
        assertThatCode(() ->
          checkEngine(e(">=1.2.3", ">=0.8"), "69.420.0-yolo", "69.420.0-yolo", false))
          .doesNotThrowAnyException();
    }

    @Test
    void nodePrerelease() {
        assertThatCode(() -> checkEngine(e(">=1.2.3", ">=0.8"), "1.2.3", "69.420.0-yolo", false))
          .doesNotThrowAnyException();
    }

    @Test
    void noNodeVersion() {
        assertThatCode(() -> checkEngine(e(">=1.2.3", ">=0.8"), "1.2.3", null, false))
          .doesNotThrowAnyException();
    }

    @Test
    void noNpmVersion() {
        assertThatCode(() -> checkEngine(e(">=1.2.3", ">=0.8"), null, "1.2.3", false))
          .doesNotThrowAnyException();
    }

    // node-semver 7.8.5, which npm 11.18.0 bundles, reads ~22 down to 22.0.0-0 under includePrerelease
    @Test
    void tildeRangeAdmitsAPrereleaseOfItsLowerBoundFrom1118() {
        assertThatThrownBy(() -> NpmInstallChecks.checkEngine(e(null, "~22"), "11.17.0", "v22.0.0-rc.1", false,
          NpmRelease.of("11.17.0"))).isInstanceOf(NpmInstallChecks.UnsupportedEngine.class);
        assertThatCode(() -> NpmInstallChecks.checkEngine(e(null, "~22"), "11.18.0", "v22.0.0-rc.1", false,
          NpmRelease.of("11.18.0"))).doesNotThrowAnyException();
    }

    private static void platform(String target) {
        platform(target, false, Host.UNKNOWN);
    }

    private static void platform(String target, boolean force, Host environment) {
        NpmInstallChecks.checkPlatform(json(target), force, environment, DARWIN, NpmRelease.PORTED);
    }

    private static boolean badPlatform(Throwable t) {
        return t instanceof NpmInstallChecks.UnsupportedPlatform && "EBADPLATFORM".equals(((NpmError) t).getCode());
    }

    @Test
    void targetCpuWrong() {
        assertThatThrownBy(() -> platform("{\"cpu\":\"enten-cpu\",\"os\":\"any\"}"))
          .matches(NpmInstallChecksTest::badPlatform);
    }

    @Test
    void osWrong() {
        assertThatThrownBy(() -> platform("{\"cpu\":\"any\",\"os\":\"enten-os\"}"))
          .matches(NpmInstallChecksTest::badPlatform);
    }

    @Test
    void nothingWrong() {
        assertThatCode(() -> platform("{\"cpu\":\"any\",\"os\":\"any\"}")).doesNotThrowAnyException();
    }

    @Test
    void force() {
        assertThatCode(() -> platform("{\"cpu\":\"enten-cpu\",\"os\":\"any\"}", true, Host.UNKNOWN))
          .doesNotThrowAnyException();
    }

    @Test
    void noOpinions() {
        assertThatCode(() -> platform("{}")).doesNotThrowAnyException();
    }

    @Test
    void onlyTargetCpuWrong() {
        assertThatThrownBy(() -> platform("{\"cpu\":\"enten-cpu\"}")).matches(NpmInstallChecksTest::badPlatform);
    }

    @Test
    void onlyOsWrong() {
        assertThatThrownBy(() -> platform("{\"os\":\"enten-os\"}")).matches(NpmInstallChecksTest::badPlatform);
    }

    @Test
    void everythingWrongWithArrays() {
        assertThatThrownBy(() -> platform("{\"cpu\":[\"enten-cpu\"],\"os\":[\"enten-os\"]}"))
          .matches(NpmInstallChecksTest::badPlatform);
    }

    @Test
    void osWrongNegation() {
        assertThatThrownBy(() -> platform("{\"cpu\":\"any\",\"os\":\"!darwin\"}"))
          .matches(NpmInstallChecksTest::badPlatform);
    }

    @Test
    void nothingWrongNegation() {
        assertThatCode(() -> platform("{\"cpu\":\"!enten-cpu\",\"os\":\"!enten-os\"}")).doesNotThrowAnyException();
    }

    @Test
    void nothingWrongWithOverriddenOs() {
        assertThatCode(() ->
          platform("{\"cpu\":\"any\",\"os\":\"enten-os\"}", false, Host.builder().os("enten-os").build()))
          .doesNotThrowAnyException();
    }

    @Test
    void nothingWrongWithOverriddenCpu() {
        assertThatCode(() ->
          platform("{\"cpu\":\"enten-cpu\",\"os\":\"any\"}", false, Host.builder().cpu("enten-cpu").build()))
          .doesNotThrowAnyException();
    }

    @Test
    void nothingWrongWithOverriddenLibc() {
        assertThatCode(() -> platform("{\"cpu\":\"enten-cpu\",\"libc\":\"enten-libc\"}", false,
          Host.builder().cpu("enten-cpu").libc("enten-libc").build())).doesNotThrowAnyException();
    }

    @Test
    void wrongOsWithOverriddenOs() {
        assertThatThrownBy(() ->
          platform("{\"cpu\":\"any\",\"os\":\"enten-os\"}", false, Host.builder().os("another-os").build()))
          .matches(NpmInstallChecksTest::badPlatform);
    }

    @Test
    void wrongCpuWithOverriddenCpu() {
        assertThatThrownBy(() ->
          platform("{\"cpu\":\"enten-cpu\",\"os\":\"any\"}", false, Host.builder().cpu("another-cpu").build()))
          .matches(NpmInstallChecksTest::badPlatform);
    }

    @Test
    void wrongLibcWithOverriddenLibc() {
        assertThatThrownBy(() -> platform("{\"cpu\":\"enten-cpu\",\"os\":\"any\",\"libc\":\"enten-libc\"}", false,
          Host.builder().libc("another-libc").build())).matches(NpmInstallChecksTest::badPlatform);
    }

    @Test
    void libcFailsWhereTheHostReportsNone() {
        assertThatThrownBy(() -> platform("{\"libc\":\"glibc\"}")).matches(NpmInstallChecksTest::badPlatform);
        assertThatThrownBy(() -> platform("{\"libc\":\"musl\"}")).matches(NpmInstallChecksTest::badPlatform);
    }

    // npm-install-checks 6.0.0 (npm 9.6.2) checks os and cpu alone; 6.1.0 (npm 9.6.3) adds libc
    @Test
    void libcUncheckedBefore9_6_3() {
        assertThatCode(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":\"glibc\"}"), false, Host.UNKNOWN, DARWIN,
          NpmRelease.of("9.6.2"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":\"glibc\"}"), false, Host.UNKNOWN,
          DARWIN, NpmRelease.of("9.6.3"))).matches(NpmInstallChecksTest::badPlatform);
        assertThatThrownBy(() -> NpmInstallChecks.checkPlatform(json("{\"os\":\"linux\",\"libc\":\"glibc\"}"), false,
          Host.UNKNOWN, DARWIN, NpmRelease.of("9.6.2"))).matches(NpmInstallChecksTest::badPlatform);
    }

    // npm-install-checks 6.1.0 (npm 9.6.3 and 9.6.4) looks for glibc under a report field that does not exist, so a
    // glibc machine fails every libc; musl is found by its shared objects
    @Test
    void glibcUndetectedIn9_6_3And9_6_4() {
        Host glibc = Host.builder().os("linux").cpu("x64").libc("glibc").build();
        Host musl = Host.builder().os("linux").cpu("x64").libc("musl").build();
        assertThatThrownBy(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false, Host.UNKNOWN,
          glibc, NpmRelease.of("9.6.4"))).matches(NpmInstallChecksTest::badPlatform);
        assertThatCode(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false, Host.UNKNOWN,
          glibc, NpmRelease.of("9.6.5"))).doesNotThrowAnyException();
        assertThatCode(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":[\"musl\"]}"), false, Host.UNKNOWN,
          musl, NpmRelease.of("9.6.3"))).doesNotThrowAnyException();
    }

    // environment.libc || currentEnv.libc(os): a configured libc is checked whatever the machine reports
    @Test
    void configuredLibcIsCheckedOnAnUnknownMachine() {
        Host glibc = Host.builder().libc("glibc").build();
        assertThatThrownBy(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":\"musl\"}"), false, glibc,
          Host.UNKNOWN, NpmRelease.PORTED)).matches(NpmInstallChecksTest::badPlatform);
        assertThatCode(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false, glibc,
          Host.UNKNOWN, NpmRelease.PORTED)).doesNotThrowAnyException();
        assertThatCode(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":\"musl\"}"), false,
          Host.builder().os("linux").build(), Host.UNKNOWN, NpmRelease.PORTED)).doesNotThrowAnyException();
    }

    // currentEnv.libc(os) answers for the effective os: a configured os other than linux has no libc
    @Test
    void configuredOsDropsTheMachinesLibc() {
        Host linuxGlibc = Host.builder().os("linux").cpu("x64").libc("glibc").build();
        NpmRelease release = NpmRelease.PORTED;
        assertThatCode(() ->
          NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false, Host.UNKNOWN, linuxGlibc, release))
          .doesNotThrowAnyException();
        assertThatThrownBy(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false,
          Host.builder().os("darwin").build(), linuxGlibc, release)).matches(NpmInstallChecksTest::badPlatform);
        assertThatThrownBy(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false,
          Host.builder().os("win32").build(), linuxGlibc, release)).matches(NpmInstallChecksTest::badPlatform);
        assertThatCode(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false,
          Host.builder().os("linux").build(), linuxGlibc, release)).doesNotThrowAnyException();
    }

    // node-semver's satisfies answers false where the Range constructor throws, so npm reports EBADENGINE;
    // the platform lists throw as JavaScript does (npm-install-checks 8.0.0 run on these values)
    @Test
    void valuesOfTheWrongType() {
        assertThatThrownBy(() -> checkEngine(json("{\"_id\":\"p@1\",\"engines\":{\"node\":5}}"),
          "11.17.0", "v20.0.0", false)).isInstanceOf(NpmInstallChecks.UnsupportedEngine.class);
        assertThatThrownBy(() -> checkEngine(json("{\"_id\":\"p@1\",\"engines\":{\"npm\":true}}"),
          "11.17.0", "v20.0.0", false)).isInstanceOf(NpmInstallChecks.UnsupportedEngine.class);
        assertThatThrownBy(() -> checkEngine(json("{\"_id\":\"p@1\"," +
                "\"engines\":{\"node\":[\">=1\"]}}"),
          "11.17.0", "v20.0.0", false)).isInstanceOf(NpmInstallChecks.UnsupportedEngine.class);
        assertThatThrownBy(() -> checkEngine(json("{\"_id\":\"p@1\",\"engines\":{\"node\":{}}}"),
          "11.17.0", "v20.0.0", false)).isInstanceOf(NpmInstallChecks.UnsupportedEngine.class);
        assertThatCode(() -> checkEngine(json("{\"_id\":\"p@1\",\"engines\":{\"node\":5}}"),
          null, "v20.0.0", true)).doesNotThrowAnyException();

        assertThatThrownBy(() -> platform("{\"os\":5}")).hasMessage("list is not iterable");
        assertThatThrownBy(() -> platform("{\"os\":{}}")).hasMessage("list is not iterable");
        assertThatThrownBy(() -> platform("{\"cpu\":true}")).hasMessage("list is not iterable");
        assertThatThrownBy(() -> platform("{\"os\":[\"any\"],\"cpu\":5}")).hasMessage("list is not iterable");
        assertThatThrownBy(() -> platform("{\"os\":[null]}"))
          .hasMessage("Cannot read properties of null (reading 'charAt')");
        assertThatThrownBy(() -> platform("{\"os\":[5]}")).hasMessage("entry.charAt is not a function");
    }

    @Test
    void unknownHostSatisfiesEveryCheck() {
        assertThatCode(() -> NpmInstallChecks.checkPlatform(json("{\"cpu\":\"enten-cpu\",\"os\":\"enten-os\"," +
                "\"libc\":\"musl\"}"),
          false, Host.UNKNOWN, Host.UNKNOWN, NpmRelease.PORTED)).doesNotThrowAnyException();
        assertThatCode(() -> checkEngine(e(null, ">=99"), "11.17.0", null, false))
          .doesNotThrowAnyException();
    }
}
