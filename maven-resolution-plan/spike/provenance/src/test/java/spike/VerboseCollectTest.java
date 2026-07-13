package spike;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spike.support.Repo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3 / P4 -- VERBOSE COLLECT. Resolver dependency collection with verbose conflict tracking and verbose dependency
 * management exposes everything rewrite's DependencyGraphMapper needs: winner + reason, retained loser with a pointer
 * to the winner, premanaged version/scope/exclusions, and per-node depth + declaring parent.
 */
class VerboseCollectTest {

    @Test
    void verboseConflictExposesWinnerLoserAndDepth(@TempDir File repoDir, @TempDir File localRepo) throws Exception {
        Repo repo = new Repo(repoDir);
        repo.pom("test", "root", "1", pom("test", "root", "1", "jar",
                deps(dep("test", "a", "1", null), dep("test", "b", "1", null)), ""));
        repo.pom("test", "a", "1", pom("test", "a", "1", "jar", deps(dep("test", "c", "1.0", null)), ""));
        repo.pom("test", "b", "1", pom("test", "b", "1", "jar", deps(dep("test", "c", "2.0", null)), ""));
        repo.pom("test", "c", "1.0", pom("test", "c", "1.0", "jar", "", ""));
        repo.pom("test", "c", "2.0", pom("test", "c", "2.0", "jar", "", ""));

        CollectResult result = collect(repo, localRepo, "test:root:1");
        List<NodeInfo> all = walk(result.getRoot());
        List<NodeInfo> cNodes = withArtifactId(all, "c");
        assertEquals(2, cNodes.size(), "verbose STANDARD retains the losing 'c' node");

        NodeInfo winner = cNodes.stream().filter(n -> winnerOf(n.node) == null).findFirst().orElse(null);
        NodeInfo loser = cNodes.stream().filter(n -> winnerOf(n.node) != null).findFirst().orElse(null);
        assertNotNull(winner, "one c node should be the winner (no conflict.winner marker)");
        assertNotNull(loser, "the other c node should be the loser (has conflict.winner marker)");

        // (a) winner version + WHY: nearest wins; here both are depth 2 so the first-declared (a -> c:1.0) wins.
        assertEquals("1.0", winner.node.getArtifact().getVersion());
        assertEquals(2, winner.depth, "winner is at graph depth 2");
        assertEquals("a", winner.parentId, "winner c:1.0 was declared by a");

        // (b) loser retained with its ORIGINAL requested version and a pointer to the winner.
        assertEquals("2.0", loser.node.getArtifact().getVersion());
        assertEquals("b", loser.parentId, "loser c:2.0 was declared by b");
        DependencyNode winnerRef = winnerOf(loser.node);
        assertNotNull(winnerRef);
        assertEquals("1.0", winnerRef.getArtifact().getVersion(), "NODE_DATA_WINNER points at the winning node");

        // (d) depth + parent chain (who declared each node).
        assertEquals(1, withArtifactId(all, "a").get(0).depth);
        assertEquals(1, withArtifactId(all, "b").get(0).depth);
        assertEquals("root", withArtifactId(all, "a").get(0).parentId);
    }

    @Test
    void verboseManagerExposesPremanagedVersionScopeAndExclusions(@TempDir File repoDir, @TempDir File localRepo) throws Exception {
        Repo repo = new Repo(repoDir);
        // Root manages the transitive c -> 1.5, scope runtime, and injects a managed exclusion of test:extra.
        String managedC =
                "        <dependency>" +
                "          <groupId>test</groupId>" +
                "          <artifactId>c</artifactId>" +
                "          <version>1.5</version>" +
                "          <scope>runtime</scope>" +
                "          <exclusions>" +
                "            <exclusion><groupId>test</groupId><artifactId>extra</artifactId></exclusion>" +
                "          </exclusions>" +
                "        </dependency>";
        repo.pom("test", "rootm", "1", pom("test", "rootm", "1", "jar",
                deps(dep("test", "a", "1", null)),
                "  <dependencyManagement><dependencies>" + managedC + "</dependencies></dependencyManagement>"));
        repo.pom("test", "a", "1", pom("test", "a", "1", "jar", deps(dep("test", "c", "1.0", null)), ""));
        repo.pom("test", "c", "1.5", pom("test", "c", "1.5", "jar", deps(dep("test", "extra", "1", null)), ""));
        repo.pom("test", "extra", "1", pom("test", "extra", "1", "jar", "", ""));

        CollectResult result = collect(repo, localRepo, "test:rootm:1");
        List<NodeInfo> all = walk(result.getRoot());
        DependencyNode c = withArtifactId(all, "c").get(0).node;

        // (c) premanaged version: node shows the managed version, util exposes the original.
        assertEquals("1.5", c.getArtifact().getVersion(), "managed version applied");
        assertEquals("1.0", DependencyManagerUtils.getPremanagedVersion(c), "original (premanaged) version exposed");
        assertTrue((c.getManagedBits() & DependencyNode.MANAGED_VERSION) != 0, "MANAGED_VERSION bit set");

        // (c) premanaged scope.
        assertEquals("runtime", c.getDependency().getScope(), "managed scope applied");
        assertEquals("compile", DependencyManagerUtils.getPremanagedScope(c), "original (premanaged) scope exposed");
        assertTrue((c.getManagedBits() & DependencyNode.MANAGED_SCOPE) != 0, "MANAGED_SCOPE bit set");

        // (P4) managed exclusion injected by root depMgmt is visible on the node AND removed 'extra' from the graph.
        assertTrue((c.getManagedBits() & DependencyNode.MANAGED_EXCLUSIONS) != 0, "MANAGED_EXCLUSIONS bit set");
        boolean excludesExtra = false;
        for (Exclusion e : c.getDependency().getExclusions()) {
            if ("extra".equals(e.getArtifactId())) {
                excludesExtra = true;
            }
        }
        assertTrue(excludesExtra, "the injected managed exclusion is visible on the node");
        java.util.Collection<Exclusion> premanagedExclusions = DependencyManagerUtils.getPremanagedExclusions(c);
        boolean premanagedHadExtra = premanagedExclusions != null && premanagedExclusions.stream()
                .anyMatch(e -> "extra".equals(e.getArtifactId()));
        assertFalse(premanagedHadExtra, "premanaged (original) exclusions did NOT contain the managed exclusion");
        assertTrue(withArtifactId(all, "extra").isEmpty(), "managed exclusion pruned 'extra' from the collected graph");
    }

