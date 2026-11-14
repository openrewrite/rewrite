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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * What npm 12.2.0 does with a root's {@code patchedDependencies} over a project that depends on is-odd 3.0.1: every
 * code and message here is one it printed for a project whose manifest holds the entries.
 */
class PatchedDependenciesTest {

    private static final Set<String> NO_PATCH_FILE = new LinkedHashSet<>(Arrays.asList("package.json",
      "package-lock.json"));

    private static Node project(@Nullable String patchedDependencies) {
        Map<String, Object> pkg = obj("name", "p", "version", "1.0.0", "dependencies", obj("is-odd", "3.0.1"));
        if (patchedDependencies != null) {
            pkg.put("patchedDependencies", json(patchedDependencies));
        }
        return node(opts().path("/p").release(NpmRelease.of("12.2.0")).pkg(pkg)
          .child(opts().pkg(obj("name", "is-odd", "version", "3.0.1", "dependencies", obj("is-number", "^6.0.0")))
            .build())
          .child(opts().pkg(obj("name", "is-number", "version", "6.0.0")).build()));
    }

    // each over a project that holds no patch file: the entry that wins is-odd 3.0.1 is the exact version, else the
    // narrowest range, else the name alone, and its path is read against the project
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "{\"is-odd@3.0.1\":\"patches/is-odd.patch\"} | EPATCHNOTFOUND | patch file not found: patches/is-odd.patch",
      "{\"is-odd\":\"../is-odd.patch\"} | EPATCHUNSAFE | patch path escapes the project: ../is-odd.patch",
      "{\"is-odd\":\"/abs/p.patch\"} | EPATCHUNSAFE | patch path escapes the project: /abs/p.patch",
      "{\"is-odd\":\".\"} | EPATCHUNSAFE | patch path escapes the project: .",
      "{\"is-odd@v3.0.1\":\"../loose.patch\"} | EPATCHUNSAFE | patch path escapes the project: ../loose.patch",
      "{\"is-odd@=3.0.1\":\"../eq.patch\"} | EPATCHUNSAFE | patch path escapes the project: ../eq.patch",
      "{\"is-odd\":1} | ERR_INVALID_ARG_TYPE | " +
      "The \"paths[1]\" argument must be of type string. Received type number (1)",
      "{\"is-odd@>=3.0.0\":\"../a.patch\",\"is-odd@3.0.x\":\"../../b.patch\"} | EPATCHUNSAFE | " +
      "patch path escapes the project: ../../b.patch",
      "{\"is-odd@>=3.0.0\":\"a.patch\",\"is-odd@<4\":\"b.patch\"} | EPATCHAMBIGUOUS | Ambiguous patch selectors for " +
      "is-odd@3.0.1: \"is-odd@>=3.0.0\" and \"is-odd@<4\" overlap but neither is a subset. Add an exact " +
      "\"is-odd@3.0.1\" entry to disambiguate.",
      "{\"left-pad@1.3.0\":\"patches/left-pad.patch\"} | EPATCHUNUSED | The following patches were registered but " +
      "matched no installed package:~  left-pad@1.3.0 -> patches/left-pad.patch~Use --allow-unused-patches to " +
      "install anyway.",
      "{\"is-odd@^2\":\"a.patch\",\"is-odd@latest\":\"b.patch\"} | EPATCHUNUSED | The following patches were " +
      "registered but matched no installed package:~  is-odd@^2 -> a.patch~  is-odd@latest -> b.patch~" +
      "Use --allow-unused-patches to install anyway."
    })
    void npmRefusesAPatchSetItCannotApply(String patchedDependencies, String code, String message) {
        Node tree = project(patchedDependencies);
        assertThat(PatchedDependencies.concerns(tree)).isTrue();
        assertThatThrownBy(() -> PatchedDependencies.resolvePatchedDependencies(tree, "/p", NO_PATCH_FILE))
          .isInstanceOfSatisfying(NpmError.class, e -> assertThat(e.getCode()).isEqualTo(code))
          .hasMessage(message.replace('~', '\n'));
    }

    // the selector's own order, which the code follows from patched-dependencies.js: an exact version over a range
    // that holds it, and a range over the name alone
    @Test
    void theMostSpecificEntryWins() {
        assertThatThrownBy(() -> PatchedDependencies.resolvePatchedDependencies(project(
          "{\"is-odd\":\"../name.patch\",\"is-odd@>=3\":\"../range.patch\",\"is-odd@3.0.1\":\"../exact.patch\"}"),
          "/p", NO_PATCH_FILE))
          .hasMessage("patch path escapes the project: ../exact.patch");
        assertThatThrownBy(() -> PatchedDependencies.resolvePatchedDependencies(project(
          "{\"is-odd\":\"../name.patch\",\"is-odd@>=3\":\"../range.patch\",\"is-odd@2.0.0\":\"../exact.patch\"}"),
          "/p", NO_PATCH_FILE))
          .hasMessage("patch path escapes the project: ../range.patch");
        assertThatThrownBy(() -> PatchedDependencies.resolvePatchedDependencies(project(
          "{\"is-odd\":\"../name.patch\",\"is-odd@^2\":\"../range.patch\"}"), "/p", NO_PATCH_FILE))
          .hasMessage("patch path escapes the project: ../name.patch");
    }

    // npm writes the patch file's integrity into the node's record: a file the project holds, or may hold where the
    // host does not say which files it has, is what the engine is not given
    @Test
    void aPatchFileTheProjectHoldsIsNotTheEnginesToRead() {
        String patched = "{\"is-odd@3.0.1\":\"patches/is-odd.patch\"}";
        Set<String> held = new LinkedHashSet<>(Arrays.asList("package.json", "patches/is-odd.patch"));
        assertThatThrownBy(() -> PatchedDependencies.resolvePatchedDependencies(project(patched), "/p", held))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("patches/is-odd.patch");
        assertThatThrownBy(() -> PatchedDependencies.resolvePatchedDependencies(project(patched), "/p", null))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("patch file patches/is-odd.patch");
    }

    // npm reads a patch file that is there and goes on: over a project that holds patches/is-odd.patch, whatever is
    // in it, its answer is what it refuses of the entries after
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "{\"is-odd@3.0.1\":\"patches/is-odd.patch\",\"left-pad\":\"x.patch\"} | EPATCHUNUSED | The following patches " +
      "were registered but matched no installed package:~  left-pad -> x.patch~Use --allow-unused-patches to " +
      "install anyway.",
      "{\"is-odd\":\"patches/is-odd.patch\",\"is-number\":\"patches/none.patch\"} | EPATCHNOTFOUND | " +
      "patch file not found: patches/none.patch"
    })
    void aPatchFileThatIsThereLeavesNpmsLaterRefusals(String patchedDependencies, String code, String message) {
        Set<String> held = new LinkedHashSet<>(Arrays.asList("package.json", "patches/is-odd.patch"));
        assertThatThrownBy(() -> PatchedDependencies.resolvePatchedDependencies(project(patchedDependencies), "/p",
          held))
          .isInstanceOfSatisfying(NpmError.class, e -> assertThat(e.getCode()).isEqualTo(code))
          .hasMessage(message.replace('~', '\n'));
        // where the host does not say what the project holds, the first patch file may be the one npm misses
        assertThatThrownBy(() -> PatchedDependencies.resolvePatchedDependencies(project(patchedDependencies), "/p",
          null))
          .isInstanceOf(CannotKnow.class);
    }

    // a record the lock holds for a package no entry names any more is dropped, and a project without either has
    // nothing for npm to resolve
    @Test
    void aRecordWithoutItsEntryIsDropped() {
        Node none = project(null);
        assertThat(PatchedDependencies.concerns(none)).isFalse();
        assertThat(PatchedDependencies.concerns(project("{}"))).isFalse();

        Node tree = node(opts().path("/p").release(NpmRelease.of("12.2.0"))
          .pkg(obj("name", "p", "version", "1.0.0", "dependencies", obj("is-odd", "3.0.1")))
          .child(opts().pkg(obj("name", "is-odd", "version", "3.0.1"))
            .patched(obj("path", "patches/is-odd.patch", "integrity", "sha512-abc")).build()));
        Node isOdd = tree.getChildren().get("is-odd");
        assertThat(isOdd.getPatched()).isNotNull();
        assertThat(PatchedDependencies.concerns(tree)).isTrue();

        PatchedDependencies.resolvePatchedDependencies(tree, "/p", NO_PATCH_FILE);

        assertThat(isOdd.getPatched()).isNull();
    }
}
