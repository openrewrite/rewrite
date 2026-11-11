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
package org.openrewrite.python.internal.uvlock;

import static java.util.Objects.requireNonNull;
import java.util.Objects;
import org.openrewrite.python.internal.uvlock.marker.MarkerTree;
import org.openrewrite.python.internal.uvlock.marker.MarkerParser;
import org.openrewrite.python.internal.uvlock.marker.Ranges;
import org.openrewrite.python.internal.pep440.PythonVersion;
import org.openrewrite.python.internal.uvlock.marker.MarkerInterner;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;

import java.util.*;

/**
 * The marker under which each lock entry is reachable from the root, and what uv derives from it: the
 * edge simplification of 0.11.25 (a clause the parent's reachability implies leaves the edge marker,
 * an edge it contradicts leaves the lock) and the wheels an entry's reachability rules out.
 */
final class UvLockReachability {

    private final List<UvLockPackage> packages;
    private final PyRange requiresPython;
    /** The lock's supported environments, under one of which the root is reachable, or null for every environment. */
    private final @Nullable List<String> environments;
    private final Map<String, List<Integer>> byName = new HashMap<>();
    private final MarkerInterner interner = new MarkerInterner();
    private final Map<Node, MarkerTree> reach = new HashMap<>();

    private UvLockReachability(List<UvLockPackage> packages, PyRange requiresPython, @Nullable List<String> environments) {
        this.packages = packages;
        this.requiresPython = requiresPython;
        this.environments = environments;
        for (int i = 0; i < packages.size(); i++) {
            byName.computeIfAbsent(packages.get(i).getName(), k -> new ArrayList<>()).add(i);
        }
    }

    static List<UvLockPackage> simplify(List<UvLockPackage> packages, int rootIndex, PyRange requiresPython,
                                        @Nullable List<String> environments) {
        UvLockReachability r = new UvLockReachability(packages, requiresPython, environments);
        r.propagate(rootIndex);
        List<UvLockPackage> simplified = new ArrayList<>(packages.size());
        for (int i = 0; i < packages.size(); i++) {
            simplified.add(r.simplifyEntry(i, i == rootIndex));
        }
        return simplified;
    }

    /** The reachability of every entry, or null when the lock's markers are outside the pass's algebra. */
    static @Nullable UvLockReachability of(List<UvLockPackage> packages, int rootIndex, PyRange requiresPython,
                                           @Nullable List<String> environments) {
        UvLockReachability r = new UvLockReachability(packages, requiresPython, environments);
        try {
            r.propagate(rootIndex);
        } catch (EngineFailure e) {
            return null;
        }
        return r;
    }

    /** The entry's reachability, in a form two passes can compare. */
    String reachKey(int index) {
        MarkerTree marker = reach.get(new Node(index, null));
        return marker == null ? "" : String.valueOf(marker.tryToString());
    }

