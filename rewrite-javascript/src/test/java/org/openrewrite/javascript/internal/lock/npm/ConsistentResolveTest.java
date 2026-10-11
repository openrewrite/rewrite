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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port of @npmcli/arborist test/consistent-resolve.js and test/relpath.js.
 */
class ConsistentResolveTest {

    private static @Nullable Object cr(@Nullable String resolved, @Nullable String fromPath, @Nullable String toPath,
                                       boolean rel) {
        return ConsistentResolve.consistentResolve(resolved, fromPath, toPath, rel, NpmRelease.PORTED);
    }

    private static @Nullable Object cr(@Nullable String resolved) {
        return cr(resolved, null, null, false);
    }

    @Test
    void fileAndDirectoriesResolvedToToPath() {
        String tp = "/foo";
        String fp = "/foo/bar";
        assertThat(cr("/foo/bar/baz", fp, tp, true)).isEqualTo("file:bar/baz");
        assertThat(cr("/foo/bar/baz", fp, tp, false)).isEqualTo("file:/foo/bar/baz");
        assertThat(cr("./baz", fp, tp, true)).isEqualTo("file:bar/baz");
        assertThat(cr("./baz", fp, tp, false)).isEqualTo("file:/foo/bar/baz");
        assertThat(cr("/foo/bar/baz.tgz", fp, tp, true)).isEqualTo("file:bar/baz.tgz");
        assertThat(cr("/foo/bar/baz.tgz", fp, tp, false)).isEqualTo("file:/foo/bar/baz.tgz");
        assertThat(cr("baz.tgz", fp, tp, true)).isEqualTo("file:bar/baz.tgz");
        assertThat(cr("baz.tgz", fp, tp, false)).isEqualTo("file:/foo/bar/baz.tgz");
        assertThat(cr("file:/foo/bar/baz", fp, tp, true)).isEqualTo("file:bar/baz");
        assertThat(cr("file:/foo/bar/baz", fp, tp, false)).isEqualTo("file:/foo/bar/baz");
        assertThat(cr("file:baz", fp, tp, true)).isEqualTo("file:bar/baz");
        assertThat(cr("file:baz", fp, tp, false)).isEqualTo("file:/foo/bar/baz");
        assertThat(cr("file:/foo/bar/baz.tgz", fp, tp, true)).isEqualTo("file:bar/baz.tgz");
        assertThat(cr("file:/foo/bar/baz.tgz", fp, tp, false)).isEqualTo("file:/foo/bar/baz.tgz");
        assertThat(cr("file:baz.tgz", fp, tp, true)).isEqualTo("file:bar/baz.tgz");
        assertThat(cr("file:baz.tgz", fp, tp, false)).isEqualTo("file:/foo/bar/baz.tgz");
    }

    @Test
    void fileAndDirectoriesMadeConsistentIfToPathNotSet() {
        String fp = "/foo/bar";
        for (boolean rel : new boolean[]{true, false}) {
            assertThat(cr("/foo/bar/baz", fp, null, rel)).isEqualTo("file:/foo/bar/baz");
            assertThat(cr("./baz", fp, null, rel)).isEqualTo("file:/foo/bar/baz");
            assertThat(cr("/foo/bar/baz.tgz", fp, null, rel)).isEqualTo("file:/foo/bar/baz.tgz");
            assertThat(cr("baz.tgz", fp, null, rel)).isEqualTo("file:/foo/bar/baz.tgz");
            assertThat(cr("file:/foo/bar/baz", fp, null, rel)).isEqualTo("file:/foo/bar/baz");
            assertThat(cr("file:baz", fp, null, rel)).isEqualTo("file:/foo/bar/baz");
            assertThat(cr("file:/foo/bar/baz.tgz", fp, null, rel)).isEqualTo("file:/foo/bar/baz.tgz");
            assertThat(cr("file:baz.tgz", fp, null, rel)).isEqualTo("file:/foo/bar/baz.tgz");
        }
    }

