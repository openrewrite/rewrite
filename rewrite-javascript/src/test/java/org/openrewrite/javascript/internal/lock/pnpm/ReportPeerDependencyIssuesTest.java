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
import org.openrewrite.javascript.internal.lock.pnpm.ReportPeerDependencyIssues.PeerDependencyRules;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.BadPeerIssue;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.MissingPeerIssue;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.ParentPackage;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.PeerDependencyIssues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Port of pnpm 11.2.2's installing/deps-installer/test/filterPeerDependencyIssues.test.ts, with the decision
 * reportPeerDependencyIssues makes over what it lets through.
 */
class ReportPeerDependencyIssuesTest {

    @Test
    void ignoreMissing() {
        PeerDependencyIssues issues = issues();
        issues.getMissing().put("aaa",
          Collections.singletonList(missing(false, ">=1.0.0 <3.0.0", parent("xxx", "1.0.0"))));
        issues.getMissing().put("@foo/bar",
          Collections.singletonList(missing(false, ">=1.0.0 <3.0.0", parent("xxx", "1.0.0"))));
        issues.getIntersections().put("aaa", "^1.0.0");
        issues.getIntersections().put("@foo/bar", "^1.0.0");

        PeerDependencyIssues filtered = filter(issues, new PeerDependencyRules(Arrays.asList("aaa", "@foo/*"), null,
          null));
        assertThat(filtered.getBad()).isEmpty();
        assertThat(filtered.getMissing()).isEmpty();
        assertThat(filtered.getConflicts()).isEmpty();
        assertThat(filtered.getIntersections()).containsOnlyKeys("aaa", "@foo/bar");
    }

    @Test
    void allowAnyVersion() {
        PeerDependencyIssues issues = issues();
        issues.getBad().put("bbb", Collections.singletonList(bad("2.0.0", "^1.0.0", parent("xxx", "1.0.0"))));
        issues.getBad().put("@foo/bar", Collections.singletonList(bad("2.0.0", "^1.0.0", parent("xxx", "1.0.0"))));

        PeerDependencyIssues filtered = filter(issues, new PeerDependencyRules(null, Arrays.asList("bbb", "@foo/*"),
          null));
        assertThat(filtered.getBad()).isEmpty();
        assertThat(filtered.getMissing()).isEmpty();
        assertThat(filtered.getConflicts()).isEmpty();
        assertThat(filtered.getIntersections()).isEmpty();
    }

    @Test
    void allowedVersions() {
        PeerDependencyIssues issues = issues();
        issues.getBad().put("bbb", Collections.singletonList(bad("2.0.0", "^1.0.0", parent("xxx", "1.0.0"))));
        BadPeerIssue fromAaa = bad("2.0.0", "^1.0.0", parent("aaa", "1.0.0"));
        issues.getBad().put("@foo/bar", Arrays.asList(
          fromAaa,
          bad("2.0.0", "^1.0.0", parent("yyy", "1.0.0"), parent("xxx", "1.0.0")),
          bad("3.0.0", "^1.0.0", parent("ccc", "3.0.0")),
          bad("4.0.0", "^1.0.0", parent("ccc", "2.3.6"))));
        Map<String, String> allowedVersions = new LinkedHashMap<>();
        allowedVersions.put("bbb", "2");
        allowedVersions.put("xxx>@foo/bar", "2");
        allowedVersions.put("ccc@3>@foo/bar", "3");
        allowedVersions.put("ccc@>=2.3.5 <3>@foo/bar", "4");

        PeerDependencyIssues filtered = filter(issues, new PeerDependencyRules(null, null, allowedVersions));
        assertThat(filtered.getBad()).containsOnlyKeys("@foo/bar");
        assertThat(filtered.getBad().get("@foo/bar")).containsExactly(fromAaa);
        assertThat(filtered.getMissing()).isEmpty();
    }

    @Test
    void ignoresMissingOptionalPeers() {
        PeerDependencyIssues issues = issues();
        issues.getMissing().put("aaa",
          Collections.singletonList(missing(true, ">=1.0.0 <3.0.0", parent("xxx", "1.0.0"))));

        PeerDependencyIssues filtered = filter(issues, new PeerDependencyRules(null, Collections.emptyList(), null));
        assertThat(filtered.getBad()).isEmpty();
        assertThat(filtered.getMissing()).isEmpty();
    }

    @Test
    void strictPeerDependenciesThrowsOnlyOverWhatTheRulesLetThrough() {
        PeerDependencyIssues issues = issues();
        issues.getMissing().put("aaa", Collections.singletonList(missing(false, "^1.0.0", parent("xxx", "1.0.0"))));
        issues.getIntersections().put("aaa", "^1.0.0");
        Map<String, PeerDependencyIssues> byProjects = Collections.singletonMap(".", issues);

        ReportPeerDependencyIssues.reportPeerDependencyIssues(byProjects,
          new PeerDependencyRules(Collections.singletonList("aaa"), null, null), true);
        ReportPeerDependencyIssues.reportPeerDependencyIssues(byProjects, null, false);
        assertThatThrownBy(() -> ReportPeerDependencyIssues.reportPeerDependencyIssues(byProjects, null, true))
          .isInstanceOfSatisfying(ReportPeerDependencyIssues.PeerDependencyIssuesError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_PEER_DEP_ISSUES");
              assertThat(e.firstPeerName()).isEqualTo("aaa");
          });
    }

    @Test
    void anInvalidAllowedVersionsSelectorIsRejected() {
        assertThatThrownBy(() -> filter(issues(), new PeerDependencyRules(null, null,
          Collections.singletonMap("%", "1"))))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_ALLOWED_VERSION_SELECTOR"));
    }

    private static PeerDependencyIssues filter(PeerDependencyIssues issues, PeerDependencyRules rules) {
        return ReportPeerDependencyIssues.filterPeerDependencyIssues(Collections.singletonMap(".", issues), rules)
          .get(".");
    }

    private static PeerDependencyIssues issues() {
        return new PeerDependencyIssues(new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>(),
          new LinkedHashMap<>());
    }

    private static MissingPeerIssue missing(boolean optional, String wantedRange, ParentPackage... parents) {
        return new MissingPeerIssue(Arrays.asList(parents), optional, wantedRange);
    }

    private static BadPeerIssue bad(String foundVersion, String wantedRange, ParentPackage... parents) {
        List<ParentPackage> resolvedFrom = new ArrayList<>();
        return new BadPeerIssue(foundVersion, resolvedFrom, Arrays.asList(parents), false, wantedRange);
    }

    private static ParentPackage parent(String name, String version) {
        return new ParentPackage(name, version);
    }
}
