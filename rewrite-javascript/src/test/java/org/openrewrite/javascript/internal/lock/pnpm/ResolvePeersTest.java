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
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.DependenciesTreeNode;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.NodeId;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.PeerDependency;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.ResolvedPackage;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.PeerDependencyIssues;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.ProjectToResolve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Port of pnpm 11.2.2's installing/deps-resolver/test/resolvePeers.ts, dedupeDepPaths.test.ts and
 * linkPathToPeerVersion.test.ts.
 */
class ResolvePeersTest {

    private final Map<NodeId, DependenciesTreeNode> tree = new LinkedHashMap<>();
    private final List<ProjectToResolve> projects = new ArrayList<>();

    private static ResolvedPackage pkg(String name, String version, String pkgIdWithPatchHash, String... peers) {
        ResolvedPackage pkg = new ResolvedPackage("", name, version);
        pkg.setPkgIdWithPatchHash(pkgIdWithPatchHash);
        Map<String, PeerDependency> peerDependencies = new LinkedHashMap<>();
        for (int i = 0; i < peers.length; i += 2) {
            boolean optional = peers[i].endsWith("?");
            PeerDependency peer = new PeerDependency(peers[i + 1]);
            if (optional) {
                peer.setOptional(true);
            }
            peerDependencies.put(optional ? peers[i].substring(0, peers[i].length() - 1) : peers[i], peer);
        }
        pkg.setPeerDependencies(peerDependencies);
        return pkg;
    }

    private void node(String nodeId, ResolvedPackage pkg, int depth, String... children) {
        DependenciesTreeNode node = new DependenciesTreeNode(pkg, depth);
        Map<String, NodeId> childNodeIds = new LinkedHashMap<>();
        for (int i = 0; i < children.length; i += 2) {
            childNodeIds.put(children[i], NodeId.of(children[i + 1]));
        }
        node.setChildren(childNodeIds);
        node.setInstallable(true);
        tree.put(NodeId.of(nodeId), node);
    }

    private void project(String id, String... directNodeIdsByAlias) {
        Map<String, NodeId> direct = new LinkedHashMap<>();
        for (int i = 0; i < directNodeIdsByAlias.length; i += 2) {
            direct.put(directNodeIdsByAlias[i], NodeId.of(directNodeIdsByAlias[i + 1]));
        }
        projects.add(new ProjectToResolve(id, "", direct, Collections.emptyList()));
    }

    private ResolvePeers.Result resolvePeers(boolean dedupePeers, String... allPeerDepNames) {
        Set<String> names = new LinkedHashSet<>(Arrays.asList(allPeerDepNames));
        return ResolvePeers.resolvePeers(names, projects, tree, false, false, dedupePeers, false, 1000, "",
          new LinkedHashMap<>(), new LinkedHashSet<>());
    }

    @Test
    void resolvePeerDependenciesOfCyclicDependencies() {
        ResolvedPackage foo = pkg("foo", "1.0.0", "foo/1.0.0", "qar", "1.0.0", "zoo", "1.0.0");
        ResolvedPackage bar = pkg("bar", "1.0.0", "bar/1.0.0", "foo", "1.0.0", "zoo", "1.0.0");
        project("", "foo", ">foo/1.0.0>");
        node(">foo/1.0.0>", foo, 0, "bar", ">foo/1.0.0>bar/1.0.0>");
        node(">foo/1.0.0>bar/1.0.0>", bar, 1, "qar", ">foo/1.0.0>bar/1.0.0>qar/1.0.0>");
        node(">foo/1.0.0>bar/1.0.0>qar/1.0.0>", pkg("qar", "1.0.0", "qar/1.0.0", "foo", "1.0.0", "bar", "1.0.0"), 2,
          "zoo", ">foo/1.0.0>bar/1.0.0>qar/1.0.0>zoo/1.0.0>");
        node(">foo/1.0.0>bar/1.0.0>qar/1.0.0>zoo/1.0.0>", pkg("zoo", "1.0.0", "zoo/1.0.0", "qar", "1.0.0"), 3,
          "foo", ">foo/1.0.0>bar/1.0.0>qar/1.0.0>zoo/1.0.0>foo/1.0.0>",
          "bar", ">foo/1.0.0>bar/1.0.0>qar/1.0.0>zoo/1.0.0>bar/1.0.0>");
        node(">foo/1.0.0>bar/1.0.0>qar/1.0.0>zoo/1.0.0>foo/1.0.0>", foo, 4);
        node(">foo/1.0.0>bar/1.0.0>qar/1.0.0>zoo/1.0.0>bar/1.0.0>", bar, 4);

        assertThat(resolvePeers(false, "foo", "bar", "qar", "zoo").getDependenciesGraph().keySet()).containsExactly(
          "foo/1.0.0",
          "bar/1.0.0(foo/1.0.0)",
          "qar/1.0.0(bar/1.0.0(foo/1.0.0))(foo/1.0.0)",
          "zoo/1.0.0(qar/1.0.0(bar/1.0.0(foo/1.0.0))(foo/1.0.0))",
          "foo/1.0.0(qar/1.0.0(bar/1.0.0(foo/1.0.0))(foo/1.0.0))" +
          "(zoo/1.0.0(qar/1.0.0(bar/1.0.0(foo/1.0.0))(foo/1.0.0)))",
          "bar/1.0.0(foo/1.0.0)(zoo/1.0.0(qar/1.0.0(bar/1.0.0(foo/1.0.0))(foo/1.0.0)))");
    }