    @Test
    void consistentHostedGitInfoUrls() {
        String expect = "git+ssh://git@github.com/a/b.git";
        String expectAuth = "git+https://user:pass@github.com/a/b.git";
        assertThat(cr("a/b")).isEqualTo(expect);
        assertThat(cr("github:a/b")).isEqualTo(expect);
        assertThat(cr("git+https://github.com/a/b")).isEqualTo(expect);
        assertThat(cr("git://github.com/a/b")).isEqualTo(expect);
        assertThat(cr("git+ssh://git@github.com/a/b")).isEqualTo(expect);
        assertThat(cr("git+https://github.com/a/b.git")).isEqualTo(expect);
        assertThat(cr("git://github.com/a/b.git")).isEqualTo(expect);
        assertThat(cr("git+ssh://git@github.com/a/b.git")).isEqualTo(expect);
        assertThat(cr("git+https://user:pass@github.com/a/b.git")).isEqualTo(expectAuth);

        String hash = "#0000000000000000000000000000000000000000";
        assertThat(cr("a/b" + hash)).isEqualTo(expect + hash);
        assertThat(cr("github:a/b" + hash)).isEqualTo(expect + hash);
        assertThat(cr("git+https://github.com/a/b" + hash)).isEqualTo(expect + hash);
        assertThat(cr("git://github.com/a/b" + hash)).isEqualTo(expect + hash);
        assertThat(cr("git+ssh://git@github.com/a/b" + hash)).isEqualTo(expect + hash);
        assertThat(cr("git+https://github.com/a/b.git" + hash)).isEqualTo(expect + hash);
        assertThat(cr("git://github.com/a/b.git" + hash)).isEqualTo(expect + hash);
        assertThat(cr("git+ssh://git@github.com/a/b.git" + hash)).isEqualTo(expect + hash);
        assertThat(cr("xyz@a/b" + hash)).isEqualTo(expect + hash);
        assertThat(cr("xyz@github:a/b" + hash)).isEqualTo(expect + hash);
        assertThat(cr("xyz@git+https://github.com/a/b" + hash)).isEqualTo(expect + hash);
        assertThat(cr("xyz@git://github.com/a/b" + hash)).isEqualTo(expect + hash);
        assertThat(cr("xyz@git+ssh://git@github.com/a/b" + hash)).isEqualTo(expect + hash);
        assertThat(cr("xyz@git+https://github.com/a/b.git" + hash)).isEqualTo(expect + hash);
        assertThat(cr("xyz@git://github.com/a/b.git" + hash)).isEqualTo(expect + hash);
        assertThat(cr("xyz@git+ssh://git@github.com/a/b.git" + hash)).isEqualTo(expect + hash);
    }

    // consistent-resolve.js of npm 11.21.0 and of npm 12.0.0, run on these: from 12.0.0 a hosted URL written as https
    // stays https (hosted.default)
    @Test
    void hostedHttpsStaysHttpsFrom12() {
        String[][] rows = {
                {"git+https://github.com/user/repo.git#abc123", "git+ssh://git@github.com/user/repo.git#abc123",
                        "git+https://github.com/user/repo.git#abc123"},
                {"https://github.com/user/repo.git#abc123", "git+ssh://git@github.com/user/repo.git#abc123",
                        "git+https://github.com/user/repo.git#abc123"},
                {"git+https://gitlab.com/user/repo.git#abc123", "git+ssh://git@gitlab.com/user/repo.git#abc123",
                        "git+https://gitlab.com/user/repo.git#abc123"},
                {"git+https://github.com/user/repo.git#semver:^1.0.0",
                        "git+ssh://git@github.com/user/repo.git#semver:^1.0.0",
                        "git+https://github.com/user/repo.git#semver:^1.0.0"},
                {"git+https://user:pass@github.com/user/repo.git#abc123",
                        "git+https://user:pass@github.com/user/repo.git#abc123",
                        "git+https://user:pass@github.com/user/repo.git#abc123"},
                {"git+ssh://git@github.com/user/repo.git#abc123", "git+ssh://git@github.com/user/repo.git#abc123",
                        "git+ssh://git@github.com/user/repo.git#abc123"},
                {"github:user/repo#abc123", "git+ssh://git@github.com/user/repo.git#abc123",
                        "git+ssh://git@github.com/user/repo.git#abc123"},
                {"git://github.com/user/repo.git#abc123", "git+ssh://git@github.com/user/repo.git#abc123",
                        "git+ssh://git@github.com/user/repo.git#abc123"},
                {"gist:11081aaa281", "git+ssh://git@gist.github.com/11081aaa281.git",
                        "git+ssh://git@gist.github.com/11081aaa281.git"},
                {"https://github.com/user/repo/tarball/v1.0.0", "https://github.com/user/repo/tarball/v1.0.0",
                        "https://github.com/user/repo/tarball/v1.0.0"},
        };
        for (String[] row : rows) {
            assertThat(ConsistentResolve.consistentResolve(row[0], NpmRelease.of("11.21.0"))).as("11.21.0 %s", row[0])
              .isEqualTo(row[1]);
            assertThat(ConsistentResolve.consistentResolve(row[0], NpmRelease.of("12.0.0"))).as("12.0.0 %s", row[0])
              .isEqualTo(row[2]);
        }
    }

    @Test
    void unhostedGitReturnsSaveSpec() {
        String r = "git+https://x.com/y.git#0000000000000000000000000000000000000000";
        assertThat(cr(r)).isEqualTo(r);
        assertThat(cr("xyz@" + r)).isEqualTo(r);
    }

    @Test
    void remotesReturnedAsIs() {
        String r = "http://x.com/y.tgz";
        assertThat(cr(r)).isEqualTo(r);
        assertThat(cr("xyz@" + r)).isEqualTo(r);
    }

    @Test
    void falseyResolvedReturnsNull() {
        assertThat(cr(null)).isNull();
        assertThat(cr("")).isNull();
    }

    @Test
    void tagReturnsTag() {
        assertThat(cr("foo@latest")).isEqualTo("latest");
    }

    @Test
    void packageNameReturnsPackageName() {
        assertThat(cr("foo")).isEqualTo("foo");
    }

    @Test
    void invalidResolvedReturnsAsIs() {
        assertThat(cr("not ! a : v@lid t*A*g")).isEqualTo("not ! a : v@lid t*A*g");
    }

    @Test
    void relpath() {
        assertThat(Relpath.relpath("/a/b/c", "/a/b/c/d/e")).isEqualTo("d/e");
    }
}
