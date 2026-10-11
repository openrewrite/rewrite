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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * pnpm 11.2.2's lockfile/utils/test/toLockfileResolution.ts.
 */
class LockfileUtilsTest {

    private static final String REGISTRY = "https://registry.npmjs.org/";
    private static final String FOO = "https://registry.npmjs.org/foo/-/foo-1.0.0.tgz";
    private static final String GIT_HOSTED = "https://codeload.github.com/foo/bar/tar.gz/abcdef";

    private static String toLockfileResolution(String name, String version, String tarball, String registry,
                                               boolean lockfileIncludeTarballUrl) {
        return json(LockfileUtils.toLockfileResolution(name, version,
          obj("{'integrity':'sha512-AAAA','tarball':'" + tarball + "'}"), registry, lockfileIncludeTarballUrl));
    }

    private static String kept(String tarball) {
        return "{'integrity':'sha512-AAAA','tarball':'" + tarball + "'}";
    }

    @Test
    void keepsTheTarballWhenLockfileIncludeTarballUrlIsTrue() {
        assertThat(toLockfileResolution("foo", "1.0.0", FOO, REGISTRY, true)).isEqualTo(kept(FOO));
    }

    @Test
    void dropsTheTarballForStandardRegistryUrlsByDefault() {
        assertThat(toLockfileResolution("foo", "1.0.0", FOO, REGISTRY, false))
          .isEqualTo("{'integrity':'sha512-AAAA'}");
    }

    @Test
    void keepsTheTarballForNonStandardRegistryUrlsWhenLockfileIncludeTarballUrlIsFalse() {
        String tarball = "https://example.com/esprima-fb/-/esprima-fb-3001.1.0-dev-harmony-fb.tgz";
        assertThat(toLockfileResolution("esprima-fb", "3001.1.0-dev-harmony-fb", tarball, REGISTRY, false))
          .isEqualTo(kept(tarball));
    }

    @Test
    void keepsGitHubPackagesDownloadTarballUrlsWhenLockfileIncludeTarballUrlIsFalse() {
        String tarball = "https://npm.pkg.github.com/download/@example/private/1.2.3/" +
                         "0123456789abcdef0123456789abcdef01234567";
        assertThat(toLockfileResolution("@example/private", "1.2.3", tarball, "https://npm.pkg.github.com/", false))
          .isEqualTo(kept(tarball));
    }

    /**
     * Upstream's two cases, lockfileIncludeTarballUrl false and undefined, are one here: the port takes a boolean.
     */
    @Test
    void keepsFileTarballsEvenWhenLockfileIncludeTarballUrlIsFalse() {
        assertThat(toLockfileResolution("test-package", "1.0.0", "file:test-package-1.0.0.tgz", REGISTRY, false))
          .isEqualTo(kept("file:test-package-1.0.0.tgz"));
    }

    @Test
    void keepsGitHostedTarballsWhenLockfileIncludeTarballUrlIsFalse() {
        assertThat(toLockfileResolution("foo", "1.0.0", GIT_HOSTED, REGISTRY, false))
          .isEqualTo("{'integrity':'sha512-AAAA','tarball':'" + GIT_HOSTED + "','gitHosted':true}");
    }

    @Test
    void recordsGitHostedOnTheLockfileEntryWhenSetOnTheResolution() {
        assertThat(json(LockfileUtils.toLockfileResolution("foo", "1.0.0",
          obj("{'integrity':'sha512-AAAA','tarball':'" + GIT_HOSTED + "','gitHosted':true}"), REGISTRY, true)))
          .isEqualTo("{'integrity':'sha512-AAAA','tarball':'" + GIT_HOSTED + "','gitHosted':true}");
    }

    /**
     * small-config-dependency-object-form-versioned: a registry.yarnpkg.com tarball under the npm registry.
     */
    @Test
    void keepsATarballOfAnotherRegistry() {
        String tarball = "https://registry.yarnpkg.com/is-positive/-/is-positive-3.1.0.tgz";
        assertThat(toLockfileResolution("is-positive", "3.1.0", tarball, REGISTRY, false)).isEqualTo(kept(tarball));
    }

    @Test
    void getNpmTarballUrl() {
        assertThat(LockfileUtils.getNpmTarballUrl("foo", "1.0.0+build.1", null)).isEqualTo(FOO);
        assertThat(LockfileUtils.getNpmTarballUrl("@scope/foo", "1.0.0", "https://npm.example.com"))
          .isEqualTo("https://npm.example.com/@scope/foo/-/foo-1.0.0.tgz");
    }
}
