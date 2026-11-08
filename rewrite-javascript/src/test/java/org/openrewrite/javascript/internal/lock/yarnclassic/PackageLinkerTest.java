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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * src/package-linker.js and src/package-hoister.js (Yarn 1.22.22). The conflicting-bin case is the graph of
 * vite@7.3.7 + vitest@4.1.12, reproduced with actual Yarn: Vitest's ordinary dependency resolves Vite 8, while
 * peer selection appends Vite 7. The hoister installs Vite 8 beneath @vitest/mocker, inaccessible to Vitest.
 */
class PackageLinkerTest {
    private static final String UNION = "^6.0.0 || ^7.0.0 || ^8.0.0";
    private static final List<String> ROOTS = Arrays.asList("vite@^7.0.0", "vitest@^4.0.0");

    @Test
    void selectsTheNearestPeerAndKeepsTheOrdinaryDependency() {
        PackageResolver resolver = viteAndVitest(true, true);
        new PackageLinker(resolver).resolvePeerModules();
        assertThat(resolver.getStrictResolvedPattern("vitest@^4.0.0").reference.dependencies)
                .containsExactly("vite@" + UNION, "@vitest/mocker@4.1.12", "vite@^7.0.0");
        assertThat(resolver.getStrictResolvedPattern("@vitest/mocker@4.1.12").reference.dependencies)
                .containsExactly("vite@" + UNION);
    }

    @Test
    void hoistsTheOrdinaryDependencyBeneathMocker() {
        PackageResolver resolver = viteAndVitest(true, true);
        new PackageLinker(resolver).resolvePeerModules();
        PackageHoister hoister = new PackageHoister(resolver, false);
        hoister.seed(ROOTS);
        Map<String, Manifest> tree = hoister.init();
        assertThat(tree.keySet()).containsExactlyInAnyOrder("node_modules/vite", "node_modules/vitest",
                "node_modules/@vitest/mocker", "node_modules/@vitest/mocker/node_modules/vite");
        assertThat(tree.get("node_modules/@vitest/mocker/node_modules/vite").getVersion()).isEqualTo("8.3.6");
    }

    @Test
    void failsWhenTheDependencyBinIsNotAccessibleFromItsDependent() {
        PackageResolver resolver = viteAndVitest(true, true);
        assertThatThrownBy(() -> new PackageLinker(resolver).copyModules(ROOTS, false))
                .isInstanceOfSatisfying(JsError.class, e -> {
                    assertThat(e.getName()).isEqualTo(Js.INVARIANT_VIOLATION);
                    assertThat(e.getMessage()).isEqualTo(
                            "could not find a copy of vite to link in node_modules/vitest/node_modules");
                });
    }

    @Test
    void differentVersionsWithoutBinsDoNotFail() {
        assertThatCode(() -> new PackageLinker(viteAndVitest(false, true)).copyModules(ROOTS, false))
                .doesNotThrowAnyException();
    }

    @Test
    void differentVersionsWithoutTheConflictingPeerAreAccessible() {
        assertThatCode(() -> new PackageLinker(viteAndVitest(true, false)).copyModules(ROOTS, false))
                .doesNotThrowAnyException();
    }

    @Test
    void aSingleVersionCanSupplyBothTheDependencyAndPeer() {
        PackageResolver resolver = viteAndVitest(true, true);
        Manifest vite = resolver.getStrictResolvedPattern("vite@^7.0.0");
        vite.reference.addPattern("vite@" + UNION, vite);
        assertThatCode(() -> new PackageLinker(resolver).copyModules(ROOTS, false)).doesNotThrowAnyException();
    }

    @Test
    void yarn170ReducesTheEmptyCandidateList() {
        PackageResolver resolver = viteAndVitest(true, true, "1.7.0");
        assertThatThrownBy(() -> new PackageLinker(resolver).copyModules(ROOTS, false))
                .isInstanceOfSatisfying(JsError.class, e -> {
                    assertThat(e.getName()).isEqualTo(JsError.TYPE_ERROR);
                    assertThat(e.getMessage()).isEqualTo("Reduce of empty array with no initial value");
                });
    }

    @Test
    void binLocationsAreSearchedFromYarn170() {
        assertThat(YarnClassicRelease.of("1.6.0").findsTheNearestInstalledVersionForBins()).isFalse();
        assertThat(YarnClassicRelease.of("1.7.0").findsTheNearestInstalledVersionForBins()).isTrue();
        assertThat(YarnClassicRelease.of("1.7.0").checksForAnAccessibleBinLocation()).isFalse();
        assertThat(YarnClassicRelease.of("1.8.0").checksForAnAccessibleBinLocation()).isTrue();
    }

    private static PackageResolver viteAndVitest(boolean bins, boolean peer) {
        return viteAndVitest(bins, peer, "1.22.22");
    }

    @Test
    void theOtherPatternOrderKeepsTheOrdinaryDependencyAccessible() {
        // Actual Yarn also succeeds when the union starts ^7: it sorts after the root pattern.
        PackageResolver resolver = viteAndVitest(true, true, "1.22.22", "^7.0.0 || ^8.0.0");
        assertThatCode(() -> new PackageLinker(resolver).copyModules(ROOTS, false)).doesNotThrowAnyException();
    }

    private static PackageResolver viteAndVitest(boolean bins, boolean peer, String release) {
        return viteAndVitest(bins, peer, release, UNION);
    }

    private static PackageResolver viteAndVitest(boolean bins, boolean peer, String release, String union) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(YarnClassicRelease.of(release)).env(Collections.emptyMap()).build();
        Config config = new Config(inputs, YarnClassicLockRegenerator.start(inputs));
        PackageResolver resolver = YarnClassicScenarios.resolver(config, new Lockfile());
        add(resolver, "vite@^7.0.0", "vite", "7.3.7", Collections.emptyList(), bins);
        add(resolver, "vite@" + union, "vite", "8.3.6", Collections.singletonList("vitest"), bins);
        Manifest vitest = add(resolver, "vitest@^4.0.0", "vitest", "4.1.12", Collections.emptyList(), false);
        vitest.reference.addDependencies(Arrays.asList("vite@" + union, "@vitest/mocker@4.1.12"));
        if (peer) {
            vitest.put("peerDependencies", Collections.singletonMap("vite", union));
        }
        Manifest mocker = add(resolver, "@vitest/mocker@4.1.12", "@vitest/mocker", "4.1.12",
                Collections.singletonList("vitest"), false);
        mocker.put("peerDependencies", Collections.singletonMap("vite", union));
        return resolver;
    }

    private static Manifest add(PackageResolver resolver, String pattern, String name, String version,
                                List<String> parentNames, boolean bin) {
        Manifest pkg = new Manifest(new LinkedHashMap<>());
        pkg.put("name", name);
        pkg.put("version", version);
        pkg.put("_uid", version);
        if (bin) {
            pkg.put("bin", Collections.singletonMap(name, "bin/cli.js"));
        }
        PackageRemote remote = new PackageRemote();
        remote.type = "tarball";
        remote.registry = "npm";
        remote.resolved = "https://registry.npmjs.org/" + name + "/-/" + version + ".tgz";
        pkg.remote = remote;
        PackageRequest request = new PackageRequest(DependencyRequestPattern.builder()
                .pattern(pattern).registry("npm").parentNames(parentNames).build(), resolver);
        pkg.reference = new PackageReference(request, pkg, remote);
        pkg.reference.addPattern(pattern, pkg);
        return pkg;
    }
}
