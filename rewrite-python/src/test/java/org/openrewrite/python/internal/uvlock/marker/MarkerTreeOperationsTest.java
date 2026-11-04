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
package org.openrewrite.python.internal.uvlock.marker;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.python.internal.pep440.PythonVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * uv's tests of the marker operations, translated from uv 0.13.4 {@code crates/uv-pep508/src/marker/tree.rs}.
 * Each method is named after the upstream test it reproduces.
 */
class MarkerTreeOperationsTest {

    private final MarkerInterner interner = new MarkerInterner();

    private MarkerTree m(String marker) {
        try {
            return MarkerTree.parse(interner, marker);
        } catch (MarkerParser.MarkerSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private MarkerTree t() {
        return MarkerTree.trueIn(interner);
    }

    private MarkerTree f() {
        return t().negate();
    }

    private static Ranges.Bound<PythonVersion> included(@Nullable String v) {
        return v == null ? Ranges.Bound.unbounded() : Ranges.Bound.included(PythonVersion.parse(v));
    }

    private static Ranges.Bound<PythonVersion> excluded(@Nullable String v) {
        return v == null ? Ranges.Bound.unbounded() : Ranges.Bound.excluded(PythonVersion.parse(v));
    }

    @Test
    void test_marker_negation() {
        assertThat(m("python_version > '3.6'").negate()).isEqualTo(m("python_version <= '3.6'"));
        assertThat(m("'3.6' < python_version").negate()).isEqualTo(m("python_version <= '3.6'"));
        assertThat(m("python_version != '3.6' and os_name == 'Linux'").negate()).isEqualTo(m("python_version == '3.6' or os_name != 'Linux'"));
        assertThat(m("python_version == '3.6' and os_name != 'Linux'").negate()).isEqualTo(m("python_version != '3.6' or os_name == 'Linux'"));
        assertThat(m("python_version != '3.6.*' and os_name == 'Linux'").negate()).isEqualTo(m("python_version == '3.6.*' or os_name != 'Linux'"));
        assertThat(m("python_version == '3.6.*'").negate()).isEqualTo(m("python_version != '3.6.*'"));
        assertThat(m("python_version != '3.6.*'").negate()).isEqualTo(m("python_version == '3.6.*'"));
        assertThat(m("python_version ~= '3.6'").negate()).isEqualTo(m("python_version < '3.6' or python_version != '3.*'"));
        assertThat(m("'3.6' ~= python_version").negate()).isEqualTo(m("python_version < '3.6' or python_version != '3.*'"));
        assertThat(m("python_version ~= '3.6.2'").negate()).isEqualTo(m("python_version < '3.6.2' or python_version != '3.6.*'"));
        assertThat(m("sys_platform == 'linux'").negate()).isEqualTo(m("sys_platform != 'linux'"));
        assertThat(m("'linux' == sys_platform").negate()).isEqualTo(m("sys_platform != 'linux'"));
        assertThat(m("sys_platform ~= 'linux'").negate()).isEqualTo(f());
        assertThat(m("'foo' == 'bar'").negate()).isEqualTo(f());
        assertThat(m("os_name == 'bar' and os_name == 'foo'").negate()).isEqualTo(m("os_name != 'bar' or os_name != 'foo'"));
        assertThat(m("os_name == 'bar' or os_name == 'foo'").negate()).isEqualTo(m("os_name != 'bar' and os_name != 'foo'"));
        assertThat(m("python_version >= '3.6' or python_version < '3.6'").negate()).isEqualTo(m("python_version < '3.6' and python_version >= '3.6'"));
    }

    @Test
    void simplify_python_versions() {
        assertThat(m("(extra == 'foo' and sys_platform == 'win32') or extra == 'foo'").simplifyExtrasWith("foo"::equals)).isEqualTo(t());
        assertThat(m("(python_version <= '3.11' and sys_platform == 'win32') or python_version > '3.11'")
          .simplifyPythonVersions(excluded("3.12"), excluded(null))).isEqualTo(t());
        assertThat(m("python_version < '3.10'").simplifyPythonVersions(excluded("3.7"), excluded(null)).tryToString())
          .isEqualTo("python_full_version < '3.10'");
        assertThat(m("python_version <= '3.12'").simplifyPythonVersions(excluded("3.12"), excluded(null)).tryToString())
          .isEqualTo("python_full_version < '3.13'");
        assertThat(m("python_full_version <= '3.12'").simplifyPythonVersions(excluded("3.12"),
                excluded(null))).isEqualTo(f());
        assertThat(m("python_full_version <= '3.12.1'").simplifyPythonVersions(excluded("3.12"),
                excluded(null)).tryToString())
          .isEqualTo("python_full_version <= '3.12.1'");
    }

    @Test
    void restrict() {
        MarkerTree environment = m("(platform_machine == 'x86_64' and sys_platform == 'darwin') or " +
          "(platform_machine == 'x86_64' and sys_platform == 'linux') or (platform_machine == 'AMD64' and sys_platform == 'win32')");
        MarkerTree marker = m("((platform_machine == 'x86_64' and sys_platform == 'darwin') or " +
          "(platform_machine == 'x86_64' and sys_platform == 'linux') or (platform_machine == 'AMD64' and sys_platform == 'win32')) and " +
          "python_version < '3.11'");
        MarkerTree simplified = marker.restrict(environment);
        assertThat(simplified).isEqualTo(m("python_version < '3.11'"));
        assertThat(simplified.and(environment)).isEqualTo(marker);
        assertThat(environment.restrict(environment)).isEqualTo(t());
        assertThat(m("python_version >= '3.12'").restrict(m("sys_platform == 'linux' or python_version >= '3.12'"))).isEqualTo(m("python_version >= '3.12'"));
        String[][] pairs = {
          {"python_version < '3.11'", "sys_platform == 'linux'"},
          {"sys_platform == 'linux'", "python_version < '3.11'"},
          {"sys_platform == 'linux' or python_version < '3.11'",
                  "sys_platform == 'darwin' or python_version >= '3.10'"},
          {"extra == 'foo' and sys_platform == 'linux'", "extra == 'foo' or sys_platform == 'darwin'"},
          {"python_version < '3.11'", "python_version >= '3.12'"},
        };
        for (String[] pair : pairs) {
            MarkerTree v = m(pair[0]);
            MarkerTree a = m(pair[1]);
            assertThat(v.restrict(a).and(a)).as(pair[0] + " under " + pair[1]).isEqualTo(v.and(a));
        }
    }

    private MarkerTree complexify(@Nullable String lower, @Nullable String upper, String marker) {
        return m(marker).complexifyPythonVersions(included(lower), excluded(upper));
    }

    @Test
    void complexified_markers() {
        MarkerTree never = m("python_full_version < '0' and python_full_version > '0'");
        assertThat(complexify(null, null, "python_full_version < '3.10'")).isEqualTo(m("python_full_version < '3.10'"));
        assertThat(complexify("3.8", null,
                "python_full_version < '3.10'")).isEqualTo(m("python_full_version >= '3.8' and python_full_version < '3.10'"));
        assertThat(complexify(null, "3.8", "python_full_version < '3.10'")).isEqualTo(m("python_full_version < '3.8'"));
        assertThat(complexify("3.8", "3.8", "python_full_version < '3.10'")).isEqualTo(never);
        assertThat(complexify("3.11", null, "python_full_version < '3.10'")).isEqualTo(never);
        assertThat(complexify("3.11", null,
                "python_full_version >= '3.10'")).isEqualTo(m("python_full_version >= '3.11'"));
        assertThat(complexify("3.11", null,
                "python_full_version >= '3.12'")).isEqualTo(m("python_full_version >= '3.12'"));
        assertThat(complexify(null, "3.11", "python_full_version > '3.12'")).isEqualTo(never);
        assertThat(complexify(null, "3.11",
                "python_full_version <= '3.12'")).isEqualTo(m("python_full_version < '3.11'"));
        assertThat(complexify(null, "3.11",
                "python_full_version <= '3.10'")).isEqualTo(m("python_full_version <= '3.10'"));
        assertThat(complexify("3.11", null, "python_full_version == '3.8'")).isEqualTo(never);
        assertThat(complexify("3.11", null,
                "python_full_version == '3.8' or python_full_version == '3.12'")).isEqualTo(m("python_full_version == '3.12'"));
        assertThat(complexify("3.11", null,
                "python_full_version == '3.8' or python_full_version == '3.11' or python_full_version == '3.12'"))
          .isEqualTo(m("python_full_version == '3.11' or python_full_version == '3.12'"));
        assertThat(complexify("3.12", null,
                "python_full_version < '3.10' or python_full_version >= '3.10'")).isEqualTo(m("python_full_version >= '3.12'"));
    }

    private MarkerTree simplify(@Nullable String lower, @Nullable String upper, String marker) {
        return m(marker).simplifyPythonVersions(included(lower), excluded(upper));
    }

    @Test
    void simplified_markers() {
        assertThat(simplify("3.8", null,
                "python_full_version >= '3.8' and python_full_version < '3.10'")).isEqualTo(m("python_full_version < '3.10'"));
        assertThat(simplify("3.8", null,
                "python_full_version < '3.7'")).isEqualTo(m("python_full_version < '0' and python_full_version > '0'"));
        assertThat(simplify("3.8", "3.11", "python_full_version == '3.7.*' or python_full_version == '3.8.*' or " +
          "python_full_version == '3.10.*' or python_full_version == '3.11.*'")).isEqualTo(m("python_full_version != '3.9.*'"));
        assertThat(simplify("3.8", null,
                "python_full_version >= '3.8' and sys_platform == 'win32'")).isEqualTo(m("sys_platform == 'win32'"));
        assertThat(simplify("3.8", null,
                "python_full_version >= '3.9' and (sys_platform == 'win32' or python_full_version >= '3.8')"))
          .isEqualTo(m("python_full_version >= '3.9'"));
    }

    @Test
    void without_extras() {
        assertThat(m("os_name == 'Linux'").withoutExtras()).isEqualTo(m("os_name == 'Linux'"));
        assertThat(m("extra == 'foo'").withoutExtras().isTrue()).isTrue();
        assertThat(m("os_name == 'Linux' and extra == 'foo'").withoutExtras()).isEqualTo(m("os_name == 'Linux'"));
        assertThat(m("(os_name == 'Linux' and extra == 'foo') or (os_name != 'Linux' and extra == 'bar')").withoutExtras().isTrue()).isTrue();
        assertThat(m("os_name == 'Linux' and extra != 'foo'").withoutExtras()).isEqualTo(m("os_name == 'Linux'"));
        assertThat(m("extra != 'extra-project-bar' and extra == 'extra-project-foo'").withoutExtras().isTrue()).isTrue();
        assertThat(m("(os_name == 'Darwin' and extra == 'foo') or (sys_platform == 'Linux' and extra != 'foo')").withoutExtras())
          .isEqualTo(m("os_name == 'Darwin' or sys_platform == 'Linux'"));
    }

    @Test
    void only_extras() {
        assertThat(m("os_name == 'Linux'").onlyExtras().isTrue()).isTrue();
        assertThat(m("extra == 'foo'").onlyExtras()).isEqualTo(m("extra == 'foo'"));
        assertThat(m("os_name == 'Linux' and extra == 'foo'").onlyExtras()).isEqualTo(m("extra == 'foo'"));
        assertThat(m("(os_name == 'foo' and extra == 'foo') or (os_name == 'bar' and extra != 'foo')").onlyExtras().isTrue()).isTrue();
        assertThat(m("(os_name == 'Linux' and extra == 'foo') or (os_name != 'Linux' and extra == 'bar')").onlyExtras())
          .isEqualTo(m("extra == 'foo' or extra == 'bar'"));
        assertThat(m("(implementation_name != 'pypy' and os_name == 'nt' and sys_platform == 'darwin') or (os_name == 'nt' and sys_platform == 'win32')").onlyExtras())
          .isEqualTo(m("os_name == 'Linux' or os_name != 'Linux'"));
    }

    @Test
    void test_simplify_extras() {
        assertThat(m("os_name == \"nt\" and extra == \"dev\"").simplifyExtrasWith("dev"::equals)).isEqualTo(m("os_name == \"nt\""));
        assertThat(m("os_name == \"nt\" or extra == \"dev\"").simplifyExtrasWith("dev"::equals)).isEqualTo(t());
        assertThat(m("extra == \"dev\"").simplifyExtrasWith("dev"::equals)).isEqualTo(t());
        assertThat(m("extra == \"dev\" and extra == \"test\"").simplifyExtrasWith("dev"::equals)).isEqualTo(m("extra == \"test\""));
        assertThat(m("os_name == \"nt\" and extra == \"test\"").simplifyExtrasWith("dev"::equals)).isEqualTo(m("os_name == \"nt\" and extra == \"test\""));
        assertThat(m("os_name == \"nt\" and (python_version == \"3.7\" or extra == \"dev\")").simplifyExtrasWith("dev"::equals)).isEqualTo(m("os_name == \"nt\""));
        assertThat(m("os_name == \"nt\" or (python_version == \"3.7\" and extra == \"dev\")").simplifyExtrasWith("dev"::equals))
          .isEqualTo(m("os_name == \"nt\" or python_version == \"3.7\""));
    }

    @Test
    void test_simplify_not_extras() {
        assertThat(m("os_name == \"nt\" and extra != \"dev\"").simplifyNotExtrasWith("dev"::equals)).isEqualTo(m("os_name == \"nt\""));
        assertThat(m("os_name == \"nt\" or extra != \"dev\"").simplifyNotExtrasWith("dev"::equals)).isEqualTo(t());
        assertThat(m("extra != \"dev\"").simplifyNotExtrasWith("dev"::equals)).isEqualTo(t());
        assertThat(m("extra != \"dev\" and extra != \"test\"").simplifyNotExtrasWith("dev"::equals)).isEqualTo(m("extra != \"test\""));
        assertThat(m("os_name != \"nt\" and extra != \"test\"").simplifyNotExtrasWith("dev"::equals)).isEqualTo(m("os_name != \"nt\" and extra != \"test\""));
        assertThat(m("os_name == \"nt\" and (python_version == \"3.7\" or extra != \"dev\")").simplifyNotExtrasWith("dev"::equals)).isEqualTo(m("os_name == \"nt\""));
        assertThat(m("os_name == \"nt\" or (python_version == \"3.7\" and extra != \"dev\")").simplifyNotExtrasWith("dev"::equals))
          .isEqualTo(m("os_name == \"nt\" or python_version == \"3.7\""));
    }
}
