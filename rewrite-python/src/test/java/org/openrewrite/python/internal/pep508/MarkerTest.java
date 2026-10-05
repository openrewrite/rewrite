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
package org.openrewrite.python.internal.pep508;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.python.internal.pep440.Corpus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkerTest {

    @Test
    void parsesAllVariableOperatorValueCombinations() {
        Map<String, List<String>> lexicon = readLexicon();
        List<String> variables = new ArrayList<>();
        variables.addAll(lexicon.get("variables"));
        variables.addAll(lexicon.get("pep345-variables"));
        variables.addAll(lexicon.get("setuptools-variables"));
        for (String variable : variables) {
            for (String op : lexicon.get("operators")) {
                for (String value : lexicon.get("values")) {
                    String forward = variable + " " + op + " '" + value + "'";
                    String reversed = "'" + value + "' " + op + " " + variable;
                    assertThat(Marker.parse(forward)).as(forward).isNotNull();
                    assertThat(Marker.parse(reversed)).as(reversed).isNotNull();
                }
            }
        }
    }

    private static Map<String, List<String>> readLexicon() {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        List<String> current = new ArrayList<>();
        for (String line : Corpus.lines("/pep508/marker-lexicon.txt")) {
            if (line.startsWith("[") && line.endsWith("]")) {
                current = new ArrayList<>();
                sections.put(line.substring(1, line.length() - 1), current);
            } else if (!line.isEmpty()) {
                current.add(Corpus.unescape(line));
            }
        }
        return sections;
    }

    @ParameterizedTest
    @MethodSource("invalidMarkers")
    void invalidMarkers(String marker) {
        assertThat(Marker.parse(marker)).isNull();
    }

    static Stream<String> invalidMarkers() {
        return Corpus.lines("/pep508/marker-invalid.txt").stream().map(Corpus::unescape);
    }

    @ParameterizedTest
    @MethodSource("normalizedMarkers")
    void normalizedSingleQuotedForm(String input, String packagingExpected) {
        // packaging normalizes to double quotes; this port emits pipenv's single-quoted form.
        String expected = packagingExpected.replace('"', '\'');
        Marker marker = Marker.parse(input);
        assertThat(marker).as(input).isNotNull();
        assertThat(marker.toString()).isEqualTo(expected);
        assertThat(marker).isEqualTo(Marker.parse(packagingExpected.replace('"', '\'')));
        assertThat(marker.hashCode()).isEqualTo(Marker.parse(input).hashCode());
    }

    static Stream<Arguments> normalizedMarkers() {
        return Corpus.rows("/pep508/marker-str.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @ParameterizedTest
    @MethodSource("evaluationCases")
    void evaluate(boolean expected, String markerString, MarkerEnvironment environment) {
        Marker marker = Marker.parse(markerString);
        assertThat(marker).as(markerString).isNotNull();
        assertThat(marker.evaluate(environment))
          .as(markerString)
          .isEqualTo(expected);
    }

    static Stream<Arguments> evaluationCases() {
        return Corpus.rows("/pep508/marker-evaluate.tsv").stream().map(row -> {
            MarkerEnvironment.MarkerEnvironmentBuilder env = MarkerEnvironment.builder();
            for (int i = 2; i < row.length; i++) {
                int eq = row[i].indexOf('=');
                set(env, row[i].substring(0, eq), row[i].substring(eq + 1));
            }
            return Arguments.of(Boolean.parseBoolean(row[0]), row[1], env.build());
        });
    }

    private static void set(MarkerEnvironment.MarkerEnvironmentBuilder env, String key, String value) {
        switch (key) {
            case "python_version":
                env.pythonVersion(value);
                break;
            case "python_full_version":
                env.pythonFullVersion(value);
                break;
            case "os_name":
                env.osName(value);
                break;
            case "sys_platform":
                env.sysPlatform(value);
                break;
            case "platform_machine":
                env.platformMachine(value);
                break;
            case "platform_release":
                env.platformRelease(value);
                break;
            case "platform_system":
                env.platformSystem(value);
                break;
            case "platform_version":
                env.platformVersion(value);
                break;
            case "platform_python_implementation":
                env.platformPythonImplementation(value);
                break;
            case "implementation_name":
                env.implementationName(value);
                break;
            case "implementation_version":
                env.implementationVersion(value);
                break;
            case "extra":
                env.extra(value);
                break;
            default:
                throw new IllegalArgumentException("Unknown marker variable: " + key);
        }
    }

    @Test
    void unknownVariableEvaluatesToNull() {
        MarkerEnvironment empty = MarkerEnvironment.builder().build();
        assertThat(Marker.parse("os_name == 'posix'").evaluate(empty)).isNull();
        assertThat(Marker.parse("'security' in extras").evaluate(empty)).isNull();
        assertThat(Marker.parse("'group' in dependency_groups").evaluate(empty)).isNull();
    }

    @Test
    void threeValuedLogic() {
        MarkerEnvironment env = MarkerEnvironment.builder().pythonVersion("3.11").build();
        assertThat(Marker.parse("python_version == '2.7' and os_name == 'posix'").evaluate(env)).isFalse();
        assertThat(Marker.parse("python_version == '3.11' or os_name == 'posix'").evaluate(env)).isTrue();
        assertThat(Marker.parse("python_version == '3.11' and os_name == 'posix'").evaluate(env)).isNull();
        assertThat(Marker.parse("python_version == '2.7' or os_name == 'posix'").evaluate(env)).isNull();
        assertThat(Marker.parse("os_name == 'posix' and python_version == '2.7'").evaluate(env)).isFalse();
        assertThat(Marker.parse("os_name == 'posix' or python_version == '3.11'").evaluate(env)).isTrue();
    }

    @Test
    void undefinedComparisonEvaluatesToNull() {
        MarkerEnvironment env = MarkerEnvironment.builder().osName("posix").build();
        assertThat(Marker.parse("os_name ~= 'posix'").evaluate(env)).isNull();
    }

    @Test
    void nonVersionStringComparisons() {
        // Port of packaging's current string rules: < and > are always false on
        // non-version keys, <= and >= degrade to equality.
        MarkerEnvironment env = MarkerEnvironment.builder().osName("posix").build();
        assertThat(Marker.parse("os_name < 'z'").evaluate(env)).isFalse();
        assertThat(Marker.parse("os_name > 'a'").evaluate(env)).isFalse();
        assertThat(Marker.parse("os_name <= 'posix'").evaluate(env)).isTrue();
        assertThat(Marker.parse("os_name >= 'posix'").evaluate(env)).isTrue();
        assertThat(Marker.parse("os_name <= 'z'").evaluate(env)).isFalse();
    }

    @Test
    void versionComparisonsAllowPrereleases() {
        MarkerEnvironment env = MarkerEnvironment.builder().pythonFullVersion("3.13.0a2").build();
        assertThat(Marker.parse("python_full_version >= '3.12'").evaluate(env)).isTrue();
        assertThat(Marker.parse("python_full_version < '3.11'").evaluate(env)).isFalse();
    }

    @Test
    void valueContainingSingleQuoteSerializedWithDoubleQuotes() {
        Marker marker = Marker.parse("os_name == \"posix's\"");
        assertThat(marker).isNotNull();
        assertThat(marker.toString()).isEqualTo("os_name == \"posix's\"");
    }

    @Test
    void valueContainingBothQuoteTypesCannotBeSerialized() {
        Marker marker = new Marker(singletonList((Object) new Pep508Parser.Comparison(
          new Pep508Parser.Operand("os_name", true), "==",
          new Pep508Parser.Operand("a'b\"c", false))));
        assertThatThrownBy(marker::toString)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("both quote characters");
    }

    @Test
    void extraLiteralsNormalizedAtParseTime() {
        Marker marker = Marker.parse("extra == 'PEP_685...norm'");
        assertThat(marker.toString()).isEqualTo("extra == 'pep-685-norm'");
    }

    @Test
    void evaluatesAgainstLockDefaults() {
        MarkerEnvironment env = MarkerEnvironment.lockDefaults("3.11", null);
        assertThat(Marker.parse("sys_platform == 'linux'").evaluate(env)).isTrue();
        assertThat(Marker.parse("sys_platform == 'win32'").evaluate(env)).isFalse();
        assertThat(Marker.parse("os_name == 'nt'").evaluate(env)).isFalse();
        assertThat(Marker.parse("python_version >= '3.8'").evaluate(env)).isTrue();
        assertThat(Marker.parse("python_full_version >= '3.11.0'").evaluate(env)).isTrue();
        assertThat(Marker.parse("platform_release > '5'").evaluate(env)).isNull();
        assertThat(Marker.parse("platform_system == 'Windows' and python_version >= '3.8'").evaluate(env)).isFalse();
    }

    @Test
    void repairsUntaggedPythonFullVersion() {
        MarkerEnvironment env = MarkerEnvironment.builder().pythonFullVersion("3.13.0+").build();
        assertThat(Marker.parse("python_full_version == '3.13.0+local'").evaluate(env)).isTrue();
    }
}