    @Test
    void aPackageWhosePeersResolveInOnlyOneOccurrenceIsResolvedThere() {
        ResolvedPackage foo = pkg("foo", "1.0.0", "foo/1.0.0", "qar", "1.0.0");
        ResolvedPackage bar = pkg("bar", "1.0.0", "bar/1.0.0");
        ResolvedPackage zoo = pkg("zoo", "1.0.0", "zoo/1.0.0");
        project("", "zoo", ">zoo/1.0.0>", "bar", ">bar/1.0.0>");
        node(">zoo/1.0.0>", zoo, 0, "foo", ">zoo/1.0.0>foo/1.0.0>");
        node(">zoo/1.0.0>foo/1.0.0>", foo, 1);
        node(">bar/1.0.0>", bar, 0, "zoo", ">bar/1.0.0>zoo/1.0.0>", "qar", ">bar/1.0.0>qar/1.0.0>");
        node(">bar/1.0.0>zoo/1.0.0>", zoo, 1, "foo", ">bar/1.0.0>zoo/1.0.0>foo/1.0.0>");
        node(">bar/1.0.0>zoo/1.0.0>foo/1.0.0>", foo, 2);
        node(">bar/1.0.0>qar/1.0.0>", pkg("qar", "1.0.0", "qar/1.0.0"), 1);

        List<String> depPaths = new ArrayList<>(resolvePeers(false, "foo", "bar", "qar", "zoo").getDependenciesGraph()
          .keySet());
        Collections.sort(depPaths);
        assertThat(depPaths).containsExactly("bar/1.0.0", "foo/1.0.0", "foo/1.0.0(qar/1.0.0)", "qar/1.0.0",
          "zoo/1.0.0", "zoo/1.0.0(qar/1.0.0)");
    }

    private Map<String, PeerDependencyIssues> peerDependencyIssues() {
        ResolvedPackage foo = pkg("foo", "1.0.0", "foo/1.0.0", "peer", "1");
        ResolvedPackage fooWithOptionalPeer = pkg("foo", "2.0.0", "foo/2.0.0", "peer?", "1");
        ResolvedPackage bar = pkg("bar", "1.0.0", "bar/1.0.0", "peer", "2");
        ResolvedPackage barWithOptionalPeer = pkg("bar", "2.0.0", "bar/2.0.0", "peer?", "2");
        ResolvedPackage qar = pkg("qar", "1.0.0", "qar/1.0.0", "peer", "^2.2.0");
        project("project1", "foo", ">project1>foo/1.0.0>");
        project("project2", "bar", ">project2>bar/1.0.0>");
        project("project3", "foo", ">project3>foo/1.0.0>", "bar", ">project3>bar/1.0.0>");
        project("project4", "bar", ">project4>bar/1.0.0>", "qar", ">project4>qar/1.0.0>");
        project("project5", "foo", ">project5>foo/1.0.0>", "bar", ">project5>bar/2.0.0>");
        project("project6", "foo", ">project6>foo/2.0.0>", "bar", ">project6>bar/2.0.0>");
        node(">project1>foo/1.0.0>", foo, 0);
        node(">project2>bar/1.0.0>", bar, 0);
        node(">project3>foo/1.0.0>", foo, 0);
        node(">project3>bar/1.0.0>", bar, 0);
        node(">project4>bar/1.0.0>", bar, 0);
        node(">project4>qar/1.0.0>", qar, 0);
        node(">project5>foo/1.0.0>", foo, 0);
        node(">project5>bar/2.0.0>", barWithOptionalPeer, 0);
        node(">project6>foo/2.0.0>", fooWithOptionalPeer, 0);
        node(">project6>bar/2.0.0>", barWithOptionalPeer, 0);
        return resolvePeers(false).getPeerDependencyIssuesByProjects();
    }

