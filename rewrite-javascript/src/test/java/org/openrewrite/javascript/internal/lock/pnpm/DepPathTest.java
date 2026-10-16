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

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * deps/path/test/index.ts of pnpm 11.2.2, and the peer suffix as the peer resolution builds it.
 */
class DepPathTest {

    @Test
    void isAbsolute() {
        assertThat(DepPath.isAbsolute("/foo/1.0.0")).isFalse();
        assertThat(DepPath.isAbsolute("registry.npmjs.org/foo/1.0.0")).isTrue();
    }

    @Test
    void parse() {
        assertThatThrownBy(() -> DepPath.parse(Js.UNDEFINED)).hasMessageContaining("got `undefined`");
        assertThatThrownBy(() -> DepPath.parse(PnpmTestSupport.obj("{}"))).hasMessageContaining("got `object`");
        assertThatThrownBy(() -> DepPath.parse(1.0)).hasMessageContaining("got `number`");
        assertThat(DepPath.parse("foo@1.0.0")).isEqualTo(new DepPath.DependencyPath("foo", null, "1.0.0", null, null));
        assertThat(DepPath.parse("@foo/bar@1.0.0"))
          .isEqualTo(new DepPath.DependencyPath("@foo/bar", null, "1.0.0", null, null));
        assertThat(DepPath.parse("foo@1.0.0(@types/babel__core@7.1.14)"))
          .isEqualTo(new DepPath.DependencyPath("foo", "(@types/babel__core@7.1.14)", "1.0.0", null, null));
        assertThat(DepPath.parse("foo@1.0.0(@types/babel__core@7.1.14)(foo@1.0.0)"))
          .isEqualTo(new DepPath.DependencyPath("foo", "(@types/babel__core@7.1.14)(foo@1.0.0)", "1.0.0", null, null));
        assertThat(DepPath.parse("@(-.-)/foo@1.0.0(@types/babel__core@7.1.14)(foo@1.0.0)"))
          .isEqualTo(new DepPath.DependencyPath("@(-.-)/foo", "(@types/babel__core@7.1.14)(foo@1.0.0)", "1.0.0", null,
            null));
        assertThat(DepPath.parse("tar-pkg@file:../tar-pkg-1.0.0.tgz"))
          .isEqualTo(new DepPath.DependencyPath("tar-pkg", null, null, "file:../tar-pkg-1.0.0.tgz", null));
        assertThat(DepPath.parse("foo@1.0.0(patch_hash=0000)(@types/babel__core@7.1.14)"))
          .isEqualTo(new DepPath.DependencyPath("foo", "(@types/babel__core@7.1.14)", "1.0.0", null,
            "(patch_hash=0000)"));
    }

    @Test
    void refToRelative() {
        assertThat(DepPath.refToRelative("1.3.0", "@most/multicast")).isEqualTo("@most/multicast@1.3.0");
        assertThat(DepPath.refToRelative("1.3.0", "most")).isEqualTo("most@1.3.0");
        assertThat(DepPath.refToRelative("m@1.3.0", "most")).isEqualTo("m@1.3.0");
        assertThat(DepPath.refToRelative("@most/multicast@1.3.0", "most")).isEqualTo("@most/multicast@1.3.0");
        assertThat(DepPath.refToRelative("@most/multicast@1.3.0", "@most/multicast"))
          .isEqualTo("@most/multicast@1.3.0");
        assertThat(DepPath.refToRelative("@most/multicast@1.3.0(@foo/bar@1.0.0)", "@most/multicast"))
          .isEqualTo("@most/multicast@1.3.0(@foo/bar@1.0.0)");
        assertThat(DepPath.refToRelative("@most/multicast@1.3.0(@foo/bar@1.0.0)(@foo/qar@1.0.0)", "@most/multicast"))
          .isEqualTo("@most/multicast@1.3.0(@foo/bar@1.0.0)(@foo/qar@1.0.0)");
        assertThat(DepPath.refToRelative("link:../foo", "foo")).isNull();
        assertThat(DepPath.refToRelative("file:../tarball.tgz", "foo")).isEqualTo("foo@file:../tarball.tgz");
        assertThat(DepPath.refToRelative("1.3.0(@foo/bar@1.0.0)", "@qar/bar"))
          .isEqualTo("@qar/bar@1.3.0(@foo/bar@1.0.0)");
        assertThat(DepPath.refToRelative("1.3.0(@foo/bar@1.0.0)(@foo/qar@1.0.0)", "@qar/bar"))
          .isEqualTo("@qar/bar@1.3.0(@foo/bar@1.0.0)(@foo/qar@1.0.0)");
    }