    // ---- collect + walk helpers ----

    static CollectResult collect(Repo repo, File localRepo, String rootCoords) throws Exception {
        RepositorySystem system = new RepositorySystemSupplier().get();
        RepositorySystemSession.SessionBuilder sb = new SessionBuilderSupplier(system).get();
        sb.withLocalRepositoryBaseDirectories(localRepo.toPath());
        sb.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.STANDARD);
        sb.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, Boolean.TRUE);
        try (RepositorySystemSession.CloseableSession session = sb.build()) {
            RemoteRepository remote = new RemoteRepository.Builder("test", "default", repo.uri()).build();
            CollectRequest cr = new CollectRequest();
            cr.setRoot(new Dependency(new DefaultArtifact(rootCoords), "compile"));
            cr.addRepository(remote);
            return system.collectDependencies(session, cr);
        }
    }

    static final class NodeInfo {
        final DependencyNode node;
        final int depth;
        final String parentId;

        NodeInfo(DependencyNode node, int depth, String parentId) {
            this.node = node;
            this.depth = depth;
            this.parentId = parentId;
        }
    }

    /** Depth-first walk recording each node's depth (root = 0) and the artifactId of its declaring parent. */
    static List<NodeInfo> walk(DependencyNode root) {
        List<NodeInfo> out = new ArrayList<>();
        walk(root, 0, null, out);
        return out;
    }

    private static void walk(DependencyNode node, int depth, String parentId, List<NodeInfo> out) {
        out.add(new NodeInfo(node, depth, parentId));
        String id = node.getArtifact() == null ? null : node.getArtifact().getArtifactId();
        for (DependencyNode child : node.getChildren()) {
            walk(child, depth + 1, id, out);
        }
    }

    static List<NodeInfo> withArtifactId(List<NodeInfo> nodes, String artifactId) {
        List<NodeInfo> out = new ArrayList<>();
        for (NodeInfo ni : nodes) {
            if (ni.node.getArtifact() != null && artifactId.equals(ni.node.getArtifact().getArtifactId())) {
                out.add(ni);
            }
        }
        return out;
    }

    static DependencyNode winnerOf(DependencyNode node) {
        Object w = node.getData().get(ConflictResolver.NODE_DATA_WINNER);
        return w instanceof DependencyNode ? (DependencyNode) w : null;
    }

    // ---- pom text helpers ----

    static String pom(String g, String a, String v, String packaging, String depsXml, String extra) {
        return "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "  <modelVersion>4.0.0</modelVersion>" +
                "  <groupId>" + g + "</groupId>" +
                "  <artifactId>" + a + "</artifactId>" +
                "  <version>" + v + "</version>" +
                "  <packaging>" + packaging + "</packaging>" +
                depsXml + extra +
                "</project>";
    }

    static String deps(String... depXml) {
        StringBuilder sb = new StringBuilder("  <dependencies>");
        for (String d : depXml) {
            sb.append(d);
        }
        return sb.append("  </dependencies>").toString();
    }

    static String dep(String g, String a, String v, String scope) {
        return "    <dependency>" +
                "      <groupId>" + g + "</groupId>" +
                "      <artifactId>" + a + "</artifactId>" +
                "      <version>" + v + "</version>" +
                (scope == null ? "" : "      <scope>" + scope + "</scope>") +
                "    </dependency>";
    }
}
