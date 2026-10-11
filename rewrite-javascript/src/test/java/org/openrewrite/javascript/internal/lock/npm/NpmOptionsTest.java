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
        return NpmOptions.fromNpmrc(null, npmrc);
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

        NpmOptions userOnly = NpmOptions.fromNpmrc(user, null);
        assertThat(userOnly.registryFor("ms")).isEqualTo("https://registry.npmmirror.com/");
        assertThat(userOnly.registryFor("@types/ms")).isEqualTo("https://registry.npmmirror.com/");

        NpmOptions project = NpmOptions.fromNpmrc(user, "registry=https://registry.yarnpkg.com/\n");
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
}
