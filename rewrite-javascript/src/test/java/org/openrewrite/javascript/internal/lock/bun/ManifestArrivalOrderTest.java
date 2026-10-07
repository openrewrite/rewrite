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
package org.openrewrite.javascript.internal.lock.bun;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The recorded case {@code dedupe-arrival-order}: debug@2.6.9 wants ms 2.0.0 and humanize-ms@1.2.1 wants ms
 * ^2.0.0, both added in one install. Eight runs of bun 1.3.10 wrote ms@2.0.0 alone four times (humanize-ms
 * deduped to it) and ms@2.0.0 beside humanize-ms/ms@2.1.3 four times. With the same ranges and picks the engine
 * names the case; with equal picks, picks neither range accepts, or a lock that already satisfies both, bun's
 * result does not depend on arrival order.
 */
class ManifestArrivalOrderTest {

    private static final String MS = "{\"name\":\"ms\",\"dist-tags\":{\"latest\":\"2.1.3\"},\"versions\":{" +
                                     version("2.0.0") + "," + version("2.1.1") + "," + version("2.1.3") + "}}";

    private static String version(String v) {
        return "\"" + v + "\":{\"name\":\"ms\",\"version\":\"" + v + "\",\"dist\":{\"tarball\":" +
               "\"https://registry.npmjs.org/ms/-/ms-" + v + ".tgz\"}}";
    }

    @Test
    void onePickSatisfiesTheOtherRange() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "2.0.0", "^2.0.0");
        List<ManifestArrivalOrder.Resolved> resolved = resolved(lockfile, debugMs, debugMs + 1);
        assertThat(ManifestArrivalOrder.detect(lockfile, lockfile.packages.size(), resolved)).containsExactly(
          "manifest arrival order: ms for debug@2.6.9 (2.0.0) and humanize-ms@1.2.1 (^2.0.0) resolved in one install");
    }

    @Test
    void samePick() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "^2.0.0", "~2.1.0");
        List<ManifestArrivalOrder.Resolved> resolved = resolved(lockfile, debugMs, debugMs + 1);
        assertThat(ManifestArrivalOrder.detect(lockfile, lockfile.packages.size(), resolved)).isEmpty();
    }

    @Test
    void neitherPickSatisfiesTheOtherRange() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "2.0.0", "2.1.3");
        List<ManifestArrivalOrder.Resolved> resolved = resolved(lockfile, debugMs, debugMs + 1);
        assertThat(ManifestArrivalOrder.detect(lockfile, lockfile.packages.size(), resolved)).isEmpty();
    }

    @Test
    void existingPackageSettlesBoth() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "2.0.0", "^2.0.0");
        append(lockfile, pkg("ms", "2.0.0"));
        List<ManifestArrivalOrder.Resolved> resolved = resolved(lockfile, debugMs, debugMs + 1);
        assertThat(ManifestArrivalOrder.detect(lockfile, lockfile.packages.size(), resolved)).isEmpty();
    }

    @Test
    void peersAreNotCounted() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "2.0.0", "^2.0.0");
        lockfile.dependencies.get(debugMs + 1).behavior = Behavior.of(Behavior.PEER);
        List<ManifestArrivalOrder.Resolved> resolved = resolved(lockfile, debugMs, debugMs + 1);
        assertThat(ManifestArrivalOrder.detect(lockfile, lockfile.packages.size(), resolved)).isEmpty();
    }

    // the root, debug@2.6.9 wanting ms as debugRange and humanize-ms@1.2.1 wanting it as humanizeRange; the id of
    // debug's ms dependency, humanize-ms's is the next
    private static int requirers(Lockfile lockfile, String debugRange, String humanizeRange) {
        Package root = new Package();
        root.name = "p";
        root.nameHash = Wyhash11.stringHash("p");
        root.resolution = Resolution.ROOT;
        append(lockfile, root, dep("debug", "2.6.9"), dep("humanize-ms", "1.2.1"));
        Package debug = append(lockfile, pkg("debug", "2.6.9"), dep("ms", debugRange));
        append(lockfile, pkg("humanize-ms", "1.2.1"), dep("ms", humanizeRange));
        return debug.dependenciesOff;
    }

    private static List<ManifestArrivalOrder.Resolved> resolved(Lockfile lockfile, int... dependencyIds) {
        PackageManifest manifest = PackageManifest.parse(new Log(), MS, "ms", false);
        assertThat(manifest).isNotNull();
        List<ManifestArrivalOrder.Resolved> out = new ArrayList<>();
        for (int dependencyId : dependencyIds) {
            DependencyVersion version = lockfile.dependencies.get(dependencyId).version;
            PackageManifest.FindResult pick = manifest.findBestVersion(version.npm().version);
            assertThat(pick).isNotNull();
            out.add(new ManifestArrivalOrder.Resolved(dependencyId, version, manifest, pick));
        }
        return out;
    }

    private static Package pkg(String name, String version) {
        Package pkg = new Package();
        pkg.name = name;
        pkg.nameHash = Wyhash11.stringHash(name);
        pkg.resolution = Resolution.npm(SemverVersion.parse(version).version.min(), "");
        return pkg;
    }

    private static Dependency dep(String name, String spec) {
        long nameHash = Wyhash11.stringHash(name);
        DependencyVersion version = DependencyVersion.parse(name, nameHash, spec);
        assertThat(version).isNotNull();
        return new Dependency(nameHash, name, version, Behavior.of(Behavior.PROD));
    }

    private static Package append(Lockfile lockfile, Package pkg, Dependency... dependencies) {
        pkg.dependenciesOff = lockfile.dependencies.size();
        pkg.dependenciesLen = dependencies.length;
        lockfile.dependencies.addAll(Arrays.asList(dependencies));
        for (int i = 0; i < dependencies.length; i++) {
            lockfile.resolutions.add(Package.INVALID_PACKAGE_ID);
        }
        return lockfile.appendPackage(pkg);
    }
}
