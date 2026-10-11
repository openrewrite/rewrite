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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.pnpm.PreferredVersions.Selector;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.InfoFromLockfile;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.MissingPeerInfo;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.WantedDependency;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/**
 * Port of pnpm 11.2.2's installing/deps-resolver tests replaceVersionInPref, unwrapPackageName, hoistPeers,
 * parentIdsContainSequence, getManifestFromResponse and validatePeerDependencies.
 */
class ResolveDependenciesTest {

    private static final List<String> NO_PREFIXES = Collections.emptyList();

    @Test
    void replaceVersionInBareSpecifier() {
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("^1.0.0", "1.1.0", NO_PREFIXES))
          .isEqualTo("1.1.0");
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("npm:foo@^1.0.0", "1.1.0", NO_PREFIXES))
          .isEqualTo("npm:foo@1.1.0");
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("npm:@foo/bar@^1.0.0", "1.1.0", NO_PREFIXES))
          .isEqualTo("npm:@foo/bar@1.1.0");
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("npm:foo", "1.1.0", NO_PREFIXES))
          .isEqualTo("npm:foo@1.1.0");
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("npm:@foo/bar", "1.1.0", NO_PREFIXES))
          .isEqualTo("npm:@foo/bar@1.1.0");
    }

    @Test
    void replaceVersionInBareSpecifierUnderNamedRegistryPrefixes() {
        List<String> prefixes = Arrays.asList("gh:", "work:");
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("gh:^1.0.0", "1.1.0", prefixes))
          .isEqualTo("gh:1.1.0");
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("gh:@acme/foo@^1.0.0", "1.1.0", prefixes))
          .isEqualTo("gh:@acme/foo@1.1.0");
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("gh:@acme/foo", "1.1.0", prefixes))
          .isEqualTo("gh:@acme/foo@1.1.0");
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("work:@corp/lib@^2.0.0", "2.1.0", prefixes))
          .isEqualTo("work:@corp/lib@2.1.0");
    }

    @Test
    void replaceVersionInBareSpecifierLeavesUnrecognizedPrefixes() {
        for (String spec : new String[]{"workspace:^1.0.0", "workspace:./pkg", "file:./pkg", "link:../pkg",
          "catalog:", "github:owner/repo", "https://example.com/tarball.tgz"}) {
            assertThat(ResolveDependencies.replaceVersionInBareSpecifier(spec, "1.1.0", NO_PREFIXES)).isEqualTo(spec);
        }
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("gh:^1.0.0", "1.1.0", NO_PREFIXES))
          .isEqualTo("gh:^1.0.0");
        assertThat(ResolveDependencies.replaceVersionInBareSpecifier("work:^1.0.0", "1.1.0",
          Collections.singletonList("gh:"))).isEqualTo("work:^1.0.0");
    }

    @Test
    void unwrapPackageName() {
        assertThat(ResolveDependencies.unwrapPackageName("is-positive", "^3.1.0"))
          .containsExactly("is-positive", "^3.1.0");
        assertThat(ResolveDependencies.unwrapPackageName("my-alias", "npm:is-positive@^3.1.0"))
          .containsExactly("is-positive", "^3.1.0");
        assertThat(ResolveDependencies.unwrapPackageName("my-alias", "npm:@pnpm.e2e/foo@^100.0.0"))
          .containsExactly("@pnpm.e2e/foo", "^100.0.0");
        assertThat(ResolveDependencies.unwrapPackageName("my-alias", "npm:is-positive"))
          .containsExactly("is-positive", "*");
        assertThat(ResolveDependencies.unwrapPackageName("my-alias", "npm:@pnpm.e2e/foo"))
          .containsExactly("@pnpm.e2e/foo", "*");
    }

    @Test
    void parentIdsContainSequence() {
        assertThat(ResolveDependencies.parentIdsContainSequence(Arrays.asList(".", "b", "a", "c", "b", "a"), "a", "b"))
          .isTrue();
    }

    private static PackageRequester.PackageResponse response(@Nullable Map<String, @Nullable Object> manifest) {
        return new PackageRequester.PackageResponse(new NpmResolver.ResolveResult(), false, null, manifest, false);
    }

    private static InfoFromLockfile currentPkg(@Nullable String name, @Nullable String version) {
        InfoFromLockfile info = new InfoFromLockfile("");
        info.setName(name);
        info.setVersion(version);
        return info;
    }

    @Test
    void manifestFromTheResponse() {
        Map<String, @Nullable Object> manifest = new LinkedHashMap<>();
        manifest.put("name", "foo");
        manifest.put("version", "1.0.0");
        assertThat(ResolveDependencies.getManifestFromResponse(response(manifest),
          new WantedDependency("foo", "foo", false, false), null))
          .containsExactly(entry("name", "foo"), entry("version", "1.0.0"));
    }

    @Test
    void manifestFromTheCurrentPackage() {
        assertThat(ResolveDependencies.getManifestFromResponse(response(null),
          new WantedDependency("node", "runtime:^22.0.0", false, false), currentPkg("node", "22.20.0")))
          .containsExactly(entry("name", "node"), entry("version", "22.20.0"));
    }

    @Test
    void manifestDefaultsTo000() {
        assertThat(ResolveDependencies.getManifestFromResponse(response(null),
          new WantedDependency("foo", "foo@^1.0.0", false, false), null))
          .containsExactly(entry("name", "foo"), entry("version", "0.0.0"));
    }

    @Test
    void manifestNameFromTheBareSpecifierWithoutAlias() {
        assertThat(ResolveDependencies.getManifestFromResponse(response(null),
          new WantedDependency(null, "@scope/package@^1.0.0", false, false), null))
          .containsExactly(entry("name", "package@^1.0.0"), entry("version", "0.0.0"));
    }

    @Test
    void currentPackageWithOnlyAName() {
        assertThat(ResolveDependencies.getManifestFromResponse(response(null),
          new WantedDependency("foo", "foo", false, false), currentPkg("foo", null)))
          .containsExactly(entry("name", "foo"), entry("version", "0.0.0"));
    }

    @Test
    void currentPackageWithOnlyAVersion() {
        assertThat(ResolveDependencies.getManifestFromResponse(response(null),
          new WantedDependency("foo", "foo", false, false), currentPkg(null, "1.0.0")))
          .containsExactly(entry("name", "foo"), entry("version", "0.0.0"));
    }

    private static Map<String, @Nullable Object> manifest(@Nullable String name, String... peers) {
        Map<String, @Nullable Object> peerDependencies = new LinkedHashMap<>();
        for (int i = 0; i < peers.length; i += 2) {
            peerDependencies.put(peers[i], peers[i + 1]);
        }
        Map<String, @Nullable Object> manifest = new LinkedHashMap<>();
        if (name != null) {
            manifest.put("name", name);
        }
        manifest.put("peerDependencies", peerDependencies);
        return manifest;
    }

    @Test
    void acceptsPeerSpecificationsThatMakeSense() {
        ResolveDependencyTree.validatePeerDependencies("/repo/packages/pkg", manifest(null,
          "semver-range", ">=1.2.3 || ^3.2.1", "workspace-scheme", "workspace:^", "catalog-scheme", "catalog:",
          "combine-all", ">=1.2.3 || ^3.2.1 || workspace:^ || catalog:"));
    }

    @Test
    void forbidsAliasesFileAndLinkSchemes() {
        for (String spec : new String[]{"bar@1.2.3", "file:../foo", "link:../foo"}) {
            assertThatThrownBy(() -> ResolveDependencyTree.validatePeerDependencies("/repo/packages/pkg",
              manifest(null, "foo", spec)))
              .hasMessage("The peerDependencies field named 'foo' of package '/repo/packages/pkg' has an invalid " +
                          "value: '" + spec + "'");
            assertThatThrownBy(() -> ResolveDependencyTree.validatePeerDependencies("/repo/packages/pkg",
              manifest("my-pkg", "foo", spec)))
              .hasMessage("The peerDependencies field named 'foo' of package 'my-pkg' has an invalid value: '" + spec +
                          "'");
        }
    }

    @Test
    void picksAnAlreadyAvailablePrereleaseVersion() {
        assertThat(hoistPeers(false, preferred("foo", "1.0.0-beta.0", Selector.of("version")), "foo", "*"))
          .containsExactly(entry("foo", "1.0.0-beta.0"));
    }

    @Test
    void respectsPeerDepRangeWhenPreferredVersionsExist() {
        Map<String, Map<String, Selector>> preferred = preferred("chai", "5.2.1", Selector.of("version"));
        preferred.get("chai").put("4.3.0", Selector.of("version"));
        assertThat(hoistPeers(true, preferred, "chai", "4.3.0")).containsExactly(entry("chai", "4.3.0"));
    }

    @Test
    void fallsBackToRangeWhenNoPreferredVersionSatisfiesIt() {
        assertThat(hoistPeers(true, preferred("chai", "5.2.1", Selector.of("version")), "chai", "4.3.0"))
          .containsExactly(entry("chai", "4.3.0"));
    }

    @Test
    void picksHighestPreferredVersionForDeduplicationWhenRangeIsNotExact() {
        Map<String, Map<String, Selector>> preferred = preferred("foo", "2.0.0", Selector.of("version"));
        preferred.get("foo").put("2.1.0", Selector.of("version"));
        preferred.get("foo").put("3.0.0", Selector.of("version"));
        assertThat(hoistPeers(true, preferred, "foo", "^2.0.0")).containsExactly(entry("foo", "3.0.0"));
    }

    @Test
    void reusesHigherPreferredVersionWhenRangeIsNotExact() {
        assertThat(hoistPeers(true, preferred("foo", "2.0.0", Selector.of("version")), "foo", "1"))
          .containsExactly(entry("foo", "2.0.0"));
    }

    @Test
    void returnsValidSpecifierWhenGivenOnlyRangePreferredVersionSelectors() {
        assertThat(hoistPeers(true, preferred("foo", "^2.0.0", Selector.of("range")), "foo", "2"))
          .containsExactly(entry("foo", "^2.0.0"));
    }

    @Test
    void handlesWorkspaceProtocolRangeWithoutThrowing() {
        assertThat(hoistPeers(true, preferred("foo", "1.0.0", Selector.of("version")), "foo", "workspace:*"))
          .containsExactly(entry("foo", "1.0.0"));
    }

    @Test
    void handlesVersionSelectorWithWeight() {
        assertThat(hoistPeers(true, preferred("foo", "1.0.0", Selector.of("version", 1)), "foo", "1"))
          .containsExactly(entry("foo", "1.0.0"));
    }

    @Test
    void getHoistableOptionalPeersOnlyPicksAVersionThatSatisfiesAllOptionalRanges() {
        Map<String, Map<String, Selector>> preferred = preferred("foo", "1.0.0", Selector.of("version"));
        for (String version : Arrays.asList("2.0.0", "2.1.0", "3.0.0")) {
            preferred.get("foo").put(version, Selector.of("version"));
        }
        assertThat(ResolveDependencies.getHoistableOptionalPeers(
          Collections.singletonMap("foo", Arrays.asList("2", "2.1")), preferred))
          .containsExactly(entry("foo", "2.1.0"));
    }

    @Test
    void getHoistableOptionalPeersPicksTheHighestVersionThatSatisfiesAllTheOptionalRanges() {
        Map<String, Map<String, Selector>> preferred = preferred("foo", "2.1.0", Selector.of("version"));
        preferred.get("foo").put("2.1.1", Selector.of("version"));
        assertThat(ResolveDependencies.getHoistableOptionalPeers(
          Collections.singletonMap("foo", Arrays.asList("2", "2.1")), preferred))
          .containsExactly(entry("foo", "2.1.1"));
    }

    private static Map<String, Map<String, Selector>> preferred(String name, String spec, Selector selector) {
        Map<String, Selector> selectors = new LinkedHashMap<>();
        selectors.put(spec, selector);
        Map<String, Map<String, Selector>> preferred = new LinkedHashMap<>();
        preferred.put(name, selectors);
        return preferred;
    }

    private static Map<String, String> hoistPeers(boolean autoInstallPeers,
                                                  Map<String, Map<String, Selector>> preferred,
                                                  String peerName, String range) {
        List<Map.Entry<String, MissingPeerInfo>> missing = Collections.singletonList(
          new SimpleEntry<>(peerName, new MissingPeerInfo(range, false)));
        return ResolveDependencies.hoistPeers(autoInstallPeers, preferred, Collections.emptyList(), missing);
    }
}
