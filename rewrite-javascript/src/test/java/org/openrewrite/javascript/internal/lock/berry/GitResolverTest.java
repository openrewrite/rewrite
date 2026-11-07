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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * plugin-git tests/gitUtils.test.ts {@code isGitUrl}: the first element of each valid pattern and the invalid ones
 * (Yarn 4.10.3).
 */
class GitResolverTest {

    @ParameterizedTest(name = "isGitUrl({0}) = {1}")
    @CsvSource(delimiter = '|', value = {
            "GitHubOrg/foo-bar.js|true",
            "GitHubOrg/foo2bar.js|true",
            "GitHubOrg/foo-bar.js#hash|true",
            "GitHubOrg/foo-bar.js#commit:hash|true",
            "GitHubOrg/foo-bar.js#commit=hash|true",
            "GitHubOrg/foo-bar.js#commit=hash&workspace=foo|true",
            "GitHubOrg/foo-bar.js#tag=hello|true",
            "GitHubOrg/foo-bar.js#workspace=foo|true",
            "github:GitHubOrg/foo-bar.js|true",
            "github:GitHubOrg/foo-bar.js#hash|true",
            "https://github.com/yarnpkg/util-deprecate.git#v1.0.0|true",
            "https://github.com/yarnpkg/util-deprecate.git#semver:v1.0.0|true",
            "https://github.com/yarnpkg/util-deprecate.git#master|true",
            "https://github.com/yarnpkg/util-deprecate.git#b3562c2798507869edb767da869cd7b85487726d|true",
            "https://github.com/yarnpkg/util-deprecate/tarball/b3562c2798507869edb767da869cd7b85487726d|true",
            "git://github.com/yarnpkg/util-deprecate.git#v1.0.1|true",
            "git+ssh://git@github.com/yarnpkg/util-deprecate.git#v1.0.1|true",
            "ssh://git@github.com/yarnpkg/util-deprecate.git#v1.0.1|true",
            "git+ssh://git@github.com:yarnpkg/berry.git#v2.1.1|true",
            "ssh://git@github.com:yarnpkg/berry.git#v2.1.1|true",
            "git+https://github.com/yarnpkg/util-deprecate#v1.0.1|true",
            "git+https://github.com/yarnpkg/util-deprecate.git#v1.0.1|true",
            "./.|false",
            "../..|false",
    })
    void detectsAGitUrl(String url, boolean expected) {
        assertThat(GitResolver.isGitUrl(url)).isEqualTo(expected);
    }
}