    @Test
    void depPathToFilename() {
        assertThat(DepPath.depPathToFilename("/foo@1.0.0", 120)).isEqualTo("foo@1.0.0");
        assertThat(DepPath.depPathToFilename("/@foo/bar@1.0.0", 120)).isEqualTo("@foo+bar@1.0.0");
        assertThat(DepPath.depPathToFilename("github.com/something/foo/0000?v=1", 120))
          .isEqualTo("github.com+something+foo+0000+v=1");
        assertThat(DepPath.depPathToFilename("\\//:*?\"<>|", 120)).isEqualTo("++++++++++");
        assertThat(DepPath.depPathToFilename("/foo@1.0.0(react@16.0.0)(react-dom@16.0.0)", 120))
          .isEqualTo("foo@1.0.0_react@16.0.0_react-dom@16.0.0");
        assertThat(DepPath.depPathToFilename("/foo@1.0.0(react@16.0.0(react-dom@1.0.0))(react-dom@16.0.0)", 120))
          .isEqualTo("foo@1.0.0_react@16.0.0_react-dom@1.0.0__react-dom@16.0.0");
        String filename = DepPath.depPathToFilename("file:test/foo-1.0.0.tgz_foo@2.0.0", 120);
        assertThat(filename).isEqualTo("file+test+foo-1.0.0.tgz_foo@2.0.0").doesNotContain(":");
        assertThat(DepPath.depPathToFilename(String.join("", Collections.nCopies(200, "abcd/")), 120))
          .isEqualTo("abcd+abcd+abcd+abcd+abcd+abcd+abcd+abcd+abcd+abcd+abcd+abcd+abcd+abcd+abcd+abcd+abcd+ab_" +
                     "e7c10c3598ebbc0ca640b6524c68e602");
        assertThat(DepPath.depPathToFilename("/JSONSteam@1.0.0", 120))
          .isEqualTo("JSONSteam@1.0.0_533d3b11e9111b7a24f914844c021ddf");
        assertThat(DepPath.depPathToFilename("foo@git+https://github.com/something/foo#1234", 120))
          .isEqualTo("foo@git+https+++github.com+something+foo+1234");
        assertThat(DepPath.depPathToFilename(
          "foo@https://codeload.github.com/something/foo/tar.gz/1234#path:packages/foo", 120))
          .isEqualTo("foo@https+++codeload.github.com+something+foo+tar.gz+1234+path+packages+foo");
    }

    @Test
    void tryGetPackageId() {
        assertThat(DepPath.tryGetPackageId("/foo@1.0.0(@types/babel__core@7.1.14)")).isEqualTo("/foo@1.0.0");
        assertThat(DepPath.tryGetPackageId("/foo@1.0.0(@types/babel__core@7.1.14(is-odd@1.0.0))"))
          .isEqualTo("/foo@1.0.0");
        assertThat(DepPath.tryGetPackageId("/@(-.-)/foo@1.0.0(@types/babel__core@7.1.14)"))
          .isEqualTo("/@(-.-)/foo@1.0.0");
        assertThat(DepPath.tryGetPackageId("foo@1.0.0(patch_hash=xxxx)(@types/babel__core@7.1.14)"))
          .isEqualTo("foo@1.0.0");
    }

    @Test
    void getPkgIdWithPatchHash() {
        assertThat(DepPath.getPkgIdWithPatchHash("node@runtime:24.11.1")).isEqualTo("node@runtime:24.11.1");
        assertThat(DepPath.getPkgIdWithPatchHash("foo@1.0.0")).isEqualTo("foo@1.0.0");
        assertThat(DepPath.getPkgIdWithPatchHash("foo@1.0.0(patch_hash=xxxx)")).isEqualTo("foo@1.0.0(patch_hash=xxxx)");
        assertThat(DepPath.getPkgIdWithPatchHash("foo@1.0.0(@types/babel__core@7.1.14)")).isEqualTo("foo@1.0.0");
        assertThat(DepPath.getPkgIdWithPatchHash("foo@1.0.0(patch_hash=xxxx)(@types/babel__core@7.1.14)"))
          .isEqualTo("foo@1.0.0(patch_hash=xxxx)");
        assertThat(DepPath.getPkgIdWithPatchHash("@foo/bar@1.0.0")).isEqualTo("@foo/bar@1.0.0");
        assertThat(DepPath.getPkgIdWithPatchHash("@foo/bar@1.0.0(patch_hash=yyyy)"))
          .isEqualTo("@foo/bar@1.0.0(patch_hash=yyyy)");
        assertThat(DepPath.getPkgIdWithPatchHash("@foo/bar@1.0.0(@types/node@18.0.0)")).isEqualTo("@foo/bar@1.0.0");
        assertThat(DepPath.getPkgIdWithPatchHash("@foo/bar@1.0.0(patch_hash=zzzz)(@types/node@18.0.0)"))
          .isEqualTo("@foo/bar@1.0.0(patch_hash=zzzz)");
    }

    @Test
    void isRuntimeDepPath() {
        assertThat(DepPath.isRuntimeDepPath("node@runtime:20.1.0")).isTrue();
        assertThat(DepPath.isRuntimeDepPath("node@20.1.0")).isFalse();
    }

    @Test
    void removeSuffix() {
        assertThat(DepPath.removeSuffix("foo@1.0.0(patch_hash=0000)(@types/babel__core@7.1.14)"))
          .isEqualTo("foo@1.0.0");
    }

    /**
     * The suffixes recorded in {@code peer-transitive} and {@code settings-peers-suffix-max-length}: the sorted
     * peer ids joined, and past the maximum the first 32 hex digits of their sha256.
     */
    @Test
    void createPeerDepGraphHash() {
        assertThat(DepPath.createPeerDepGraphHash(Collections.singletonList(
          DepPath.PeerId.of("@isaacs/testing-peer-deps-b@2.0.1(@isaacs/testing-peer-deps-c@2.0.0)")), 1000))
          .isEqualTo("(@isaacs/testing-peer-deps-b@2.0.1(@isaacs/testing-peer-deps-c@2.0.0))");
        assertThat(DepPath.createPeerDepGraphHash(Arrays.asList(DepPath.PeerId.of("b", "2.0.0"),
          DepPath.PeerId.of("/a@1.0.0")), 1000)).isEqualTo("(a@1.0.0)(b@2.0.0)");
        assertThat(DepPath.createPeerDepGraphHash(Collections.singletonList(
          DepPath.PeerId.of("@isaacs/testing-peer-deps-c@2.0.0")), 10))
          .isEqualTo("(873e29c1c507d11af6a47e60489c9da2)");
    }
}