    @Test
    void findsPeerDependencyConflicts() {
        assertThat(peerDependencyIssues().get("project3").getConflicts()).containsExactly("peer");
    }

    @Test
    void findsConflictsWhenThePeerIsAnOptionalPeerOfOneDependency() {
        assertThat(peerDependencyIssues().get("project5").getConflicts()).containsExactly("peer");
    }

    @Test
    void ignoresConflictsBetweenMissingOptionalPeers() {
        assertThat(peerDependencyIssues().get("project6").getConflicts()).isEmpty();
    }

    @Test
    void picksTheSingleWantedPeerRange() {
        Map<String, PeerDependencyIssues> issues = peerDependencyIssues();
        assertThat(issues.get("project1").getIntersections()).containsExactly(entry("peer", "1"));
        assertThat(issues.get("project2").getIntersections()).containsExactly(entry("peer", "2"));
    }

    @Test
    void intersectsTwoCompatibleRanges() {
        assertThat(peerDependencyIssues().get("project4").getIntersections())
          .containsExactly(entry("peer", ">=2.2.0 <3.0.0-0"));
    }

    private Map<String, PeerDependencyIssues> unmetPeerDependencyIssues() {
        project("project1", "foo", ">project1>foo/1.0.0>", "peer1", ">project1>peer1/1.0.0-rc.0>", "peer2",
          ">project1>peer2/1.1.0-rc.0>");
        node(">project1>foo/1.0.0>", pkg("foo", "1.0.0", "foo/1.0.0", "peer1", "*", "peer2", ">=1"), 0);
        node(">project1>peer1/1.0.0-rc.0>", pkg("peer1", "1.0.0-rc.0", "peer/1.0.0-rc.0"), 0);
        node(">project1>peer2/1.1.0-rc.0>", pkg("peer2", "1.1.0-rc.0", "peer/1.1.0-rc.0"), 0);
        return resolvePeers(false).getPeerDependencyIssuesByProjects();
    }

    @Test
    void noBadPeerForAPrereleaseUnderStar() {
        PeerDependencyIssues issues = unmetPeerDependencyIssues().get("project1");
        assertThat(issues == null || !issues.getBad().containsKey("peer1")).isTrue();
    }

    @Test
    void noBadPeerForAPrereleaseThatSatisfiesTheRange() {
        PeerDependencyIssues issues = unmetPeerDependencyIssues().get("project1");
        assertThat(issues == null || !issues.getBad().containsKey("peer2")).isTrue();
    }

    @Test
    void reportsWhereABadPeerIsResolvedFrom() {
        project("project", "foo", ">project>foo/1.0.0>");
        node(">project>foo/1.0.0>", pkg("foo", "1.0.0", "foo/1.0.0"), 0, "dep", ">project>foo/1.0.0>dep/1.0.0>",
          "bar", ">project>foo/1.0.0>bar/1.0.0>");
        node(">project>foo/1.0.0>dep/1.0.0>", pkg("dep", "1.0.0", "dep/1.0.0"), 1);
        node(">project>foo/1.0.0>bar/1.0.0>", pkg("bar", "1.0.0", "bar/1.0.0", "dep", "10"), 1);
        assertThat(resolvePeers(false, "dep").getPeerDependencyIssuesByProjects().get("project").getBad().get("dep")
          .get(0).getResolvedFrom()).containsExactly(new ResolvePeers.ParentPackage("foo", "1.0.0"));
    }

