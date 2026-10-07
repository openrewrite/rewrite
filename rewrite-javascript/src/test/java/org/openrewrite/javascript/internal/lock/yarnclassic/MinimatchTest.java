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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The globs a resolution or a workspaces list takes, as yarn 1.22.22's bundled minimatch 3.0.4 answers them: path
 * globs, extglobs, classes, braces, negation, comments and escapes.
 */
class MinimatchTest {

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = ';', value = {
            "**/ms;ms;true",
            "**/ms;debug/ms;true",
            "**/ms;a/b/ms;true",
            "**/ms;msx;false",
            "**/ms;debug/msx;false",
            "**/ms;.x/ms;false",
            "debug/ms;debug/ms;true",
            "debug/ms;morgan/debug/ms;false",
            "debug/ms;ms;false",
            "**/debug/ms;debug/ms;true",
            "**/debug/ms;morgan/debug/ms;true",
            "**/debug/ms;debug/x/ms;false",
            "a/**/ms;a/ms;true",
            "a/**/ms;a/b/ms;true",
            "a/**/ms;a/b/c/ms;true",
            "a/**/ms;b/ms;false",
            "**/@scope/pkg;@scope/pkg;true",
            "**/@scope/pkg;x/@scope/pkg;true",
            "**/@scope/pkg;x/@scope/other;false",
            "@scope/pkg/**/ms;@scope/pkg/ms;true",
            "@scope/pkg/**/ms;@scope/pkg/x/ms;true",
            "workspace-aggregator-x/a/left-pad;workspace-aggregator-x/a/left-pad;true",
            "workspace-aggregator-x/a/left-pad;a/left-pad;false",
            "**/a/left-pad;workspace-aggregator-x/a/left-pad;true",
            "**/a/left-pad;a/left-pad;true",
            "**/a/left-pad;b/left-pad;false",
            "packages/*/+(package.json);packages/a/package.json;true",
            "packages/*/+(package.json);packages/a/b/package.json;false",
            "packages/*/+(package.json);packages/.a/package.json;false",
            "packages/*/+(package.json);packages/a/package.jsonpackage.json;true",
            "packages/*/+(package.json);other/a/package.json;false",
            "packages/*/node_modules/**/+(package.json);packages/a/node_modules/x/package.json;true",
            "packages/*/node_modules/**/+(package.json);packages/a/package.json;false",
            "packages/**/+(package.json);packages/a/package.json;true",
            "packages/**/+(package.json);packages/a/b/package.json;true",
            "packages/**/+(package.json);packages/a/node_modules/x/package.json;true",
            "packages/**/+(package.json);packages/.a/b/package.json;false",
            "{a,b}/+(package.json);a/package.json;true",
            "{a,b}/+(package.json);b/package.json;true",
            "{a,b}/+(package.json);c/package.json;false",
            "pkg-[ab]/+(package.json);pkg-a/package.json;true",
            "pkg-[ab]/+(package.json);pkg-c/package.json;false",
            "pkg-[!a]/+(package.json);pkg-a/package.json;false",
            "pkg-[!a]/+(package.json);pkg-c/package.json;true",
            "pkg-?/+(package.json);pkg-a/package.json;true",
            "pkg-?/+(package.json);pkg-ab/package.json;false",
            "!(x)/+(package.json);a/package.json;true",
            "!(x)/+(package.json);x/package.json;true",
            "@(a|b)/+(package.json);a/package.json;true",
            "@(a|b)/+(package.json);b/package.json;true",
            "@(a|b)/+(package.json);c/package.json;false",
            "packages/a/+(package.json);packages/a/package.json;true",
            "packages/a/+(package.json);packages/b/package.json;false",
            "*;ms;true",
            "*;.ms;false",
            "*;a/ms;false",
            "a\\*b;a*b;true",
            "a\\*b;axb;false",
            "'#comment';'#comment';false",
            "!ms;ms;false",
            "!ms;debug;true",
            "{1..3}/x;1/x;true",
            "{1..3}/x;2/x;true",
            "{1..3}/x;4/x;false",
            "[z-a]/x;[z-a]/x;true",
            "[z-a]/x;b/x;false",
    })
    void matchesAsMinimatchDoes(String pattern, String path, boolean match) {
        assertThat(Minimatch.minimatch(path, pattern)).isEqualTo(match);
    }
}
