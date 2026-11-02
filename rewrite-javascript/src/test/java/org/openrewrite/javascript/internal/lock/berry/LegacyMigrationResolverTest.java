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
package org.openrewrite.javascript.internal.lock.berry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Yarn's packages/yarnpkg-core/tests/LegacyMigrationResolver.test.ts, every case.
 */
class LegacyMigrationResolverTest {

    @ParameterizedTest(name = "{1}")
    @CsvSource(delimiter = '|', value = {
            "4.17.21|http://registry.npmjs.org/lodash/-/lodash-4.17.21.tgz|npm:4.17.21",
            "1.0.0|https://foo.bar/@types%2fno-deps/-/no-deps-1.0.0.tgz|npm:1.0.0",
            "1.0.0|https://company.jfrog.io/company/api/npm/registry-name/@scope/package-name/-/@scope/" +
            "package-name-1.0.0.tgz#eeeec1e4e8850bed0468f938292b06cda793bf34|npm:1.0.0::__archiveUrl=" +
            "https%3A%2F%2Fcompany.jfrog.io%2Fcompany%2Fapi%2Fnpm%2Fregistry-name%2F%40scope%2Fpackage-name%2F-" +
            "%2F%40scope%2Fpackage-name-1.0.0.tgz%23",
            "1.0.0|https://packages.example.com/artifactory/api/npm/registry-name/@scope/package-name/-/@scope/" +
            "package-name-1.0.0.tgz#eeeec1e4e8850bed0468f938292b06cda793bf34|npm:1.0.0::__archiveUrl=" +
            "https%3A%2F%2Fpackages.example.com%2Fartifactory%2Fapi%2Fnpm%2Fregistry-name%2F%40scope%2F" +
            "package-name%2F-%2F%40scope%2Fpackage-name-1.0.0.tgz%23",
            "0.1.9|https://npm.fontawesome.com/@fortawesome/vue-fontawesome/-/0.1.10/vue-fontawesome-0.1.10.tgz" +
            "#eeeec1e4e8850bed0468f938292b06cda793bf34|npm:0.1.9",
            "12.0.5|https://registry.npm.taobao.org/yargs/download/yargs-12.0.5.tgz?cache=0&other_urls=https%3A%2F%2F" +
            "registry.npm.taobao.org%2Fyargs%2Fdownload%2Fyargs-12.0.5.tgz#05f5997b609647b64f66b81e3b4b10a368e7ad13" +
            "|npm:12.0.5",
            "0.0.0|git://github.com/nash-io/react-input-range#7895c98aaadb0f115557efd17330e10da8b785c7|" +
            "git://github.com/nash-io/react-input-range#commit=7895c98aaadb0f115557efd17330e10da8b785c7",
            "3.1.1|https://nexusrepomgr.$CORPORATION.com/repository/npm-proxy/yn/-/yn-3.1.1.tgz" +
            "#1e87401a09d767c1d5eab26a6e4c185182d2eb50|npm:3.1.1",
            "0.3.2|https://npm.pkg.github.com/download/@datadog/build-plugin/0.3.2/" +
            "92bb5d31f0463b9c175da48fcdbc5a6743ba65d9b337383c4a2deae2ad7dd531|npm:0.3.2",
            "1.0.0|https://npm.pkg.github.com/download/@foo/bar/1.0.0/" +
            "45936cf20c2c6a884858e18ee56d7c8db9ca080e47929e1691bef048ee2802cf" +
            "#1c16d31238ac5f93e311b44b0eced1f8251d44d4|npm:1.0.0",
    })
    void matchesTheResolvedUrl(String version, String resolved, String expected) {
        String reference = null;
        for (Map.Entry<Pattern, BiFunction<String, Matcher, String>> imported :
                LegacyMigrationResolver.IMPORTED_PATTERNS) {
            Matcher match = imported.getKey().matcher(resolved);
            if (match.find()) {
                reference = imported.getValue().apply(version, match);
                break;
            }
        }
        assertThat(reference).isEqualTo(expected);
    }
}
