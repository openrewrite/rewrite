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
package org.openrewrite.javascript.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.marker.NodeResolutionResult;

import static org.assertj.core.api.Assertions.assertThat;

class PnpmLockAdapterTest {

    @Test
    void convertsTopLevelDependency() {
        String pnpm = "lockfileVersion: '6.0'\n" +
                "\n" +
                "dependencies:\n" +
                "  lodash:\n" +
                "    specifier: ^4.17.21\n" +
                "    version: 4.17.21\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  /lodash@4.17.21:\n" +
                "    resolution: {integrity: sha512-x}\n" +
                "    dev: false\n";
        String npm = PnpmLockAdapter.toNpmV3(pnpm);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).hasSize(1);
        assertThat(result.getAll().get(0).getName()).isEqualTo("lodash");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("4.17.21");
        assertThat(result.getTopLevel()).containsKey("lodash");
    }

    @Test
    void convertsScopedTopLevelPackage() {
        String pnpm = "lockfileVersion: '6.0'\n" +
                "\n" +
                "dependencies:\n" +
                "  '@types/node':\n" +
                "    specifier: ^20.0.0\n" +
                "    version: 20.0.0\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  /@types/node@20.0.0:\n" +
                "    resolution: {integrity: sha512-x}\n" +
                "    dev: true\n";
        String npm = PnpmLockAdapter.toNpmV3(pnpm);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll().get(0).getName()).isEqualTo("@types/node");
        assertThat(result.getTopLevel()).containsKey("@types/node");
    }

    @Test
    void distinguishesTopLevelFromTransitive() {
        // accepts is transitive (not in root deps); express is top-level.
        String pnpm = "lockfileVersion: '6.0'\n" +
                "\n" +
                "dependencies:\n" +
                "  express:\n" +
                "    specifier: ^4.18.0\n" +
                "    version: 4.18.0\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  /express@4.18.0:\n" +
                "    resolution: {integrity: sha512-x}\n" +
                "    dependencies:\n" +
                "      accepts: 1.3.8\n" +
                "\n" +
                "  /accepts@1.3.8:\n" +
                "    resolution: {integrity: sha512-y}\n";
        String npm = PnpmLockAdapter.toNpmV3(pnpm);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll())
                .extracting(d -> d.getName() + "@" + d.getVersion())
                .containsExactlyInAnyOrder("express@4.18.0", "accepts@1.3.8");
        // Only express is top-level; accepts is transitive.
        assertThat(result.getTopLevel()).containsOnlyKeys("express");
    }

    @Test
    void stripsPeerDependencySuffixFromKey() {
        String pnpm = "lockfileVersion: '6.0'\n" +
                "\n" +
                "dependencies:\n" +
                "  react-redux:\n" +
                "    specifier: ^8.0.0\n" +
                "    version: 8.0.5(react@18.2.0)\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  /react-redux@8.0.5(react@18.2.0):\n" +
                "    resolution: {integrity: sha512-x}\n";
        String npm = PnpmLockAdapter.toNpmV3(pnpm);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll().get(0).getName()).isEqualTo("react-redux");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("8.0.5");
        assertThat(result.getTopLevel()).containsKey("react-redux");
    }

    @Test
    void includesDevDependenciesAndPeerDependenciesAsTopLevel() {
        String pnpm = "lockfileVersion: '6.0'\n" +
                "\n" +
                "dependencies:\n" +
                "  prod-dep:\n" +
                "    specifier: ^1.0.0\n" +
                "    version: 1.0.0\n" +
                "\n" +
                "devDependencies:\n" +
                "  dev-dep:\n" +
                "    specifier: ^2.0.0\n" +
                "    version: 2.0.0\n" +
                "\n" +
                "peerDependencies:\n" +
                "  peer-dep:\n" +
                "    specifier: ^3.0.0\n" +
                "    version: 3.0.0\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  /prod-dep@1.0.0:\n" +
                "    resolution: {integrity: sha512-x}\n" +
                "  /dev-dep@2.0.0:\n" +
                "    resolution: {integrity: sha512-y}\n" +
                "  /peer-dep@3.0.0:\n" +
                "    resolution: {integrity: sha512-z}\n";
        String npm = PnpmLockAdapter.toNpmV3(pnpm);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getTopLevel().keySet())
                .containsExactlyInAnyOrder("prod-dep", "dev-dep", "peer-dep");
    }

    @Test
    void convertsLockfileVersion5TopLevelAndTransitive() {
        // pnpm 7.x and earlier: name and version are separated by '/', not '@'.
        String pnpm = "lockfileVersion: 5.4\n" +
                "\n" +
                "specifiers:\n" +
                "  is-even: 1.0.0\n" +
                "\n" +
                "dependencies:\n" +
                "  is-even: 1.0.0\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  /is-even/1.0.0:\n" +
                "    resolution: {integrity: sha512-x}\n" +
                "    engines: {node: '>=0.10.0'}\n" +
                "    dependencies:\n" +
                "      is-odd: 0.1.2\n" +
                "    dev: false\n" +
                "\n" +
                "  /is-odd/0.1.2:\n" +
                "    resolution: {integrity: sha512-y}\n" +
                "    dev: false\n";
        String npm = PnpmLockAdapter.toNpmV3(pnpm);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll())
                .extracting(d -> d.getName() + "@" + d.getVersion())
                .containsExactlyInAnyOrder("is-even@1.0.0", "is-odd@0.1.2");
        // Only is-even is top-level; is-odd is transitive.
        assertThat(result.getTopLevel()).containsOnlyKeys("is-even");
        assertThat(result.getTopLevel().get("is-even").getDependencies())
                .extracting(NodeResolutionResult.Dependency::getName)
                .containsExactly("is-odd");
    }

    @Test
    void convertsLockfileVersion5ScopedAndPeerSuffix() {
        String pnpm = "lockfileVersion: 5.4\n" +
                "\n" +
                "dependencies:\n" +
                "  '@scope/foo': 1.0.0\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  /@scope/foo/1.0.0_react@18.2.0:\n" +
                "    resolution: {integrity: sha512-x}\n" +
                "    dev: false\n";
        String npm = PnpmLockAdapter.toNpmV3(pnpm);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll().get(0).getName()).isEqualTo("@scope/foo");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("1.0.0");
        assertThat(result.getTopLevel()).containsKey("@scope/foo");
    }

    @Test
    void parseLegacyPackageKeyHandlesShapes() {
        var simple = PnpmLockAdapter.parseLegacyPackageKey("/lodash/4.17.21");
        assertThat(simple).isNotNull();
        assertThat(simple.name).isEqualTo("lodash");
        assertThat(simple.version).isEqualTo("4.17.21");

        var scoped = PnpmLockAdapter.parseLegacyPackageKey("/@types/node/20.0.0");
        assertThat(scoped).isNotNull();
        assertThat(scoped.name).isEqualTo("@types/node");
        assertThat(scoped.version).isEqualTo("20.0.0");

        var peer = PnpmLockAdapter.parseLegacyPackageKey("/react-redux/8.0.5_react@18.2.0");
        assertThat(peer).isNotNull();
        assertThat(peer.name).isEqualTo("react-redux");
        assertThat(peer.version).isEqualTo("8.0.5");

        assertThat(PnpmLockAdapter.parseLegacyPackageKey("/no-version-suffix")).isNull();
        assertThat(PnpmLockAdapter.parseLegacyPackageKey("not-a-valid-key")).isNull();
        assertThat(PnpmLockAdapter.parseLegacyPackageKey("")).isNull();
        assertThat(PnpmLockAdapter.parseLegacyPackageKey(null)).isNull();
    }

    @Test
    void convertsLockfileVersion9TopLevelAndTransitive() {
        // pnpm 9.x/10.x: top-level deps live under `importers`, package keys have no
        // leading slash, and the dependency graph lives in `snapshots`.
        String pnpm = "lockfileVersion: '9.0'\n" +
                "\n" +
                "importers:\n" +
                "\n" +
                "  .:\n" +
                "    dependencies:\n" +
                "      is-even:\n" +
                "        specifier: 1.0.0\n" +
                "        version: 1.0.0\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  is-even@1.0.0:\n" +
                "    resolution: {integrity: sha512-x}\n" +
                "    engines: {node: '>=0.10.0'}\n" +
                "\n" +
                "  is-odd@0.1.2:\n" +
                "    resolution: {integrity: sha512-y}\n" +
                "\n" +
                "snapshots:\n" +
                "\n" +
                "  is-even@1.0.0:\n" +
                "    dependencies:\n" +
                "      is-odd: 0.1.2\n" +
                "\n" +
                "  is-odd@0.1.2: {}\n";
        String npm = PnpmLockAdapter.toNpmV3(pnpm);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll())
                .extracting(d -> d.getName() + "@" + d.getVersion())
                .containsExactlyInAnyOrder("is-even@1.0.0", "is-odd@0.1.2");
        // Only is-even is top-level; is-odd is transitive.
        assertThat(result.getTopLevel()).containsOnlyKeys("is-even");
        assertThat(result.getTopLevel().get("is-even").getDependencies())
                .extracting(NodeResolutionResult.Dependency::getName)
                .containsExactly("is-odd");
    }

    @Test
    void convertsLockfileVersion9ScopedAndPeerSuffix() {
        String pnpm = "lockfileVersion: '9.0'\n" +
                "\n" +
                "importers:\n" +
                "\n" +
                "  .:\n" +
                "    dependencies:\n" +
                "      '@scope/foo':\n" +
                "        specifier: ^1.0.0\n" +
                "        version: 1.0.0(react@18.2.0)\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  '@scope/foo@1.0.0':\n" +
                "    resolution: {integrity: sha512-x}\n";
        String npm = PnpmLockAdapter.toNpmV3(pnpm);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll().get(0).getName()).isEqualTo("@scope/foo");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("1.0.0");
        assertThat(result.getTopLevel()).containsKey("@scope/foo");
    }

    @Test
    void parsePackageKeyHandlesNoLeadingSlash() {
        var nv = PnpmLockAdapter.parsePackageKey("is-even@1.0.0");
        assertThat(nv).isNotNull();
        assertThat(nv.name).isEqualTo("is-even");
        assertThat(nv.version).isEqualTo("1.0.0");

        var scoped = PnpmLockAdapter.parsePackageKey("@types/node@20.0.0");
        assertThat(scoped).isNotNull();
        assertThat(scoped.name).isEqualTo("@types/node");
        assertThat(scoped.version).isEqualTo("20.0.0");
    }

    @Test
    void emptyPackagesMapProducesNoEntries() {
        String pnpm = "lockfileVersion: '6.0'\n";
        String npm = PnpmLockAdapter.toNpmV3(pnpm);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).isEmpty();
        assertThat(result.getTopLevel()).isEmpty();
    }

    @Test
    void parsePackageKeyHandlesUnscopedSimple() {
        var nv = PnpmLockAdapter.parsePackageKey("/lodash@4.17.21");
        assertThat(nv).isNotNull();
        assertThat(nv.name).isEqualTo("lodash");
        assertThat(nv.version).isEqualTo("4.17.21");
    }

    @Test
    void parsePackageKeyHandlesScopedSimple() {
        var nv = PnpmLockAdapter.parsePackageKey("/@types/node@20.0.0");
        assertThat(nv).isNotNull();
        assertThat(nv.name).isEqualTo("@types/node");
        assertThat(nv.version).isEqualTo("20.0.0");
    }

    @Test
    void parsePackageKeyHandlesPeerDepSuffix() {
        var nv = PnpmLockAdapter.parsePackageKey("/react-redux@8.0.5(react@18.2.0)");
        assertThat(nv).isNotNull();
        assertThat(nv.name).isEqualTo("react-redux");
        assertThat(nv.version).isEqualTo("8.0.5");
    }

    @Test
    void parsePackageKeyHandlesScopedWithPeerDepSuffix() {
        var nv = PnpmLockAdapter.parsePackageKey("/@scope/foo@1.0.0(react@18.0.0)");
        assertThat(nv).isNotNull();
        assertThat(nv.name).isEqualTo("@scope/foo");
        assertThat(nv.version).isEqualTo("1.0.0");
    }

    @Test
    void parsePackageKeyReturnsNullForMalformed() {
        assertThat(PnpmLockAdapter.parsePackageKey("not-a-valid-key")).isNull();
        assertThat(PnpmLockAdapter.parsePackageKey("/no-version-suffix")).isNull();
        assertThat(PnpmLockAdapter.parsePackageKey("")).isNull();
        assertThat(PnpmLockAdapter.parsePackageKey(null)).isNull();
    }
}
