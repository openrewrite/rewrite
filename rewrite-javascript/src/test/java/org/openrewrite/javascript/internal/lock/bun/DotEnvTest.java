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
package org.openrewrite.javascript.internal.lock.bun;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port of bun's test/cli/run/env.test.ts (bun-v1.3.10): the parser cases and the file precedence an install
 * ({@code NODE_ENV=production}) sees, with the test's expected values.
 */
class DotEnvTest {

    private static Map<String, String> parse(String src) {
        Map<String, String> map = new LinkedHashMap<>();
        DotEnv.parse(src, map);
        return map;
    }

    private static Map<String, String> files(String... pairs) {
        Map<String, String> files = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            files.put(pairs[i], pairs[i + 1]);
        }
        return files;
    }

    @Test
    void envFileIsLoaded() {
        assertThat(DotEnv.load(Collections.emptyMap(), files(".env", "FOO=bar\n"))).containsEntry("FOO", "bar");
    }

    @Test
    void envLocalOverridesEnv() {
        Map<String, String> env = DotEnv.load(Collections.emptyMap(),
          files(".env", "FOO=fail\nBAR=baz\n", ".env.local", "FOO=bar\n"));
        assertThat(env).containsEntry("FOO", "bar").containsEntry("BAR", "baz");
    }

    @Test
    void envProductionOverridesEnvAndEnvLocalIsRead() {
        Map<String, String> env = DotEnv.load(Collections.emptyMap(),
          files(".env", "FOO=fail\nBAR=baz\n", ".env.production", "FOO=bar\n", ".env.local", "LOCAL=true\n"));
        assertThat(env).containsEntry("FOO", "bar").containsEntry("BAR", "baz").containsEntry("LOCAL", "true");
    }

    @Test
    void envDevelopmentAndEnvTestIgnored() {
        Map<String, String> env = DotEnv.load(Collections.emptyMap(),
          files(".env", "FOO=.env\n", ".env.development", "FOO=.env.development\n",
            ".env.development.local", "FOO=.env.development.local\n", ".env.test", "FOO=.env.test\n",
            ".env.test.local", "FOO=.env.test.local\n"));
        assertThat(env).containsEntry("FOO", ".env");
    }

    @Test
    void processEnvOverridesEverythingElse() {
        Map<String, String> env = DotEnv.load(Collections.singletonMap("FOO", "override"),
          files(".env", "FOO=.env\n", ".env.production", "FOO=.env.production\n",
            ".env.production.local", "FOO=.env.production.local\n", ".env.local", "FOO=.env.local\n"));
        assertThat(env).containsEntry("FOO", "override");
    }

    @Test
    void envProductionLocalOverridesEnvLocal() {
        Map<String, String> env = DotEnv.load(Collections.emptyMap(),
          files(".env", "FOO=.env\n", ".env.production", "FOO=.env.production\n",
            ".env.production.local", "FOO=.env.production.local\n", ".env.local", "FOO=.env.local\n"));
        assertThat(env).containsEntry("FOO", ".env.production.local");
    }

    @Test
    void envLocalOverridesEnvProduction() {
        Map<String, String> env = DotEnv.load(Collections.emptyMap(),
          files(".env", "FOO=.env\n", ".env.production", "FOO=.env.production\n", ".env.local", "FOO=.env.local\n"));
        assertThat(env).containsEntry("FOO", ".env.local");
    }

    @Test
    void envProductionOverridesEnv() {
        Map<String, String> env = DotEnv.load(Collections.emptyMap(),
          files(".env", "FOO=.env\n", ".env.production", "FOO=.env.production\n"));
        assertThat(env).containsEntry("FOO", ".env.production");
    }

    @Test
    void colonAssign() {
        assertThat(parse("FOO: foo")).containsEntry("FOO", "foo");
    }

    @Test
    void exportAssign() {
        assertThat(parse("export FOO = foo\nexport = bar"))
          .containsEntry("FOO", "foo").containsEntry("export", "bar");
    }

    @Test
    void valueExpansion() {
        Map<String, String> env = parse("FOO=foo\nBAR=$FOO bar\nMOO=${FOO} ${BAR:-fail} ${MOZ:-moo}");
        assertThat(env).containsEntry("FOO", "foo").containsEntry("BAR", "foo bar")
          .containsEntry("MOO", "foo foo bar moo");
    }

    @Test
    void comments() {
        Map<String, String> env = parse("#FOZ\nFOO = foo#FAIL\nBAR='bar' #BAZ");
        assertThat(env).containsEntry("FOO", "foo").containsEntry("BAR", "bar").hasSize(2);
    }

    @Test
    void escapedDollarSign() {
        assertThat(parse("FOO=foo\nBAR=\\$FOO")).containsEntry("FOO", "foo").containsEntry("BAR", "$FOO");
    }

    @Test
    void doesNotCrashWith159Bytes() {
        String value = "12345678901234567890123456789012345678901234567890123456789012345678901234567890" +
                       "12345678901234567890123456789012345678901234567890123456789012345678";
        assertThat(parse("123456789=" + value + "\n")).containsEntry("123456789", value);
    }

    @Test
    void manyEntries() {
        StringBuilder src = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            src.append("TEST_VAR").append(i).append("=TEST_VAL").append(i).append('\n');
        }
        Map<String, String> env = parse(src.toString());
        assertThat(env).hasSize(50000).containsEntry("TEST_VAR0", "TEST_VAL0")
          .containsEntry("TEST_VAR49999", "TEST_VAL49999");
    }

    @Test
    void spaceEdgecase() {
        assertThat(parse("VARNAME=A B")).containsEntry("VARNAME", "A B");
    }

    @Test
    void specialCharacters() {
        assertThat(parse("A=\"a$t\"\nC=`c\\$v`")).containsEntry("A", "a").containsEntry("C", "c$v");
    }

    @Test
    void windowsStyleNewline() {
        Map<String, String> env = parse("FOO=\rBAR='bar\r\rbaz'\r\nMOO=moo\r");
        assertThat(env).containsEntry("FOO", "").containsEntry("BAR", "bar\n\nbaz").containsEntry("MOO", "moo");
    }

    @Test
    void zeroLengthStrings() {
        assertThat(parse("FOO=''\n")).containsEntry("FOO", "");
    }

    @Test
    void escapedNewlineInDoubleQuotes() {
        assertThat(parse("KEY=\"a\\nb\"")).containsEntry("KEY", "a\nb");
    }

    @Test
    void largeQuotedValuesWithEscapeSequences() {
        StringBuilder backslashes = new StringBuilder();
        for (int i = 0; i < 2049; i++) {
            backslashes.append("\\\\");
        }
        assertThat(parse("OVERFLOW_VAR=\"" + backslashes + "\"").get("OVERFLOW_VAR")).hasSize(4098);
    }

    @Test
    void multipleLargeValuesInSameFile() {
        StringBuilder src = new StringBuilder("\n");
        for (char c : "abc".toCharArray()) {
            src.append("LARGE").append(c - 'a' + 1).append("=\"");
            for (int i = 0; i < 3000; i++) {
                src.append(c);
            }
            src.append("\"\n");
        }
        Map<String, String> env = parse(src.toString());
        assertThat(env.get("LARGE1")).hasSize(3000);
        assertThat(env.get("LARGE2")).hasSize(3000);
        assertThat(env.get("LARGE3")).hasSize(3000);
    }

    @Test
    void aKeyDefinedLaterInTheSameFileOverridesAnEarlierOne() {
        assertThat(parse("FOO=first\nFOO=second\n")).containsEntry("FOO", "second");
    }
}
