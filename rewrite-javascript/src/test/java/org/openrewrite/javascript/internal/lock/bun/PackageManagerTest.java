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
import org.openrewrite.javascript.internal.lock.HostEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The arrival-order check: the recorded {@code dedupe-arrival-order} and {@code small-dedupe-arrival-existing}, where
 * bun 1.3.10 wrote two locks over repeated runs, and the shapes {@code getPackageID} settles whatever the order.
 */
class PackageManagerTest {

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
        PackageManager manager = firstPass(lockfile, debugMs, debugMs + 1);
        assertThat(manager.manifestArrivalOrder(lockfile.packages.size())).containsExactly(
          "manifest arrival order: ms for debug@2.6.9 (2.0.0) and humanize-ms@1.2.1 (^2.0.0) resolved in one install");
    }

    @Test
    void samePick() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "^2.0.0", "~2.1.0");
        PackageManager manager = firstPass(lockfile, debugMs, debugMs + 1);
        assertThat(manager.manifestArrivalOrder(lockfile.packages.size())).isEmpty();
    }

    @Test
    void neitherPickSatisfiesTheOtherRange() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "2.0.0", "2.1.3");
        PackageManager manager = firstPass(lockfile, debugMs, debugMs + 1);
        assertThat(manager.manifestArrivalOrder(lockfile.packages.size())).isEmpty();
    }

    @Test
    void existingPackageSettlesBoth() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "2.0.0", "^2.0.0");
        append(lockfile, pkg("ms", "2.0.0"));
        PackageManager manager = firstPass(lockfile, debugMs, debugMs + 1);
        assertThat(manager.manifestArrivalOrder(lockfile.packages.size())).isEmpty();
    }

    @Test
    void samePickAboveAnExistingPackageOneRangeAccepts() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "^2.1.3", "^2.0.0");
        append(lockfile, pkg("ms", "2.0.0"));
        PackageManager manager = firstPass(lockfile, debugMs, debugMs + 1);
        assertThat(manager.manifestArrivalOrder(lockfile.packages.size())).containsExactly(
          "manifest arrival order: ms for debug@2.6.9 (^2.1.3) and humanize-ms@1.2.1 (^2.0.0) resolved in one install");
    }

    @Test
    void existingPackageAboveTheOtherPick() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "2.0.0", "^2.0.0");
        append(lockfile, pkg("ms", "2.1.3"));
        PackageManager manager = firstPass(lockfile, debugMs, debugMs + 1);
        assertThat(manager.manifestArrivalOrder(lockfile.packages.size())).isEmpty();
    }

    @Test
    void packagesAppendedByTheInstallDoNotSettle() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "^2.1.3", "^2.0.0");
        int packagesBefore = lockfile.packages.size();
        append(lockfile, pkg("ms", "2.0.0"));
        PackageManager manager = firstPass(lockfile, debugMs, debugMs + 1);
        assertThat(manager.manifestArrivalOrder(packagesBefore)).isEmpty();
    }

    @Test
    void peersAreNotCounted() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "2.0.0", "^2.0.0");
        lockfile.dependencies.get(debugMs + 1).behavior = Behavior.of(Behavior.PEER);
        PackageManager manager = firstPass(lockfile, debugMs, debugMs + 1);
        assertThat(manager.manifestArrivalOrder(lockfile.packages.size())).isEmpty();
    }

    @Test
    void peersOfANewNameWithDifferentPicks() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "2.0.0", "^2.0.0");
        lockfile.dependencies.get(debugMs).behavior = Behavior.of(Behavior.PEER);
        lockfile.dependencies.get(debugMs + 1).behavior = Behavior.of(Behavior.PEER);
        PackageManager manager = manager(lockfile);
        assertThat(manager.peerArrivalOrder(resolved(lockfile, debugMs, debugMs + 1))).containsExactly(
          "manifest arrival order: peer ms for debug@2.6.9 (2.0.0) and humanize-ms@1.2.1 (^2.0.0) " +
          "installed in one install");
    }

    // open-stub-peer-arrival-local-first-fast and -slow: a folder dependency's peer, queued before any manifest arrives
    @Test
    void peersOfANewNameWhoseFirstWasQueuedBeforeAnyManifestArrived() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "2.0.0", "^2.0.0");
        lockfile.dependencies.get(debugMs).behavior = Behavior.of(Behavior.PEER);
        lockfile.dependencies.get(debugMs + 1).behavior = Behavior.of(Behavior.PEER);
        PackageManager manager = manager(lockfile);
        manager.peersQueuedFirst.add(debugMs);
        assertThat(manager.peerArrivalOrder(resolved(lockfile, debugMs, debugMs + 1))).isEmpty();
    }

    @Test
    void peersOfANewNameWithOnePick() {
        Lockfile lockfile = new Lockfile();
        int debugMs = requirers(lockfile, "^2.1.0", "^2.0.0");
        PackageManager manager = manager(lockfile);
        assertThat(manager.peerArrivalOrder(resolved(lockfile, debugMs, debugMs + 1))).isEmpty();
    }

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

    private static PackageManager manager(Lockfile lockfile) {
        return new PackageManager(lockfile, BunOptions.load(null, null, null,
          HostEnvironment.UNKNOWN), new Log(),
          Collections.emptyMap(),
          (registry, name, extended) -> {
              throw new AssertionError("no packument is needed: " + name);
          }, 0);
    }

    private static PackageManager firstPass(Lockfile lockfile, int... dependencyIds) {
        PackageManager manager = manager(lockfile);
        manager.firstPass.addAll(resolved(lockfile, dependencyIds));
        return manager;
    }

    private static List<PackageManager.Resolved> resolved(Lockfile lockfile, int... dependencyIds) {
        PackageManifest manifest = PackageManifest.parse(new Log(), MS, "ms", false);
        assertThat(manifest).isNotNull();
        List<PackageManager.Resolved> out = new ArrayList<>();
        for (int dependencyId : dependencyIds) {
            DependencyVersion version = lockfile.dependencies.get(dependencyId).version;
            PackageManifest.FindResult pick = manifest.findBestVersion(version.npm().version);
            assertThat(pick).isNotNull();
            out.add(new PackageManager.Resolved(dependencyId, version, manifest, pick));
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
