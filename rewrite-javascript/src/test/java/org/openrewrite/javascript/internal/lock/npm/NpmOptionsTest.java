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
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.HostEnvironment;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What npm 11.17.0 does with each {@code lockfile-version} an .npmrc can hold, and with each boolean's value
 * (parseField maps four words; arborist reads any other string as truthy).
 */
class NpmOptionsTest {

    private static NpmOptions fromNpmrc(@Nullable String npmrc) {
        return NpmOptions.fromNpmrc(NpmRelease.PORTED, null, npmrc);
    }

    @Test
    void defaults() {
        assertThat(fromNpmrc(null)).isEqualTo(NpmOptions.DEFAULT);
        assertThat(fromNpmrc("")).isEqualTo(NpmOptions.DEFAULT);
        assertThat(NpmOptions.DEFAULT.getLockfileVersion()).isNull();
        assertThat(NpmOptions.DEFAULT.isLegacyPeerDeps()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
      "2, 2", "' 3 ', 3", "+2, 2", "1.5, 1", "2abc, 2", "null,", "undefined,"
    })
    void lockfileVersionNpmAccepts(String value, Integer expected) {
        assertThat(fromNpmrc("lockfile-version=" + value).getLockfileVersion()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
      "4, 4", "abc, NaN", "'', ''", "false, false", "true, NaN", "0, 0", "-1, -1", "0x10, 0"
    })
    void lockfileVersionNpmRejects(String value, String reported) {
        assertThatThrownBy(() -> fromNpmrc("lockfile-version=" + value))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid lockfileVersion config: " + reported);
    }