    @Test
    void resolvePeerDependenciesWithNpmAliases() {
        ResolvedPackage foo = pkg("foo", "1.0.0", "foo/1.0.0", "bar", "1.0.0");
        ResolvedPackage fooAlias = pkg("foo", "2.0.0", "foo/2.0.0", "bar", "2.0.0");
        ResolvedPackage bar = pkg("bar", "1.0.0", "bar/1.0.0");
        ResolvedPackage barAlias = pkg("bar", "2.0.0", "bar/2.0.0");
        project("", "foo", ">foo/1.0.0>", "bar", ">bar/1.0.0>", "foo-next", ">foo/2.0.0>", "bar-next",
          ">bar/2.0.0>");
        node(">foo/1.0.0>", foo, 0, "bar", ">foo/1.0.0>bar/1.0.0>");
        node(">foo/1.0.0>bar/1.0.0>", bar, 1);
        node(">foo/2.0.0>", fooAlias, 0, "bar", ">foo/2.0.0>bar/2.0.0>");
        node(">foo/2.0.0>bar/2.0.0>", barAlias, 1);
        node(">bar/1.0.0>", bar, 0);
        node(">bar/2.0.0>", barAlias, 0);

        List<String> depPaths = new ArrayList<>(resolvePeers(false, "bar").getDependenciesGraph().keySet());
        Collections.sort(depPaths);
        assertThat(depPaths).containsExactly("bar/1.0.0", "bar/2.0.0", "foo/1.0.0(bar/1.0.0)", "foo/2.0.0(bar/2.0.0)");
    }

    @Test
    void dedupePeersUsesVersionOnlySuffixesWithoutNestedDepPaths() {
        project("", "react", ">react/18.0.0>", "@emotion/react", ">@emotion/react/11.0.0>", "@emotion/styled",
          ">@emotion/styled/11.0.0>");
        node(">react/18.0.0>", pkg("react", "18.0.0", "react/18.0.0"), 0);
        node(">@emotion/react/11.0.0>", pkg("@emotion/react", "11.0.0", "@emotion/react/11.0.0", "react", ">=16"), 0);
        node(">@emotion/styled/11.0.0>", pkg("@emotion/styled", "11.0.0", "@emotion/styled/11.0.0", "react", ">=16",
          "@emotion/react", ">=11"), 0);

        List<String> depPaths = new ArrayList<>(resolvePeers(true, "react", "@emotion/react", "@emotion/styled")
          .getDependenciesGraph().keySet());
        Collections.sort(depPaths);
        assertThat(depPaths).containsExactly("@emotion/react/11.0.0(react@18.0.0)",
          "@emotion/styled/11.0.0(@emotion/react@11.0.0)(react@18.0.0)", "react/18.0.0");
    }

    @Test
    void dedupePeersGivesTransitivePeersVersionOnlySuffixes() {
        project("", "a", ">a/1.0.0>", "c", ">c/1.0.0>");
        node(">a/1.0.0>", pkg("a", "1.0.0", "a/1.0.0"), 0, "b", ">a/1.0.0>b/1.0.0>");
        node(">a/1.0.0>b/1.0.0>", pkg("b", "1.0.0", "b/1.0.0", "c", "1.0.0"), 1);
        node(">c/1.0.0>", pkg("c", "1.0.0", "c/1.0.0"), 0);

        List<String> depPaths = new ArrayList<>(resolvePeers(true, "c").getDependenciesGraph().keySet());
        Collections.sort(depPaths);
        assertThat(depPaths).containsExactly("a/1.0.0(c@1.0.0)", "b/1.0.0(c@1.0.0)", "c/1.0.0");
    }

