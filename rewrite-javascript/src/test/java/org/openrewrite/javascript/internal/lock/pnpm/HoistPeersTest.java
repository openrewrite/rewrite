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
import org.openrewrite.javascript.internal.lock.pnpm.PreferredVersions.Selector;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.MissingPeerInfo;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Port of pnpm 11.2.2's installing/deps-resolver/test/hoistPeers.test.ts.
 */
class HoistPeersTest {

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
