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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * yarn 1.22.22's __tests__/resolvers/index.js and the exotics cases a lock-only install reaches: git-resolver.js,
 * registry-resolver.js, hosted-git-resolver.js and bitbucket-resolver.js's explodeHostedGitFragment.
 */
class ExoticResolversTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(nullValues = "null", value = {
            "0.0.0, null",
            "npm:0.0.0, NPM",
            "yarn:0.0.0, YARN",
            "git+ssh://user@hostname/path/repo#master, GIT",
            "git+https://hostname/path/repo#master, GIT",
            "git+file:///path/repo#master, GIT",
            "git://user@hostname/path/repo#master, GIT",
            "ssh://user@hostname/path/repo#master, GIT",
            "http://hostname/path/repo.git#master, GIT",
            "https://hostname/path/repo.git#master, GIT",
            "http://hostname/path/file.tgz, TARBALL",
            "https://hostname/path/file.tgz, TARBALL",
            "gist:id#hash, GIST",
    })
    void getExoticResolver(String pattern, @Nullable String resolver) {
        ExoticResolvers.Kind kind = ExoticResolvers.getExoticResolver(pattern);
        assertThat(kind == null ? null : kind.name()).isEqualTo(resolver);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "ssh://git@github.com/sindresorhus/beeper.git, true",
            "git://git@github.com/sindresorhus/beeper.git, true",
            "git+ssh://git@github.com/sindresorhus/beeper.git, true",
            "http://example.com/sindresorhus/beeper.git, true",
            "http://example.com/sindresorhus/beeper.git#branch, true",
            "http://example.com/sindresorhus/beeper, false",
            "http://example.com/sindresorhus/beeper#branch, false",
            "https://example.com/sindresorhus/beeper.git, true",
            "https://example.com/sindresorhus/beeper.git#branch, true",
            "https://example.com/sindresorhus/beeper, false",
            "https://example.com/sindresorhus/beeper#branch, false",
            "file:../project.git, false",
            "foo://github.com/sindresorhus/beeper, true",
            "foo://gitlab.com/sindresorhus/beeper, true",
            "foo://bitbucket.com/sindresorhus/beeper, true",
            "http://github.com/sindresorhus/beeper/archive/v1.0.0.tar.gz, false",
    })
    void gitIsVersion(String pattern, boolean isVersion) {
        assertThat(ExoticResolvers.isVersion(ExoticResolvers.Kind.GIT, pattern)).isEqualTo(isVersion);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "npm:foo@0.0.1, foo",
            "npm:@org/foo@0.0.1, @org/foo",
            "yarn:foo@0.0.1, foo",
            "yarn:@org/foo@0.0.1, @org/foo",
    })
    void registryResolverName(String fragment, String name) {
        assertThat(ExoticResolvers.parseRegistryFragment(fragment).getName()).isEqualTo(name);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "jure/lens#fix-issue-#96|jure|lens|fix-issue-#96",
            "jure/lens#feature/fix-issue|jure|lens|feature/fix-issue",
            "jure/lens.github|jure|lens.github|''",
            "jure/lens#semver:^3.0.0|jure|lens|semver:^3.0.0",
            "github:jure/lens|jure|lens|''",
            "bitbucket:jure/lens|jure|lens|''",
            "git+ssh://git@github.com:jure/lens|jure|lens|''",
            "git+ssh://git@github.com:jure/lens.git|jure|lens|''",
            "git://git@github.com:jure/lens.git|jure|lens|''",
            "git+https://login@github.com/jure/lens.git|jure|lens|''",
            "git+ssh://git@github.com/jure/lens.git#semver:^3.1.0|jure|lens|semver:^3.1.0",
            "git+http://login@github.com/jure/lens.git|jure|lens|''",
            "git@bitbucket.org:team2/repo.git|team2|repo|''",
            "git+ssh://git@bitbucket.org:team/repo.git|team|repo|''",
            "git@bitbucket.org/team/repo.git|team|repo|''",
            "package@git@bitbucket.org:team/repo.git|team|repo|''",
    })
    void explodeHostedGitFragment(String fragment, String user, String repo, String hash) {
        assertThat(GitResolver.explodeHostedGitFragment(fragment))
                .isEqualTo(new GitResolver.ExplodedFragment(user, repo, hash));
    }
}