    @Test
    void dedupePeersKeepsAnInstancePerPeerVersionAcrossProjects() {
        ResolvedPackage plugin = pkg("plugin", "1.0.0", "plugin/1.0.0", "react", ">=16");
        project("project-a", "react", ">project-a>react/17.0.0>", "plugin", ">project-a>plugin/1.0.0>");
        project("project-b", "react", ">project-b>react/18.0.0>", "plugin", ">project-b>plugin/1.0.0>");
        node(">project-a>react/17.0.0>", pkg("react", "17.0.0", "react/17.0.0"), 0);
        node(">project-a>plugin/1.0.0>", plugin, 0);
        node(">project-b>react/18.0.0>", pkg("react", "18.0.0", "react/18.0.0"), 0);
        node(">project-b>plugin/1.0.0>", plugin, 0);

        ResolvePeers.Result result = resolvePeers(true, "react");
        List<String> depPaths = new ArrayList<>(result.getDependenciesGraph().keySet());
        Collections.sort(depPaths);
        assertThat(depPaths).containsExactly("plugin/1.0.0(react@17.0.0)", "plugin/1.0.0(react@18.0.0)",
          "react/17.0.0", "react/18.0.0");
        assertThat(result.getDependenciesByProjectId().get("project-a").get("plugin"))
          .isEqualTo("plugin/1.0.0(react@17.0.0)");
        assertThat(result.getDependenciesByProjectId().get("project-b").get("plugin"))
          .isEqualTo("plugin/1.0.0(react@18.0.0)");
    }

    @Test
    void packagesAreNotDeduplicatedWhenVersionsDoNotMatch() {
        ResolvedPackage foo = pkg("foo", "1.0.0", "foo/1.0.0", "bar", "1.0.0 || 2.0.0", "baz?", "1.0.0 || 2.0.0");
        project("project1", "foo", ">project1>foo/1.0.0>", "bar", ">project1>bar/1.0.0>");
        project("project2", "foo", ">project2>foo/1.0.0>", "bar", ">project2>bar/1.0.0>", "baz",
          ">project2>baz/1.0.0>");
        project("project3", "foo", ">project3>foo/1.0.0>", "bar", ">project3>bar/2.0.0>");
        project("project4", "foo", ">project4>foo/1.0.0>", "bar", ">project4>bar/2.0.0>", "baz",
          ">project4>baz/2.0.0>");
        node(">project1>foo/1.0.0>", foo, 0);
        node(">project1>bar/1.0.0>", pkg("bar", "1.0.0", "bar/1.0.0"), 0);
        node(">project2>foo/1.0.0>", foo, 0);
        node(">project2>bar/1.0.0>", pkg("bar", "1.0.0", "bar/1.0.0"), 0);
        node(">project2>baz/1.0.0>", pkg("baz", "1.0.0", "baz/1.0.0"), 0);
        node(">project3>foo/1.0.0>", foo, 0);
        node(">project3>bar/2.0.0>", pkg("bar", "2.0.0", "bar/2.0.0"), 0);
        node(">project4>foo/1.0.0>", foo, 0);
        node(">project4>bar/2.0.0>", pkg("bar", "2.0.0", "bar/2.0.0"), 0);
        node(">project4>baz/2.0.0>", pkg("baz", "2.0.0", "baz/2.0.0"), 0);

        Map<String, Map<String, String>> byProject = ResolvePeers.resolvePeers(
          new LinkedHashSet<>(Arrays.asList("bar", "baz")), projects, tree, false, true, false, false, 1000, "",
          new LinkedHashMap<>(), new LinkedHashSet<>()).getDependenciesByProjectId();
        assertThat(byProject.get("project1").get("foo")).isEqualTo(byProject.get("project2").get("foo"));
        assertThat(byProject.get("project1").get("foo")).isNotEqualTo(byProject.get("project3").get("foo"));
        assertThat(byProject.get("project3").get("foo")).isEqualTo(byProject.get("project4").get("foo"));
    }

    @Test
    void linkPathToPeerVersion() {
        String[][] rows = {
          {"../packages/b", "packages+b"}, {"./packages/b", "packages+b"}, {"packages/b", "packages+b"},
          {"../../a/b", "..+a+b"}, {"a/b/c", "a+b+c"}, {"abc", "abc"}, {"..", "+"}, {"...", "+"},
          {".hidden/pkg", "hidden+pkg"}, {"..\\packages\\b", "packages+b"}, {"a/b\\c", "a+b+c"},
          {"foo+bar", "foo+bar"}, {"foo++bar", "foo+bar"}, {"+foo", "foo"}, {"foo+", "foo"}, {"foo.", "foo"},
          {"abc...", "abc"}, {"", ""},
        };
        for (String[] row : rows) {
            assertThat(ResolvePeers.linkPathToPeerVersion(row[0])).as(row[0]).isEqualTo(row[1]);
        }
    }
}
