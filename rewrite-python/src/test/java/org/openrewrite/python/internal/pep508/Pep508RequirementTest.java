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
import org.openrewrite.python.internal.pep440.PythonVersionSpecifierSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class Pep508RequirementTest {

    @Test
    void basicComponents() {
        Pep508Requirement req = Pep508Requirement.parse(
          "requests[security,tests]>=2.8.1,==2.8.* ; python_version < \"2.7\"");
        assertThat(req).isNotNull();
        assertThat(req.getName()).isEqualTo("requests");
        assertThat(req.getCanonicalName()).isEqualTo("requests");
        assertThat(req.getExtras()).containsExactly("security", "tests");
        assertThat(req.getSpecifiers()).isEqualTo(PythonVersionSpecifierSet.parse(">=2.8.1,==2.8.*"));
        assertThat(req.getUrl()).isNull();
        assertThat(req.getMarker()).hasToString("python_version < '2.7'");
        assertThat(req.getValue()).startsWith("requests[");
    }

    @Test
    void urlRequirement() {
        Pep508Requirement req = Pep508Requirement.parse("pip @ https://github.com/pypa/pip/archive/1.3.1.zip");
        assertThat(req).isNotNull();
        assertThat(req.getName()).isEqualTo("pip");
        assertThat(req.getUrl()).isEqualTo("https://github.com/pypa/pip/archive/1.3.1.zip");
        assertThat(req.getSpecifiers()).isNull();
        assertThat(req.getMarker()).isNull();
    }

    @Test
    void bareNameAndEmptyBrackets() {
        Pep508Requirement req = Pep508Requirement.parse("foobar");
        assertThat(req).isNotNull();
        assertThat(req.getName()).isEqualTo("foobar");
        assertThat(req.getExtras()).isEmpty();
        assertThat(req.getSpecifiers()).isNull();
        // packaging accepts empty extras brackets and empty specifier parens.
        assertThat(Pep508Requirement.parse("name[]")).isNotNull();
        assertThat(Pep508Requirement.parse("name()")).isNotNull();
    }

    /**
     * Port of pypa/packaging's test_basic_valid_requirement_parsing product
     * (tests/test_requirements.py, retrieved 2026-07-16).
     */
    @Test
    void combinatorialParsing() {
        List<String> names = Arrays.asList(
          "package", "pAcKaGe", "Package", "foo-bar.quux_bAz", "installer", "android12");
        List<List<String>> extrasSets = Arrays.asList(
          Collections.emptyList(),
          Collections.singletonList("a"),
          Arrays.asList("a", "b"),
          Arrays.asList("a", "B", "CDEF123"));
        // Pairs of (url, specifier); exactly one may be non-empty.
        List<String[]> urlSpecs = Arrays.asList(
          new String[]{null, ""},
          new String[]{"https://example.com/packagename.zip", ""},
          new String[]{"ssh://user:pass%20word@example.com/packagename.zip", ""},
          new String[]{"https://example.com/name;v=1.1/?query=foo&bar=baz#blah", ""},
          new String[]{"git+ssh://git.example.com/MyProject", ""},
          new String[]{"git+ssh://git@github.com:pypa/packaging.git", ""},
          new String[]{"git+https://git.example.com/MyProject.git@master", ""},
          new String[]{"git+https://git.example.com/MyProject.git@v1.0", ""},
          new String[]{"git+https://git.example.com/MyProject.git@refs/pull/123/head", ""},
          new String[]{"gopher:/foo/com", ""},
          new String[]{null, "==={ws}arbitrarystring"},
          new String[]{null, "({ws}==={ws}arbitrarystring{ws})"},
          new String[]{null, "=={ws}1.0"},
          new String[]{null, "({ws}=={ws}1.0{ws})"},
          new String[]{null, "=={ws}1.0-alpha"},
          new String[]{null, "<={ws}1!3.0.0.rc2"},
          new String[]{null, ">{ws}2.2{ws},{ws}<{ws}3"},
          new String[]{null, "(>{ws}2.2{ws},{ws}<{ws}3)"});
        List<String> markers = Arrays.asList(
          null,
          "python_version{ws}>={ws}'3.3'",
          "({ws}python_version{ws}>={ws}\"3.4\"{ws}){ws}and extra{ws}=={ws}\"oursql\"",
          "sys_platform{ws}!={ws}'linux' and(os_name{ws}=={ws}'linux' or python_version{ws}>={ws}'3.3'{ws}){ws}");
        List<String> whitespaces = Arrays.asList("", " ", "\t");

        for (String name : names) {
            for (List<String> extras : extrasSets) {
                for (String[] urlSpec : urlSpecs) {
                    for (String marker : markers) {
                        for (String ws : whitespaces) {
                            assertCombination(name, extras, urlSpec[0], urlSpec[1], marker, ws);
                        }
                    }
                }
            }
        }
    }

    private void assertCombination(String name, List<String> extras, String url,
                                   String specifier, String marker, String ws) {
        List<String> parts = new ArrayList<>();
        parts.add(name);
        if (!extras.isEmpty()) {
            List<String> sorted = new ArrayList<>(extras);
            Collections.sort(sorted);
            parts.add("[");
            parts.add(String.join(ws + "," + ws, sorted));
            parts.add("]");
        }
        if (!specifier.isEmpty()) {
            parts.add(specifier.replace("{ws}", ws));
        }
        if (url != null) {
            parts.add("@");
            parts.add(url);
        }
        if (marker != null) {
            parts.add(url != null ? " ;" : ";");
            parts.add(marker.replace("{ws}", ws));
        }
        String toParse = String.join(ws, parts);

        Pep508Requirement req = Pep508Requirement.parse(toParse);
        assertThat(req).as(toParse).isNotNull();
        assertThat(req.getName()).as(toParse).isEqualTo(name);
        assertThat(req.getExtras()).as(toParse).isEqualTo(new LinkedHashSet<>(extras));
        assertThat(req.getUrl()).as(toParse).isEqualTo(url);
        String expectedSpecifier = stripParens(specifier.replace("{ws}", ""));
        if (expectedSpecifier.isEmpty()) {
            assertThat(req.getSpecifiers()).as(toParse).isNull();
        } else {
            assertThat(req.getSpecifiers()).as(toParse)
              .isEqualTo(PythonVersionSpecifierSet.parse(expectedSpecifier));
        }
        if (marker == null) {
            assertThat(req.getMarker()).as(toParse).isNull();
        } else {
            assertThat(req.getMarker()).as(toParse).isEqualTo(Marker.parse(marker.replace("{ws}", "")));
        }
    }

    private static String stripParens(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && (s.charAt(start) == '(' || s.charAt(start) == ')')) {
            start++;
        }
        while (end > start && (s.charAt(end - 1) == '(' || s.charAt(end - 1) == ')')) {
            end--;
        }
        return s.substring(start, end);
    }

    @ParameterizedTest
    @MethodSource("invalidRequirements")
    void invalidRequirements(String requirement) {
        assertThat(Pep508Requirement.parse(requirement)).isNull();
    }

    static Stream<String> invalidRequirements() {
        return Corpus.lines("/pep508/requirement-invalid.txt").stream().map(Corpus::unescape);
    }

    @ParameterizedTest
    @MethodSource("requirementPairs")
    void equivalence(String kind, String left, String right) {
        Pep508Requirement a = Pep508Requirement.parse(left);
        Pep508Requirement b = Pep508Requirement.parse(right);
        assertThat(a).as(left).isNotNull();
        assertThat(b).as(right).isNotNull();
        if ("DIFFERENT".equals(kind)) {
            assertThat(componentsEqual(a, b)).as("%s != %s", left, right).isFalse();
        } else {
            assertThat(componentsEqual(a, b)).as("%s == %s", left, right).isTrue();
        }
    }

    static Stream<Arguments> requirementPairs() {
        return Corpus.rows("/pep508/requirement-pairs.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1], row[2]));
    }

    // Semantic equality per packaging Requirement.__eq__: canonical names and extras,
    // canonical specifiers, url, marker.
    private static boolean componentsEqual(Pep508Requirement a, Pep508Requirement b) {
        return a.getCanonicalName().equals(b.getCanonicalName()) &&
               canonicalExtras(a).equals(canonicalExtras(b)) &&
               Objects.equals(a.getSpecifiers(), b.getSpecifiers()) &&
               Objects.equals(a.getUrl(), b.getUrl()) &&
               Objects.equals(a.getMarker(), b.getMarker());
    }

    private static Set<String> canonicalExtras(Pep508Requirement req) {
        return req.getExtras().stream().map(Pep508Requirement::canonicalize).collect(Collectors.toSet());
    }

    @ParameterizedTest
    @MethodSource("normalizedRequirements")
    void markerExtraNormalization(String input, String normalized) {
        Pep508Requirement a = Pep508Requirement.parse(input);
        Pep508Requirement b = Pep508Requirement.parse(normalized);
        assertThat(a.getMarker()).isEqualTo(b.getMarker());
        assertThat(a.getMarker().toString()).isEqualTo(b.getMarker().toString());
    }

    static Stream<Arguments> normalizedRequirements() {
        return Corpus.rows("/pep508/requirement-normalized.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @ParameterizedTest
    @MethodSource("canonicalNames")
    void canonicalizeName(String name, String expected) {
        assertThat(Pep508Requirement.canonicalize(name)).isEqualTo(expected);
    }

    static Stream<Arguments> canonicalNames() {
        return Corpus.rows("/pep508/canonicalize-name.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }
}