    /** Whether every path to the entry carries a clause on the platform, so some platform rules it out. */
    boolean platformScoped(int index) {
        MarkerTree marker = reach.get(new Node(index, null));
        if (marker == null || marker.isFalse()) {
            return false;
        }
        for (Os os : Os.values()) {
            for (Arch arch : Arch.values()) {
                if (OS_CLAUSES.containsKey(os) && MACHINES.containsKey(arch) &&
                        marker.isDisjoint(constant(os, arch))) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---- wheels the reachability rules out (uv-lock is_wheel_unreachable_for_marker)

    private enum Os { LINUX, WINDOWS, MACOS, ANDROID, IOS, OTHER }

    private enum Arch { ARM, X86_64, X86, PPC64LE, PPC64, S390X, RISCV64, LOONGARCH64, ARMV7L, ARMV6L, OTHER }

    /** One platform tag of a wheel filename as uv classifies it, or null for a tag uv does not recognize. */
    @Value
    private static class Platform {
        Os os;
        Arch arch;

        private static final java.util.regex.Pattern MANYLINUX =
                java.util.regex.Pattern.compile("(?:manylinux|musllinux)_\\d+_\\d+_(.+)");
        private static final java.util.regex.Pattern MACOSX = java.util.regex.Pattern.compile("macosx_\\d+_\\d+_(.+)");
        private static final java.util.regex.Pattern ANDROID = java.util.regex.Pattern.compile("android_\\d+_(.+)");
        private static final java.util.regex.Pattern IOS = java.util.regex.Pattern.compile("ios_\\d+_\\d+_(.+)");

        static @Nullable Platform of(String tag) {
            for (String prefix : new String[]{"manylinux1_", "manylinux2010_", "manylinux2014_", "linux_"}) {
                if (tag.startsWith(prefix)) {
                    return new Platform(Os.LINUX, linuxArch(tag.substring(prefix.length())));
                }
            }
            java.util.regex.Matcher m = MANYLINUX.matcher(tag);
            if (m.matches()) {
                return new Platform(Os.LINUX, linuxArch(m.group(1)));
            }
            switch (tag) {
                case "win32": return new Platform(Os.WINDOWS, Arch.X86);
                case "win_amd64": return new Platform(Os.WINDOWS, Arch.X86_64);
                case "win_arm64": return new Platform(Os.WINDOWS, Arch.ARM);
                case "win_ia64": return new Platform(Os.WINDOWS, Arch.OTHER);
                default: break;
            }
            m = MACOSX.matcher(tag);
            if (m.matches()) {
                String format = m.group(1);
                return new Platform(Os.MACOS, "arm64".equals(format) ? Arch.ARM : "x86_64".equals(format) ? Arch.X86_64 :
                        "i386".equals(format) ? Arch.X86 : Arch.OTHER);
            }
            m = ANDROID.matcher(tag);
            if (m.matches()) {
                String abi = m.group(1);
                return new Platform(Os.ANDROID, "arm64_v8a".equals(abi) ? Arch.ARM : "x86_64".equals(abi) ? Arch.X86_64 :
                        "x86".equals(abi) ? Arch.X86 : Arch.OTHER);
            }
            m = IOS.matcher(tag);
            if (m.matches()) {
                String multiarch = m.group(1);
                return new Platform(Os.IOS, multiarch.startsWith("arm64_") ? Arch.ARM :
                        "x86_64_iphonesimulator".equals(multiarch) ? Arch.X86_64 : Arch.OTHER);
            }
            return null;
        }

        private static Arch linuxArch(String arch) {
            switch (arch) {
                case "aarch64": return Arch.ARM;
                case "x86_64": return Arch.X86_64;
                case "i686": return Arch.X86;
                case "ppc64le": return Arch.PPC64LE;
                case "ppc64": return Arch.PPC64;
                case "s390x": return Arch.S390X;
                case "riscv64": return Arch.RISCV64;
                case "loongarch64": return Arch.LOONGARCH64;
                case "armv7l": return Arch.ARMV7L;
                case "armv6l": return Arch.ARMV6L;
                default: return Arch.OTHER;
            }
        }
    }

    private static final Map<Os, String[]> OS_CLAUSES = new HashMap<>();
    private static final Map<Arch, String[]> MACHINES = new HashMap<>();

    static {
        OS_CLAUSES.put(Os.LINUX, new String[]{"os_name == 'posix'", "sys_platform == 'linux'"});
        OS_CLAUSES.put(Os.WINDOWS, new String[]{"os_name == 'nt'", "sys_platform == 'win32'"});
        OS_CLAUSES.put(Os.MACOS, new String[]{"os_name == 'posix'", "sys_platform == 'darwin'"});
        OS_CLAUSES.put(Os.ANDROID, new String[]{"sys_platform == 'android'"});
        MACHINES.put(Arch.ARM, new String[]{"aarch64", "arm64", "ARM64"});
        MACHINES.put(Arch.X86_64, new String[]{"x86_64", "amd64", "AMD64"});
        MACHINES.put(Arch.X86, new String[]{"i686", "i386", "win32", "x86"});
        MACHINES.put(Arch.PPC64LE, new String[]{"ppc64le"});
        MACHINES.put(Arch.PPC64, new String[]{"ppc64"});
        MACHINES.put(Arch.S390X, new String[]{"s390x"});
        MACHINES.put(Arch.RISCV64, new String[]{"riscv64"});
        MACHINES.put(Arch.LOONGARCH64, new String[]{"loongarch64"});
        MACHINES.put(Arch.ARMV7L, new String[]{"armv7l", "armv8l"});
        MACHINES.put(Arch.ARMV6L, new String[]{"armv6l"});
    }

    // the constant marker for an OS, an architecture, or both, as a disjunction over machine aliases
    private MarkerTree constant(@Nullable Os os, @Nullable Arch arch) {
        MarkerTree union = MarkerTree.trueIn(interner).negate();
        try {
            for (String machine : arch != null ? MACHINES.get(arch) : new String[]{null}) {
                MarkerTree term = MarkerTree.trueIn(interner);
                for (String clause : os != null ? OS_CLAUSES.get(os) : new String[0]) {
                    term = term.and(MarkerTree.parse(interner, clause));
                }
                if (machine != null) {
                    term = term.and(MarkerTree.parse(interner, "platform_machine == '" + machine + "'"));
                }
                union = union.or(term);
            }
        } catch (MarkerParser.MarkerSyntaxException e) {
            throw new IllegalStateException("platform constants are well formed", e);
        }
        return union;
    }

    /** Whether the wheel's platform tags can never be installed where the entry is reachable. */
    boolean wheelUnreachable(int index, String filename) {
        MarkerTree marker = reach.get(new Node(index, null));
        if (marker == null) {
            return false;
        }
        String base = filename.substring(0, filename.length() - ".whl".length());
        List<Platform> platforms = new ArrayList<>();
        boolean allAny = true;
        for (String tag : base.substring(base.lastIndexOf('-') + 1).split("\\.")) {
            if ("any".equals(tag)) {
                continue;
            }
            allAny = false;
            Platform platform = Platform.of(tag);
            if (platform == null) {
                return false;
            }
            platforms.add(platform);
        }
        if (allAny) {
            return false;
        }
        // emulated x86_64 Python on Windows ARM64 installs win_amd64 wheels
        if (platforms.contains(new Platform(Os.WINDOWS, Arch.X86_64)) && !marker.isDisjoint(constant(Os.WINDOWS, Arch.ARM))) {
            return false;
        }
        for (Os os : new Os[]{Os.LINUX, Os.WINDOWS, Os.MACOS, Os.ANDROID}) {
            if (allOs(platforms, os)) {
                Arch arch = sharedArch(platforms, os);
                if (marker.isDisjoint(constant(os, arch))) {
                    return true;
                }
            }
        }
        for (Arch arch : MACHINES.keySet()) {
            if (allArch(platforms, arch) && marker.isDisjoint(constant(null, arch))) {
                return true;
            }
        }
        return false;
    }

    private static boolean allOs(List<Platform> platforms, Os os) {
        for (Platform p : platforms) {
            if (p.getOs() != os) {
                return false;
            }
        }
        return true;
    }

    private static boolean allArch(List<Platform> platforms, Arch arch) {
        for (Platform p : platforms) {
            if (p.getArch() != arch) {
                return false;
            }
        }
        return true;
    }

    // the architecture an OS check narrows to: Linux knows every one, Windows, macOS and Android only arm, x86_64 and x86
    private static @Nullable Arch sharedArch(List<Platform> platforms, Os os) {
        Arch arch = platforms.get(0).getArch();
        if (arch == Arch.OTHER || !allArch(platforms, arch)) {
            return null;
        }
        if (os != Os.LINUX && arch != Arch.ARM && arch != Arch.X86_64 && arch != Arch.X86) {
            return null;
        }
        return arch;
    }

    /** A lock entry, or one of its extras, as a node of the reachability graph. */
    @Value
    private static class Node {
        int index;
        @Nullable String extra;
    }

    private static EngineFailure unsupported(String what) {
        return new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                "Simplifying dependency markers under reachability is not supported for " + what);
    }

    /** The marker as the algebra holds it, or true where an edge or environment states none. */
    private MarkerTree marker(@Nullable String marker) {
        if (marker == null) {
            return MarkerTree.trueIn(interner);
        }
        try {
            return MarkerTree.parse(interner, marker);
        } catch (MarkerParser.MarkerSyntaxException e) {
            throw unsupported("marker \"" + marker + "\"");
        }
    }

    // ---- propagation

    private void propagate(int rootIndex) {
        Deque<Node> queue = new ArrayDeque<>();
        Node root = new Node(rootIndex, null);
        MarkerTree seed = MarkerTree.trueIn(interner).negate();
        for (String environment : environments != null ? environments : Collections.<String>singletonList(null)) {
            seed = seed.or(marker(environment));
        }
        reach.put(root, seed);
        queue.add(root);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            MarkerTree parent = reach.get(node);
            for (List<UvLockDependency> edges : edgeLists(node, rootIndex)) {
                for (UvLockDependency edge : edges) {
                    MarkerTree via = parent.and(marker(edge.getMarker()));
                    int target = target(edge);
                    List<Node> children = new ArrayList<>();
                    children.add(new Node(target, null));
                    if (edge.getExtra() != null) {
                        for (String e : edge.getExtra()) {
                            children.add(new Node(target, e));
                        }
                    }
                    for (Node child : children) {
                        MarkerTree was = reach.get(child);
                        MarkerTree now = was == null ? via : was.or(via);
                        if (!now.equals(was)) {
                            reach.put(child, now);
                            queue.add(child);
                        }
                    }
                }
            }
        }
    }

    private List<List<UvLockDependency>> edgeLists(Node node, int rootIndex) {
        UvLockPackage pkg = packages.get(node.getIndex());
        List<List<UvLockDependency>> lists = new ArrayList<>();
        if (node.getExtra() != null) {
            if (pkg.getOptionalDependencies() != null && pkg.getOptionalDependencies().get(node.getExtra()) != null) {
                lists.add(pkg.getOptionalDependencies().get(node.getExtra()));
            }
            return lists;
        }
        if (pkg.getDependencies() != null) {
            lists.add(pkg.getDependencies());
        }
        if (pkg.getDevDependencies() != null) {
            lists.addAll(pkg.getDevDependencies().values());
        }
        // the root's extras are all locked, so its groups are reachable with it
        if (node.getIndex() == rootIndex && pkg.getOptionalDependencies() != null) {
            lists.addAll(pkg.getOptionalDependencies().values());
        }
        return lists;
    }

    private int target(UvLockDependency edge) {
        int found = -1;
        for (int i : byName.getOrDefault(edge.getName(), Collections.emptyList())) {
            UvLockPackage pkg = packages.get(i);
            if ((edge.getVersion() != null && !edge.getVersion().equals(pkg.getVersion())) ||
                    (edge.getSource() != null && !edge.getSource().equals(pkg.getSource()))) {
                continue;
            }
            if (found >= 0) {
                throw unsupported("an edge to " + edge.getName() + " that several entries match");
            }
            found = i;
        }
        if (found < 0) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, edge.getName(),
                    "Dependency edge to " + edge.getName() + " has no [[package]] entry");
        }
        return found;
    }

    // ---- simplification

    private UvLockPackage simplifyEntry(int index, boolean root) {
        UvLockPackage pkg = packages.get(index);
        MarkerTree own = reach.get(new Node(index, null));
        if (own == null) {
            return pkg;
        }
        UvLockPackage.UvLockPackageBuilder b = pkg.toBuilder();
        if (pkg.getDependencies() != null) {
            b.dependencies(simplifyList(pkg.getDependencies(), own));
        }
        if (pkg.getDevDependencies() != null) {
            Map<String, List<UvLockDependency>> dev = new LinkedHashMap<>();
            for (Map.Entry<String, List<UvLockDependency>> e : pkg.getDevDependencies().entrySet()) {
                dev.put(e.getKey(), simplifyList(e.getValue(), own));
            }
            b.devDependencies(dev);
        }
        if (pkg.getOptionalDependencies() != null) {
            Map<String, List<UvLockDependency>> groups = new LinkedHashMap<>();
            for (Map.Entry<String, List<UvLockDependency>> e : pkg.getOptionalDependencies().entrySet()) {
                MarkerTree parent = root ? own : reach.get(new Node(index, e.getKey()));
                groups.put(e.getKey(), parent == null ? e.getValue() : simplifyList(e.getValue(), parent));
            }
            b.optionalDependencies(groups);
        }
        return b.build();
    }

    private List<UvLockDependency> simplifyList(List<UvLockDependency> edges, MarkerTree parent) {
        List<UvLockDependency> out = new ArrayList<>(edges.size());
        for (UvLockDependency edge : edges) {
            UvLockDependency simplified = edge.getMarker() == null ? edge : restrict(edge, parent);
            if (simplified != null && !out.contains(simplified)) {
                out.add(simplified);
            }
        }
        return out;
    }

    /**
     * The edge with its marker restricted by the parent's reachability, as uv-lock's
     * {@code simplify_dependency_marker} does, or null when the edge cannot fire where the parent is reachable.
     */
    private @Nullable UvLockDependency restrict(UvLockDependency edge, MarkerTree parent) {
        try {
            MarkerTree restricted = narrowed(new MarkerInterner(true), edge, parent);
            if (restricted.isFalse()) {
                return null;
            }
            String marker = restricted.tryToString();
            if (Objects.equals(marker, edge.getMarker())) {
                return edge;
            }
            if (edge.getMarker().equals(narrowed(new MarkerInterner(false), edge, parent).tryToString())) {
                return edge;
            }
            return edge.withMarker(marker);
        } catch (MarkerParser.MarkerSyntaxException e) {
            throw unsupported("the marker \"" + edge.getMarker() + "\"");
        }
    }

    /** The edge's marker where the parent is reached, under the lock's Python range. */
    private MarkerTree narrowed(MarkerInterner interner, UvLockDependency edge, MarkerTree parent)
            throws MarkerParser.MarkerSyntaxException {
        // the pass holds each node's reachability in its own interner, so it crosses into this one as text
        String text = parent.tryToString();
        MarkerTree reachable = parent.isFalse() ? MarkerTree.trueIn(interner).negate() :
                text == null ? MarkerTree.trueIn(interner) : MarkerTree.parse(interner, text);
        Ranges.Bound<PythonVersion> lower = requiresPython.lower == null ? Ranges.Bound.unbounded() :
                requiresPython.lowerExclusive ? Ranges.Bound.excluded(requiresPython.lower) : Ranges.Bound.included(requiresPython.lower);
        Ranges.Bound<PythonVersion> upper = requiresPython.upper == null ? Ranges.Bound.unbounded() :
                requiresPython.upperInclusive ? Ranges.Bound.included(requiresPython.upper) : Ranges.Bound.excluded(requiresPython.upper);
        return MarkerTree.parse(interner, requireNonNull(edge.getMarker()))
                .simplifyPythonVersions(lower, upper)
                .restrict(reachable.simplifyPythonVersions(lower, upper));
    }
}
