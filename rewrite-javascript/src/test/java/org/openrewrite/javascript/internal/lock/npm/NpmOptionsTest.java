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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What npm 11.17.0 does with each {@code lockfile-version} an .npmrc can hold (the config layer's
 * parseField and flatten, then arborist's own check in its constructor) and with each boolean's
 * value (parseField maps four words; arborist reads any other string as truthy).
 */
class NpmOptionsTest {

    @Test
    void defaults() {
        assertThat(NpmOptions.fromNpmrc(null)).isEqualTo(NpmOptions.DEFAULT);
        assertThat(NpmOptions.fromNpmrc("")).isEqualTo(NpmOptions.DEFAULT);
        assertThat(NpmOptions.DEFAULT.getLockfileVersion()).isNull();
        assertThat(NpmOptions.DEFAULT.isLegacyPeerDeps()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
      "2, 2", "' 3 ', 3", "+2, 2", "1.5, 1", "2abc, 2", "null,", "undefined,"
    })
    void lockfileVersionNpmAccepts(String value, Integer expected) {
        assertThat(NpmOptions.fromNpmrc("lockfile-version=" + value).getLockfileVersion()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
      "4, 4", "abc, NaN", "'', ''", "false, false", "true, NaN", "0, 0", "-1, -1", "0x10, 0"
    })
    void lockfileVersionNpmRejects(String value, String reported) {
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("lockfile-version=" + value))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid lockfileVersion config: " + reported);
    }

    @Test
    void bareKeyIsTrue() {
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("lockfile-version"))
          .hasMessage("Invalid lockfileVersion config: NaN");
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps").isLegacyPeerDeps()).isTrue();
    }

    // each value installed with npm 11.17.0: the peer chain of arb-legacy-peer-deps-add stays out when legacy
    @ParameterizedTest
    @CsvSource({
      "true, true", "false, false", "0, true", "FALSE, true", "True, true", "yes, true", "00, true", "2, true",
      "undefined, false", "null, false", "'', true", "'true ', true", "' false', false"
    })
    void booleansAreNpmConfigsTruthiness(String value, boolean expected) {
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps=" + value).isLegacyPeerDeps()).isEqualTo(expected);
        assertThat(NpmOptions.fromNpmrc("install-links=" + value).isInstallLinks()).isEqualTo(expected);
        assertThat(NpmOptions.fromNpmrc("omit-lockfile-registry-resolved=" + value).isOmitLockfileRegistryResolved())
          .isEqualTo(expected);
    }

    @Test
    void otherKeysPassOver() {
        NpmOptions options = NpmOptions.fromNpmrc("# comment\n; another\nforce=true\nlegacy-peer-deps=true\n\n" +
                                                  "install-links=false\nomit-lockfile-registry-resolved=1\n" +
                                                  "lockfile-version=3\n");
        assertThat(options.getLockfileVersion()).isEqualTo(3);
        assertThat(options.isLegacyPeerDeps()).isTrue();
        assertThat(options.isInstallLinks()).isFalse();
        assertThat(options.isOmitLockfileRegistryResolved()).isTrue();
    }


    // ini strips a matching pair of quotes (JSON.parse for double, the text first for single) and an unescaped
    // ; or # comment before parseField reads the value; installed with npm 11.17.0: "false" and 'false' leave the
    // peer chain in, "true" and "0" keep it out
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', ignoreLeadingAndTrailingWhitespace = false, value = {
      "\"false\"|false", "'false'|false", "\"true\"|true", "'true'|true", "\"0\"|true", "'0'|false", "\"null\"|false",
      "'null'|false", "\"undefined\"|false", "\" false\"|false", "\"FALSE\"|true", "\"abc\"|true", "\"|true", "'|true",
      "\"\"|true", "''|true", "\"abc|true", "abc\"|true", "'[false]'|true", "'{}'|true", "\"\\u0066alse\"|false",
      "false ; comment|false", "false # comment|false", "false\\;x|true", "\\false|true", "fal se|true", "false;|false",
      "\tfalse|false", "false\t|false", ";|true", "#|true", "\\|true"
    })
    void iniQuotesCommentsAndEscapes(String value, boolean expected) {
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps=" + value + "\n").isLegacyPeerDeps()).isEqualTo(expected);
    }

    // installed with npm 11.17.0: "2" and '2' write a v2 lock, "abc" exits 1 with NaN
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', ignoreLeadingAndTrailingWhitespace = false, value = {
      "\"2\"|2", "'2'|2", "'1.5'|1", "'+2'|2", "'[2]'|2", "'[\"2\",\"3\"]'|2", "'null'|", "'undefined'|", "\"null\"|",
      "\" 2\"|2", "\"\\u00a02\"|2", "\"2\\n\"|2", "'\"2\"'|2", "'3 4'|3", "2'|2", "2 ; two|2", "2 # two|2", "2\\;3|2",
      "1e3|1", "2,3|2", "\u00a02|2", "03|3", "2-|2", "1_0|1", "2.9|2"
    })
    void iniLockfileVersionNpmAccepts(String value, Integer expected) {
        assertThat(NpmOptions.fromNpmrc("lockfile-version=" + value + "\n").getLockfileVersion()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', ignoreLeadingAndTrailingWhitespace = false, value = {
      "\"abc\"|NaN", "'abc'|NaN", "'1e3'|1000", "'007'|7", "'-0'|0", "'[null,\"2\"]'|NaN", "'{}'|NaN", "'true'|NaN",
      "'false'|false", "\"\"|", "''|", "\"|NaN", "'|", "\"2|NaN", "'[]'|NaN", "'[true]'|NaN", "\\2|NaN", "0x2|0",
      "-2|-2", "Infinity|NaN", "00|0", "+-2|NaN", "٢|NaN"
    })
    void iniLockfileVersionNpmRejects(String value, String reported) {
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("lockfile-version=" + value + "\n"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid lockfileVersion config: " + (reported == null ? "" : reported));
    }

    @Test
    void iniKeys() {
        assertThat(NpmOptions.fromNpmrc(" legacy-peer-deps = true \n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("\"legacy-peer-deps\"=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("'[\"legacy-peer-deps\"]'=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("Legacy-Peer-Deps=true\n").isLegacyPeerDeps()).isFalse();
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps;x=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("legacy-peer\\;-deps=true\n").isLegacyPeerDeps()).isFalse();
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps==true\n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps=true\nlegacy-peer-deps=false\n").isLegacyPeerDeps())
          .isFalse();
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps=true\r\nlockfile-version=2\r\n").getLockfileVersion())
          .isEqualTo(2);
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps=true\rlockfile-version=2\r").getLockfileVersion())
          .isEqualTo(2);
        assertThat(NpmOptions.fromNpmrc("\ufefflegacy-peer-deps=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("\u00a0# x\nlegacy-peer-deps=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps=true\u2028lockfile-version=2\n"))
          .isEqualTo(NpmOptions.DEFAULT);
        assertThat(NpmOptions.fromNpmrc("a.b=null\n__proto__=true\nlegacy-peer-deps=true\n").isLegacyPeerDeps())
          .isTrue();
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("'null'=true\n"))
          .hasMessage("Cannot read properties of null (reading 'length')");
    }

    // an array value is truthy whatever it holds, and parseInt reads String(array)
    @Test
    void iniArrays() {
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps[]=false\n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps=true\nlegacy-peer-deps[]=false\n").isLegacyPeerDeps())
          .isTrue();
        assertThat(NpmOptions.fromNpmrc("[]=true\n")).isEqualTo(NpmOptions.DEFAULT);
        assertThat(NpmOptions.fromNpmrc("lockfile-version[]=2\nlockfile-version[]=3\n").getLockfileVersion())
          .isEqualTo(2);
        assertThat(NpmOptions.fromNpmrc("lockfile-version[]=2\nlockfile-version=3\n").getLockfileVersion())
          .isEqualTo(2);
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("lockfile-version[]=null\nlockfile-version[]=2\n"))
          .hasMessage("Invalid lockfileVersion config: NaN");
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("lockfile-version[]=true\n"))
          .hasMessage("Invalid lockfileVersion config: NaN");
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("lockfile-version[]=\n"))
          .hasMessage("Invalid lockfileVersion config: NaN");
    }

    // a section hides the keys under it; a section named like an option is an object, truthy and without a
    // string form; a dotted section nests
    @Test
    void iniSections() {
        assertThat(NpmOptions.fromNpmrc("[section]\nlegacy-peer-deps=true\nlockfile-version=2\n"))
          .isEqualTo(NpmOptions.DEFAULT);
        assertThat(NpmOptions.fromNpmrc("[section]   \nlegacy-peer-deps=true\n")).isEqualTo(NpmOptions.DEFAULT);
        assertThat(NpmOptions.fromNpmrc("[section]\nx=1\n[]\nlegacy-peer-deps=true\n")).isEqualTo(NpmOptions.DEFAULT);
        assertThat(NpmOptions.fromNpmrc("[__proto__]\nlegacy-peer-deps=true\n")).isEqualTo(NpmOptions.DEFAULT);
        assertThat(NpmOptions.fromNpmrc("[legacy-peer-deps]\n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("[\"legacy-peer-deps\"]\nx=1\n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps=false\n[legacy-peer-deps]\nx=1\n").isLegacyPeerDeps())
          .isTrue();
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps=true\n[legacy-peer-deps]\nx=1\n").isLegacyPeerDeps())
          .isTrue();
        assertThat(NpmOptions.fromNpmrc("lockfile-version=2\n[lockfile-version]\nx=1\n").getLockfileVersion())
          .isEqualTo(2);
        assertThat(NpmOptions.fromNpmrc("[a.legacy-peer-deps]\nx=1\n").isLegacyPeerDeps()).isFalse();
        assertThat(NpmOptions.fromNpmrc("[legacy-peer-deps.b]\nx=1\n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("[legacy-peer\\.deps]\nx=1\n").isLegacyPeerDeps()).isFalse();
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("[lockfile-version]\n"))
          .hasMessage("Cannot convert object to primitive value");
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("[lockfile-version]\nlockfile-version=2\n"))
          .hasMessage("Cannot convert object to primitive value");
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("a=null\n[a.b]\nlegacy-peer-deps=true\n"))
          .hasMessage("Cannot set properties of null (setting 'b')");
    }

    // the environment npm would expand ${VAR} from is not an input: a reference in a value read, or in a key that
    // could expand to one, is a failure; an escaped reference is the literal text
    @Test
    void environmentReferences() {
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("lockfile-version=${LFV}\n"))
          .isInstanceOf(NpmOptions.NeedsEnvironment.class)
          .hasMessage("\"${LFV}\" in the npmrc reads the environment, which is not known");
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("legacy-peer-deps=${LPD?}\n"))
          .isInstanceOf(NpmOptions.NeedsEnvironment.class);
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("legacy-peer-deps=\"${LPD}\"\n"))
          .isInstanceOf(NpmOptions.NeedsEnvironment.class);
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("${K}=true\n"))
          .isInstanceOf(NpmOptions.NeedsEnvironment.class)
          .hasMessage("\"${K}\" in the npmrc reads the environment, which is not known");
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("${K}-peer-deps=true\n"))
          .isInstanceOf(NpmOptions.NeedsEnvironment.class);
        assertThat(NpmOptions.fromNpmrc("//${REG}/:_authToken=x\nlegacy-peer-deps=true\n").isLegacyPeerDeps()).isTrue();
        assertThat(NpmOptions.fromNpmrc("//r/:_authToken=${TOKEN}\nlegacy-peer-deps=true\n").isLegacyPeerDeps())
          .isTrue();
        assertThat(NpmOptions.fromNpmrc("\\${K}=true\nlegacy-peer-deps=true\n").isLegacyPeerDeps()).isTrue();
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("lockfile-version=\\${LFV}\n"))
          .hasMessage("Invalid lockfileVersion config: NaN");
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("lockfile-version=${}\n"))
          .hasMessage("Invalid lockfileVersion config: NaN");
    }
}
