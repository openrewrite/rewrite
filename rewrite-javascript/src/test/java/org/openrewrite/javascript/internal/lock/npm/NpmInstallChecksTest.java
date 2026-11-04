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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;

/**
 * npm-install-checks 8.0.0 test/check-engine.js, test/check-platform.js and test/check-dev-engines.js over a known
 * {@link Host}; the libc detection cases mock the OS, whose libc is an input here.
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

    // test/check-dev-engines.js, whose environment is a plain object by engine

    private static List<NpmInstallChecks.InvalidDevEngine> devEngines(@Nullable Object wanted, String current) {
        return NpmInstallChecks.checkDevEngines(wanted, json(current), NpmRelease.PORTED);
    }

    private static List<NpmInstallChecks.InvalidDevEngine> devEngines(String wanted, String current) {
        return devEngines((Object) json(wanted), current);
    }

    private static NpmInstallChecks.InvalidDevEngine invalid(String engine, String current, String required,
                                                             String... errors) {
        return new NpmInstallChecks.InvalidDevEngine("Invalid devEngines." + engine, Arrays.asList(errors), engine,
          false, true, json(current), json(required));
    }

    private static Map<String, Object> object(String key, @Nullable Object value) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put(key, value);
        return object;
    }

    private static final String NONDESCRIPT = "{\"runtime\":{\"name\":\"nondescript\",\"version\":\"14\"}}";

    @Test
    void noopOptions() {
        assertThat(NpmInstallChecks.checkDevEngines(json("{\"runtime\":[]}"),
          NpmInstallChecks.devEngines(DARWIN, "11.17.0"), NpmRelease.PORTED)).isEmpty();
    }

    @Test
    void unrecognizedProperty() {
        assertThatThrownBy(() -> devEngines("{\"unrecognized\":{\"name\":\"alpha\",\"version\":\"1\"}}",
          "{\"os\":{\"name\":\"alpha\"}}"))
          .isInstanceOf(JsError.class).hasMessage("Invalid property \"devEngines.unrecognized\"");
    }

    @Test
    void emptyDevEngines() {
        assertThat(devEngines("{}", "{\"os\":{\"name\":\"darwin\"}}")).isEmpty();
    }

    @Test
    void invalidName() {
        assertThat(devEngines("{\"os\":{\"name\":\"alpha\",\"onFail\":\"download\"}}", "{\"os\":{\"name\":\"beta\"}}"))
          .containsExactly(invalid("os", "{\"name\":\"beta\"}", "{\"name\":\"alpha\",\"onFail\":\"download\"}",
            "Invalid name \"alpha\" does not match \"beta\" for \"os\""));
    }

    @Test
    void defaultOptions() {
        assertThat(NpmInstallChecks.checkDevEngines(json("{}"), NpmInstallChecks.devEngines(DARWIN, "11.17.0"),
          NpmRelease.PORTED)).isEmpty();
    }

    static Stream<Arguments> nonObjects() {
        List<Object> core = Arrays.asList(1, true, false, null, Js.UNDEFINED);
        List<Arguments> nonObjects = new ArrayList<>();
        for (Object value : core) {
            nonObjects.add(Arguments.of(Js.string(value), value));
        }
        nonObjects.add(Arguments.of("[[]]", singletonList(new ArrayList<>())));
        for (Object value : core) {
            nonObjects.add(Arguments.of("[" + Js.string(value) + "]", singletonList(value)));
        }
        return nonObjects.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonObjects")
    void testsNonObject(String name, @Nullable Object nonObject) {
        assertThatThrownBy(() -> devEngines(nonObject, NONDESCRIPT))
          .isInstanceOf(JsError.class).hasMessage("Invalid non-object value for \"devEngines\"");
        assertThatThrownBy(() -> devEngines(object("runtime", nonObject), NONDESCRIPT))
          .isInstanceOf(JsError.class).hasMessage("Invalid non-object value for \"runtime\"");
    }

    static Stream<Arguments> nonStrings() {
        return Stream.of(1, true, false, null, Js.UNDEFINED, new LinkedHashMap<>(), new ArrayList<>())
          .map(value -> Arguments.of(value instanceof Map ? "{}" : value instanceof List ? "[]" : Js.string(value),
            value));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonStrings")
    void testsNonString(String name, @Nullable Object nonString) {
        Map<String, Object> invalidName = object("name", nonString);
        invalidName.put("version", "14");
        assertThatThrownBy(() -> devEngines(object("runtime", invalidName), NONDESCRIPT))
          .isInstanceOf(JsError.class).hasMessage("Invalid non-string value for \"name\" within \"runtime\"");

        Map<String, Object> invalidVersion = object("name", "nondescript");
        invalidVersion.put("version", nonString);
        assertThatThrownBy(() -> devEngines(object("runtime", invalidVersion), NONDESCRIPT))
          .isInstanceOf(JsError.class).hasMessage("Invalid non-string value for \"version\" within \"runtime\"");

        Map<String, Object> invalidOnFail = object("name", "nondescript");
        invalidOnFail.put("version", "14");
        invalidOnFail.put("onFail", nonString);
        assertThatThrownBy(() -> devEngines(object("runtime", invalidOnFail), NONDESCRIPT))
          .isInstanceOf(JsError.class).hasMessage("Invalid non-string value for \"onFail\" within \"runtime\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"packageManager", "runtime", "cpu", "libc", "os"})
    void testsAllTheRightFields(String env) {
        // current name does not match, wanted has extra attribute
        assertThatThrownBy(() -> devEngines("{\"" + env + "\":{\"name\":\"test-" + env + "-wanted\",\"extra\":\"test-" +
                                            env + "-extra\"}}", "{\"" + env + "\":{\"name\":\"test-" + env +
                                                                "-current\"}}"))
          .isInstanceOf(JsError.class).hasMessage("Invalid property \"extra\" for \"" + env + "\"");
        // current is not given
        assertThatThrownBy(() -> devEngines("{\"" + env + "\":{\"name\":\"test-" + env + "-wanted\"}}", "{}"))
          .isInstanceOf(JsError.class).hasMessage("Unable to determine \"name\" for \"" + env + "\"");
        // name only
        assertThat(devEngines("{\"" + env + "\":{\"name\":\"test-name\"}}",
          "{\"" + env + "\":{\"name\":\"test-name\"}}")).isEmpty();
        // non-semver version is not the same
        assertThat(devEngines("{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"test-version-wanted\"}}",
          "{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"test-version-current\"}}"))
          .containsExactly(invalid(env, "{\"name\":\"test-name\",\"version\":\"test-version-current\"}",
            "{\"name\":\"test-name\",\"version\":\"test-version-wanted\"}",
            "Invalid version \"test-version-wanted\" does not match \"test-version-current\" for \"" + env + "\""));
        // non-semver version is the same
        assertThat(devEngines("{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"test-version\"}}",
          "{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"test-version\"}}")).isEmpty();
        // semver version is not in range
        assertThat(devEngines("{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"^1.0.0\"}}",
          "{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"2.0.0\"}}"))
          .containsExactly(invalid(env, "{\"name\":\"test-name\",\"version\":\"2.0.0\"}",
            "{\"name\":\"test-name\",\"version\":\"^1.0.0\"}",
            "Invalid semver version \"^1.0.0\" does not match \"2.0.0\" for \"" + env + "\""));
        // semver version is in range
        assertThat(devEngines("{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"^1.0.0\"}}",
          "{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"1.0.0\"}}")).isEmpty();
        // returns the last failure
        String alternatives = "[{\"name\":\"test-name\",\"version\":\"test-version-one\"}," +
                              "{\"name\":\"test-name\",\"version\":\"test-version-two\"}]";
        assertThat(devEngines("{\"" + env + "\":" + alternatives + "}",
          "{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"test-version-three\"}}"))
          .containsExactly(invalid(env, "{\"name\":\"test-name\",\"version\":\"test-version-three\"}", alternatives,
            "Invalid version \"test-version-one\" does not match \"test-version-three\" for \"" + env + "\"",
            "Invalid version \"test-version-two\" does not match \"test-version-three\" for \"" + env + "\""));
        // unrecognized onFail
        assertThatThrownBy(() -> devEngines("{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"^1.0.0\"," +
                                            "\"onFail\":\"unrecognized\"}}",
          "{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"1.0.0\"}}"))
          .isInstanceOf(JsError.class).hasMessage("Invalid onFail value \"unrecognized\" for \"" + env + "\"");
        // missing name
        assertThatThrownBy(() -> devEngines("{\"" + env + "\":{\"version\":\"^1.0.0\"}}",
          "{\"" + env + "\":{\"name\":\"test-name\",\"version\":\"1.0.0\"}}"))
          .isInstanceOf(JsError.class).hasMessage("Missing \"name\" property for \"" + env + "\"");
        // invalid name
        assertThat(devEngines("{\"" + env + "\":{\"name\":\"alpha\"}}", "{\"" + env + "\":{\"name\":\"beta\"}}"))
          .containsExactly(invalid(env, "{\"name\":\"beta\"}", "{\"name\":\"alpha\"}",
            "Invalid name \"alpha\" does not match \"beta\" for \"" + env + "\""));
        // missing version
        assertThatThrownBy(() -> devEngines("{\"" + env + "\":{\"name\":\"alpha\",\"version\":\"1\"}}",
          "{\"" + env + "\":{\"name\":\"alpha\"}}"))
          .isInstanceOf(JsError.class)
          .hasMessage("Unable to determine \"version\" for \"" + env + "\" \"alpha\"");
    }

    private static final String SPEC_CURRENT = "{\"os\":{\"name\":\"darwin\",\"version\":\"23.0.0\"}," +
                                               "\"cpu\":{\"name\":\"arm\"},\"libc\":{\"name\":\"glibc\"}," +
                                               "\"runtime\":{\"name\":\"node\",\"version\":\"20.0.0\"}," +
                                               "\"packageManager\":{\"name\":\"yarn\",\"version\":\"3.2.3\"}}";

    @Test
    void spec1() {
        assertThat(devEngines("{\"runtime\":{\"name\":\"node\",\"version\":\">= 20.0.0\",\"onFail\":\"error\"}," +
                              "\"packageManager\":{\"name\":\"yarn\",\"version\":\"3.2.3\",\"onFail\":\"download\"}}",
          SPEC_CURRENT)).isEmpty();
    }

    @Test
    void spec2() {
        String runtime = "[{\"name\":\"bun\",\"version\":\">= 1.0.0\",\"onFail\":\"ignore\"}," +
                         "{\"name\":\"node\",\"version\":\">= 20.0.0\",\"onFail\":\"error\"}]";
        String example = "{\"os\":{\"name\":\"darwin\",\"version\":\">= 23.0.0\"}," +
                         "\"cpu\":[{\"name\":\"arm\"},{\"name\":\"x86\"}],\"libc\":{\"name\":\"glibc\"}," +
                         "\"runtime\":" + runtime + "," +
                         "\"packageManager\":[{\"name\":\"bun\",\"version\":\">= 1.0.0\",\"onFail\":\"ignore\"}," +
                         "{\"name\":\"yarn\",\"version\":\"3.2.3\",\"onFail\":\"download\"}]}";

        assertThat(devEngines(example, SPEC_CURRENT)).isEmpty();

        assertThat(devEngines(example, SPEC_CURRENT.replace("23.0.0", "10.0.0")))
          .containsExactly(invalid("os", "{\"name\":\"darwin\",\"version\":\"10.0.0\"}",
            "{\"name\":\"darwin\",\"version\":\">= 23.0.0\"}",
            "Invalid semver version \">= 23.0.0\" does not match \"10.0.0\" for \"os\""));

        assertThat(devEngines(example, SPEC_CURRENT.replace("\"node\"", "\"nondescript\"")))
          .containsExactly(invalid("runtime", "{\"name\":\"nondescript\",\"version\":\"20.0.0\"}", runtime,
            "Invalid name \"bun\" does not match \"nondescript\" for \"runtime\"",
            "Invalid name \"node\" does not match \"nondescript\" for \"runtime\""));
    }

    @Test
    void emptyArrayAlongSideError() {
        assertThat(devEngines("{\"cpu\":[],\"runtime\":{\"name\":\"bun\",\"onFail\":\"error\"}}",
          "{\"cpu\":{\"name\":\"arm\"},\"runtime\":{\"name\":\"node\",\"version\":\"20.0.0\"}}"))
          .containsExactly(invalid("runtime", "{\"name\":\"node\",\"version\":\"20.0.0\"}",
            "{\"name\":\"bun\",\"onFail\":\"error\"}", "Invalid name \"bun\" does not match \"node\" for \"runtime\""));
    }

    // dev-engines.js: isWarn and isError follow the last alternative's onFail, download as error, neither for ignore
    @Test
    void theLastAlternativesOnFailDecides() {
        String current = "{\"runtime\":{\"name\":\"node\",\"version\":\"20.0.0\"}}";
        for (String onFail : new String[]{"warn", "ignore", "error", "download"}) {
            List<NpmInstallChecks.InvalidDevEngine> failures = devEngines("{\"runtime\":[{\"name\":\"bun\"," +
              "\"onFail\":\"error\"},{\"name\":\"deno\",\"onFail\":\"" + onFail + "\"}]}", current);
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).isWarn()).as(onFail).isEqualTo("warn".equals(onFail));
            assertThat(failures.get(0).isError()).as(onFail).isEqualTo(!"warn".equals(onFail) &&
                                                                       !"ignore".equals(onFail));
        }
        assertThat(devEngines("{\"runtime\":[{\"name\":\"node\",\"onFail\":\"error\"},{\"name\":\"deno\"}]}", current))
          .isEmpty();
    }

    // npm-install-checks 7.1.2 reworded three messages: npm 10.9.5 bundles it and, of npm 11, 11.6.1 and later
    @ParameterizedTest
    @CsvSource({"10.9.0,false", "10.9.4,false", "10.9.5,true", "10.9.9,true", "11.0.0,false", "11.6.0,false",
      "11.6.1,true", "11.17.0,true", "12.2.0,true"})
    void aFailureNamesThePropertyFromNpmInstallChecks712(String release, boolean names) {
        NpmRelease npm = NpmRelease.of(release);
        Map<String, ?> current = json("{\"os\":{\"name\":\"beta\"}}");

        assertThat(NpmInstallChecks.checkDevEngines(json("{\"os\":{\"name\":\"alpha\"}}"), current, npm))
          .extracting(NpmInstallChecks.InvalidDevEngine::getMessage)
          .containsExactly(names ? "Invalid devEngines.os" : "Invalid engine \"os\"");
        assertThatThrownBy(() -> NpmInstallChecks.checkDevEngines("npm", current, npm))
          .hasMessage(names ? "Invalid non-object value for \"devEngines\"" :
            "Invalid non-object value for devEngines");
        assertThatThrownBy(() -> NpmInstallChecks.checkDevEngines(json("{\"engine\":{\"name\":\"v8\"}}"), current, npm))
          .hasMessage(names ? "Invalid property \"devEngines.engine\"" : "Invalid property \"engine\"");
    }

    // current-env.js devEngines over a Host: process.arch, process.platform, the libc detected on linux only,
    // npm's own version and process.version
    @Test
    void theEnvironmentIsTheHosts() {
        Host linux = Host.builder().os("linux").cpu("x64").libc("glibc").nodeVersion("v24.21.0").build();
        Map<String, Object> current = NpmInstallChecks.devEngines(linux, "11.17.0");

        assertThat(NpmInstallChecks.checkDevEngines(json("{\"os\":{\"name\":\"linux\"},\"cpu\":{\"name\":\"x64\"}," +
          "\"libc\":{\"name\":\"glibc\"},\"runtime\":{\"name\":\"node\",\"version\":\">=24\"}," +
          "\"packageManager\":{\"name\":\"npm\",\"version\":\"^11.17.0\"}}"), current, NpmRelease.PORTED)).isEmpty();
        assertThat(NpmInstallChecks.checkDevEngines(json("{\"os\":{\"name\":\"darwin\"},\"cpu\":{\"name\":\"arm64\"}," +
          "\"libc\":{\"name\":\"musl\"},\"runtime\":{\"name\":\"node\",\"version\":\"^22\"}," +
          "\"packageManager\":{\"name\":\"npm\",\"version\":\"^10\"}}"), current, NpmRelease.PORTED))
          .extracting(failure -> failure.getErrors().get(0))
          .containsExactly("Invalid name \"darwin\" does not match \"linux\" for \"os\"",
            "Invalid name \"arm64\" does not match \"x64\" for \"cpu\"",
            "Invalid name \"musl\" does not match \"glibc\" for \"libc\"",
            "Invalid semver version \"^22\" does not match \"v24.21.0\" for \"runtime\"",
            "Invalid semver version \"^10\" does not match \"11.17.0\" for \"packageManager\"");
        // a version that is no range is compared as text, and process.version starts with a v
        assertThat(NpmInstallChecks.checkDevEngines(json("{\"runtime\":{\"name\":\"node\",\"version\":\"lts\"}}"),
          current, NpmRelease.PORTED)).extracting(failure -> failure.getErrors().get(0))
          .containsExactly("Invalid version \"lts\" does not match \"v24.21.0\" for \"runtime\"");
    }

    // libc(osName) is undefined off linux, and null on a linux where neither glibc nor musl is detected
    @Test
    void libcIsDeterminedOnLinuxOnly() {
        Object libc = json("{\"libc\":{\"name\":\"glibc\"}}");
        assertThatThrownBy(() -> NpmInstallChecks.checkDevEngines(libc, NpmInstallChecks.devEngines(DARWIN,
          "11.17.0"), NpmRelease.PORTED))
          .isInstanceOf(JsError.class).hasMessage("Unable to determine \"name\" for \"libc\"");
        assertThatThrownBy(() -> NpmInstallChecks.checkDevEngines(libc, NpmInstallChecks.devEngines(
          Host.builder().os("linux").cpu("x64").build(), "11.17.0"), NpmRelease.PORTED))
          .isInstanceOf(JsError.class).hasMessage("Unable to determine \"name\" for \"libc\"");
        // a cpu or a libc has no version
        assertThatThrownBy(() -> NpmInstallChecks.checkDevEngines(
          json("{\"cpu\":{\"name\":\"arm64\",\"version\":\"1\"}}"), NpmInstallChecks.devEngines(DARWIN, "11.17.0"),
          NpmRelease.PORTED))
          .isInstanceOf(JsError.class).hasMessage("Unable to determine \"version\" for \"cpu\" \"arm64\"");
    }

    @Test
    void whatTheHostDoesNotSaySatisfiesDevEngines() {
        Map<String, Object> unknown = NpmInstallChecks.devEngines(Host.UNKNOWN, "11.17.0");

        assertThat(NpmInstallChecks.checkDevEngines(json("{\"os\":{\"name\":\"enten-os\",\"version\":\">=99\"}," +
          "\"cpu\":[{\"name\":\"enten-cpu\"},{\"name\":\"another-cpu\"}],\"libc\":{\"name\":\"musl\"}," +
          "\"runtime\":{\"name\":\"node\",\"version\":\">=99\"}}"), unknown, NpmRelease.PORTED)).isEmpty();
        // what the host does say is still checked: the runtime's name, npm's own name and version
        assertThat(NpmInstallChecks.checkDevEngines(json("{\"runtime\":{\"name\":\"bun\"}," +
          "\"packageManager\":{\"name\":\"npm\",\"version\":\"^10\"}}"), unknown, NpmRelease.PORTED))
          .extracting(NpmInstallChecks.InvalidDevEngine::getEngine).containsExactly("runtime", "packageManager");
        // the release of an os no host gives
        assertThat(NpmInstallChecks.checkDevEngines(json("{\"os\":{\"name\":\"darwin\",\"version\":\">=99\"}}"),
          NpmInstallChecks.devEngines(DARWIN, "11.17.0"), NpmRelease.PORTED)).isEmpty();
        // a shape npm fails on whatever it runs on is still npm failing
        assertThatThrownBy(() -> NpmInstallChecks.checkDevEngines(json("{\"cpu\":{\"name\":\"arm64\"," +
          "\"onFail\":\"never\"}}"), unknown, NpmRelease.PORTED))
          .isInstanceOf(JsError.class).hasMessage("Invalid onFail value \"never\" for \"cpu\"");
    }

    // npm fails on a version for a cpu only where the cpu is the one named, and reports the other cpu elsewhere
    @Test
    void anUnknownNameThatDecidesWhetherNpmFailsIsCannotKnow() {
        Map<String, Object> unknown = NpmInstallChecks.devEngines(Host.UNKNOWN, "11.17.0");

        assertThatThrownBy(() -> NpmInstallChecks.checkDevEngines(
          json("{\"cpu\":{\"name\":\"arm64\",\"version\":\"1\"}}"), unknown, NpmRelease.PORTED))
          .isInstanceOf(CannotKnow.class)
          .hasMessage("the engine cannot know the cpu npm runs on: where it is arm64 npm fails (Unable to determine " +
                      "\"version\" for \"cpu\" \"arm64\"), and elsewhere devEngines.cpu does not match");
        assertThatThrownBy(() -> NpmInstallChecks.checkDevEngines(
          json("{\"os\":{\"name\":\"linux\",\"version\":5}}"), unknown, NpmRelease.PORTED))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("Invalid non-string value for \"version\" within \"os\"");
    }
}
