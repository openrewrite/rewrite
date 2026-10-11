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

    @Test
    void noEngine() {
        assertThatCode(() -> NpmInstallChecks.checkEngine(new LinkedHashMap<>(), "1.3.2", "0.2.1", false))
          .doesNotThrowAnyException();
    }

    @Test
    void emptyEngineObject() {
        assertThatCode(() -> NpmInstallChecks.checkEngine(e(null, null), "1.1.2", "0.2.1", false))
          .doesNotThrowAnyException();
    }

    @Test
    void nodeVersionTooOld() {
        assertThatThrownBy(() -> NpmInstallChecks.checkEngine(e(null, "0.10.24"), "1.1.2", "0.10.18", false))
          .isInstanceOf(NpmInstallChecks.UnsupportedEngine.class)
          .matches(t -> "0.10.24".equals(Js.get(((NpmInstallChecks.UnsupportedEngine) t).getRequired(), "node")))
          .matches(t -> "EBADENGINE".equals(((NpmError) t).getCode()));
    }

    @Test
    void npmVersionTooOld() {
        assertThatThrownBy(() -> NpmInstallChecks.checkEngine(e("^1.4.6", null), "1.3.2", "0.2.1", false))
          .matches(t -> "^1.4.6".equals(Js.get(((NpmInstallChecks.UnsupportedEngine) t).getRequired(), "npm")))
          .matches(t -> "EBADENGINE".equals(((NpmError) t).getCode()));
    }

    @Test
    void forceNodeVersionTooOld() {
        assertThatCode(() -> NpmInstallChecks.checkEngine(e(null, "0.1.0", "test@1.0.0"), "1.3.2", "0.2.1", true))
          .doesNotThrowAnyException();
    }

    @Test
    void cannotForceWhenNpmVersionTooOld() {
        assertThatThrownBy(() -> NpmInstallChecks.checkEngine(e("^1.4.6", null, "test@1.0.0"), "1.3.2", "0.2.1", false))
          .matches(t -> "EBADENGINE".equals(((NpmError) t).getCode()));
    }

    @Test
    void npmPrerelease() {
        assertThatCode(() ->
          NpmInstallChecks.checkEngine(e(">=1.2.3", ">=0.8"), "69.420.0-yolo", "69.420.0-yolo", false))
          .doesNotThrowAnyException();
    }

    @Test
    void nodePrerelease() {
        assertThatCode(() -> NpmInstallChecks.checkEngine(e(">=1.2.3", ">=0.8"), "1.2.3", "69.420.0-yolo", false))
          .doesNotThrowAnyException();
    }

    @Test
    void noNodeVersion() {
        assertThatCode(() -> NpmInstallChecks.checkEngine(e(">=1.2.3", ">=0.8"), "1.2.3", null, false))
          .doesNotThrowAnyException();
    }

    @Test
    void noNpmVersion() {
        assertThatCode(() -> NpmInstallChecks.checkEngine(e(">=1.2.3", ">=0.8"), null, "1.2.3", false))
          .doesNotThrowAnyException();
    }

    private static void platform(String target) {
        platform(target, false, Host.UNKNOWN);
    }

    private static void platform(String target, boolean force, Host environment) {
        NpmInstallChecks.checkPlatform(json(target), force, environment, DARWIN);
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

    // environment.libc || currentEnv.libc(os): a configured libc is checked whatever the machine reports
    @Test
    void configuredLibcIsCheckedOnAnUnknownMachine() {
        Host glibc = Host.builder().libc("glibc").build();
        assertThatThrownBy(() ->
          NpmInstallChecks.checkPlatform(json("{\"libc\":\"musl\"}"), false, glibc, Host.UNKNOWN))
          .matches(NpmInstallChecksTest::badPlatform);
        assertThatCode(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false, glibc, Host.UNKNOWN))
          .doesNotThrowAnyException();
        assertThatCode(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":\"musl\"}"), false,
          Host.builder().os("linux").build(), Host.UNKNOWN)).doesNotThrowAnyException();
    }

    // currentEnv.libc(os) answers for the effective os: a configured os other than linux has no libc
    @Test
    void configuredOsDropsTheMachinesLibc() {
        Host linuxGlibc = Host.builder().os("linux").cpu("x64").libc("glibc").build();
        assertThatCode(() ->
          NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false, Host.UNKNOWN, linuxGlibc))
          .doesNotThrowAnyException();
        assertThatThrownBy(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false,
          Host.builder().os("darwin").build(), linuxGlibc)).matches(NpmInstallChecksTest::badPlatform);
        assertThatThrownBy(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false,
          Host.builder().os("win32").build(), linuxGlibc)).matches(NpmInstallChecksTest::badPlatform);
        assertThatCode(() -> NpmInstallChecks.checkPlatform(json("{\"libc\":[\"glibc\"]}"), false,
          Host.builder().os("linux").build(), linuxGlibc)).doesNotThrowAnyException();
    }

    // node-semver's satisfies answers false where the Range constructor throws, so npm reports EBADENGINE;
    // the platform lists throw as JavaScript does (npm-install-checks 8.0.0 run on these values)
    @Test
    void valuesOfTheWrongType() {
        assertThatThrownBy(() -> NpmInstallChecks.checkEngine(json("{\"_id\":\"p@1\",\"engines\":{\"node\":5}}"),
          "11.17.0", "v20.0.0", false)).isInstanceOf(NpmInstallChecks.UnsupportedEngine.class);
        assertThatThrownBy(() -> NpmInstallChecks.checkEngine(json("{\"_id\":\"p@1\",\"engines\":{\"npm\":true}}"),
          "11.17.0", "v20.0.0", false)).isInstanceOf(NpmInstallChecks.UnsupportedEngine.class);
        assertThatThrownBy(() -> NpmInstallChecks.checkEngine(json("{\"_id\":\"p@1\"," +
                "\"engines\":{\"node\":[\">=1\"]}}"),
          "11.17.0", "v20.0.0", false)).isInstanceOf(NpmInstallChecks.UnsupportedEngine.class);
        assertThatThrownBy(() -> NpmInstallChecks.checkEngine(json("{\"_id\":\"p@1\",\"engines\":{\"node\":{}}}"),
          "11.17.0", "v20.0.0", false)).isInstanceOf(NpmInstallChecks.UnsupportedEngine.class);
        assertThatCode(() -> NpmInstallChecks.checkEngine(json("{\"_id\":\"p@1\",\"engines\":{\"node\":5}}"),
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
          false, Host.UNKNOWN, Host.UNKNOWN)).doesNotThrowAnyException();
        assertThatCode(() -> NpmInstallChecks.checkEngine(e(null, ">=99"), "11.17.0", null, false))
          .doesNotThrowAnyException();
    }
}
