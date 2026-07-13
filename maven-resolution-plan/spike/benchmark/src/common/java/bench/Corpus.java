package bench;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Reactor pom discovery shared by both engines. */
public final class Corpus {

    private Corpus() {
    }

    /** Every reactor module pom.xml (root + modules), excluding test/resource poms under a src/ directory. */
    public static List<Path> reactorPoms(Path root) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> "pom.xml".equals(p.getFileName().toString()))
                    .filter(p -> {
                        String rel = root.relativize(p).toString();
                        return !rel.contains("src" + File.separator) && !rel.startsWith(".git");
                    })
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Select a contiguous, dependency-coherent subtree of at most {@code maxModules} module poms for graduated
     * benchmarking of very large reactors (e.g. apache/camel, ~548 modules).
     *
     * <p>Selection rule (a pure, reproducible function of the on-disk tree):
     * <ol>
     *   <li><b>Scaffolding is always included.</b> Every {@code <packaging>pom</packaging>} module — parents, imported
     *       BOMs, and aggregators (the root aggregator, camel-parent, camel-dependencies, components/, etc.) — is kept
     *       regardless of the cap. This guarantees every selected leaf's on-disk parent/aggregator chain resolves
     *       locally from the reactor pool and that the shared parent/BOM set is present to exercise the ModelCache
     *       clone-on-hit path (the scaling question).</li>
     *   <li><b>Leaves fill the cap.</b> The remaining artifact-producing modules (jar / maven-plugin / archetype) are
     *       added in path-sorted order until the total reaches {@code maxModules}.</li>
     * </ol>
     * Because the scaffolding set is fixed and leaves are added in a stable sorted order, a larger cap is a strict
     * superset of a smaller one (50 &sub; 150 &sub; 400 &sub; full). If {@code maxModules <= 0} or exceeds the reactor
     * size, the full reactor is returned. If the scaffolding alone exceeds the cap, all scaffolding is still returned
     * (a reactor without its parents is not coherent), so the effective size is {@code max(cap, scaffoldingCount)}.
     */
    public static List<Path> selectSubtree(Path root, int maxModules) throws IOException {
        List<Path> all = reactorPoms(root);
        if (maxModules <= 0 || maxModules >= all.size()) {
            return all;
        }
        Set<Path> scaffolding = scaffolding(root, all);
        Set<Path> selected = new LinkedHashSet<>(scaffolding); // sorted order preserved from `all`
        for (Path p : all) {
            if (selected.size() >= maxModules) {
                break;
            }
            selected.add(p);
        }
        return all.stream().filter(selected::contains).collect(Collectors.toList());
    }

    private static final Pattern POM_PACKAGING = Pattern.compile("<packaging>\\s*pom\\s*</packaging>");

    /** Scaffolding = every pom-packaging module, unioned with any pom that is an on-disk ancestor of another pom. */
    private static Set<Path> scaffolding(Path root, List<Path> all) {
        Set<Path> scaffolding = new LinkedHashSet<>();
        for (Path p : all) {
            if (isPomPackaging(p) || isAncestorOfAnother(p, all)) {
                scaffolding.add(p);
            }
        }
        return scaffolding;
    }

    private static boolean isPomPackaging(Path pom) {
        try {
            byte[] head = new byte[4096];
            try (var in = Files.newInputStream(pom)) {
                int n = in.read(head);
                if (n <= 0) {
                    return false;
                }
                return POM_PACKAGING.matcher(new String(head, 0, n, StandardCharsets.UTF_8)).find();
            }
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isAncestorOfAnother(Path pom, List<Path> all) {
        Path dir = pom.getParent();
        for (Path other : all) {
            if (other != pom && other.getParent() != null && other.getParent().startsWith(dir)
                    && !other.getParent().equals(dir)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A representative non-aggregator leaf module for the re-resolution hot loop. Prefers a module whose directory is
     * named {@code preferredDirName}; otherwise the deepest-nested selected pom (which is a real leaf module rather than
     * top-level scaffolding).
     */
    public static Path leaf(List<Path> poms, String preferredDirName) {
        for (Path p : poms) {
            Path parent = p.getParent();
            if (parent != null && preferredDirName.equals(parent.getFileName().toString())) {
                return p;
            }
        }
        Path deepest = poms.get(poms.size() - 1);
        for (Path p : poms) {
            if (p.getNameCount() > deepest.getNameCount()) {
                deepest = p;
            }
        }
        return deepest;
    }
}
