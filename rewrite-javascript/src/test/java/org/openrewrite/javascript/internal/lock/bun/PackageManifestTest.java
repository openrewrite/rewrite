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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.HostEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PackageManifest#findBestVersion} against bun's choice in every scenario whose edited manifest has a single
 * registry dependency: the version {@code fresh} locks, and the package {@link Package#fromNPM} builds from it.
 */
class PackageManifestTest {

    private static final String[] GROUPS = {"dependencies", "devDependencies", "optionalDependencies",
            "peerDependencies"};

    static Stream<Arguments> picks() {
        List<Arguments> out = new ArrayList<>();
        for (BunScenarios.Scenario scenario : BunScenarios.discover()) {
            String pkg = scenario.has("pkg") ? scenario.file("pkg") : scenario.file("pkg-after");
            String fresh = scenario.has("fresh") ? scenario.file("fresh") : addedByEdit(scenario);
            if (fresh == null || pkg == null) {
                continue;
            }
            Json.Expr manifest;
            try {
                manifest = Json.parse(pkg);
            } catch (Json.SyntaxError e) {
                continue;
            }
            if (manifest.get("workspaces") != null || manifest.get("overrides") != null ||
                manifest.get("resolutions") != null) {
                continue;
            }
            Json.Expr deps = manifest.get("dependencies");
            int groups = 0;
            for (String group : GROUPS) {
                Json.Expr g = manifest.get(group);
                groups += g == null ? 0 : g.properties().size();
            }
            if (deps == null || deps.properties().size() != 1 || groups != 1) {
                continue;
            }
            Json.Property dep = deps.properties().get(0);
            DependencyVersion version = DependencyVersion.parse(dep.key.asString(), null, dep.value.asString());
            if (version == null ||
                version.tag != DependencyVersion.Tag.NPM && version.tag != DependencyVersion.Tag.DIST_TAG) {
                continue;
            }
            String realName = version.tag == DependencyVersion.Tag.NPM ? version.npm().name : version.distTag().name;
            String packument = scenario.http(realName);
            if (packument == null) {
                continue;
            }
            out.add(Arguments.of(scenario.getLabel(), scenario, dep.key.asString(), version, realName, packument,
                    fresh));
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("picks")
    void picksWhatBunPicked(String name, BunScenarios.Scenario scenario, String alias, DependencyVersion version,
                            String realName, String packument, String fresh) {
        PackageManifest manifest = PackageManifest.parse(new Log(), packument, realName, false);
        assertThat(manifest).isNotNull();
        PackageManifest.FindResult found = version.tag == DependencyVersion.Tag.NPM ?
          manifest.findBestVersion(version.npm().version) : manifest.findByDistTag(version.distTag().tag);
        Lockfile lockfile = TextLockfileReader.load(fresh,
          BunOptions.load(null, null, null, HostEnvironment.UNKNOWN));
        Package locked = topLevel(lockfile, alias);
        if (found == null) {
            assertThat(locked).as("bun installed nothing for " + alias).isNull();
            return;
        }
        assertThat(locked).as(alias + " in fresh").isNotNull();
        assertThat(found.version.toString()).isEqualTo(locked.resolution.version.toString());

        Lockfile scratch = new Lockfile();
        Package fromNpm = Package.fromNPM(scratch, manifest, found.version, found.packageVersion, Features.NPM);
        assertThat(fromNpm.meta.os).isEqualTo(locked.meta.os);
        assertThat(fromNpm.meta.arch).isEqualTo(locked.meta.arch);
        assertThat(fromNpm.meta.integrity.toString()).isEqualTo(locked.meta.integrity.toString());
        assertThat(fromNpm.bin.eql(locked.bin)).as("bin").isTrue();
        assertThat(describe(scratch, fromNpm)).isEqualTo(describe(lockfile, locked));
    }

    // without a fresh lock, an edit's after whose before had no packages at all resolved its one dependency anew
    private static @Nullable String addedByEdit(BunScenarios.Scenario scenario) {
        String before = scenario.file("before");
        String after = scenario.file("after");
        if (before == null || after == null) {
            return null;
        }
        Json.Expr packages;
        try {
            packages = Json.parse(before).get("packages");
        } catch (Json.SyntaxError e) {
            return null;
        }
        return packages == null || packages.properties().isEmpty() ? after : null;
    }

    private static @Nullable Package topLevel(Lockfile lockfile, String alias) {
        Package root = lockfile.rootPackage();
        for (int depId = root.dependenciesOff; depId < root.dependenciesEnd(); depId++) {
            if (lockfile.dependencies.get(depId).name.equals(alias)) {
                int pkgId = lockfile.resolutions.get(depId);
                return pkgId == Package.INVALID_PACKAGE_ID ? null : lockfile.packages.get(pkgId);
            }
        }
        return null;
    }

    // the dependencies by name, as the lock records them: name, literal and behavior
    private static List<String> describe(Lockfile lockfile, Package pkg) {
        List<String> out = new ArrayList<>();
        for (Dependency dep : lockfile.dependenciesOf(pkg)) {
            out.add(dep.name + "@" + dep.version.literal + " " + dep.behavior.bits);
        }
        out.sort(String::compareTo);
        return out;
    }
}
