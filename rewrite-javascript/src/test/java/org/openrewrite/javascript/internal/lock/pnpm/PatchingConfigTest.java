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
import org.openrewrite.javascript.internal.lock.pnpm.PatchingConfig.PatchGroup;
import org.openrewrite.javascript.internal.lock.pnpm.PatchingConfig.PatchGroupRangeItem;
import org.openrewrite.javascript.internal.lock.pnpm.PatchingConfig.PatchInfo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Port of pnpm 11.2.2's patching/config/test/getPatchInfo.test.ts, groupPatchedDependencies.test.ts and
 * index.test.ts, with verifyPatches.
 */
class PatchingConfigTest {

    private static final String HASH = "00000000000000000000000000000000";

    @Test
    void getPatchInfoOfUndefinedIsUndefined() {
        assertThat(PatchingConfig.getPatchInfo(null, "foo", "1.0.0")).isNull();
    }

    @Test
    void exactVersionPatchWhenNameAndVersionMatch() {
        PatchGroup foo = new PatchGroup();
        foo.exact.put("1.0.0", info("foo@1.0.0"));
        Map<String, PatchGroup> patched = Collections.singletonMap("foo", foo);
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "1.0.0")).isEqualTo(foo.exact.get("1.0.0"));
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "1.1.0")).isNull();
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "2.0.0")).isNull();
        assertThat(PatchingConfig.getPatchInfo(patched, "bar", "1.0.0")).isNull();
    }

    @Test
    void rangeVersionPatchWhenNameMatchesAndVersionSatisfies() {
        PatchGroup foo = new PatchGroup();
        foo.range.add(new PatchGroupRangeItem("1", info("foo@1")));
        Map<String, PatchGroup> patched = Collections.singletonMap("foo", foo);
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "1.0.0")).isEqualTo(foo.range.get(0).getPatch());
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "1.1.0")).isEqualTo(foo.range.get(0).getPatch());
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "2.0.0")).isNull();
        assertThat(PatchingConfig.getPatchInfo(patched, "bar", "1.0.0")).isNull();
    }

    @Test
    void nameOnlyPatchWhenNameMatches() {
        PatchGroup foo = new PatchGroup();
        foo.all = info("foo");
        Map<String, PatchGroup> patched = Collections.singletonMap("foo", foo);
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "1.0.0")).isEqualTo(foo.all);
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "1.1.0")).isEqualTo(foo.all);
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "2.0.0")).isEqualTo(foo.all);
        assertThat(PatchingConfig.getPatchInfo(patched, "bar", "1.0.0")).isNull();
    }

    @Test
    void exactOverridesRangeAndRangeOverridesNameOnly() {
        PatchGroup foo = new PatchGroup();
        foo.exact.put("1.0.0", info("foo@1.0.0"));
        foo.exact.put("1.1.0", info("foo@1.1.0"));
        foo.range.add(new PatchGroupRangeItem("1", info("foo@1")));
        foo.range.add(new PatchGroupRangeItem("2", info("foo@2")));
        foo.all = info("foo");
        Map<String, PatchGroup> patched = Collections.singletonMap("foo", foo);
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "1.0.0")).isEqualTo(foo.exact.get("1.0.0"));
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "1.1.0")).isEqualTo(foo.exact.get("1.1.0"));
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "1.1.1")).isEqualTo(foo.range.get(0).getPatch());
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "2.0.0")).isEqualTo(foo.range.get(1).getPatch());
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "2.1.0")).isEqualTo(foo.range.get(1).getPatch());
        assertThat(PatchingConfig.getPatchInfo(patched, "foo", "3.0.0")).isEqualTo(foo.all);
        assertThat(PatchingConfig.getPatchInfo(patched, "bar", "1.0.0")).isNull();
    }

    @Test
    void throwsWhenMoreThanOneRangeMatches() {
        PatchGroup foo = new PatchGroup();
        foo.range.add(new PatchGroupRangeItem(">=1.0.0 <3.0.0", info("foo@>=1.0.0 <3.0.0")));
        foo.range.add(new PatchGroupRangeItem(">=2.0.0", info("foo@>=2.0.0")));
        assertThatThrownBy(() -> PatchingConfig.getPatchInfo(Collections.singletonMap("foo", foo), "foo", "2.1.0"))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_PATCH_KEY_CONFLICT");
              assertThat(e.getMessage())
                .isEqualTo("Unable to choose between 2 version ranges to patch foo@2.1.0: >=1.0.0 <3.0.0, >=2.0.0");
              assertThat(e.getHint()).isEqualTo("Explicitly set the exact version (foo@2.1.0) to resolve conflict");
          });
    }

    @Test
    void exactVersionWinsOverSeveralMatchingRanges() {
        PatchGroup foo = new PatchGroup();
        foo.exact.put("2.1.0", info("foo@>=1.0.0 <3.0.0"));
        foo.range.add(new PatchGroupRangeItem(">=1.0.0 <3.0.0", info("foo@>=1.0.0 <3.0.0")));
        foo.range.add(new PatchGroupRangeItem(">=2.0.0", info("foo@>=2.0.0")));
        assertThat(PatchingConfig.getPatchInfo(Collections.singletonMap("foo", foo), "foo", "2.1.0"))
          .isEqualTo(foo.exact.get("2.1.0"));
    }

    @Test
    void groupsByNameMatchTypeAndVersion() {
        Map<String, String> patchedDependencies = new LinkedHashMap<>();
        for (String key : new String[]{"exact-version-only@0.0.0", "exact-version-only@1.2.3",
          "exact-version-only@2.1.0", "version-range-only@~1.2.0", "version-range-only@4", "star-version-range@*",
          "without-versions", "mixed-style@0.1.2", "mixed-style@1.x.x", "mixed-style"}) {
            patchedDependencies.put(key, HASH);
        }
        Map<String, PatchGroup> groups = PatchingConfig.groupPatchedDependencies(patchedDependencies);
        assertThat(groups).containsOnlyKeys("exact-version-only", "version-range-only", "star-version-range",
          "without-versions", "mixed-style");

        PatchGroup exactOnly = groups.get("exact-version-only");
        assertThat(exactOnly.exact).containsOnlyKeys("0.0.0", "1.2.3", "2.1.0");
        assertThat(exactOnly.exact.get("1.2.3")).isEqualTo(info("exact-version-only@1.2.3"));
        assertThat(exactOnly.range).isEmpty();
        assertThat(exactOnly.all).isNull();

        PatchGroup rangeOnly = groups.get("version-range-only");
        assertThat(rangeOnly.exact).isEmpty();
        assertThat(rangeOnly.range).containsExactly(
          new PatchGroupRangeItem("~1.2.0", info("version-range-only@~1.2.0")),
          new PatchGroupRangeItem("4", info("version-range-only@4")));
        assertThat(rangeOnly.all).isNull();

        PatchGroup star = groups.get("star-version-range");
        assertThat(star.exact).isEmpty();
        assertThat(star.range).isEmpty();
        assertThat(star.all).isEqualTo(info("star-version-range@*"));

        PatchGroup without = groups.get("without-versions");
        assertThat(without.exact).isEmpty();
        assertThat(without.range).isEmpty();
        assertThat(without.all).isEqualTo(info("without-versions"));

        PatchGroup mixed = groups.get("mixed-style");
        assertThat(mixed.exact).containsOnlyKeys("0.1.2");
        assertThat(mixed.exact.get("0.1.2")).isEqualTo(info("mixed-style@0.1.2"));
        assertThat(mixed.range).containsExactly(new PatchGroupRangeItem("1.x.x", info("mixed-style@1.x.x")));
        assertThat(mixed.all).isEqualTo(info("mixed-style"));
    }

    @Test
    void errorsOnInvalidVersionRange() {
        assertThatThrownBy(() -> PatchingConfig.groupPatchedDependencies(
          Collections.singletonMap("foo@link:packages/foo", HASH)))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_PATCH_NON_SEMVER_RANGE"));
    }

    @Test
    void patchInfoOfAnExistingNameAtVersion() {
        assertThat(getPatchInfo(Collections.singletonMap("foo@1.0.0", HASH), "foo", "1.0.0"))
          .isEqualTo(info("foo@1.0.0"));
    }

    @Test
    void patchInfoOfAnExistingNameWithoutThatVersion() {
        assertThat(getPatchInfo(Collections.singletonMap("foo", HASH), "foo", "1.0.0")).isEqualTo(info("foo"));
    }

    @Test
    void nameAtVersionIsPreferredOverName() {
        Map<String, String> patched = new LinkedHashMap<>();
        patched.put("foo", HASH);
        patched.put("foo@1.0.0", HASH);
        assertThat(getPatchInfo(patched, "foo", "1.0.0")).isEqualTo(info("foo@1.0.0"));
    }

    @Test
    void doesNotMatchAnotherName() {
        assertThat(getPatchInfo(Collections.singletonMap("bar@1.0.0", HASH), "foo", "1.0.0")).isNull();
    }

    @Test
    void doesNotMatchAnotherVersion() {
        assertThat(getPatchInfo(Collections.singletonMap("foo@2.0.0", HASH), "foo", "1.0.0")).isNull();
    }

    @Test
    void verifyPatchesRejectsAnUnusedPatch() {
        Map<String, String> patched = new LinkedHashMap<>();
        patched.put("foo@1.0.0", HASH);
        patched.put("bar", HASH);
        Map<String, PatchGroup> groups = PatchingConfig.groupPatchedDependencies(patched);
        assertThatThrownBy(() -> PatchingConfig.verifyPatches(groups,
          new LinkedHashSet<>(Collections.singletonList("bar")), false))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_UNUSED_PATCH");
              assertThat(e.getMessage()).isEqualTo("The following patches were not used: foo@1.0.0");
          });
        PatchingConfig.verifyPatches(groups, new LinkedHashSet<>(Collections.singletonList("bar")), true);
    }

    private static PatchInfo getPatchInfo(Map<String, String> patchedDependencies, String name, String version) {
        return PatchingConfig.getPatchInfo(PatchingConfig.groupPatchedDependencies(patchedDependencies), name,
          version);
    }

    private static PatchInfo info(String key) {
        return new PatchInfo(HASH, key);
    }
}