    // npm 8.0.0's definitions have no lockfile-version: an unknown key, which no flatten reads and nothing checks;
    // 8.1.0 defines it, and from 8.1.2 its type takes the string an npmrc file holds
    @Test
    void lockfileVersionIsNoConfigBefore8_1_0() {
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("8.0.0"), null, "lockfile-version=3\n").getLockfileVersion())
          .isNull();
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("8.1.2"), null, "lockfile-version=3\n").getLockfileVersion())
          .isEqualTo(3);
    }

    @Test
    void bareKeyIsTrue() {
        assertThatThrownBy(() -> fromNpmrc("lockfile-version"))
          .hasMessage("Invalid lockfileVersion config: NaN");
        assertThat(fromNpmrc("legacy-peer-deps").isLegacyPeerDeps()).isTrue();
    }

    // npm 11.17.0's npm-registry-fetch pickRegistry over the same options
    @Test
    void theRegistryIsTheOneTheConfigNamesForThePackage() {
        NpmOptions options = fromNpmrc("registry=https://r.example/npm/\n@corp:registry=https://corp.example/\n");
        assertThat(options.registryFor("lodash")).isEqualTo("https://r.example/npm/");
        assertThat(options.registryFor("@corp/a")).isEqualTo("https://corp.example/");
        assertThat(options.registryFor("@other/a")).isEqualTo("https://r.example/npm/");
        NpmOptions scoped = fromNpmrc("@corp:registry=https://corp.example/\nscope=corp\n");
        assertThat(scoped.registryFor("lodash")).isEqualTo("https://corp.example/");
        assertThat(scoped.registryFor("@corp/a")).isEqualTo("https://corp.example/");
        assertThat(fromNpmrc(null).registryFor("@corp/a")).isEqualTo("https://registry.npmjs.org/");
    }

    // the host's registries are the user's config, the project's .npmrc sets each key over it: recorded with npm
    // 11.17.0 and a --userconfig naming a mirror (lock/npm/open-user-registry-*)
    @Test
    void theHostsRegistriesAreTheUserConfigUnderTheProjectsNpmrc() {
        String user = "registry=https://registry.npmmirror.com/\n@types:registry=https://registry.npmmirror.com/\n";

        NpmOptions userOnly = NpmOptions.fromNpmrc(NpmRelease.PORTED, user, null);
        assertThat(userOnly.registryFor("ms")).isEqualTo("https://registry.npmmirror.com/");
        assertThat(userOnly.registryFor("@types/ms")).isEqualTo("https://registry.npmmirror.com/");

        NpmOptions project = NpmOptions.fromNpmrc(NpmRelease.PORTED, user, "registry=https://registry.yarnpkg.com/\n");
        assertThat(project.registryFor("ms")).isEqualTo("https://registry.yarnpkg.com/");
        assertThat(project.registryFor("@types/ms")).isEqualTo("https://registry.npmmirror.com/");
        assertThat(fromNpmrc("registry=https://registry.yarnpkg.com/\n").registryFor("@types/ms"))
          .isEqualTo("https://registry.yarnpkg.com/");
    }

    // location=global turns global on in its flatten; the definitions flatten in the file's key order, so a
    // later global=false wins (npm 11.17.0: global=false then location=global is ESHRINKWRAPGLOBAL)
    @Test
    void locationGlobalIsGlobal() {
        assertThat(fromNpmrc("location=global").isGlobal()).isTrue();
        assertThat(fromNpmrc("location=\"global\"").isGlobal()).isTrue();
        assertThat(fromNpmrc("location=user").isGlobal()).isFalse();
        assertThat(fromNpmrc("global=false\nlocation=global").isGlobal()).isTrue();
        assertThat(fromNpmrc("location=global\nglobal=false").isGlobal()).isFalse();
        assertThat(fromNpmrc("global=1").isGlobal()).isTrue();
        assertThat(fromNpmrc("global=null").isGlobal()).isFalse();
    }

    // before npm 8.11.0 location's flatten only follows global (lib/utils/config/definitions.js)
    @Test
    void locationGlobalIsGlobalFrom8_11_0() {
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("8.10.0"), null, "location=global").isGlobal()).isFalse();
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("7.20.3"), null, "location=global").isGlobal()).isFalse();
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("8.10.0"), null, "global=true").isGlobal()).isTrue();
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("8.11.0"), null, "location=global").isGlobal()).isTrue();
    }

    // each value installed with npm 11.17.0: an added package's peer chain stays out of the lock where it is true
    @ParameterizedTest
    @CsvSource({
      "true, true", "false, false", "0, true", "FALSE, true", "True, true", "yes, true", "00, true", "2, true",
      "undefined, false", "null, false", "'', true", "'true ', true", "' false', false"
    })
    void booleansAreNpmConfigsTruthiness(String value, boolean expected) {
        assertThat(fromNpmrc("legacy-peer-deps=" + value).isLegacyPeerDeps()).isEqualTo(expected);
        assertThat(fromNpmrc("install-links=" + value).isInstallLinks()).isEqualTo(expected);
        assertThat(fromNpmrc("omit-lockfile-registry-resolved=" + value).isOmitLockfileRegistryResolved())
          .isEqualTo(expected);
    }

    @Test
    void otherKeysPassOver() {
        NpmOptions options = fromNpmrc("# comment\n; another\nforce=true\nlegacy-peer-deps=true\n\n" +
                                                  "install-links=false\nomit-lockfile-registry-resolved=1\n" +
                                                  "lockfile-version=3\n");
        assertThat(options.getLockfileVersion()).isEqualTo(3);
        assertThat(options.isLegacyPeerDeps()).isTrue();
        assertThat(options.isInstallLinks()).isFalse();
        assertThat(options.isOmitLockfileRegistryResolved()).isTrue();
    }

    // installed with npm 11.17.0: ini strips matching quotes and an unescaped ; or # comment first, so "false" and
    // 'false' leave the peer chain in, "true" and "0" keep it out
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', ignoreLeadingAndTrailingWhitespace = false, value = {
      "\"false\"|false", "'false'|false", "\"true\"|true", "'true'|true", "\"0\"|true", "'0'|false", "\"null\"|false",
      "'null'|false", "\"undefined\"|false", "\" false\"|false", "\"FALSE\"|true", "\"abc\"|true", "\"|true", "'|true",
      "\"\"|true", "''|true", "\"abc|true", "abc\"|true", "'[false]'|true", "'{}'|true", "\"\\u0066alse\"|false",
      "false ; comment|false", "false # comment|false", "false\\;x|true", "\\false|true", "fal se|true", "false;|false",
      "\tfalse|false", "false\t|false", ";|true", "#|true", "\\|true"
    })
    void iniQuotesCommentsAndEscapes(String value, boolean expected) {
        assertThat(fromNpmrc("legacy-peer-deps=" + value + "\n").isLegacyPeerDeps()).isEqualTo(expected);
    }

    // installed with npm 11.17.0: "2" and '2' write a v2 lock, "abc" exits 1 with NaN
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', ignoreLeadingAndTrailingWhitespace = false, value = {
      "\"2\"|2", "'2'|2", "'1.5'|1", "'+2'|2", "'[2]'|2", "'[\"2\",\"3\"]'|2", "'null'|", "'undefined'|", "\"null\"|",
      "\" 2\"|2", "\"\\u00a02\"|2", "\"2\\n\"|2", "'\"2\"'|2", "'3 4'|3", "2'|2", "2 ; two|2", "2 # two|2", "2\\;3|2",
      "1e3|1", "2,3|2", "\u00a02|2", "03|3", "2-|2", "1_0|1", "2.9|2"
    })
    void iniLockfileVersionNpmAccepts(String value, Integer expected) {
        assertThat(fromNpmrc("lockfile-version=" + value + "\n").getLockfileVersion()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', ignoreLeadingAndTrailingWhitespace = false, value = {
      "\"abc\"|NaN", "'abc'|NaN", "'1e3'|1000", "'007'|7", "'-0'|0", "'[null,\"2\"]'|NaN", "'{}'|NaN", "'true'|NaN",
      "'false'|false", "\"\"|", "''|", "\"|NaN", "'|", "\"2|NaN", "'[]'|NaN", "'[true]'|NaN", "\\2|NaN", "0x2|0",
      "-2|-2", "Infinity|NaN", "00|0", "+-2|NaN", "٢|NaN"
    })
    void iniLockfileVersionNpmRejects(String value, String reported) {
        assertThatThrownBy(() -> fromNpmrc("lockfile-version=" + value + "\n"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid lockfileVersion config: " + (reported == null ? "" : reported));
    }

    @Test
    void iniKeys() {
        assertThat(fromNpmrc(" legacy-peer-deps = true \n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("\"legacy-peer-deps\"=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("'[\"legacy-peer-deps\"]'=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("Legacy-Peer-Deps=true\n").isLegacyPeerDeps()).isFalse();
        assertThat(fromNpmrc("legacy-peer-deps;x=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("legacy-peer\\;-deps=true\n").isLegacyPeerDeps()).isFalse();
        assertThat(fromNpmrc("legacy-peer-deps==true\n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("legacy-peer-deps=true\nlegacy-peer-deps=false\n").isLegacyPeerDeps())
          .isFalse();
        assertThat(fromNpmrc("legacy-peer-deps=true\r\nlockfile-version=2\r\n").getLockfileVersion())
          .isEqualTo(2);
        assertThat(fromNpmrc("legacy-peer-deps=true\rlockfile-version=2\r").getLockfileVersion())
          .isEqualTo(2);
        assertThat(fromNpmrc("\ufefflegacy-peer-deps=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("\u00a0# x\nlegacy-peer-deps=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("legacy-peer-deps=true\u2028lockfile-version=2\n"))
          .isEqualTo(NpmOptions.DEFAULT);
        assertThat(fromNpmrc("a.b=null\n__proto__=true\nlegacy-peer-deps=true\n").isLegacyPeerDeps())
          .isTrue();
        assertThatThrownBy(() -> fromNpmrc("'null'=true\n"))
          .hasMessage("Cannot read properties of null (reading 'length')");
    }

    // an array value is truthy whatever it holds, and parseInt reads String(array)
    @Test
    void iniArrays() {
        assertThat(fromNpmrc("legacy-peer-deps[]=false\n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("legacy-peer-deps=true\nlegacy-peer-deps[]=false\n").isLegacyPeerDeps())
          .isTrue();
        assertThat(fromNpmrc("[]=true\n")).isEqualTo(NpmOptions.DEFAULT);
        assertThat(fromNpmrc("lockfile-version[]=2\nlockfile-version[]=3\n").getLockfileVersion())
          .isEqualTo(2);
        assertThat(fromNpmrc("lockfile-version[]=2\nlockfile-version=3\n").getLockfileVersion())
          .isEqualTo(2);
        assertThatThrownBy(() -> fromNpmrc("lockfile-version[]=null\nlockfile-version[]=2\n"))
          .hasMessage("Invalid lockfileVersion config: NaN");
        assertThatThrownBy(() -> fromNpmrc("lockfile-version[]=true\n"))
          .hasMessage("Invalid lockfileVersion config: NaN");
        assertThatThrownBy(() -> fromNpmrc("lockfile-version[]=\n"))
          .hasMessage("Invalid lockfileVersion config: NaN");
    }

    // a section hides the keys under it; a section named like an option is an object, truthy and without a
    // string form; a dotted section nests
    @Test
    void iniSections() {
        assertThat(fromNpmrc("[section]\nlegacy-peer-deps=true\nlockfile-version=2\n"))
          .isEqualTo(NpmOptions.DEFAULT);
        assertThat(fromNpmrc("[section]   \nlegacy-peer-deps=true\n")).isEqualTo(NpmOptions.DEFAULT);
        assertThat(fromNpmrc("[section]\nx=1\n[]\nlegacy-peer-deps=true\n")).isEqualTo(NpmOptions.DEFAULT);
        assertThat(fromNpmrc("[__proto__]\nlegacy-peer-deps=true\n")).isEqualTo(NpmOptions.DEFAULT);
        assertThat(fromNpmrc("[legacy-peer-deps]\n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("[\"legacy-peer-deps\"]\nx=1\n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("legacy-peer-deps=false\n[legacy-peer-deps]\nx=1\n").isLegacyPeerDeps())
          .isTrue();
        assertThat(fromNpmrc("legacy-peer-deps=true\n[legacy-peer-deps]\nx=1\n").isLegacyPeerDeps())
          .isTrue();
        assertThat(fromNpmrc("lockfile-version=2\n[lockfile-version]\nx=1\n").getLockfileVersion())
          .isEqualTo(2);
        assertThat(fromNpmrc("[a.legacy-peer-deps]\nx=1\n").isLegacyPeerDeps()).isFalse();
        assertThat(fromNpmrc("[legacy-peer-deps.b]\nx=1\n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("[legacy-peer\\.deps]\nx=1\n").isLegacyPeerDeps()).isFalse();
        assertThatThrownBy(() -> fromNpmrc("[lockfile-version]\n"))
          .hasMessage("Cannot convert object to primitive value");
        assertThatThrownBy(() -> fromNpmrc("[lockfile-version]\nlockfile-version=2\n"))
          .hasMessage("Cannot convert object to primitive value");
        assertThatThrownBy(() -> fromNpmrc("a=null\n[a.b]\nlegacy-peer-deps=true\n"))
          .hasMessage("Cannot set properties of null (setting 'b')");
    }

    // the host's environment rule: a ${VAR} the host does not know, in a value read or in a key that could expand to
    // one, is cannot-know; an escaped reference is the literal text
    @Test
    void environmentReferences() {
        assertThatThrownBy(() -> fromNpmrc("lockfile-version=${LFV}\n"))
          .isInstanceOf(CannotKnow.class)
          .hasMessage("the engine cannot know the environment variable LFV a config file names (the host does not " +
                      "know the environment)");
        assertThatThrownBy(() -> fromNpmrc("legacy-peer-deps=${LPD?}\n"))
          .isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> fromNpmrc("legacy-peer-deps=\"${LPD}\"\n"))
          .isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> fromNpmrc("${K}=true\n"))
          .isInstanceOf(CannotKnow.class)
          .hasMessage("the engine cannot know the environment variable K a config file names (the host does not " +
                      "know the environment)");
        assertThatThrownBy(() -> fromNpmrc("${K}-peer-deps=true\n"))
          .isInstanceOf(CannotKnow.class);
        assertThat(fromNpmrc("//${REG}/:_authToken=x\nlegacy-peer-deps=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("//r/:_authToken=${TOKEN}\nlegacy-peer-deps=true\n").isLegacyPeerDeps())
          .isTrue();
        assertThat(fromNpmrc("\\${K}=true\nlegacy-peer-deps=true\n").isLegacyPeerDeps()).isTrue();
        assertThatThrownBy(() -> fromNpmrc("lockfile-version=\\${LFV}\n"))
          .hasMessage("Invalid lockfileVersion config: NaN");
        assertThatThrownBy(() -> fromNpmrc("lockfile-version=${}\n"))
          .hasMessage("Invalid lockfileVersion config: NaN");
    }

    // @npmcli/config 10.11.0 test/env-replace.js (npm 11.17.0), every case, under the environment it names
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "${foo}|bar",
      "${foo?}|bar",
      "${foo}${bar}|barbaz",
      "${foo?}${baz?}|bar",
      "\\${foo}|${foo}",
      "\\\\${foo}|\\bar",
      "\\\\\\${foo}|\\${foo}",
      "${baz}|${baz}",
      "\\${baz}|${baz}",
      "\\\\${baz}|\\${baz}",
      "\\${foo?}|${foo?}",
      "\\\\${foo?}|\\bar",
      "${baz?}|''",
      "\\${baz?}|${baz?}",
      "\\\\${baz?}|\\"
    })
    void envReplaceAsNpmsOwnTests(String f, String expected) {
        Map<String, String> env = new HashMap<>();
        env.put("foo", "bar");
        env.put("bar", "baz");
        assertThat(NpmOptions.envReplace(f, HostEnvironment.of(env))).isEqualTo(expected);
    }

    // installed with npm 11.17.0: a truthy global-style or legacy-bundling overrides install-strategy wherever it
    // stands, the later of the two wins, and a value arborist does not know hoists
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "legacy-bundling=true+install-strategy=hoisted|NESTED",
      "install-strategy=nested+global-style=true|SHALLOW",
      "global-style=true+install-strategy=nested|SHALLOW",
      "global-style=true+legacy-bundling=true|NESTED",
      "legacy-bundling=true+global-style=true|SHALLOW",
      "legacy-bundling=true+install-strategy=bogus|NESTED",
      "install-strategy=bogus|HOISTED",
      "install-strategy=nested|NESTED",
      "install-strategy=shallow|SHALLOW",
      "install-strategy=linked|LINKED",
      "install-strategy=null|HOISTED",
      "global-style=false+install-strategy=nested|NESTED",
      "legacy-bundling=0+install-strategy=shallow|NESTED"
    })
    void installStrategyFlattensAsNpmDoes(String npmrc, NpmOptions.InstallStrategy expected) {
        assertThat(fromNpmrc(npmrc.replace('+', '\n') + "\n").getInstallStrategy()).isEqualTo(expected);
    }

    private static NpmOptions fromNpmrc(String release, String npmrc) {
        return NpmOptions.fromNpmrc(NpmRelease.of(release), null, npmrc.replace('+', '\n') + "\n");
    }

    // lib/utils/config/definitions.js of each release: a key it does not define never reaches arborist
    @Test
    void aKeyCountsFromTheReleaseThatDefinesIt() {
        assertThat(fromNpmrc("9.6.7", "prefer-dedupe=true").isPreferDedupe()).isFalse();
        assertThat(fromNpmrc("9.7.0", "prefer-dedupe=true").isPreferDedupe()).isTrue();
        assertThat(fromNpmrc("8.7.0", "install-links=true").isInstallLinks()).isFalse();
        assertThat(fromNpmrc("8.8.0", "install-links=true").isInstallLinks()).isTrue();
        assertThat(fromNpmrc("8.9.0", "omit-lockfile-registry-resolved=true").isOmitLockfileRegistryResolved())
          .isFalse();
        assertThat(fromNpmrc("8.10.0", "omit-lockfile-registry-resolved=true").isOmitLockfileRegistryResolved())
          .isTrue();
    }

    // install-links defaults to true in npm 9.0.0 until 9.4.2
    @Test
    void installLinksDefault() {
        assertThat(fromNpmrc("8.19.4", "").isInstallLinks()).isFalse();
        assertThat(fromNpmrc("9.0.0", "").isInstallLinks()).isTrue();
        assertThat(fromNpmrc("9.4.1", "").isInstallLinks()).isTrue();
        assertThat(fromNpmrc("9.4.1", "install-links=false").isInstallLinks()).isFalse();
        assertThat(fromNpmrc("9.4.2", "").isInstallLinks()).isFalse();
    }

    // install-strategy is a config from npm 9.0.0 and takes linked from 9.4.0; before 9.0.0 global-style and
    // legacy-bundling are arborist's own options, legacyBundling read first; 9.1.0 and 9.2.0 flatten the validated
    // config, which drops a value of none of the type's
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "8.19.4|install-strategy=nested|HOISTED",
      "8.19.4|global-style=true|SHALLOW",
      "8.19.4|global-style=true+legacy-bundling=true|NESTED",
      "8.19.4|legacy-bundling=true+global-style=true|NESTED",
      "8.19.4|legacy-bundling=true+legacy-bundling=false|HOISTED",
      "9.0.0|install-strategy=nested|NESTED",
      "9.3.1|install-strategy=linked|HOISTED",
      "9.2.0|install-strategy=linked|HOISTED",
      "9.4.0|install-strategy=linked|LINKED"
    })
    void installStrategyOfTheRelease(String release, String npmrc, NpmOptions.InstallStrategy expected) {
        assertThat(fromNpmrc(release, npmrc).getInstallStrategy()).isEqualTo(expected);
    }

    // nopt's clean drops the project's invalid value, so the user's own counts
    @Test
    void aValueValidationDropsLeavesTheUsersFileIts() {
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("9.2.0"), "install-strategy=shallow\n",
          "install-strategy=linked\n").getInstallStrategy()).isEqualTo(NpmOptions.InstallStrategy.SHALLOW);
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("8.19.4"), "lockfile-version=2\n", "lockfile-version=4\n")
          .getLockfileVersion()).isEqualTo(2);
    }

    // before npm 8.1.2 lockfile-version's type is [null, 1, 2, 3], which no string an npmrc file holds is; npm 8
    // validates the config before flattening it and drops such a value, as do 9.1.0 and 9.2.0, while the others
    // flatten it as written and arborist refuses it
    @Test
    void lockfileVersionOfTheRelease() {
        assertThat(fromNpmrc("8.1.1", "lockfile-version=1").getLockfileVersion()).isNull();
        assertThat(fromNpmrc("8.1.2", "lockfile-version=1").getLockfileVersion()).isEqualTo(1);
        assertThat(fromNpmrc("8.19.4", "lockfile-version=4").getLockfileVersion()).isNull();
        assertThat(fromNpmrc("9.1.0", "lockfile-version=4").getLockfileVersion()).isNull();
        assertThatThrownBy(() -> fromNpmrc("9.0.0", "lockfile-version=4"))
          .hasMessage("Invalid lockfileVersion config: 4");
    }

    // nopt's validateBoolean makes a number of a string, where the config is validated before it is flattened
    @Test
    void booleansOfAValidatedConfig() {
        assertThat(fromNpmrc("8.19.4", "legacy-peer-deps=0").isLegacyPeerDeps()).isFalse();
        assertThat(fromNpmrc("8.19.4", "legacy-peer-deps=00").isLegacyPeerDeps()).isFalse();
        assertThat(fromNpmrc("8.19.4", "legacy-peer-deps=2").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("8.19.4", "legacy-peer-deps=yes").isLegacyPeerDeps()).isTrue();
        assertThat(fromNpmrc("9.2.0", "legacy-peer-deps=0").isLegacyPeerDeps()).isFalse();
        assertThat(fromNpmrc("9.0.0", "legacy-peer-deps=0").isLegacyPeerDeps()).isTrue();
    }

    // nopt's validateDate drops a date Date.parse cannot read, where the config is validated before it is flattened
    @Test
    void beforeOfAValidatedConfig() {
        Instant now = Instant.parse("2026-10-06T12:00:00Z");
        assertThat(fromNpmrc("8.19.4", "before=abc").before(now)).isNull();
        assertThat(fromNpmrc("8.19.4", "before=2021-04-23").before(now))
          .isEqualTo((double) Instant.parse("2021-04-23T00:00:00Z").toEpochMilli());
        assertThat(fromNpmrc("9.0.0", "before=abc").before(now)).isEqualTo(Double.NaN);
        assertThat(fromNpmrc("9.0.0", "before=").before(now)).isNull();
    }

    // installed with npm 11.17.0: before=abc exits 1 with "Invalid time value" at the first fetch, since nopt
    // rejects the value only after the options are flattened; min-release-age is npm's `age ? new Date(...) : null`
    @Test
    void beforeAndMinReleaseAge() {
        Instant now = Instant.parse("2026-10-06T12:00:00Z");
        double day = 86400000d;
        assertThat(fromNpmrc("before=2021-04-23T16:24:00.000Z\n").before(now))
          .isEqualTo((double) Instant.parse("2021-04-23T16:24:00Z").toEpochMilli());
        assertThat(fromNpmrc("before=2021-04-23\n").before(now))
          .isEqualTo((double) Instant.parse("2021-04-23T00:00:00Z").toEpochMilli());
        assertThat(fromNpmrc("before=abc\n").before(now)).isEqualTo(Double.NaN);
        assertThat(fromNpmrc("before=\n").before(now)).isEqualTo(Double.NaN);
        assertThat(fromNpmrc("before=true\n").before(now)).isEqualTo(Double.NaN);
        assertThat(fromNpmrc("before=false\n").before(now)).isNull();
        assertThat(fromNpmrc("before=null\n").before(now)).isNull();
        assertThat(fromNpmrc("min-release-age=7\n").before(now)).isEqualTo(now.toEpochMilli() - 7 * day);
        assertThat(fromNpmrc("min-release-age\n").before(now)).isEqualTo(now.toEpochMilli() - day);
        assertThat(fromNpmrc("min-release-age=0\n").before(now)).isNull();
        assertThat(fromNpmrc("min-release-age=\n").before(now)).isNull();
        // ini itself reads the literal as null, quoted or not
        assertThat(fromNpmrc("min-release-age=null\n").before(now)).isNull();
        assertThat(fromNpmrc("min-release-age=\"null\"\n").before(now)).isNull();
        assertThat(fromNpmrc("min-release-age=NaN\n").before(now)).isEqualTo(Double.NaN);
        assertThat(fromNpmrc("min-release-age=abc\n").before(now)).isEqualTo(Double.NaN);
        assertThat(fromNpmrc("min-release-age=Infinity\n").before(now)).isEqualTo(Double.NaN);
        assertThat(fromNpmrc("min-release-age=1e12\n").before(now)).isEqualTo(Double.NaN);
        assertThat(fromNpmrc("min-release-age=7\nbefore=null\n").before(now)).isNull();
        assertThat(fromNpmrc("before=2021-04-23\nmin-release-age=7\n").before(now))
          .isEqualTo((double) Instant.parse("2021-04-23T00:00:00Z").toEpochMilli());
    }

    // node 24: a string without an ASCII digit is never a date (300,000 random ones tried); one with a digit
    // outside the date-time string format may be one through V8's legacy parser, which is not ported
    @Test
    void beforeOnlyTheLegacyDateParserReadsIsKeptAsText() {
        Instant now = Instant.parse("2026-10-06T12:00:00Z");
        assertThat(fromNpmrc("before=2021/04/23\n").before(now)).isEqualTo("2021/04/23");
        assertThat(fromNpmrc("before=Apr 23 2021\n").before(now)).isEqualTo("Apr 23 2021");
        assertThat(fromNpmrc("before=0\n").before(now)).isEqualTo("0");
        assertThat(fromNpmrc("before=2021-4-23\n").before(now)).isEqualTo("2021-4-23");
        assertThat(fromNpmrc("before= 2021-04-23 \n").before(now))
          .isEqualTo((double) Instant.parse("2021-04-23T00:00:00Z").toEpochMilli());
        assertThat(fromNpmrc("before=Apr\n").before(now)).isEqualTo(Double.NaN);
        assertThat(fromNpmrc("before=now\n").before(now)).isEqualTo(Double.NaN);
        assertThat(fromNpmrc("before=\u0661\u0662\n").before(now)).isEqualTo(Double.NaN);
    }

    // pacote reads `opts.defaultTag || 'latest'`
    @Test
    void tag() {
        assertThat(fromNpmrc("").getTag()).isEqualTo("latest");
        assertThat(fromNpmrc("tag=beta\n").getTag()).isEqualTo("beta");
        assertThat(fromNpmrc("tag=\n").getTag()).isNull();
        assertThat(fromNpmrc("tag=null\n").getTag()).isNull();
    }

    // installed with npm 11.17.0: allow-directory=bogus still refuses a non-root directory dependency
    // (EALLOWDIRECTORY), as nopt's rejection comes after the options are flattened; arborist reads `?? 'all'`
    @Test
    void allowValuesAreReadAsArboristReads() {
        assertThat(fromNpmrc("allow-git=null\n").getAllowGit()).isEqualTo("all");
        assertThat(fromNpmrc("allow-git=undefined\n").getAllowGit()).isEqualTo("all");
        assertThat(fromNpmrc("allow-directory=root\n").getAllowDirectory()).isEqualTo("root");
        assertThat(fromNpmrc("allow-directory=bogus\n").getAllowDirectory()).isEqualTo("bogus");
        assertThat(fromNpmrc("allow-file\n").getAllowFile()).isEqualTo("true");
        assertThat(fromNpmrc("allow-remote=\n").getAllowRemote()).isEmpty();
        assertThat(NpmOptions.DEFAULT.getAllowGit()).isEqualTo("all");
    }

    private static final String NPMJS = "//registry.npmjs.org/";

    private static String invalidAuth(String problems) {
        return "Invalid auth configuration found: " + problems.replace("; ", ", ") +
               "\nPlease run `npm config fix` to repair your configuration.`";
    }

    // each .npmrc installed with npm 11.17.0, the first three with 9.0.0, 10.0.0, 10.9.9 and 12.2.0 as well: the
    // config's validate() refuses an auth key outside a registry's scope (ERR_INVALID_AUTH), ahead of arborist's own
    // reading of lockfile-version. npm 8.19.4 writes the lock for each of the first three.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "_authToken=abc | `_authToken` must be renamed to `//registry.npmjs.org/:_authToken` in project config",
      "username=u | `username` is not allowed in project config",
      "username=u+_password=cA== | `username` must be renamed to `//registry.npmjs.org/:username` in project config; " +
      "`_password` must be renamed to `//registry.npmjs.org/:_password` in project config",
      "_password=cA== | `_password` is not allowed in project config",
      "username=u+_password=false | `username` is not allowed in project config",
      "_auth=dTpw | `_auth` must be renamed to `//registry.npmjs.org/:_auth` in project config",
      "_authtoken=abc | `_authtoken` is not allowed in project config",
      "-authtoken=abc+_authtoken=def+_auth=x+_authToken=y+username=u+_password=p | " +
      "`_authtoken` is not allowed in project config; `-authtoken` is not allowed in project config; " +
      "`_auth` must be renamed to `//registry.npmjs.org/:_auth` in project config; " +
      "`_authToken` must be renamed to `//registry.npmjs.org/:_authToken` in project config; " +
      "`username` must be renamed to `//registry.npmjs.org/:username` in project config; " +
      "`_password` must be renamed to `//registry.npmjs.org/:_password` in project config",
      "_authToken=true | `_authToken` must be renamed to `//registry.npmjs.org/:_authToken` in project config",
      "_authToken | `_authToken` must be renamed to `//registry.npmjs.org/:_authToken` in project config",
      "_authToken=0 | `_authToken` must be renamed to `//registry.npmjs.org/:_authToken` in project config",
      "[_authToken]+x=1 | `_authToken` must be renamed to `//registry.npmjs.org/:_authToken` in project config",
      "_authToken[]=a | `_authToken` must be renamed to `//registry.npmjs.org/:_authToken` in project config",
      "_auth=undefined | `_auth` must be renamed to `//registry.npmjs.org/:_auth` in project config",
      "_auth | `_auth` must be renamed to `//registry.npmjs.org/:_auth` in project config",
      "_auth[]=a | `_auth` must be renamed to `//registry.npmjs.org/:_auth` in project config",
      "_authToken=abc+lockfile-version=7 | " +
      "`_authToken` must be renamed to `//registry.npmjs.org/:_authToken` in project config",
      "_authToken=abc+global=true | " +
      "`_authToken` must be renamed to `//registry.npmjs.org/:_authToken` in project config",
      "registry=https://r.example/npm/+_authToken=abc | " +
      "`_authToken` must be renamed to `//r.example/npm/:_authToken` in project config",
      "_authToken=abc+registry=https://r.example/npm | " +
      "`_authToken` must be renamed to `//r.example/:_authToken` in project config",
      "registry=http://localhost:4873+_authToken=abc | " +
      "`_authToken` must be renamed to `//localhost:4873/:_authToken` in project config",
      "registry=https://r.example/a/b/c+_authToken=abc | " +
      "`_authToken` must be renamed to `//r.example/a/b/:_authToken` in project config",
      "registry=https://USER:pw@R.Example:443/A/b/?q=1#f+_authToken=abc | " +
      "`_authToken` must be renamed to `//r.example/A/b/:_authToken` in project config",
      "registry=mailto:a@b.c+_authToken=abc | `_authToken` must be renamed to `//b.c/:_authToken` in project config",
      "registry=https://registry.npmjs.org+_authToken=abc | " +
      "`_authToken` must be renamed to `//registry.npmjs.org/:_authToken` in project config"
    })
    void anAuthKeyOutsideARegistrysScopeIsRefusedFrom9(String npmrc, String problems) {
        String text = npmrc.replace('+', '\n') + "\n";
        for (String release : new String[]{"9.0.0", "11.17.0", "12.2.0"}) {
            assertThatThrownBy(() -> NpmOptions.fromNpmrc(NpmRelease.of(release), null, text))
              .isInstanceOfSatisfying(NpmRejection.class, e -> assertThat(e.getCode()).isEqualTo("ERR_INVALID_AUTH"))
              .hasMessage(invalidAuth(problems));
        }
        if (!npmrc.contains("lockfile-version")) {
            assertThat(NpmOptions.fromNpmrc(NpmRelease.of("8.19.4"), null, text)).isNotNull();
        }
    }

    // each .npmrc installed with npm 11.17.0, which writes the lock: a key that is unset, emptied or scoped
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', value = {
      "_authToken=", "_authToken=false", "_authToken=null", "_authToken=undefined", "_authToken=\" \"", "_auth=",
      "_auth=false", "_auth=null", "//example.invalid/:_authToken=abc", "email=a@b.c", "certfile=/tmp/c",
      "certfile=/tmp/c+keyfile=/tmp/k", "keyfile=/tmp/k", "//example.invalid/:email=a@b.c"
    })
    void anAuthKeyThatIsUnsetOrScopedIsNoneOfValidates(String npmrc) {
        assertThat(fromNpmrc(npmrc.replace('+', '\n') + "\n")).isNotNull();
    }

    // each .npmrc installed with npm 12.2.0, the first four with 12.0.0 as well, where 11.17.0 writes the lock for
    // each: email, certfile and keyfile outside a registry's scope are refused as the other auth keys
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "email=a@b.c | `email` must be renamed to `//registry.npmjs.org/:email` in project config",
      "certfile=/tmp/c | `certfile` is not allowed in project config",
      "keyfile=/tmp/k | `keyfile` is not allowed in project config",
      "certfile=/tmp/c+keyfile=/tmp/k | `certfile` must be renamed to `//registry.npmjs.org/:certfile` in project " +
      "config; `keyfile` must be renamed to `//registry.npmjs.org/:keyfile` in project config",
      "email | `email` must be renamed to `//registry.npmjs.org/:email` in project config",
      "email=0 | `email` must be renamed to `//registry.npmjs.org/:email` in project config",
      "[email]+x=1 | `email` must be renamed to `//registry.npmjs.org/:email` in project config",
      "certfile=/c+keyfile=false | `certfile` is not allowed in project config"
    })
    void emailAndCertificateOutsideARegistrysScopeAreRefusedFrom12(String npmrc, String problems) {
        String text = npmrc.replace('+', '\n') + "\n";
        for (String release : new String[]{"12.0.0", "12.2.0"}) {
            assertThatThrownBy(() -> NpmOptions.fromNpmrc(NpmRelease.of(release), null, text))
              .isInstanceOfSatisfying(NpmRejection.class, e -> assertThat(e.getCode()).isEqualTo("ERR_INVALID_AUTH"))
              .hasMessage(invalidAuth(problems));
        }
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("11.21.0"), null, text)).isNotNull();
    }

    // installed with npm 12.2.0: the keys npm 12 adds follow the others, file by file, and a value that is unset
    // or scoped is none of validate's
    @Test
    void validateReadsTheUsersFileThenTheProjects() {
        NpmRelease npm12 = NpmRelease.of("12.2.0");
        assertThatThrownBy(() -> NpmOptions.fromNpmrc(npm12, null,
          "email=a@b.c\ncertfile=/tmp/c\nkeyfile=/tmp/k\n_authToken=abc\n"))
          .hasMessage(invalidAuth("`_authToken` must be renamed to `" + NPMJS + ":_authToken` in project config; " +
                                  "`email` must be renamed to `" + NPMJS + ":email` in project config; " +
                                  "`certfile` must be renamed to `" + NPMJS + ":certfile` in project config; " +
                                  "`keyfile` must be renamed to `" + NPMJS + ":keyfile` in project config"));
        assertThatThrownBy(() -> NpmOptions.fromNpmrc(npm12, "email=a@b.c\n", "certfile=/c\n"))
          .hasMessage(invalidAuth("`email` must be renamed to `" + NPMJS + ":email` in user config; " +
                                  "`certfile` is not allowed in project config"));
        assertThat(NpmOptions.fromNpmrc(npm12, null, "email=false\ncertfile=\n")).isNotNull();
        assertThat(NpmOptions.fromNpmrc(npm12, null, "//example.invalid/:email=a@b.c\n")).isNotNull();

        // installed with npm 11.17.0: the registry that names the scope is the project's over the user's
        assertThatThrownBy(() -> NpmOptions.fromNpmrc(NpmRelease.PORTED, "_authToken=abc\n", "username=u\n"))
          .hasMessage(invalidAuth("`_authToken` must be renamed to `" + NPMJS + ":_authToken` in user config; " +
                                  "`username` is not allowed in project config"));
        assertThatThrownBy(() -> NpmOptions.fromNpmrc(NpmRelease.PORTED, "registry=https://user.example/\n",
          "_authToken=abc\n"))
          .hasMessage(invalidAuth("`_authToken` must be renamed to `//user.example/:_authToken` in project config"));
        assertThatThrownBy(() -> NpmOptions.fromNpmrc(NpmRelease.PORTED, "_authToken=abc\n",
          "registry=https://project.example/\n"))
          .hasMessage(invalidAuth("`_authToken` must be renamed to `//project.example/:_authToken` in user config"));
    }

    // each .npmrc installed with npm 11.17.0 and 12.2.0, `registry=foo` with 9.0.0, 10.0.0 and 10.9.9 as well:
    // validate() nerf-darts the registry in every file's turn, and new URL() refuses one that is none
    // (ERR_INVALID_URL), ahead of any auth key and of npm 12's strict-npmrc. npm 8.19.4 writes the lock.
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', value = {
      "registry=", "registry=foo", "registry=null", "registry=false", "registry", "registry=undefined",
      "registry=//registry.npmjs.org/", "registry=registry.npmjs.org", "registry=foo+_authToken=abc",
      "registry=foo+foo=bar+strict-npmrc=true"
    })
    void aRegistryThatIsNoUrlIsRefusedFrom9(String npmrc) {
        String text = npmrc.replace('+', '\n') + "\n";
        for (String release : new String[]{"9.0.0", "11.17.0", "12.2.0"}) {
            assertThatThrownBy(() -> NpmOptions.fromNpmrc(NpmRelease.of(release), null, text))
              .isInstanceOfSatisfying(NpmRejection.class, e -> assertThat(e.getCode()).isEqualTo("ERR_INVALID_URL"))
              .hasMessage("Invalid URL");
        }
    }

    // installed with npm 11.17.0 and a --userconfig: only the registry in effect is read, the project's over the
    // user's; a section in its place has no string for new URL(), nor has one under _auth for nopt
    @Test
    void theRegistryInEffectIsTheOneValidateReads() {
        assertThat(NpmOptions.fromNpmrc(NpmRelease.PORTED, "registry=foo\n", "registry=https://registry.npmjs.org/\n")
          .getRegistry()).isEqualTo("https://registry.npmjs.org/");
        assertThatThrownBy(() -> NpmOptions.fromNpmrc(NpmRelease.PORTED, "registry=foo\n", "legacy-peer-deps=true\n"))
          .isInstanceOfSatisfying(NpmRejection.class, e -> assertThat(e.getCode()).isEqualTo("ERR_INVALID_URL"));
        assertThatThrownBy(() -> fromNpmrc("[registry]\nx=1\n"))
          .isExactlyInstanceOf(IllegalArgumentException.class)
          .hasMessage("Cannot convert object to primitive value");
        assertThatThrownBy(() -> fromNpmrc("[_auth]\nx=1\n"))
          .isExactlyInstanceOf(IllegalArgumentException.class)
          .hasMessage("Cannot convert object to primitive value");
    }

    // a value or a key of the environment's: npm 11.17.0 refuses `_authToken=${NOPE}` with the variable unset and
    // installs with `_authToken=${NOPE?}`, so what it does is the environment's to say
    @Test
    void validateTheEnvironmentDecides() {
        assertThatThrownBy(() -> fromNpmrc("_authToken=${NOPE}\n")).isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> fromNpmrc("_authToken=${NOPE?}\n")).isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> fromNpmrc("_auth${T}=abc\n")).isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> fromNpmrc("registry=${REG}\n")).isInstanceOf(CannotKnow.class);
        assertThat(fromNpmrc("//${REG}/:_authToken=abc\n")).isNotNull();
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("8.19.4"), null, "_auth${T}=abc\n")).isNotNull();
    }

    // each .npmrc installed with npm 12.2.0, the first with 12.0.0 as well: validateCli names every key without a
    // definition in its EUNKNOWNCONFIG, a scoped one by its last part, ahead of arborist's own reading of
    // lockfile-version; nopt has cleaned strict-npmrc by then, so 1 and any word are true
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "strict-npmrc=true+foo=bar | \"foo\"",
      "strict-npmrc=1+foo=bar | \"foo\"",
      "strict-npmrc+foo=bar | \"foo\"",
      "strict-npmrc=yes+foo=bar | \"foo\"",
      "strict-npmrc=+foo=bar | \"foo\"",
      "foo=bar+strict-npmrc=true+baz=1 | \"foo\";\"baz\"",
      "strict-npmrc=true+//example.invalid/:always-auth=true | \"always-auth\" (//example.invalid/:always-auth)",
      "strict-npmrc=true+[foo]+bar=1 | \"foo\"",
      "strict-npmrc=true+foo[]=a+foo[]=b | \"foo\"",
      "strict-npmrc=true+lockfile-version=7+foo=bar | \"foo\"",
      "strict-npmrc=true+shrinkwrap=false | \"shrinkwrap\"",
      "strict-npmrc=true+Foo=bar+legacy-peer-deps=true | \"Foo\""
    })
    void strictNpmrcRefusesKeysNpm12DoesNotDefine(String npmrc, String shown) {
        String[] keys = shown.split(";");
        StringBuilder message = new StringBuilder("Unknown npm configuration key" + (keys.length > 1 ? "s" : "") + ":");
        for (String key : keys) {
            message.append("\n  - project config ").append(key).append(" from .npmrc");
        }
        message.append("\nSee `npm help npmrc` for supported config options.");
        String text = npmrc.replace('+', '\n') + "\n";
        for (String release : new String[]{"12.0.0", "12.2.0"}) {
            assertThatThrownBy(() -> NpmOptions.fromNpmrc(NpmRelease.of(release), null, text))
              .isInstanceOfSatisfying(NpmRejection.class, e -> assertThat(e.getCode()).isEqualTo("EUNKNOWNCONFIG"))
              .hasMessage(message.toString());
        }
        // npm 11.21.0 warns of strict-npmrc as of any other key it does not define, and installs
        if (!npmrc.contains("lockfile-version")) {
            assertThat(NpmOptions.fromNpmrc(NpmRelease.of("11.21.0"), null, text)).isNotNull();
        }
    }

    // each .npmrc installed with npm 12.2.0, which writes the lock: strict-npmrc cleaned to false, or every key one
    // npm defines, sets for the npm it runs (npm-version, global-prefix), or reads by its last part
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "strict-npmrc=0+foo=bar",
      "strict-npmrc=false+foo=bar",
      "strict-npmrc=null+foo=bar",
      "strict-npmrc=undefined+foo=bar",
      "strict-npmrc=true+strict-npmrc=false+foo=bar",
      "strict-npmrc=true+legacy-peer-deps=true",
      "strict-npmrc=true+@foo:registry=https://registry.npmjs.org/+//example.invalid/:_authToken=abc" +
      "+//example.invalid/:username=u",
      "strict-npmrc=true+npm-version=1.0.0+global-prefix=/tmp/gp",
      "strict-npmrc=true+a:b:registry=x",
      "foo=bar"
    })
    void strictNpmrcLeavesTheInstallBe(String npmrc) {
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("12.2.0"), null, npmrc.replace('+', '\n') + "\n")).isNotNull();
    }

    // installed with npm 12.2.0 and a --userconfig: the user's strict-npmrc holds for the project's keys, and the
    // project's own stands over it. A key of the user's own file fails it as well (`user config "foo"`), which is
    // not ported: the user's file here is the host's rendering of its registries.
    @Test
    void strictNpmrcIsTheProjectsOverTheUsers() {
        NpmRelease npm12 = NpmRelease.of("12.2.0");
        assertThatThrownBy(() -> NpmOptions.fromNpmrc(npm12, "strict-npmrc=true\n", "foo=bar\n"))
          .isInstanceOfSatisfying(NpmRejection.class, e -> assertThat(e.getCode()).isEqualTo("EUNKNOWNCONFIG"))
          .hasMessageContaining("  - project config \"foo\" from .npmrc");
        assertThat(NpmOptions.fromNpmrc(npm12, "strict-npmrc=true\n", "strict-npmrc=false\nfoo=bar\n")).isNotNull();
        assertThat(NpmOptions.fromNpmrc(npm12, "//r.example/:always-auth=true\n", "strict-npmrc=true\n")).isNotNull();
    }

    // a key or a value that may turn strict-npmrc on is the environment's to say, where npm 12 asks; a later
    // strict-npmrc of the file's own stands over the key
    @Test
    void strictNpmrcTheEnvironmentDecides() {
        NpmRelease npm12 = NpmRelease.of("12.2.0");
        assertThatThrownBy(() -> NpmOptions.fromNpmrc(npm12, null, "strict-npmrc=${STRICT}\nfoo=bar\n"))
          .isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> NpmOptions.fromNpmrc(npm12, null, "strict-n${RC}=true\nfoo=bar\n"))
          .isInstanceOf(CannotKnow.class);
        assertThat(NpmOptions.fromNpmrc(npm12, null, "strict-n${RC}=true\nstrict-npmrc=false\nfoo=bar\n")).isNotNull();
        assertThat(NpmOptions.fromNpmrc(NpmRelease.of("11.21.0"), null, "strict-n${RC}=true\nfoo=bar\n")).isNotNull();
        assertThat(NpmOptions.fromNpmrc(npm12, null, "strict-npmrc=${STRICT}\nlegacy-peer-deps=true\n")).isNotNull();
    }
}
