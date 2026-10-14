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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port of pnpm 11.2.2's lockfile/pruner/test/index.ts over {@code pruneSharedLockfile}, the pruner a lock-only install
 * runs; {@code pruneLockfile}'s cases where their importers already match their manifests.
 */
class UpdateLockfileTest {

    private final LockfileObject lockfile = new LockfileObject();

    private Map<String, @Nullable String> refs(String... aliasAndRef) {
        Map<String, @Nullable String> refs = new LinkedHashMap<>();
        for (int i = 0; i < aliasAndRef.length; i += 2) {
            refs.put(aliasAndRef[i], aliasAndRef[i + 1]);
        }
        return refs;
    }

    private ProjectSnapshot importer(String id) {
        return lockfile.getImporters().computeIfAbsent(id, i -> new ProjectSnapshot());
    }

    private PackageSnapshot pkg(String depPath) {
        if (lockfile.getPackages() == null) {
            lockfile.setPackages(new LinkedHashMap<>());
        }
        PackageSnapshot snapshot = new PackageSnapshot();
        Map<String, @Nullable Object> resolution = new LinkedHashMap<>();
        resolution.put("integrity", "sha1-ChbBDewTLAqLCzb793Fo5VDvg/g=");
        snapshot.setResolution(resolution);
        lockfile.getPackages().put(depPath, snapshot);
        return snapshot;
    }

    private Map<String, PackageSnapshot> pruned() {
        Map<String, PackageSnapshot> packages = UpdateLockfile.pruneSharedLockfile(lockfile, null).getPackages();
        return packages == null ? new LinkedHashMap<>() : packages;
    }

    @Test
    void removeOneRedundantPackage() {
        importer(".").setDependencies(refs("is-positive", "1.0.0"));
        pkg("is-positive@1.0.0");
        pkg("is-positive@2.0.0");
        assertThat(pruned()).containsOnlyKeys("is-positive@1.0.0");
    }

    @Test
    void keepAll() {
        importer(".").setDependencies(refs("is-negative", "1.0.0", "is-positive", "1.0.0"));
        pkg("is-negative@1.0.0").setDependencies(refs("is-positive", "2.0.0"));
        pkg("is-positive@1.0.0");
        pkg("is-positive@2.0.0");
        assertThat(pruned()).containsOnlyKeys("is-negative@1.0.0", "is-positive@1.0.0", "is-positive@2.0.0");
    }

    @Test
    void optionalDependencyShouldHaveOptionalTrue() {
        ProjectSnapshot root = importer(".");
        root.setDependencies(refs("parent-of-foo", "1.0.0", "pkg-with-good-optional", "1.0.0"));
        root.setOptionalDependencies(refs("is-positive", "1.0.0"));
        pkg("foo-child@1.0.0").setOptional(true);
        PackageSnapshot foo = pkg("foo@1.0.0");
        foo.setDependencies(refs("foo-child", "1.0.0"));
        foo.setOptional(true);
        pkg("is-positive@1.0.0");
        pkg("parent-of-foo@1.0.0").setDependencies(refs("foo", "1.0.0"));
        pkg("pkg-with-good-optional@1.0.0").setOptionalDependencies(refs("foo", "1.0.0", "is-positive", "1.0.0"));

        Map<String, PackageSnapshot> packages = pruned();
        assertThat(packages).containsOnlyKeys("foo-child@1.0.0", "foo@1.0.0", "is-positive@1.0.0",
          "parent-of-foo@1.0.0", "pkg-with-good-optional@1.0.0");
        assertThat(packages.get("foo-child@1.0.0").getOptional()).isNull();
        assertThat(packages.get("foo@1.0.0").getOptional()).isNull();
        assertThat(packages.get("is-positive@1.0.0").getOptional()).isEqualTo(true);
        assertThat(packages.get("parent-of-foo@1.0.0").getOptional()).isNull();
        assertThat(packages.get("pkg-with-good-optional@1.0.0").getOptional()).isNull();
    }

    @Test
    void optionalDependencyUsedNotOnlyAsOptionalIsNotOptional() {
        importer(".").setDependencies(refs("is-positive", "1.0.0", "pkg-with-good-optional", "1.0.0"));
        pkg("is-positive@1.0.0");
        pkg("pkg-with-good-optional@1.0.0").setOptionalDependencies(refs("is-positive", "1.0.0"));

        Map<String, PackageSnapshot> packages = pruned();
        assertThat(packages).containsOnlyKeys("is-positive@1.0.0", "pkg-with-good-optional@1.0.0");
        assertThat(packages.get("is-positive@1.0.0").getOptional()).isNull();
    }

    @Test
    void subdependencyIsBothOptionalAndDev() {
        ProjectSnapshot root = importer(".");
        root.setDependencies(refs("prod-parent", "1.0.0"));
        root.setDevDependencies(refs("parent", "1.0.0"));
        pkg("parent@1.0.0").setOptionalDependencies(refs("subdep", "1.0.0", "subdep2", "1.0.0"));
        pkg("prod-parent@1.0.0").setDependencies(refs("subdep2", "1.0.0"));
        pkg("subdep@1.0.0");
        pkg("subdep2@1.0.0");

        Map<String, PackageSnapshot> packages = pruned();
        assertThat(packages).containsOnlyKeys("parent@1.0.0", "prod-parent@1.0.0", "subdep@1.0.0", "subdep2@1.0.0");
        assertThat(packages.get("subdep@1.0.0").getOptional()).isEqualTo(true);
        assertThat(packages.get("subdep2@1.0.0").getOptional()).isNull();
        assertThat(packages.get("parent@1.0.0").getOptional()).isNull();
    }

    @Test
    void optionalIsRemovedFromADependencyUsedAsOptionalAndProd() {
        ProjectSnapshot root = importer(".");
        root.setDependencies(refs("foo", "inflight@1.0.6"));
        root.setOptionalDependencies(refs("inflight", "1.0.6"));
        PackageSnapshot inflight = pkg("inflight@1.0.6");
        inflight.setOptional(true);
        inflight.setDependencies(refs("once", "1.4.0", "wrappy", "1.0.2"));
        PackageSnapshot once = pkg("once@1.4.0");
        once.setOptional(true);
        once.setDependencies(refs("wrappy", "1.0.2"));
        pkg("wrappy@1.0.2").setOptional(true);

        Map<String, PackageSnapshot> packages = pruned();
        assertThat(packages).containsOnlyKeys("inflight@1.0.6", "once@1.4.0", "wrappy@1.0.2");
        for (PackageSnapshot snapshot : packages.values()) {
            assertThat(snapshot.getOptional()).isNull();
        }
    }

    @Test
    void doesNotRemoveAPackageAnotherImporterUses() {
        importer("packages/package-1").setDependencies(refs("is-positive", "1.0.0"));
        importer("packages/package-2").setDependencies(refs("is-negative", "1.0.0"));
        pkg("is-negative@1.0.0");
        pkg("is-positive@1.0.0");
        pkg("is-positive@2.0.0");
        assertThat(pruned()).containsOnlyKeys("is-negative@1.0.0", "is-positive@1.0.0");
    }

    @Test
    void pruneSharedLockfileRemovesOneRedundantPackage() {
        importer("packages/package-1").setDependencies(refs("is-positive", "1.0.0"));
        pkg("is-positive@1.0.0");
        pkg("is-positive@2.0.0");
        assertThat(pruned()).containsOnlyKeys("is-positive@1.0.0");
    }
}
